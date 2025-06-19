package com.aquaa.markly.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.room.Transaction;

import com.aquaa.markly.data.database.AppDatabase;
import com.aquaa.markly.data.database.Attendance;
import com.aquaa.markly.data.database.AttendanceDao;
import com.aquaa.markly.data.database.Notification; // Import Notification entity
import com.aquaa.markly.data.database.NotificationDao; // Import NotificationDao
import com.aquaa.markly.data.database.Student;
import com.aquaa.markly.data.database.StudentDao;
import com.aquaa.markly.utils.ExcelUtils;
import com.aquaa.markly.utils.ExcelUtils.StudentImport;
import com.aquaa.markly.utils.ExcelUtils.AttendanceImport;
import com.aquaa.markly.utils.NotificationHelper; // Import NotificationHelper

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StudentRepository {
    private static final String TAG = "StudentRepository";

    private StudentDao studentDao;
    private AttendanceDao attendanceDao;
    private NotificationDao notificationDao; // Declare NotificationDao
    private Application application; // Store the application context

    private LiveData<List<Student>> allStudents;
    private LiveData<List<Attendance>> allAttendanceRecords;
    private LiveData<List<Notification>> allNotifications; // New LiveData for all notifications
    private LiveData<Integer> unreadNotificationCount; // New LiveData for unread count

    private static final int NUMBER_OF_THREADS = 4;
    private final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public StudentRepository(Application application) {
        this.application = application; // Initialize the application context
        AppDatabase db = AppDatabase.getDatabase(application);
        studentDao = db.studentDao();
        attendanceDao = db.attendanceDao();
        notificationDao = db.notificationDao(); // Initialize NotificationDao

        allStudents = studentDao.getAllStudents();
        allAttendanceRecords = attendanceDao.getAllAttendanceForMonth(0, Long.MAX_VALUE);
        allNotifications = notificationDao.getAllNotifications(); // Initialize all notifications
        unreadNotificationCount = notificationDao.getUnreadNotificationCount(); // Initialize unread count
    }

    // --- Student operations ---
    public LiveData<List<Student>> getAllStudents() {
        return allStudents;
    }

    public long insertStudent(Student student) {
        Log.d(TAG, "Attempting to insert student: " + student.getName());
        long id = studentDao.insert(student);
        Log.d(TAG, "Student '" + student.getName() + "' inserted with ID: " + id);
        return id;
    }

    public void updateStudent(Student student) {
        databaseWriteExecutor.execute(() -> studentDao.updateStudent(student));
    }

    public void deleteStudent(Student student) {
        databaseWriteExecutor.execute(() -> studentDao.deleteStudent(student));
    }

    public LiveData<Student> getStudentById(long studentId) {
        return studentDao.getStudentByIdLiveData(studentId);
    }

    public Student getStudentByIdSync(long studentId) {
        return studentDao.getStudentById(studentId);
    }

    public List<Student> getAllStudentsSync() {
        return studentDao.getAllStudentsSync();
    }

    public List<Integer> getAllSemestersSync() {
        return studentDao.getAllSemestersSync();
    }

    public List<Student> getStudentsBySemesterSync(int semester) {
        return studentDao.getStudentsBySemesterSync(semester);
    }

    public LiveData<List<Integer>> getAllSemesters() {
        return studentDao.getAllSemesters();
    }

    public LiveData<List<Student>> getStudentsBySemester(int semester) {
        return studentDao.getStudentsBySemester(semester);
    }

    public void deleteAllStudentsSync() {
        Log.d(TAG, "Attempting to delete all students synchronously.");
        studentDao.deleteAllStudents();
        Log.d(TAG, "All students deleted synchronously.");
    }


    // --- Attendance operations ---
    public LiveData<List<Attendance>> getAttendanceForStudent(long studentId) {
        return attendanceDao.getAttendanceForStudent(studentId);
    }

    public LiveData<List<Attendance>> getMonthlyAttendanceForStudent(long studentId, long startDate, long endDate) {
        return attendanceDao.getMonthlyAttendanceForStudent(studentId, startDate, endDate);
    }

    public long insertAttendance(Attendance attendance) {
        Log.d(TAG, "Attempting to insert attendance for student ID: " + attendance.getStudentId() + " on date: " + attendance.getDate());
        long id = attendanceDao.insertAttendance(attendance);
        Log.d(TAG, "Attendance for student ID: " + attendance.getStudentId() + " inserted with ID: " + id);
        return id;
    }

    public void updateAttendance(Attendance attendance) {
        databaseWriteExecutor.execute(() -> attendanceDao.updateAttendance(attendance));
    }

    // New method to update SMS sent status for an attendance record
    public void updateAttendanceSmsSentStatus(long studentId, long date, boolean isSmsSent) {
        databaseWriteExecutor.execute(() -> {
            Attendance attendance = attendanceDao.getAttendanceByStudentAndDate(studentId, date);
            if (attendance != null) {
                attendance.setSmsSent(isSmsSent);
                attendanceDao.updateAttendance(attendance);
                Log.d(TAG, "Updated SMS sent status for student ID " + studentId + " on date " + date + " to " + isSmsSent);
            } else {
                Log.w(TAG, "Attendance record not found for student ID " + studentId + " on date " + date + ". Cannot update SMS sent status.");
            }
        });
    }


    public void deleteAttendance(long attendanceId) {
        databaseWriteExecutor.execute(() -> attendanceDao.deleteAttendance(attendanceId));
    }

    public LiveData<Integer> getPresentCountForStudentMonth(long studentId, long startDate, long endDate) {
        return attendanceDao.getPresentCountForStudentMonth(studentId, startDate, endDate);
    }

    public LiveData<Integer> getAbsentCountForStudentMonth(long studentId, long startDate, long endDate) {
        return attendanceDao.getAbsentCountForStudentMonth(studentId, startDate, endDate);
    }

    public LiveData<List<Attendance>> getAllAttendanceForMonth(long startDate, long endDate) {
        return attendanceDao.getAllAttendanceForMonth(startDate, endDate);
    }

    public List<Attendance> getAllAttendanceSync() {
        return attendanceDao.getAllAttendanceRecordsSync();
    }

    public LiveData<List<Long>> getAllStudentIdsWithAttendance() {
        return attendanceDao.getAllStudentIdsWithAttendance();
    }

    public LiveData<Student> getStudent(long studentId) {
        return studentDao.getStudentByIdLiveData(studentId);
    }

    public Attendance getAttendanceByStudentAndDate(long studentId, long date) {
        return attendanceDao.getAttendanceByStudentAndDate(studentId, date);
    }

    public Long getLatestAttendanceDate() {
        return attendanceDao.getLatestAttendanceDate();
    }

    // This method now fetches students who are absent AND whose SMS has NOT been sent
    public List<Student> getAbsentStudentsOnDate(long date) {
        List<Long> absentStudentIds = attendanceDao.getAbsentStudentIdsOnDateForSms(date); // Use new DAO method
        List<Student> absentStudents = new ArrayList<>();
        if (absentStudentIds != null) {
            for (long studentId : absentStudentIds) {
                Student student = studentDao.getStudentById(studentId); // This is a synchronous call to get a student
                if (student != null) {
                    absentStudents.add(student);
                }
            }
        }
        return absentStudents;
    }

    /**
     * Retrieves students who do NOT have an attendance record for a specific date and semester.
     * This is used to display students whose attendance is still pending for a given date.
     * @param semester The semester to filter students by.
     * @param dateMillis The date (in milliseconds) for which to check for existing attendance records.
     * @return A list of Students who do not have an attendance record for the specified date.
     */
    public List<Student> getStudentsWithoutAttendanceForDateAndSemester(int semester, long dateMillis) {
        // This method will use the new query in StudentDao
        return studentDao.getStudentsWithoutAttendanceForDateAndSemester(semester, dateMillis);
    }


    public void deleteAllAttendanceSync() {
        Log.d(TAG, "Attempting to delete all attendance synchronously.");
        attendanceDao.deleteAllAttendance();
        Log.d(TAG, "All attendance deleted synchronously.");
    }

    // --- Notification operations ---
    public LiveData<List<Notification>> getAllNotifications() {
        return allNotifications;
    }

    public LiveData<Integer> getUnreadNotificationCount() {
        return unreadNotificationCount;
    }

    public void insertNotification(Notification notification) {
        databaseWriteExecutor.execute(() -> notificationDao.insertNotification(notification));
    }

    public void markNotificationAsRead(long notificationId) {
        databaseWriteExecutor.execute(() -> notificationDao.markNotificationAsRead(notificationId));
    }

    public void markAllNotificationsAsRead() {
        databaseWriteExecutor.execute(() -> notificationDao.markAllNotificationsAsRead());
    }

    public void deleteNotification(long notificationId) {
        databaseWriteExecutor.execute(() -> notificationDao.deleteNotification(notificationId));
    }

    public void deleteAllNotifications() {
        databaseWriteExecutor.execute(() -> notificationDao.deleteAllNotifications());
    }


    /**
     * Data class to hold the comprehensive result of an import operation.
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
     * Performs a full import operation within a Room transaction.
     * This ensures that the deletion of old data and insertion of new data are atomic.
     * @param importedStudents The list of students to import.
     * @param importedAttendances The list of attendance records to import.
     * @param oldIdToNewIdMap A map to store the mapping of old student IDs to new generated IDs.
     * @return An ImportResult object containing counts and details of skipped records.
     */
    @Transaction
    public ImportResult performFullImportTransaction(List<ExcelUtils.StudentImport> importedStudents,
                                                     List<ExcelUtils.AttendanceImport> importedAttendances,
                                                     Map<Long, Long> oldIdToNewIdMap) {
        Log.d(TAG, "Starting performFullImportTransaction...");
        ImportResult result = new ImportResult();
        // Use the stored application context
        Application app = this.application;

        // Clear existing database data within the transaction
        try {
            Log.d(TAG, "Attempting to delete all existing attendance records.");
            attendanceDao.deleteAllAttendance();
            Log.d(TAG, "All existing attendance records deleted.");
            Log.d(TAG, "Attempting to delete all existing student records.");
            studentDao.deleteAllStudents();
            Log.d(TAG, "All existing student records deleted.");
        } catch (Exception e) {
            result.errorMessage = "Failed to clear existing data: " + e.getMessage();
            Log.e(TAG, "Error during data cleanup in transaction: " + e.getMessage(), e);
            // Insert IN-APP notification for import failure
            insertNotification(new Notification("Data Import Failed", "Failed to clear existing data during import: " + e.getMessage(), System.currentTimeMillis(), false, "ERROR"));
            // Send SYSTEM notification for import failure
            NotificationHelper.sendImportExportNotification(app, "Data Import Failed", "Failed to clear existing data during import: " + e.getMessage(), "ERROR");
            return result;
        }


        // Insert Students and map old IDs to new Room generated IDs
        Log.d(TAG, "Starting student import section. Total students to import: " + importedStudents.size());
        for (ExcelUtils.StudentImport sImport : importedStudents) {
            String name = sImport.name != null ? sImport.name.trim() : "";
            String mobile = sImport.mobile != null ? sImport.mobile.trim() : "";
            String guardianMobile = sImport.guardianMobile != null ? sImport.guardianMobile.trim() : "";
            String section = sImport.section != null ? sImport.section.trim() : "";
            String gender = sImport.gender != null ? sImport.gender.trim() : "";

            try {
                Log.d(TAG, "Processing student: " + name + " (Old ID: " + sImport.oldStudentId + ")");
                // Basic validation for imported data
                if (name.isEmpty() || mobile.isEmpty() || guardianMobile.isEmpty() || section.isEmpty() || sImport.currentSemester <= 0 || gender.equalsIgnoreCase("Select Gender") || gender.isEmpty()) {
                    result.skippedStudents.add(name + " (Validation Failed: Missing/Invalid fields)");
                    Log.w(TAG, "Skipping student due to validation: " + name + " - Missing/Invalid fields.");
                    continue;
                }
                if (mobile.length() != 10 || !mobile.matches("\\d+")) {
                    result.skippedStudents.add(name + " (Validation Failed: Invalid mobile format)");
                    Log.w(TAG, "Skipping student due to validation: " + name + " - Invalid mobile format: " + mobile);
                    continue;
                }
                if (guardianMobile.length() != 10 || !guardianMobile.matches("\\d+")) {
                    result.skippedStudents.add(name + " (Validation Failed: Invalid guardian mobile format)");
                    Log.w(TAG, "Skipping student due to validation: " + name + " - Invalid guardian mobile format: " + guardianMobile);
                    continue;
                }

                Student student = new Student(name, gender, mobile, guardianMobile, sImport.currentSemester, section);
                long newStudentId = studentDao.insert(student); // Direct call to DAO within transaction
                Log.d(TAG, "Student '" + name + "' insertion result (new ID or -1): " + newStudentId);

                if (newStudentId != -1) {
                    oldIdToNewIdMap.put(sImport.oldStudentId, newStudentId);
                    result.importedStudentCount++;
                    Log.d(TAG, "Student '" + name + "' successfully inserted. Mapped Old ID " + sImport.oldStudentId + " to New ID " + newStudentId);
                } else {
                    result.skippedStudents.add(name + " (Database insertion failed or duplicate detected)");
                    Log.e(TAG, "Student '" + name + "' insertion failed (returned -1), likely duplicate PK or constraint violation.");
                }
            } catch (Exception e) {
                result.skippedStudents.add(name + " (Unexpected error during student insertion: " + e.getMessage() + ")");
                Log.e(TAG, "Unexpected error inserting student during import: " + name + " - " + e.getMessage(), e);
            }
        }
        Log.d(TAG, "Finished student import section. Imported students: " + result.importedStudentCount + ", Skipped: " + result.skippedStudents.size());


        // Insert Attendance records, mapping to new student IDs
        Log.d(TAG, "Starting attendance import section. Total attendance records to import from Excel: " + (importedAttendances != null ? importedAttendances.size() : 0));
        if (importedAttendances != null) {
            for (ExcelUtils.AttendanceImport aImport : importedAttendances) {
                try {
                    Log.d(TAG, "Processing attendance for Old Student ID: " + aImport.oldStudentId + " on date: " + aImport.date);
                    if (oldIdToNewIdMap.containsKey(aImport.oldStudentId)) {
                        long newStudentId = oldIdToNewIdMap.get(aImport.oldStudentId);
                        Log.d(TAG, "Found new student ID " + newStudentId + " for Old Student ID " + aImport.oldStudentId);

                        // When importing attendance, assume isSmsSent is false by default.
                        Attendance attendance = new Attendance(newStudentId, aImport.date, aImport.isPresent);
                        long rowId = attendanceDao.insertAttendance(attendance); // Direct call to DAO within transaction
                        Log.d(TAG, "Attendance insertion result (new ID or -1): " + rowId + " for student ID " + newStudentId + " on date " + aImport.date);

                        if (rowId != -1) {
                            result.importedAttendanceCount++;
                            Log.d(TAG, "Attendance successfully inserted for student ID " + newStudentId + " on date " + aImport.date);
                        } else {
                            result.skippedAttendance.add("Attendance for old Student ID " + aImport.oldStudentId + " on date " + aImport.date + " (Database insertion failed or duplicate)");
                            Log.e(TAG, "Attendance insertion failed (returned -1) for old Student ID " + aImport.oldStudentId + " on date " + aImport.date + ", likely duplicate or constraint violation.");
                        }
                    } else {
                        result.skippedAttendance.add("Attendance for old Student ID " + aImport.oldStudentId + " on date " + aImport.date + " (Corresponding student not imported)");
                        Log.w(TAG, "Skipping attendance: No corresponding new student ID found for old Student ID " + aImport.oldStudentId);
                    }
                } catch (Exception e) {
                    result.skippedAttendance.add("Attendance for old Student ID " + aImport.oldStudentId + " on date " + aImport.date + " (Unexpected error: " + e.getMessage() + ")");
                    System.err.println("Error inserting attendance during import for old student ID " + aImport.oldStudentId + " on date " + aImport.date + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        Log.d(TAG, "Finished attendance import section. Imported attendance: " + result.importedAttendanceCount + ", Skipped: " + result.skippedAttendance.size());
        Log.d(TAG, "performFullImportTransaction completed. Result: " + result.importedStudentCount + " students, " + result.importedAttendanceCount + " attendance records imported.");

        // Insert final IN-APP notification for import operation
        String notificationMessage;
        String notificationType;
        String notificationTitle = "Data Import Report";

        if (result.errorMessage != null) {
            notificationMessage = "Import failed: " + result.errorMessage;
            notificationType = "ERROR";
        } else if (!result.skippedStudents.isEmpty() || !result.skippedAttendance.isEmpty()) {
            notificationMessage = "Import completed with warnings. Students imported: " + result.importedStudentCount + ", Attendance imported: " + result.importedAttendanceCount + ". Skipped students: " + result.skippedStudents.size() + ", Skipped attendance: " + result.skippedAttendance.size() + ".";
            notificationType = "WARNING";
        } else if (result.importedStudentCount > 0 || result.importedAttendanceCount > 0) {
            notificationMessage = "Data import successful! Students imported: " + result.importedStudentCount + ", Attendance imported: " + result.importedAttendanceCount + ".";
            notificationType = "SUCCESS";
        } else {
            notificationMessage = "Data import completed, but no records were imported.";
            notificationType = "INFO";
        }
        insertNotification(new Notification(notificationTitle, notificationMessage, System.currentTimeMillis(), false, notificationType));

        // Send SYSTEM notification for import operation
        NotificationHelper.sendImportExportNotification(app, notificationTitle, notificationMessage, notificationType);

        return result;
    }
}
