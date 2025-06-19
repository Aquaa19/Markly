package com.aquaa.markly.data.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.aquaa.markly.utils.NotificationHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Room Database class for Markly application.
 * Defines the database entities and DAOs.
 */
@Database(entities = {Student.class, Attendance.class, Notification.class}, version = 11, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String TAG = "AppDatabase";

    public abstract StudentDao studentDao();
    public abstract AttendanceDao attendanceDao();
    public abstract NotificationDao notificationDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 5;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    /**
     * Gets the singleton instance of the AppDatabase.
     * @param context The application context.
     * @return The singleton AppDatabase instance.
     */
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "markly_database")
                            .fallbackToDestructiveMigration()
                            .build();
                    NotificationHelper.createNotificationChannels(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Exports the entire database content to a consolidated JSON string.
     * Reads data from each table and formats it into a single JSON object
     * where keys are table names (representing "books") and values are lists of row data.
     *
     * @return A JSON string representing the database content. Returns null on error.
     */
    public String exportDatabaseToJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        // The outer map will use the "book" names as keys
        Map<String, List<Map<String, Object>>> databaseContent = new HashMap<>();

        SupportSQLiteDatabase db = getOpenHelper().getWritableDatabase();

        try {
            // --- Export Students "Book" ---
            List<Map<String, Object>> studentsData = new ArrayList<>();
            Cursor studentsCursor = null;
            try {
                // Select and rename columns to match "Students" book format
                // Note: The original 'Student ID' column might not exist if your current Student entity
                // only has 'studentId' (internal Room ID). If your source XLSX has 'Student ID',
                // ensure it's mapped to a temporary column during import if you need to preserve it.
                // For export, we use `student_id` which is the internal Room ID.
                studentsCursor = db.query(
                        "SELECT student_id AS 'Student ID', " + // This is the Room generated ID
                                "name AS 'Name', " +
                                "gender AS 'Gender', " +
                                "mobile AS 'Mobile', " +
                                "guardian_mobile AS 'Guardian Mobile', " +
                                "current_semester AS 'Current Semester', " +
                                "section AS 'Section' " +
                                "FROM students ORDER BY name ASC"
                );
                if (studentsCursor != null && studentsCursor.moveToFirst()) {
                    String[] columnNames = studentsCursor.getColumnNames();
                    do {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 0; i < columnNames.length; i++) {
                            String columnName = columnNames[i];
                            Object value = null;
                            switch (studentsCursor.getType(i)) {
                                case Cursor.FIELD_TYPE_INTEGER: value = studentsCursor.getLong(i); break;
                                case Cursor.FIELD_TYPE_FLOAT: value = studentsCursor.getDouble(i); break;
                                case Cursor.FIELD_TYPE_STRING: value = studentsCursor.getString(i); break;
                                case Cursor.FIELD_TYPE_BLOB: // BLOBs are not typically exported to JSON in this way
                                case Cursor.FIELD_TYPE_NULL:
                                default: value = null; break;
                            }
                            row.put(columnName, value);
                        }
                        studentsData.add(row);
                    } while (studentsCursor.moveToNext());
                }
            } finally {
                if (studentsCursor != null) {
                    studentsCursor.close();
                }
            }
            databaseContent.put("Students", studentsData); // Key is "Students" book name (matches your export format)

            // --- Export Attendance "Book" ---
            List<Map<String, Object>> attendanceData = new ArrayList<>();
            Cursor attendanceCursor = null;
            try {
                // Select and rename columns to match "Attendance" book format
                // attendance_id and student_id are internal Room IDs
                attendanceCursor = db.query(
                        "SELECT attendance_id AS 'Attendance', " + // This is the internal Room ID
                                "student_id AS 'Student ID', " + // This is the internal Room student ID
                                "date AS 'Date (Timestamp)', " + // Date stored as long timestamp
                                "is_present AS 'Is Present' " + // Boolean stored as INTEGER (1/0)
                                // 'is_sms_sent' is an internal flag and likely not part of the external backup format
                                "FROM attendance ORDER BY date ASC, student_id ASC"
                );
                if (attendanceCursor != null && attendanceCursor.moveToFirst()) {
                    String[] columnNames = attendanceCursor.getColumnNames();
                    do {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 0; i < columnNames.length; i++) {
                            String columnName = columnNames[i];
                            Object value = null;
                            switch (attendanceCursor.getType(i)) {
                                case Cursor.FIELD_TYPE_INTEGER: value = attendanceCursor.getLong(i); break; // For Attendance ID, Student ID, Date (Timestamp), isPresent (0/1)
                                case Cursor.FIELD_TYPE_FLOAT: value = attendanceCursor.getDouble(i); break;
                                case Cursor.FIELD_TYPE_STRING: value = attendanceCursor.getString(i); break;
                                case Cursor.FIELD_TYPE_BLOB:
                                case Cursor.FIELD_TYPE_NULL:
                                default: value = null; break;
                            }
                            row.put(columnName, value);
                        }
                        attendanceData.add(row);
                    } while (attendanceCursor.moveToNext());
                }
            } finally {
                if (attendanceCursor != null) {
                    attendanceCursor.close();
                }
            }
            databaseContent.put("Attendance", attendanceData); // Key is "Attendance" book name (matches your export format)

            // Return the consolidated JSON
            return gson.toJson(databaseContent);
        } catch (Exception e) {
            Log.e(TAG, "Error exporting database to JSON: " + e.getMessage(), e);
            return null;
        } finally {
            // Room manages its database connection lifecycle, no explicit close needed here.
        }
    }


    /**
     * Data class to hold the comprehensive result of an import operation.
     * This class needs to be public static so it can be accessed from other classes.
     */
    public static class ImportResult {
        public int importedStudentCount = 0;
        public int importedAttendanceCount = 0;
        public List<String> skippedStudents = new ArrayList<>();
        public List<String> skippedAttendance = new ArrayList<>();
        public String errorMessage = null;

        public ImportResult() {}
    }


    /**
     * Imports data from a consolidated JSON string into the database.
     * This method will:
     * 1. Clear existing student and attendance data.
     * 2. Reset SQLite AUTOINCREMENT sequence numbers.
     * 3. Insert data from the JSON, preserving original IDs from the backup.
     *
     * @param jsonString The JSON string representing the database content.
     * @return An ImportResult object containing counts and details of the import process.
     */
    public ImportResult importDatabaseFromJson(String jsonString) {
        ImportResult result = new ImportResult();
        Gson gson = new Gson();
        SupportSQLiteDatabase db = getOpenHelper().getWritableDatabase();

        // Define the expected structure of the incoming JSON
        Type type = new TypeToken<Map<String, List<Map<String, Object>>>>(){}.getType();
        Map<String, List<Map<String, Object>>> databaseContent = null;

        try {
            databaseContent = gson.fromJson(jsonString, type);
            if (databaseContent == null) {
                result.errorMessage = "Failed to parse JSON. It might be empty or malformed.";
                Log.e(TAG, result.errorMessage);
                return result;
            }
        } catch (Exception e) {
            result.errorMessage = "Error parsing JSON data: " + e.getMessage();
            Log.e(TAG, result.errorMessage, e);
            return result;
        }

        // IMPORTANT: Define table import order to respect foreign key constraints
        // Parents tables must be imported before child tables.
        // Use the exact keys from your JSON ("Students", "Attendance")
        String[] jsonTableKeysInImportOrder = {
                "Students", // Corresponds to the "Students" key in your JSON
                "Attendance" // Corresponds to the "Attendance" key in your JSON
        };

        // IMPORTANT: Define table delete order (reverse of import order)
        // Children tables must be deleted before parent tables.
        // Use the actual Room tableName values for DELETE statements
        String[] roomTableNamesInReverseOrder = {
                "attendance", // Room table name
                "students" // Room table name
        };


        db.beginTransaction();
        try {
            // Disable foreign key checks for bulk delete/insert
            db.execSQL("PRAGMA foreign_keys = OFF;");
            Log.d(TAG, "Foreign keys OFF.");

            // 1. Clear existing data in reverse order
            for (String tableName : roomTableNamesInReverseOrder) {
                db.execSQL("DELETE FROM " + tableName);
                Log.d(TAG, "Cleared table: " + tableName);
            }

            // 2. Reset AUTOINCREMENT sequence numbers (if applicable)
            // This is crucial to ensure that new inserts after restore start from 1,
            // or if we want to preserve old IDs, it resets the sequence.
            db.execSQL("DELETE FROM sqlite_sequence WHERE name = 'students';");
            db.execSQL("DELETE FROM sqlite_sequence WHERE name = 'attendance';");
            db.execSQL("DELETE FROM sqlite_sequence WHERE name = 'notifications';"); // Also reset notifications sequence
            Log.d(TAG, "Resetting sqlite_sequence.");


            // Create a mapping from old student IDs from the backup to new internal student IDs (if IDs change during import).
            Map<Long, Long> oldStudentIdToNewStudentIdMap = new HashMap<>();


            // 3. Insert data for each table in the specified import order
            for (String jsonKey : jsonTableKeysInImportOrder) { // Iterate using the JSON keys
                List<Map<String, Object>> tableData = databaseContent.get(jsonKey);
                if (tableData == null) {
                    Log.w(TAG, "No data found for JSON key: " + jsonKey + " in JSON.");
                    continue;
                }
                Log.d(TAG, "Importing data for JSON key: " + jsonKey + ", records: " + tableData.size());

                String roomTableName = null; // To store the actual Room table name
                if ("Students".equals(jsonKey)) {
                    roomTableName = "students";
                } else if ("Attendance".equals(jsonKey)) {
                    roomTableName = "attendance";
                } else {
                    Log.w(TAG, "Skipping import for unknown JSON key: " + jsonKey);
                    continue;
                }

                for (Map<String, Object> row : tableData) {
                    try {
                        StringBuilder columns = new StringBuilder();
                        StringBuilder values = new StringBuilder();
                        List<Object> args = new ArrayList<>(); // To hold arguments for bind

                        // Handle specific tables for column mapping and data type conversion
                        if ("students".equals(roomTableName)) {
                            // Column names in INSERT statement must match Room Entity column names (lowercase)
                            columns.append("student_id, name, gender, mobile, guardian_mobile, current_semester, section");
                            values.append("?, ?, ?, ?, ?, ?, ?");
                            args.add(convertToLong(row.get("Student ID"))); // JSON key 'Student ID'
                            args.add(convertToString(row.get("Name")));    // JSON key 'Name'
                            args.add(convertToString(row.get("Gender")));
                            args.add(convertToString(row.get("Mobile")));
                            args.add(convertToString(row.get("Guardian Mobile"))); // JSON key 'Guardian Mobile'

                            // FIX: Add explicit logging before conversion for debugging
                            Object currentSemesterValue = row.get("Current Semester");
                            Log.d(TAG, "Importing student ID: " + row.get("Student ID") + ", Raw 'Current Semester' value: " + currentSemesterValue + " (Type: " + (currentSemesterValue != null ? currentSemesterValue.getClass().getName() : "null") + ")");
                            Integer semester = convertToInteger(currentSemesterValue);

                            if (semester == null) { // If conversion results in null, provide a default
                                semester = 0; // Default to 0 or another sensible default if null is not allowed
                                Log.w(TAG, "Current Semester for student ID " + row.get("Student ID") + " was null/invalid after conversion, defaulting to 0.");
                            }
                            args.add(semester); // JSON key 'Current Semester'

                            args.add(convertToString(row.get("Section")));

                            // Store mapping of old Student ID to itself (as we are preserving IDs)
                            oldStudentIdToNewStudentIdMap.put(convertToLong(row.get("Student ID")), convertToLong(row.get("Student ID")));
                            result.importedStudentCount++;

                        } else if ("attendance".equals(roomTableName)) {
                            // Column names in INSERT statement must match Room Entity column names (lowercase)
                            columns.append("attendance_id, student_id, date, is_present, is_sms_sent"); // Always include is_sms_sent
                            values.append("?, ?, ?, ?, ?");
                            args.add(convertToLong(row.get("Attendance"))); // JSON key 'Attendance'

                            Long oldStudentId = convertToLong(row.get("Student ID")); // JSON key 'Student ID'
                            Long newStudentId = oldStudentIdToNewStudentIdMap.get(oldStudentId);
                            if (newStudentId == null) {
                                Log.w(TAG, "Skipping attendance record for Student ID " + oldStudentId + " as student was not imported.");
                                result.skippedAttendance.add("Attendance for Student ID " + oldStudentId + " (Student not found/imported)");
                                continue; // Skip this attendance record if student not imported
                            }
                            args.add(newStudentId); // Use the (potentially re-mapped) student_id
                            args.add(convertToLong(row.get("Date (Timestamp)"))); // JSON key 'Date (Timestamp)'
                            args.add(convertToBoolean(row.get("Is Present")) ? 1 : 0); // JSON key 'Is Present', convert to 0/1 integer
                            args.add(0); // Initialize is_sms_sent to false (0) on import (new column, or reset from backup)

                            result.importedAttendanceCount++;

                        } else {
                            // This block should ideally not be reached if jsonTableKeysInImportOrder is correctly defined
                            Log.e(TAG, "Attempted to import data for unexpected JSON key: " + jsonKey);
                            continue;
                        }

                        // Use SQL INSERT OR REPLACE INTO to handle existing IDs and ensure conflicts resolve
                        db.execSQL("INSERT OR REPLACE INTO " + roomTableName + " (" + columns.toString() + ") VALUES (" + values.toString() + ");", args.toArray());
                        Log.d(TAG, "Imported row into " + roomTableName);

                    } catch (Exception e) {
                        Log.e(TAG, "Error importing row into " + roomTableName + ": " + e.getMessage() + " for row data: " + gson.toJson(row), e);
                        if ("students".equals(roomTableName)) {
                            result.skippedStudents.add("Student ID: " + row.get("Student ID") + ", Name: " + row.get("Name") + " (Error: " + e.getMessage() + ")");
                        } else if ("attendance".equals(roomTableName)) {
                            result.skippedAttendance.add("Student ID: " + row.get("Student ID") + ", Date: " + row.get("Date (Timestamp)") + " (Error: " + e.getMessage() + ")");
                        }
                    }
                }
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "Database import transaction successful.");

        } catch (Exception e) {
            result.errorMessage = "Database import transaction failed: " + e.getMessage();
            Log.e(TAG, result.errorMessage, e);
        } finally {
            db.endTransaction();
            // Re-enable foreign key checks
            db.execSQL("PRAGMA foreign_keys = ON;");
            Log.d(TAG, "Foreign keys ON.");
        }
        return result;
    }

    // --- Helper methods for type conversion from JSON Map values ---
    private Long convertToLong(Object value) {
        if (value == null) {
            return null; // Handle null values
        }
        if (value instanceof Double) { // JSON numbers are often parsed as Doubles
            return ((Double) value).longValue();
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof String) {
            try { return Long.parseLong((String) value); } catch (NumberFormatException e) { return null; }
        }
        return (Long) value;
    }

    private Integer convertToInteger(Object value) {
        if (value == null) {
            Log.w(TAG, "convertToInteger: Received null value for integer. Returning 0 to satisfy NOT NULL constraint.");
            return 0; // Return 0 as a fallback if null is not allowed
        }
        Log.d(TAG, "convertToInteger: Attempting to convert value: " + value + " (Type: " + value.getClass().getName() + ")");
        if (value instanceof Double) {
            // Use Math.round to handle potential floating point inaccuracies before converting to int
            // Then cast to Integer
            try {
                // Safely convert Double to Integer, handling potential precision issues for values like 4.0
                return (int) Math.round((Double) value);
            } catch (ClassCastException | NumberFormatException e) {
                Log.e(TAG, "convertToInteger: Error converting Double to Integer: " + value + ", returning 0.", e);
                return 0;
            }
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                // Try parsing as integer, then as double then intValue() if string might represent float
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e1) {
                try {
                    // If direct integer parsing fails, try parsing as a double and then convert to int
                    return (int) Math.round(Double.parseDouble((String) value));
                } catch (NumberFormatException e2) {
                    Log.e(TAG, "convertToInteger: Error parsing String to Integer/Double: " + value + ", returning 0.", e2);
                    return 0;
                }
            }
        }
        Log.e(TAG, "convertToInteger: Unhandled type for conversion: " + value.getClass().getName() + " with value: " + value + ". Returning 0.");
        return 0; // Final fallback
    }


    private String convertToString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Double) { // JSON numbers can be 0.0 or 1.0 for boolean
            return ((Double) value).intValue() == 1;
        } else if (value instanceof Integer) {
            return (Integer) value == 1;
        } else if (value instanceof String) {
            if ("TRUE".equalsIgnoreCase((String) value) || "1".equals((String) value)) {
                return true;
            } else if ("FALSE".equalsIgnoreCase((String) value) || "0".equals((String) value)) {
                return false;
            }
        }
        return false; // Default or error case for null or unhandled type
    }
}
