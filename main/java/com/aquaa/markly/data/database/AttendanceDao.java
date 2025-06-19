package com.aquaa.markly.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AttendanceDao {
    // Inserts a new attendance record, returning the row ID
    @Insert
    long insertAttendance(Attendance attendance);

    // Updates an existing attendance record
    @Update
    void updateAttendance(Attendance attendance);

    // Deletes an attendance record by ID
    @Query("DELETE FROM attendance WHERE attendance_id = :attendanceId")
    void deleteAttendance(long attendanceId);

    // Retrieves all attendance records for a specific student
    @Query("SELECT * FROM attendance WHERE student_id = :studentId ORDER BY date DESC")
    LiveData<List<Attendance>> getAttendanceForStudent(long studentId);

    // Retrieves attendance records for a specific student within a date range (for monthly tracking)
    // The 'startDate' and 'endDate' are Unix timestamps in milliseconds
    @Query("SELECT * FROM attendance WHERE student_id = :studentId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    LiveData<List<Attendance>> getMonthlyAttendanceForStudent(long studentId, long startDate, long endDate);

    // Retrieves all unique student IDs that have attendance records
    @Query("SELECT DISTINCT student_id FROM attendance")
    LiveData<List<Long>> getAllStudentIdsWithAttendance();

    // Retrieves all attendance records for a given date (useful for daily attendance taking)
    @Query("SELECT * FROM attendance WHERE date = :date ORDER BY student_id ASC")
    LiveData<List<Attendance>> getAttendanceByDate(long date);

    // Retrieves a single attendance record by student ID and date
    @Query("SELECT * FROM attendance WHERE student_id = :studentId AND date = :date LIMIT 1")
    Attendance getAttendanceByStudentAndDate(long studentId, long date);

    // Query to count present days for a student in a month
    @Query("SELECT COUNT(*) FROM attendance WHERE student_id = :studentId AND date BETWEEN :startDate AND :endDate AND is_present = 1")
    LiveData<Integer> getPresentCountForStudentMonth(long studentId, long startDate, long endDate);

    // Query to count absent days for a student in a month
    @Query("SELECT COUNT(*) FROM attendance WHERE student_id = :studentId AND date BETWEEN :startDate AND :endDate AND is_present = 0")
    LiveData<Integer> getAbsentCountForStudentMonth(long studentId, long startDate, long endDate);

    // Query to get all attendance records for a given month and year for all students
    @Query("SELECT * FROM attendance WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, student_id ASC")
    LiveData<List<Attendance>> getAllAttendanceForMonth(long startDate, long endDate);

    // Query to get the latest attendance date (max timestamp)
    @Query("SELECT MAX(date) FROM attendance")
    Long getLatestAttendanceDate();

    // Query to get student IDs of absent students on a specific date AND for whom SMS has NOT been sent
    @Query("SELECT student_id FROM attendance WHERE date = :date AND is_present = 0 AND is_sms_sent = 0")
    List<Long> getAbsentStudentIdsOnDateForSms(long date); // This is the missing method

    // Get all attendance records synchronously (for export)
    @Query("SELECT * FROM attendance ORDER BY date ASC, student_id ASC")
    List<Attendance> getAllAttendanceRecordsSync();

    // Delete all attendance records
    @Query("DELETE FROM attendance")
    void deleteAllAttendance();
}
