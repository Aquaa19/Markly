package com.aquaa.markly.data.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;
import androidx.room.Index;

import static androidx.room.ForeignKey.CASCADE;

/**
 * Room Entity for storing attendance records.
 * Each instance represents an attendance entry for a student on a specific date.
 * Uses a foreign key to link to the Student entity.
 */
@Entity(tableName = "attendance",
        foreignKeys = @ForeignKey(entity = Student.class,
                parentColumns = "student_id",
                childColumns = "student_id",
                onDelete = CASCADE), // If a student is deleted, their attendance records are also deleted.
        indices = {@Index(value = {"student_id", "date"}, unique = true)}) // Composite index for performance, ensuring unique attendance per student per day
public class Attendance {

    // Primary key for the attendance record, auto-generated by Room
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "attendance_id")
    public long attendanceId; // Made public for direct access in some cases, or use getter/setter

    // Foreign key linking to the Student entity - now long to match Student.studentId
    @ColumnInfo(name = "student_id")
    public long studentId; // Changed to long to match Student.studentId type

    // Date of the attendance record (Unix timestamp in milliseconds)
    @ColumnInfo(name = "date")
    public long date;

    // Boolean indicating if the student was present (true) or absent (false)
    @ColumnInfo(name = "is_present")
    public boolean isPresent;

    // New field: Flag to indicate if an SMS has been sent for this specific absence record
    @ColumnInfo(name = "is_sms_sent", defaultValue = "0") // Default to false (0)
    public boolean isSmsSent;

    /**
     * Constructor for the Attendance entity.
     *
     * @param studentId The ID of the student this attendance record belongs to.
     * @param date The date of the attendance (Unix timestamp in milliseconds).
     * @param isPresent True if the student was present, false otherwise.
     */
    public Attendance(long studentId, long date, boolean isPresent) {
        this.studentId = studentId;
        this.date = date;
        this.isPresent = isPresent;
        this.isSmsSent = false; // Default to false when a new attendance record is created
    }

    // --- Getters and Setters ---

    public long getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(long attendanceId) {
        this.attendanceId = attendanceId;
    }

    public long getStudentId() {
        return studentId;
    }

    public void setStudentId(long studentId) {
        this.studentId = studentId;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public boolean isPresent() {
        return isPresent;
    }

    public void setPresent(boolean present) {
        isPresent = present;
    }

    public boolean isSmsSent() {
        return isSmsSent;
    }

    public void setSmsSent(boolean smsSent) {
        isSmsSent = smsSent;
    }
}
