// PromoteStudentActivity.java
package com.aquaa.markly.ui.promotestudent;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.aquaa.markly.R;
import com.aquaa.markly.data.database.Student;

import java.util.ArrayList;
import java.util.List;

public class PromoteStudentActivity extends AppCompatActivity {

    private PromoteStudentViewModel promoteStudentViewModel;
    private Spinner semesterSpinner;
    private RecyclerView studentsRecyclerView;
    private Button promoteSelectedButton;
    private StudentPromoteAdapter adapter;

    private int selectedSemester = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promote_student);

        // Initialize ViewModel
        promoteStudentViewModel = new ViewModelProvider(this).get(PromoteStudentViewModel.class);

        // Initialize UI components
        semesterSpinner = findViewById(R.id.spinner_semester_promote_from);
        studentsRecyclerView = findViewById(R.id.recycler_view_students_promote);
        promoteSelectedButton = findViewById(R.id.button_promote_selected_students);

        // Setup RecyclerView Adapter
        adapter = new StudentPromoteAdapter();
        studentsRecyclerView.setAdapter(adapter);

        // Observe LiveData for available semesters
        promoteStudentViewModel.getAllSemesters().observe(this, semesters -> {
            List<String> spinnerItems = new ArrayList<>();
            spinnerItems.add("Select Semester"); // Hint
            for (Integer sem : semesters) {
                spinnerItems.add(String.valueOf(sem));
            }

            ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, spinnerItems);
            semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            semesterSpinner.setAdapter(semesterAdapter);

            // If there's only one semester, select it automatically
            if (semesters != null && semesters.size() == 1) {
                semesterSpinner.setSelection(1); // Select the first actual semester
            }
        });

        // Observe LiveData for students to display
        promoteStudentViewModel.getStudentsToPromote().observe(this, students -> {
            adapter.setStudents(students);
            if (students == null || students.isEmpty()) {
                Toast.makeText(this, "No students found for this semester.", Toast.LENGTH_SHORT).show();
            }
        });

        // Observe LiveData for promotion result
        promoteStudentViewModel.getPromotionResult().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(PromoteStudentActivity.this, message, Toast.LENGTH_LONG).show();
                // After promotion, reload students for the current semester to reflect changes
                if (selectedSemester != -1) {
                    promoteStudentViewModel.loadStudentsForSemester(selectedSemester);
                }
            }
        });

        // Spinner OnItemSelectedListener
        semesterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Avoid "Select Semester" item
                    selectedSemester = Integer.parseInt(parent.getItemAtPosition(position).toString());
                    promoteStudentViewModel.loadStudentsForSemester(selectedSemester);
                } else {
                    selectedSemester = -1; // No semester selected
                    adapter.setStudents(new ArrayList<>()); // Clear student list
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Promote Selected Button Listener
        promoteSelectedButton.setOnClickListener(v -> handlePromoteStudents());

        // Load all semesters when activity starts
        promoteStudentViewModel.loadAllSemesters();
    }

    /**
     * Handles the promotion of selected students to the next semester.
     */
    private void handlePromoteStudents() {
        List<Student> studentsToPromote = adapter.getSelectedStudents();

        if (studentsToPromote.isEmpty()) {
            Toast.makeText(this, "Please select students to promote.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Student Promotion")
                .setMessage("Are you sure you want to promote " + studentsToPromote.size() + " selected student(s) to the next semester?")
                .setPositiveButton("Yes, Promote", (dialog, which) -> {
                    promoteStudentViewModel.promoteStudents(studentsToPromote);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
