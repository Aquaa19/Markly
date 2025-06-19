package com.aquaa.markly.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface StudentDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Student student); // Changed return type to long to get the generated row ID

    @Update
    void updateStudent(Student student);

    @Delete
    void deleteStudent(Student student);

    @Query("SELECT * FROM students ORDER BY name ASC")
    LiveData<List<Student>> getAllStudents();

    // Synchronous version for background operations in repository/viewmodel
    @Query("SELECT * FROM students ORDER BY name ASC")
    List<Student> getAllStudentsSync();

    @Query("SELECT * FROM students WHERE current_semester = :semester ORDER BY name ASC")
    LiveData<List<Student>> getStudentsBySemester(int semester);

    // Synchronous version for background operations in repository/viewmodel
    @Query("SELECT * FROM students WHERE current_semester = :semester ORDER BY name ASC")
    List<Student> getStudentsBySemesterSync(int semester);

    @Query("SELECT DISTINCT current_semester FROM students ORDER BY current_semester ASC")
    LiveData<List<Integer>> getAllSemesters();

    // Synchronous version for background operations in repository/viewmodel
    @Query("SELECT DISTINCT current_semester FROM students ORDER BY current_semester ASC")
    List<Integer> getAllSemestersSync();

    // Get a student by their ID (synchronous)
    @Query("SELECT * FROM students WHERE student_id = :studentId LIMIT 1")
    Student getStudentById(long studentId);

    // Get a student by their ID (LiveData version for UI updates)
    @Query("SELECT * FROM students WHERE student_id = :studentId LIMIT 1")
    LiveData<Student> getStudentByIdLiveData(long studentId);

    // NEW: Method to delete all students
    @Query("DELETE FROM students")
    void deleteAllStudents();

    /**
     * Retrieves students who do NOT have an attendance record for a specific date and semester.
     * This is used to display students whose attendance is still pending for a given date.
     * @param semester The semester to filter students by.
     * @param dateMillis The date (in milliseconds) for which to check for existing attendance records.
     * @return A list of Students who do not have an attendance record for the specified date.
     */
    @Query("SELECT s.* FROM students s LEFT JOIN attendance a ON s.student_id = a.student_id AND a.date = :dateMillis WHERE s.current_semester = :semester AND a.attendance_id IS NULL ORDER BY s.name ASC")
    List<Student> getStudentsWithoutAttendanceForDateAndSemester(int semester, long dateMillis);
}
