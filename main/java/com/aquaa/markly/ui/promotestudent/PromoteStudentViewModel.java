package com.aquaa.markly.ui.promotestudent;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aquaa.markly.data.database.Notification; // Import Notification
import com.aquaa.markly.data.database.Student;
import com.aquaa.markly.data.repository.StudentRepository;
import com.aquaa.markly.utils.NotificationHelper; // Import NotificationHelper

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for the PromoteStudentActivity.
 * Handles fetching students by semester and updating their semester in the database.
 */
public class PromoteStudentViewModel extends AndroidViewModel {

    private static final String TAG = "PromoteStudentViewModel"; // Define a TAG for logging

    private StudentRepository studentRepository;
    private MutableLiveData<List<Integer>> allSemesters = new MutableLiveData<>();
    private MutableLiveData<List<Student>> studentsToPromote = new MutableLiveData<>();
    private MutableLiveData<String> promotionResult = new MutableLiveData<>(); // Still used for immediate Toast/Snackbar feedback

    // Executor for background database operations
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public PromoteStudentViewModel(Application application) {
        super(application);
        studentRepository = new StudentRepository(application);
    }

    /**
     * LiveData to observe all unique semesters available in the database.
     * @return LiveData list of semester integers.
     * DOES NOT create a persistent notification.
     */
    public LiveData<List<Integer>> getAllSemesters() {
        return allSemesters;
    }

    /**
     * LiveData to observe the list of students to be displayed for promotion.
     * @return LiveData list of Student objects.
     */
    public LiveData<List<Student>> getStudentsToPromote() {
        return studentsToPromote;
    }

    /**
     * LiveData to observe the result message of promotion operations.
     * This is primarily for immediate UI feedback (like a Toast or Snackbar).
     * @return LiveData string message.
     */
    public LiveData<String> getPromotionResult() {
        return promotionResult;
    }

    /**
     * Loads all unique semester numbers from the database.
     * Posts the result to `allSemesters` LiveData.
     * DOES NOT create a persistent notification.
     */
    public void loadAllSemesters() {
        dbExecutor.execute(() -> {
            try {
                List<Integer> semesters = studentRepository.getAllSemestersSync();
                if (semesters != null && !semesters.isEmpty()) {
                    allSemesters.postValue(semesters);
                    Log.d(TAG, "Available semesters loaded.");
                } else {
                    allSemesters.postValue(new ArrayList<>()); // Post empty list if no semesters
                    String msg = "No semesters found in the database.";
                    promotionResult.postValue(msg); // For immediate UI feedback
                    Log.w(TAG, msg);
                }
            } catch (Exception e) {
                allSemesters.postValue(new ArrayList<>()); // Post empty list on error
                String msg = "Error loading semesters: " + e.getMessage();
                promotionResult.postValue(msg); // For immediate UI feedback
                Log.e(TAG, msg, e);
            }
        });
    }

    /**
     * Loads students for a given semester from the database.
     * Posts the result to `studentsToPromote` LiveData.
     * DOES NOT create a persistent notification.
     * @param semester The semester number to load students for.
     */
    public void loadStudentsForSemester(int semester) {
        dbExecutor.execute(() -> {
            try {
                List<Student> students = studentRepository.getStudentsBySemesterSync(semester);
                if (students != null && !students.isEmpty()) {
                    studentsToPromote.postValue(students);
                    Log.d(TAG, "Students for semester " + semester + " loaded.");
                } else {
                    studentsToPromote.postValue(new ArrayList<>()); // Post empty list if no students
                    String msg = "No students found for semester " + semester + ".";
                    promotionResult.postValue(msg); // For immediate UI feedback
                    Log.w(TAG, msg);
                }
            } catch (Exception e) {
                studentsToPromote.postValue(new ArrayList<>()); // Post empty list on error
                String msg = "Error loading students for semester " + semester + ": " + e.getMessage();
                promotionResult.postValue(msg); // For immediate UI feedback
                Log.e(TAG, msg, e);
            }
        });
    }

    /**
     * Promotes a list of selected students to the next semester.
     * Updates their `currentSemester` in the database.
     * This operation WILL generate a persistent in-app notification and a system notification.
     * @param students The list of Student objects to promote.
     */
    public void promoteStudents(List<Student> students) {
        dbExecutor.execute(() -> {
            String notificationTitle = "Student Promotion Report"; // Changed title for clarity
            String notificationType = "SUCCESS"; // Default type
            String msg; // Message for both promotionResult and Notification

            if (students == null || students.isEmpty()) {
                msg = "No students selected for promotion.";
                promotionResult.postValue(msg);
                Log.w(TAG, msg);
                return;
            }

            try {
                int promotedCount = 0;
                List<String> failedPromotions = new ArrayList<>();
                for (Student student : students) {
                    if (student == null) {
                        Log.w(TAG, "Attempted to promote a null student object.");
                        continue;
                    }
                    try {
                        // Increment the semester
                        student.setCurrentSemester(student.getCurrentSemester() + 1);
                        studentRepository.updateStudent(student); // Update student in DB
                        promotedCount++;
                    } catch (Exception e) {
                        failedPromotions.add(student.getName() + " (ID: " + student.getStudentId() + "): " + e.getMessage());
                        Log.e(TAG, "Failed to promote student " + student.getName() + " (ID: " + student.getStudentId() + ")", e);
                    }
                }

                if (promotedCount > 0) { // Only create a notification if at least one student was successfully promoted
                    if (failedPromotions.isEmpty()) {
                        msg = "Successfully promoted " + promotedCount + " student(s) to the next semester!";
                    } else {
                        msg = "Promoted " + promotedCount + " student(s). Failed to promote " + failedPromotions.size() + " student(s): " + String.join(", ", failedPromotions);
                        notificationType = "WARNING"; // If some failed, it's a warning
                    }
                    promotionResult.postValue(msg); // For immediate UI feedback
                    studentRepository.insertNotification(new Notification(notificationTitle, msg, System.currentTimeMillis(), false, notificationType)); // In-app notification
                    NotificationHelper.sendPromoteReportNotification(getApplication(), msg, notificationType); // System notification
                    Log.i(TAG, "Promotion operation completed. Notifications generated.");
                } else {
                    // If no students were promoted at all (e.g., all failed or list was empty initially)
                    if (failedPromotions.isEmpty()) {
                        msg = "No students were promoted.";
                    } else {
                        msg = "Failed to promote all " + students.size() + " selected student(s). Details: " + String.join(", ", failedPromotions);
                        notificationType = "ERROR";
                        studentRepository.insertNotification(new Notification(notificationTitle, msg, System.currentTimeMillis(), false, notificationType)); // In-app notification
                        NotificationHelper.sendPromoteReportNotification(getApplication(), msg, notificationType); // System notification
                    }
                    promotionResult.postValue(msg); // For immediate UI feedback
                    Log.w(TAG, msg);
                }

            } catch (Exception e) {
                // Catch-all for unexpected errors during the promotion batch process itself
                msg = "An unexpected error occurred during student promotion: " + e.getMessage();
                promotionResult.postValue(msg); // For immediate UI feedback
                studentRepository.insertNotification(new Notification(notificationTitle, msg, System.currentTimeMillis(), false, "ERROR")); // In-app notification
                NotificationHelper.sendPromoteReportNotification(getApplication(), msg, "ERROR"); // System notification
                Log.e(TAG, msg, e);
            }
        });
    }

    /**
     * Called when the ViewModel is no longer used and will be destroyed.
     * This is crucial for cleaning up resources like the ExecutorService.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        if (!dbExecutor.isShutdown()) {
            dbExecutor.shutdown();
            Log.d(TAG, "dbExecutor shutdown initiated in PromoteStudentViewModel.");
        }
    }
}
