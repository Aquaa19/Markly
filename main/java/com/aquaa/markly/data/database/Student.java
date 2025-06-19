package com.aquaa.markly.data.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects; // Import for Objects.equals and Objects.hash

/**
 * Room Entity for storing student information.
 */
@Entity(tableName = "students")
public class Student {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "student_id")
    private long studentId; // Consistent with long type

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "gender")
    private String gender;

    @ColumnInfo(name = "mobile")
    private String mobile; // Student's own mobile number

    @ColumnInfo(name = "guardian_mobile")
    private String guardianMobile;

    @ColumnInfo(name = "current_semester")
    private int currentSemester;

    @ColumnInfo(name = "section")
    private String section; // Added section field

    /**
     * Constructor for the Student entity.
     *
     * @param name The name of the student.
     * @param gender The gender of the student.
     * @param mobile The student's mobile number.
     * @param guardianMobile The guardian's mobile number.
     * @param currentSemester The current semester the student is in.
     * @param section The section the student belongs to.
     */
    public Student(String name, String gender, String mobile, String guardianMobile, int currentSemester, String section) {
        this.name = name;
        this.gender = gender;
        this.mobile = mobile;
        this.guardianMobile = guardianMobile;
        this.currentSemester = currentSemester;
        this.section = section;
    }

    // --- Getters and Setters ---

    public long getStudentId() {
        return studentId;
    }

    public void setStudentId(long studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getGuardianMobile() {
        return guardianMobile;
    }

    public void setGuardianMobile(String guardianMobile) {
        this.guardianMobile = guardianMobile;
    }

    public int getCurrentSemester() {
        return currentSemester;
    }

    public void setCurrentSemester(int currentSemester) {
        this.currentSemester = currentSemester;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    // --- Override equals() and hashCode() for proper object comparison ---
    // This is crucial for RecyclerView selection logic to work correctly

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return studentId == student.studentId; // Students are equal if their studentId is the same
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId); // Hash code based on the unique studentId
    }
}
