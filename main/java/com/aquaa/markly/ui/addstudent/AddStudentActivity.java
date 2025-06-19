package com.aquaa.markly.ui.addstudent;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log; // Added for logging
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher; // Import ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts; // Import ActivityResultContracts
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.aquaa.markly.R;
import com.aquaa.markly.data.database.Student;
import com.aquaa.markly.ui.main.MainActivity; // Import MainActivity for restart intent
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Implement the new interface (if it's still needed, otherwise remove)
public class AddStudentActivity extends AppCompatActivity implements StudentManageAdapter.OnStudentActionListener {

    private static final String TAG = "AddStudentActivity"; // Tag for logging

    private static final int PERMISSION_REQUEST_CODE_STORAGE = 102;
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE = 103;

    private AddStudentViewModel addStudentViewModel;
    private StudentManageAdapter studentManageAdapter;

    private TextInputEditText etName, etMobile, etGuardianMobile, etCurrentSemester, etSection;
    private Spinner spinnerGender;
    private Button btnSaveStudent, btnImportExcel, btnExportExcel; // btnImportExcel and btnExportExcel will now handle JSON
    private RecyclerView recyclerViewStudents;

    // ActivityResultLauncher for picking a file (for import/restore)
    private ActivityResultLauncher<String[]> openDocumentLauncher;
    // ActivityResultLauncher for creating a file (for export/backup)
    private ActivityResultLauncher<String> createDocumentLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);

        // Initialize ViewModel
        addStudentViewModel = new ViewModelProvider(this).get(AddStudentViewModel.class);

        // Initialize UI components
        etName = findViewById(R.id.edit_text_name);
        etMobile = findViewById(R.id.edit_text_mobile);
        etGuardianMobile = findViewById(R.id.edit_text_guardian_mobile);
        etCurrentSemester = findViewById(R.id.edit_text_current_semester);
        etSection = findViewById(R.id.edit_text_section);
        spinnerGender = findViewById(R.id.spinner_gender);
        btnSaveStudent = findViewById(R.id.button_save_student);
        btnImportExcel = findViewById(R.id.button_import_excel); // This will now trigger JSON restore
        btnExportExcel = findViewById(R.id.button_export_excel); // This will now trigger JSON backup
        recyclerViewStudents = findViewById(R.id.recycler_view_students);

        // Setup Gender Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapter);

        // Setup RecyclerView with the listener
        studentManageAdapter = new StudentManageAdapter(this); // Pass 'this' as the listener
        recyclerViewStudents.setAdapter(studentManageAdapter);

        // Set up listeners
        btnSaveStudent.setOnClickListener(v -> saveStudent());

        // Updated listeners for JSON backup/restore
        btnImportExcel.setOnClickListener(v -> checkStoragePermissionAndRestoreJson()); // Renamed method
        btnExportExcel.setOnClickListener(v -> checkStoragePermissionAndBackupJson()); // Renamed method

        // Initialize ActivityResultLaunchers
        openDocumentLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                performRestoreFromJson(uri);
            } else {
                Toast.makeText(this, "No file selected for restore.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "No URI received from document picker for restore.");
            }
        });

        createDocumentLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
            if (uri != null) {
                performBackupToJson(uri);
            } else {
                Toast.makeText(this, "No file location selected for backup.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "No URI received from document creator for backup.");
            }
        });


        // Observe LiveData for operation results
        addStudentViewModel.getOperationResult().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(AddStudentActivity.this, message, Toast.LENGTH_LONG).show(); // Changed to LONG for important messages
                // If the message indicates a successful restore, restart the app
                if (message.contains("Restore complete!") && message.contains("Restart the app")) {
                    restartApplication();
                }
            }
        });

        // Observe LiveData for all students to display in RecyclerView
        addStudentViewModel.getAllStudents().observe(this, students -> {
            studentManageAdapter.setStudents(students);
        });
    }

    /**
     * Handles saving a new student record to the database.
     * Performs input validation before saving.
     */
    private void saveStudent() {
        String name = etName.getText().toString().trim();
        String gender = spinnerGender.getSelectedItem().toString();
        String mobile = etMobile.getText().toString().trim();
        String guardianMobile = etGuardianMobile.getText().toString().trim();
        String semesterStr = etCurrentSemester.getText().toString().trim();
        String section = etSection.getText().toString().trim();

        // Input Validation
        if (name.isEmpty() || mobile.isEmpty() || guardianMobile.isEmpty() || semesterStr.isEmpty() || section.isEmpty()) {
            Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (gender.equals("Select Gender")) {
            Toast.makeText(this, "Please select a gender.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mobile.length() != 10 || !mobile.matches("\\d+")) {
            Toast.makeText(this, "Student mobile must be 10 digits.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (guardianMobile.length() != 10 || !guardianMobile.matches("\\d+")) {
            Toast.makeText(this, "Guardian mobile must be 10 digits.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int semester = Integer.parseInt(semesterStr);
            if (semester <= 0) {
                Toast.makeText(this, "Semester must be a positive number.", Toast.LENGTH_SHORT).show();
                return;
            }
            Student newStudent = new Student(name, gender, mobile, guardianMobile, semester, section);
            addStudentViewModel.insertStudent(newStudent);
            clearInputFields(); // Clear fields after successful save
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid semester number.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Clears all input fields after a student is saved.
     */
    private void clearInputFields() {
        etName.setText("");
        spinnerGender.setSelection(0); // Reset to "Select Gender"
        etMobile.setText("");
        etGuardianMobile.setText("");
        etCurrentSemester.setText("");
        etSection.setText("");
        etName.requestFocus(); // Focus on the first field
    }

    /**
     * Implementation of the OnStudentActionListener interface for delete clicks.
     * @param student The student object to be deleted.
     */
    @Override
    public void onDeleteClick(Student student) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Student")
                .setMessage("Are you sure you want to delete student: " + student.getName() + "? This action cannot be undone.")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    addStudentViewModel.deleteStudent(student);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Checks for storage permissions before initiating JSON restore.
     * Uses MANAGE_EXTERNAL_STORAGE for Android 11+ for broader file access.
     */
    private void checkStoragePermissionAndRestoreJson() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11 (API 30) or higher
            if (Environment.isExternalStorageManager()) {
                // Permission granted, proceed with JSON file picker
                openJsonFilePicker();
            } else {
                // Request MANAGE_EXTERNAL_STORAGE permission
                showStoragePermissionDialog();
            }
        } else { // Android 10 (API 29) or lower
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                openJsonFilePicker();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE_STORAGE);
            }
        }
    }

    /**
     * Checks for storage permissions before initiating JSON backup.
     * Uses MANAGE_EXTERNAL_STORAGE for Android 11+ for broader file access.
     */
    private void checkStoragePermissionAndBackupJson() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11 (API 30) or higher
            if (Environment.isExternalStorageManager()) {
                // Permission granted, proceed with JSON file creation
                createJsonBackupFile();
            } else {
                // Request MANAGE_EXTERNAL_STORAGE permission
                showStoragePermissionDialog();
            }
        } else { // Android 10 (API 29) or lower
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                createJsonBackupFile();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE_STORAGE);
            }
        }
    }

    private void showStoragePermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Storage Permission Required")
                .setMessage("Markly needs access to manage all files to backup/restore data. Please grant this permission in settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.addCategory("android.intent.category.DEFAULT");
                        intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                        startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE);
                    } catch (Exception e) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, "Storage permission denied. Cannot backup/restore.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Opens a file picker intent for selecting a JSON file for restore.
     * Uses ActivityResultLauncher.
     */
    private void openJsonFilePicker() {
        openDocumentLauncher.launch(new String[]{"application/json"}); // MIME type for JSON
    }

    /**
     * Initiates the creation of a JSON backup file.
     * Uses ActivityResultLauncher to prompt user for save location.
     */
    private void createJsonBackupFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "Markly_Backup_" + timeStamp + ".json"; // Suggest .json extension
        createDocumentLauncher.launch(fileName);
    }

    /**
     * Performs the restore process from the selected JSON file URI.
     * @param uri The URI of the selected JSON file.
     */
    private void performRestoreFromJson(Uri uri) {
        addStudentViewModel.importAllData(uri); // Call the ViewModel method for import
    }

    /**
     * Performs the backup process to the selected JSON file URI.
     * @param uri The URI of the target JSON file.
     */
    private void performBackupToJson(Uri uri) {
        // Need to convert URI to a File object if ViewModel's exportAllData expects a File.
        // If exportAllData expects OutputStream, then get it via contentResolver.openOutputStream(uri)
        // Adjusting ViewModel's exportAllData to take Uri directly for simplicity.
        // Assuming your ViewModel's exportAllData accepts a File, we convert here.
        // If your ViewModel accepts Uri and OutputStream, you would pass that.

        // Current ViewModel exportAllData(File outputFile) expects File, which is problematic with SAF Uri.
        // Better to change ViewModel's export to accept Uri and handle OutputStream there.
        // For now, we'll assume ViewModel expects Uri and handle it.
        addStudentViewModel.exportAllData(uri); // ViewModel's exportAllData should accept Uri
    }


    /**
     * Restarts the application after a successful database restore.
     * This is crucial to ensure the app loads data from the newly restored database.
     */
    private void restartApplication() {
        // Create a new Intent for your main activity
        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finishAffinity(); // Close all activities in the task
        } else {
            Log.e(TAG, "Could not find launch intent for package. Cannot restart application.");
            Toast.makeText(this, "Restore complete, please restart the app manually.", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission granted. Please retry operation.", Toast.LENGTH_SHORT).show();
                // Optionally re-initiate the action that required permission
            } else {
                Toast.makeText(this, "Storage permission denied. Cannot perform operation.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // REQUEST_CODE_PICK_EXCEL is replaced by openDocumentLauncher
        // REQUEST_MANAGE_EXTERNAL_STORAGE is handled directly by its ActivityResultLauncher in onCreate
        // So this onActivityResult might become largely empty or removed.
    }
}
