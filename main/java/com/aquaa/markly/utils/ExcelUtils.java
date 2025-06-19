package com.aquaa.markly.utils;

import com.aquaa.markly.data.database.Attendance;
import com.aquaa.markly.data.database.Student;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import android.util.Log; // Import Log

/**
 * Utility class for importing and exporting student and attendance data to/from Excel files.
 * Uses Apache POI library.
 */
public class ExcelUtils {

    private static final String TAG = "ExcelUtils"; // Tag for logging
    private static final String STUDENTS_SHEET_NAME = "Students";
    private static final String ATTENDANCE_SHEET_NAME = "Attendance";

    /**
     * Exports student and attendance data to an XLSX (Excel) file.
     * The file will contain two sheets: "Students" and "Attendance".
     *
     * @param students The list of Student objects to export.
     * @param attendances The list of Attendance objects to export.
     * @param outputStream The OutputStream to write the Excel file to.
     * @throws IOException If an I/O error occurs.
     */
    public static void exportDataToXLSX(List<Student> students, List<Attendance> attendances, OutputStream outputStream) throws IOException {
        Log.d(TAG, "Starting export to XLSX...");
        Workbook workbook = new XSSFWorkbook();

        // 1. Create Students Sheet
        Sheet studentsSheet = workbook.createSheet(STUDENTS_SHEET_NAME);
        // Header Row for Students
        Row studentHeader = studentsSheet.createRow(0);
        studentHeader.createCell(0).setCellValue("Student ID (Original)");
        studentHeader.createCell(1).setCellValue("Name");
        studentHeader.createCell(2).setCellValue("Gender");
        studentHeader.createCell(3).setCellValue("Mobile");
        studentHeader.createCell(4).setCellValue("Guardian Mobile");
        studentHeader.createCell(5).setCellValue("Current Semester");
        studentHeader.createCell(6).setCellValue("Section");

        // Data Rows for Students
        int studentRowIdx = 1;
        for (Student student : students) {
            Row row = studentsSheet.createRow(studentRowIdx++);
            row.createCell(0).setCellValue(student.getStudentId()); // Export original ID
            row.createCell(1).setCellValue(student.getName());
            row.createCell(2).setCellValue(student.getGender());
            row.createCell(3).setCellValue(student.getMobile()); // Keep as string to preserve leading zeros
            row.createCell(4).setCellValue(student.getGuardianMobile()); // Keep as string to preserve leading zeros
            row.createCell(5).setCellValue(student.getCurrentSemester());
            row.createCell(6).setCellValue(student.getSection());
            Log.d(TAG, "Exported student row: " + student.getName() + ", ID: " + student.getStudentId());
        }
        Log.d(TAG, "Students sheet exported. Total students: " + students.size());

        // 2. Create Attendance Sheet
        Sheet attendanceSheet = workbook.createSheet(ATTENDANCE_SHEET_NAME);
        // Header Row for Attendance
        Row attendanceHeader = attendanceSheet.createRow(0);
        attendanceHeader.createCell(0).setCellValue("Attendance ID (Original)"); // For reference
        attendanceHeader.createCell(1).setCellValue("Student ID (Original)"); // Link to original student ID
        attendanceHeader.createCell(2).setCellValue("Date (Timestamp)");
        attendanceHeader.createCell(3).setCellValue("Is Present");

        // Data Rows for Attendance
        int attendanceRowIdx = 1;
        for (Attendance attendance : attendances) {
            Row row = attendanceSheet.createRow(attendanceRowIdx++);
            row.createCell(0).setCellValue(attendance.getAttendanceId()); // Export original attendance ID
            row.createCell(1).setCellValue(attendance.getStudentId()); // Export original student ID
            row.createCell(2).setCellValue(attendance.getDate()); // Export timestamp (long)
            row.createCell(3).setCellValue(attendance.isPresent() ? "TRUE" : "FALSE"); // Store as string for clarity
            Log.d(TAG, "Exported attendance row: Student ID " + attendance.getStudentId() + ", Date: " + attendance.getDate() + ", Present: " + attendance.isPresent());
        }
        Log.d(TAG, "Attendance sheet exported. Total attendance records: " + attendances.size());

        workbook.write(outputStream);
        workbook.close();
        Log.d(TAG, "Export completed successfully.");
    }

    /**
     * Imports student and attendance data from an XLSX (Excel) file.
     * The file is expected to contain "Students" and "Attendance" sheets.
     *
     * @param inputStream The InputStream to read the Excel file from.
     * @return A Map containing lists of imported students and attendance.
     * Key "students" for List<StudentImport>, Key "attendances" for List<AttendanceImport>.
     * @throws IOException If an I/O error occurs.
     */
    public static Map<String, Object> importDataFromXLSX(InputStream inputStream) throws IOException {
        Log.d(TAG, "Starting import from XLSX...");
        Workbook workbook = new XSSFWorkbook(inputStream);
        Map<String, Object> importedData = new HashMap<>();

        List<StudentImport> importedStudents = new ArrayList<>();
        List<AttendanceImport> importedAttendances = new ArrayList<>();

        DataFormatter dataFormatter = new DataFormatter();
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

        // 1. Read Students Sheet
        Sheet studentsSheet = workbook.getSheet(STUDENTS_SHEET_NAME);
        if (studentsSheet != null) {
            Log.d(TAG, "Reading Students sheet.");
            Iterator<Row> rowIterator = studentsSheet.iterator();
            if (rowIterator.hasNext()) {
                rowIterator.next(); // Skip header row
            }
            while (rowIterator.hasNext()) {
                Row currentRow = rowIterator.next();
                int rowNum = currentRow.getRowNum();

                if (isRowEmpty(currentRow)) {
                    Log.w(TAG, "Skipping empty row " + rowNum + " in Students sheet.");
                    continue;
                }

                try {
                    long oldStudentId = (long) parseNumericCell(currentRow.getCell(0), dataFormatter, evaluator, rowNum, "Student ID (Original)");
                    String name = parseStringCell(currentRow.getCell(1), dataFormatter, evaluator, rowNum, "Name");
                    String gender = parseStringCell(currentRow.getCell(2), dataFormatter, evaluator, rowNum, "Gender");
                    String mobile = parseStringCell(currentRow.getCell(3), dataFormatter, evaluator, rowNum, "Mobile");
                    String guardianMobile = parseStringCell(currentRow.getCell(4), dataFormatter, evaluator, rowNum, "Guardian Mobile");
                    int currentSemester = (int) parseNumericCell(currentRow.getCell(5), dataFormatter, evaluator, rowNum, "Current Semester");
                    String section = parseStringCell(currentRow.getCell(6), dataFormatter, evaluator, rowNum, "Section");

                    importedStudents.add(new StudentImport(oldStudentId, name, gender, mobile, guardianMobile, currentSemester, section));
                    Log.d(TAG, "Imported student data from row " + rowNum + ": " + name + ", Old ID: " + oldStudentId);
                } catch (Exception e) {
                    Log.e(TAG, "Error reading student row " + rowNum + " in Students sheet: " + e.getMessage(), e);
                }
            }
        } else {
            Log.w(TAG, "Students sheet not found in the Excel file.");
        }
        Log.d(TAG, "Finished reading Students sheet. Imported " + importedStudents.size() + " students.");


        // 2. Read Attendance Sheet
        Sheet attendanceSheet = workbook.getSheet(ATTENDANCE_SHEET_NAME);
        if (attendanceSheet != null) {
            Log.d(TAG, "Reading Attendance sheet.");
            Iterator<Row> rowIterator = attendanceSheet.iterator();
            if (rowIterator.hasNext()) {
                rowIterator.next(); // Skip header row
            }
            while (rowIterator.hasNext()) {
                Row currentRow = rowIterator.next();
                int rowNum = currentRow.getRowNum();

                if (isRowEmpty(currentRow)) {
                    Log.w(TAG, "Skipping empty row " + rowNum + " in Attendance sheet.");
                    continue;
                }

                try {
                    // long oldAttendanceId = (long) parseNumericCell(currentRow.getCell(0), dataFormatter, evaluator, rowNum, "Attendance ID (Original)");
                    long oldStudentId = (long) parseNumericCell(currentRow.getCell(1), dataFormatter, evaluator, rowNum, "Student ID (Original)");
                    long date = (long) parseNumericCell(currentRow.getCell(2), dataFormatter, evaluator, rowNum, "Date (Timestamp)");
                    boolean isPresent = parseStringCell(currentRow.getCell(3), dataFormatter, evaluator, rowNum, "Is Present").equalsIgnoreCase("TRUE");

                    importedAttendances.add(new AttendanceImport(oldStudentId, date, isPresent));
                    Log.d(TAG, "Imported attendance data from row " + rowNum + ": Old Student ID " + oldStudentId + ", Date: " + date + ", Present: " + isPresent);
                } catch (Exception e) {
                    Log.e(TAG, "Error reading attendance row " + rowNum + " in Attendance sheet: " + e.getMessage(), e);
                }
            }
        } else {
            Log.w(TAG, "Attendance sheet not found in the Excel file.");
        }
        Log.d(TAG, "Finished reading Attendance sheet. Imported " + importedAttendances.size() + " attendance records.");

        workbook.close();
        importedData.put("students", importedStudents);
        importedData.put("attendances", importedAttendances);
        Log.d(TAG, "Import process completed.");
        return importedData;
    }

    /** Helper to get string value from cell, handling null cells and different types. */
    private static String parseStringCell(Cell cell, DataFormatter dataFormatter, FormulaEvaluator evaluator, int rowNum, String columnName) {
        if (cell == null) {
            Log.d(TAG, "Cell is null (Col: " + columnName + ", Row: " + rowNum + "), returning empty string.");
            return "";
        }
        try {
            String value;
            if (cell.getCellType() == CellType.FORMULA) {
                value = dataFormatter.formatCellValue(cell, evaluator);
            } else {
                value = dataFormatter.formatCellValue(cell);
            }
            Log.d(TAG, "Read string cell (Col: " + columnName + ", Row: " + rowNum + "): '" + value + "'");
            return value;
        } catch (Exception e) {
            Log.e(TAG, "Error reading string cell (Col: " + columnName + ", Row: " + rowNum + "): " + e.getMessage(), e);
            return "";
        }
    }

    /** Helper to get numeric value from cell, handling null cells and different types. */
    private static double parseNumericCell(Cell cell, DataFormatter dataFormatter, FormulaEvaluator evaluator, int rowNum, String columnName) throws NumberFormatException {
        if (cell == null) {
            Log.d(TAG, "Numeric cell is null (Col: " + columnName + ", Row: " + rowNum + "), returning 0.0.");
            return 0.0;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                double numericValue = cell.getNumericCellValue();
                Log.d(TAG, "Read numeric cell (Col: " + columnName + ", Row: " + rowNum + "): " + numericValue + " (Type: NUMERIC)");
                return numericValue;
            } else {
                String stringValue;
                if (cell.getCellType() == CellType.FORMULA) {
                    stringValue = dataFormatter.formatCellValue(cell, evaluator);
                } else {
                    stringValue = dataFormatter.formatCellValue(cell);
                }
                Log.d(TAG, "Attempting to parse string for numeric cell (Col: " + columnName + ", Row: " + rowNum + "): '" + stringValue + "'");
                // Remove any non-digit characters except for '-' and '.' which might be present in numbers.
                // This is specifically important for mobile numbers that might be read as general text.
                stringValue = stringValue.replaceAll("[^\\d.-]", "");
                if (stringValue.isEmpty()) {
                    Log.w(TAG, "Cleaned string for numeric cell (Col: " + columnName + ", Row: " + rowNum + ") is empty. Returning 0.0.");
                    return 0.0;
                }
                double parsedValue = Double.parseDouble(stringValue);
                Log.d(TAG, "Parsed string to numeric: " + parsedValue);
                return parsedValue;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "NumberFormatException parsing numeric cell (Col: " + columnName + ", Row: " + rowNum + "): " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Error reading numeric cell (Col: " + columnName + ", Row: " + rowNum + "): " + e.getMessage(), e);
            throw new NumberFormatException("Could not parse numeric value from cell: " + e.getMessage());
        }
    }

    /** Helper to check if a row is completely empty. */
    private static boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        if (row.getLastCellNum() <= 0) {
            return true;
        }
        for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            if (cell != null && cell.getCellType() != CellType.BLANK && !cell.toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }


    // --- Helper classes for import data ---

    /** Temporary class to hold student data during import, including original ID. */
    public static class StudentImport {
        public long oldStudentId;
        public String name;
        public String gender;
        public String mobile;
        public String guardianMobile;
        public int currentSemester;
        public String section;

        public StudentImport(long oldStudentId, String name, String gender, String mobile, String guardianMobile, int currentSemester, String section) {
            this.oldStudentId = oldStudentId;
            this.name = name;
            this.gender = gender;
            this.mobile = mobile;
            this.guardianMobile = guardianMobile;
            this.currentSemester = currentSemester;
            this.section = section;
        }
    }

    /** Temporary class to hold attendance data during import, linking to original student ID. */
    public static class AttendanceImport {
        public long oldStudentId;
        public long date;
        public boolean isPresent;

        public AttendanceImport(long oldStudentId, long date, boolean isPresent) {
            this.oldStudentId = oldStudentId;
            this.date = date;
            this.isPresent = isPresent;
        }
    }
}
