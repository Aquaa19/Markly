package com.aquaa.markly.ui.addstudent;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aquaa.markly.data.database.AppDatabase; // Import AppDatabase
import com.aquaa.markly.data.database.Notification;
import com.aquaa.markly.data.database.Student;
import com.aquaa.markly.data.repository.StudentRepository;
import com.aquaa.markly.utils.NotificationHelper; // For system notifications

import java.io.BufferedReader;
import java.io.File; // Keep File import if it's still used elsewhere, but remove for exportAllData param
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream; // Import OutputStream
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for the AddStudentActivity.
 * Handles logic related to adding, importing, exporting, and deleting student data,
 * now including comprehensive data backup/restore for students and attendance using JSON.
 */
public class AddStudentViewModel extends AndroidViewModel {

    private static final String TAG = "AddStudentViewModel"; // Tag for logging

    private StudentRepository studentRepository;
    private MutableLiveData<String> operationResult = new MutableLiveData<>();
    private LiveData<List<Student>> allStudentsLiveData;

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public AddStudentViewModel(Application application) {
        super(application);
        studentRepository = new StudentRepository(application);
        allStudentsLiveData = studentRepository.getAllStudents();
    }

    public LiveData<String> getOperationResult() {
        return operationResult;
    }

    public LiveData<List<Student>> getAllStudents() {
        return allStudentsLiveData;
    }

    public void insertStudent(Student student) {
        ioExecutor.execute(() -> {
            try {
                long result = studentRepository.insertStudent(student);
                if (result != -1) {
                    String msg = "Student '" + student.getName() + "' added successfully!";
                    operationResult.postValue(msg);
                    studentRepository.insertNotification(new Notification("Student Added", msg, System.currentTimeMillis(), false, "SUCCESS"));
                    NotificationHelper.sendImportExportNotification(getApplication(), "Student Added", msg, "SUCCESS"); // System notification
                } else {
                    String msg = "Failed to add student '" + student.getName() + "'. It might already exist or there was a database error.";
                    operationResult.postValue(msg);
                    studentRepository.insertNotification(new Notification("Student Add Failed", msg, System.currentTimeMillis(), false, "ERROR"));
                    NotificationHelper.sendImportExportNotification(getApplication(), "Student Add Failed", msg, "ERROR"); // System notification
                }
            } catch (Exception e) {
                String msg = "Failed to add student: " + e.getMessage();
                operationResult.postValue(msg);
                studentRepository.insertNotification(new Notification("Student Add Failed", msg, System.currentTimeMillis(), false, "ERROR"));
                NotificationHelper.sendImportExportNotification(getApplication(), "Student Add Failed", msg, "ERROR"); // System notification
                Log.e(TAG, "Error inserting student", e);
            }
        });
    }

    public void deleteStudent(Student student) {
        ioExecutor.execute(() -> {
            try {
                studentRepository.deleteStudent(student);
                String msg = "Student '" + student.getName() + "' deleted successfully!";
                operationResult.postValue(msg);
                studentRepository.insertNotification(new Notification("Student Deleted", msg, System.currentTimeMillis(), false, "SUCCESS"));
                NotificationHelper.sendImportExportNotification(getApplication(), "Student Deleted", msg, "SUCCESS"); // System notification
            } catch (Exception e) {
                String msg = "Failed to delete student: " + e.getMessage();
                operationResult.postValue(msg);
                studentRepository.insertNotification(new Notification("Student Delete Failed", msg, System.currentTimeMillis(), false, "ERROR"));
                NotificationHelper.sendImportExportNotification(getApplication(), "Student Delete Failed", msg, "ERROR"); // System notification
                Log.e(TAG, "Error deleting student", e);
            }
        });
    }

    /**
     * Initiates the import/restore process from a selected JSON file URI.
     * @param uri The URI of the selected JSON backup file.
     */
    public void importAllData(Uri uri) {
        ioExecutor.execute(() -> {
            String notificationTitle = "Data Restore";
            String notificationType = "SUCCESS";
            StringBuilder resultMessage = new StringBuilder();

            try (InputStream inputStream = getApplication().getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                if (inputStream == null) {
                    resultMessage.append("Failed to open selected file for restore.");
                    notificationType = "ERROR";
                    Log.e(TAG, "Failed to open selected file for restore. URI: " + uri);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                    String jsonString = stringBuilder.toString();

                    if (jsonString.isEmpty()) {
                        resultMessage.append("Selected file is empty.");
                        notificationType = "ERROR";
                        Log.e(TAG, "Selected JSON file is empty.");
                    } else {
                        AppDatabase db = AppDatabase.getDatabase(getApplication());
                        AppDatabase.ImportResult importResult = db.importDatabaseFromJson(jsonString);

                        if (importResult.errorMessage != null) {
                            resultMessage.append("Restore failed: ").append(importResult.errorMessage);
                            notificationType = "ERROR";
                            Log.e(TAG, "Database restore failed: " + importResult.errorMessage);
                        } else {
                            resultMessage.append("Restore complete! ")
                                    .append(importResult.importedStudentCount).append(" students and ")
                                    .append(importResult.importedAttendanceCount).append(" attendance records restored.");

                            if (!importResult.skippedStudents.isEmpty()) {
                                resultMessage.append("\nSkipped students (").append(importResult.skippedStudents.size()).append("): ")
                                        .append(String.join(", ", importResult.skippedStudents));
                                notificationType = "WARNING";
                            }
                            if (!importResult.skippedAttendance.isEmpty()) {
                                resultMessage.append("\nSkipped attendance (").append(importResult.skippedAttendance.size()).append("): ")
                                        .append(String.join(", ", importResult.skippedAttendance));
                                notificationType = "WARNING";
                            }
                            resultMessage.append("\nRestart the app for changes to take full effect.");
                        }
                    }
                }
            } catch (Exception e) {
                resultMessage.append("Error restoring data: ").append(e.getMessage());
                notificationType = "ERROR";
                Log.e(TAG, "Error restoring data from JSON", e);
            }

            operationResult.postValue(resultMessage.toString());
            studentRepository.insertNotification(new Notification(notificationTitle, resultMessage.toString(), System.currentTimeMillis(), false, notificationType));
            NotificationHelper.sendImportExportNotification(getApplication(), notificationTitle, resultMessage.toString(), notificationType);
        });
    }

    /**
     * Initiates the export/backup process to a JSON file URI.
     * Replaces previous Excel export that expected a File.
     * @param outputUri The URI where the JSON backup will be saved.
     */
    public void exportAllData(Uri outputUri) { // Changed parameter from File to Uri
        ioExecutor.execute(() -> {
            String notificationTitle = "Data Backup";
            String notificationType = "SUCCESS";
            StringBuilder resultMessage = new StringBuilder();

            try {
                AppDatabase db = AppDatabase.getDatabase(getApplication());
                String jsonString = db.exportDatabaseToJson();

                if (jsonString == null || jsonString.isEmpty()) {
                    resultMessage.append("No data to export or export failed.");
                    notificationType = "WARNING";
                    Log.w(TAG, "Exported JSON string is null or empty.");
                } else {
                    // Use ContentResolver to open OutputStream for the given Uri
                    try (OutputStream outputStream = getApplication().getContentResolver().openOutputStream(outputUri);
                         Writer writer = new OutputStreamWriter(outputStream)) {
                        writer.write(jsonString);
                        resultMessage.append("Database backed up to: ").append(outputUri.getPath()); // Use getPath() for display
                        notificationType = "SUCCESS";
                        Log.d(TAG, "Database backup successful.");
                    }
                }
            } catch (Exception e) {
                resultMessage.append("Error backing up data: ").append(e.getMessage());
                notificationType = "ERROR";
                Log.e(TAG, "Error backing up data to JSON", e);
            }

            operationResult.postValue(resultMessage.toString());
            studentRepository.insertNotification(new Notification(notificationTitle, resultMessage.toString(), System.currentTimeMillis(), false, notificationType));
            NotificationHelper.sendImportExportNotification(getApplication(), notificationTitle, resultMessage.toString(), notificationType);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ioExecutor.shutdown();
        Log.d(TAG, "ioExecutor shutdown initiated in AddStudentViewModel.");
    }
}
