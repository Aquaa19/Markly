package com.aquaa.markly.ui.attendance;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.aquaa.markly.data.database.Attendance;
import com.aquaa.markly.data.database.Notification;
import com.aquaa.markly.data.database.Student;
import com.aquaa.markly.data.repository.StudentRepository;
import com.aquaa.markly.utils.NotificationHelper; // Import NotificationHelper

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for the AttendanceActivity.
 * Handles fetching students and saving their attendance status for a specific date.
 */
public class AttendanceViewModel extends AndroidViewModel {

    private static final String TAG = "AttendanceViewModel";
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()); // For consistent date logging


    private StudentRepository studentRepository;
    private MutableLiveData<List<Integer>> allSemesters = new MutableLiveData<>();
    private MutableLiveData<Integer> selectedSemester = new MutableLiveData<>();
    private MutableLiveData<Long> selectedAttendanceDateMillis = new MutableLiveData<>();

    // LiveData to hold students for a selected semester (without attendance status initially)
    private MutableLiveData<List<Student>> studentsForAttendance = new MutableLiveData<>();

    // LiveData to hold students with their attendance status for the selected date
    private MutableLiveData<List<StudentAttendanceStatus>> studentsWithAttendanceStatus = new MutableLiveData<>();

    // Executor for background database operations
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public AttendanceViewModel(Application application) {
        super(application);
        studentRepository = new StudentRepository(application);
    }

    /**
     * LiveData to observe all unique semesters available in the database.
     * @return LiveData list of semester integers.
     */
    public LiveData<List<Integer>> getAllSemesters() {
        return allSemesters;
    }

    /**
     * LiveData to observe the list of students for the currently selected semester/date.
     * @return LiveData list of Student objects.
     */
    public LiveData<List<Student>> getStudentsForAttendance() {
        return studentsForAttendance;
    }

    /**
     * LiveData to observe the list of students with their attendance status for the selected date.
     * This LiveData is updated after loading students and then fetching their attendance status.
     * @return LiveData list of StudentAttendanceStatus objects.
     */
    public LiveData<List<StudentAttendanceStatus>> getStudentsWithAttendanceStatus() {
        return studentsWithAttendanceStatus;
    }

    /**
     * Loads all unique semester numbers from the database.
     */
    public void loadAllSemesters() {
        dbExecutor.execute(() -> {
            try {
                List<Integer> semesters = studentRepository.getAllSemestersSync();
                allSemesters.postValue(semesters);
            } catch (Exception e) {
                Log.e(TAG, "Error loading semesters: " + e.getMessage(), e);
                allSemesters.postValue(new ArrayList<>());
            }
        });
    }

    /**
     * Loads students for a specific semester and date.
     * This will now load students who *do not yet have an attendance record* for the given date.
     * @param semester The semester to load students for.
     * @param dateMillis The date for which to load attendance status.
     */
    public void loadStudentsForSemesterAndDate(int semester, long dateMillis) {
        selectedSemester.postValue(semester);
        selectedAttendanceDateMillis.postValue(dateMillis);
        Log.d(TAG, "loadStudentsForSemesterAndDate: Called for semester " + semester + " and date " + sdf.format(new Date(dateMillis)) + " (" + dateMillis + ")");

        dbExecutor.execute(() -> {
            try {
                // Fetch students who DO NOT have an attendance record for this date and semester
                List<Student> studentsPendingAttendance = studentRepository.getStudentsWithoutAttendanceForDateAndSemester(semester, dateMillis);
                Log.d(TAG, "loadStudentsForSemesterAndDate: Found " + studentsPendingAttendance.size() + " students pending attendance for " + sdf.format(new Date(dateMillis)));

                // For these students, their default status will be 'present' as no record exists yet.
                List<StudentAttendanceStatus> studentStatuses = new ArrayList<>();
                for (Student student : studentsPendingAttendance) {
                    // Default to true (present) since no record exists yet for this date
                    studentStatuses.add(new StudentAttendanceStatus(student, true));
                }
                studentsWithAttendanceStatus.postValue(studentStatuses);

            } catch (Exception e) {
                Log.e(TAG, "Error loading students for semester " + semester + " and date " + dateMillis + ": " + e.getMessage(), e);
                studentsWithAttendanceStatus.postValue(new ArrayList<>());
            }
        });
    }


    /**
     * Helper method to load the attendance status for a given list of students on a specific date.
     * This method is now implicitly handled by `loadStudentsForSemesterAndDate`'s new logic.
     * It remains for completeness but its direct usage might be reduced.
     * @param students The list of students whose attendance status needs to be checked.
     * @param dateMillis The date for which to check attendance.
     */
    public void loadAttendanceStatusForStudents(List<Student> students, long dateMillis) {
        dbExecutor.execute(() -> {
            List<StudentAttendanceStatus> studentStatuses = new ArrayList<>();
            for (Student student : students) {
                boolean isPresent = false;
                Attendance attendance = studentRepository.getAttendanceByStudentAndDate(student.getStudentId(), dateMillis);
                if (attendance != null) {
                    isPresent = attendance.isPresent();
                }
                studentStatuses.add(new StudentAttendanceStatus(student, isPresent));
            }
            studentsWithAttendanceStatus.postValue(studentStatuses);
            Log.d(TAG, "Loaded attendance status for " + studentStatuses.size() + " students for date " + sdf.format(new Date(dateMillis)) + " (" + dateMillis + ")");
        });
    }

    /**
     * Saves the attendance for the given date based on the provided map of student IDs and their status.
     * After saving, it reloads the list, which will now exclude the students whose attendance was just saved.
     * @param dateMillis The date for which attendance is being saved.
     * @param attendanceStatusMap A map where key is Student ID and value is true if present, false if absent.
     */
    public void saveAttendanceForDate(long dateMillis, Map<Long, Boolean> attendanceStatusMap) {
        dbExecutor.execute(() -> {
            int savedCount = 0;
            int updatedCount = 0;
            int failedCount = 0;
            for (Map.Entry<Long, Boolean> entry : attendanceStatusMap.entrySet()) {
                long studentId = entry.getKey();
                boolean isPresent = entry.getValue();

                try {
                    Attendance existingAttendance = studentRepository.getAttendanceByStudentAndDate(studentId, dateMillis);
                    if (existingAttendance == null) {
                        // Insert new attendance record
                        Attendance newAttendance = new Attendance(studentId, dateMillis, isPresent);
                        studentRepository.insertAttendance(newAttendance);
                        savedCount++;
                        Log.d(TAG, "Inserted attendance for student " + studentId + " on " + sdf.format(new Date(dateMillis)) + ": Present=" + isPresent);
                    } else {
                        // Update existing attendance record if status changed
                        if (existingAttendance.isPresent() != isPresent) {
                            existingAttendance.setPresent(isPresent);
                            // Also reset isSmsSent to false if student is marked absent (and was previously present or absent but SMS was sent)
                            if (!isPresent) { // If student is now marked absent
                                existingAttendance.setSmsSent(false); // Reset SMS sent status
                                Log.d(TAG, "Student " + studentId + " marked absent. Resetting isSmsSent flag to false.");
                            } else { // If student is now marked present, and was previously absent with SMS sent, we don't care about SMS status for this record.
                                // If they were previously absent and isSmsSent was false, keep it false.
                            }
                            studentRepository.updateAttendance(existingAttendance);
                            updatedCount++;
                            Log.d(TAG, "Updated attendance for student " + studentId + " on " + sdf.format(new Date(dateMillis)) + ": Present=" + isPresent);
                        } else {
                            // Status is the same. If it's still absent AND isSmsSent was true, reset it to false.
                            // This scenario is for when user re-saves attendance for an already absent student who was messaged.
                            if (!isPresent && existingAttendance.isSmsSent()) {
                                existingAttendance.setSmsSent(false); // Clear the SMS sent flag
                                studentRepository.updateAttendance(existingAttendance);
                                Log.d(TAG, "Student " + studentId + " re-marked absent. Resetting isSmsSent flag as it was true.");
                            }
                        }
                    }
                } catch (Exception e) {
                    failedCount++;
                    Log.e(TAG, "Failed to save/update attendance for student " + studentId + " on " + sdf.format(new Date(dateMillis)) + ": " + e.getMessage(), e);
                }
            }
            String message = "Attendance saved: " + (savedCount + updatedCount) + " records. Failed: " + failedCount + ".";
            String notificationType = "SUCCESS";
            if (failedCount > 0) {
                notificationType = "WARNING";
            } else if (savedCount + updatedCount == 0) {
                notificationType = "INFO";
                message = "No attendance changes to save for " + sdf.format(new Date(dateMillis)) + ".";
            }
            Log.i(TAG, message);
            // After saving, reload the students for the current semester and date to reflect changes.
            // This will now fetch *only* students who *still need attendance taken* for this date.
            Integer currentSemester = selectedSemester.getValue();
            Long currentDateMillis = selectedAttendanceDateMillis.getValue();
            if (currentSemester != null && currentDateMillis != null) {
                loadStudentsForSemesterAndDate(currentSemester, currentDateMillis);
            }


            // Insert IN-APP notification about attendance save
            studentRepository.insertNotification(new Notification("Attendance Report", message, System.currentTimeMillis(), false, notificationType));
            Log.d(TAG, "In-app notification generated for attendance report.");

            // Send SYSTEM notification about attendance save
            NotificationHelper.sendAttendanceReportNotification(getApplication(), message, notificationType);
            Log.d(TAG, "System notification triggered for attendance report.");
        });
    }


    /**
     * Data class to encapsulate a Student and their attendance status for a given date.
     */
    public static class StudentAttendanceStatus {
        public Student student;
        public boolean isPresent; // True if present, false if absent

        public StudentAttendanceStatus(Student student, boolean isPresent) {
            this.student = student;
            this.isPresent = isPresent;
        }

        // For RecyclerView.DiffUtil to compare content
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StudentAttendanceStatus that = (StudentAttendanceStatus) o;
            // Compare based on studentId and isPresent status
            return student.getStudentId() == that.student.getStudentId() &&
                    isPresent == that.isPresent;
        }

        @Override
        public int hashCode() {
            // Include studentId and isPresent in hash code
            return (int) (student.getStudentId() * 31 + (isPresent ? 1 : 0));
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!dbExecutor.isShutdown()) {
            dbExecutor.shutdown();
        }
        Log.d(TAG, "dbExecutor shutdown initiated in AttendanceViewModel.");
    }
}
