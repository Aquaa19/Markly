package com.aquaa.markly.ui.attendance;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager; // Explicitly imported
import androidx.recyclerview.widget.RecyclerView;

import com.aquaa.markly.R;
import com.aquaa.markly.data.database.Student;
import com.aquaa.markly.data.database.Attendance; // Ensure Attendance is imported if needed
import com.aquaa.markly.ui.attendance.AttendanceViewModel.StudentAttendanceStatus; // Import the nested class

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceActivity extends AppCompatActivity { // Removed `implements StudentAttendanceAdapter.OnAttendanceChangeListener` as it's not used in current logic

    private AttendanceViewModel attendanceViewModel;
    private Spinner semesterSpinner;
    private RecyclerView studentsRecyclerView;
    private Button saveAttendanceButton; // Changed from submitAttendanceButton
    private Button chooseAttendanceDateButton; // New button for date selection
    private TextView selectedAttendanceDateTextView; // New TextView to display selected date, replaces currentDateTextView

    private StudentAttendanceAdapter adapter;

    private int selectedSemester = -1;
    private Long selectedAttendanceDateMillis = null; // Stores the selected attendance date in milliseconds

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()); // Corrected format string

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        // Initialize ViewModel
        attendanceViewModel = new ViewModelProvider(this).get(AttendanceViewModel.class);

        // Initialize UI components
        semesterSpinner = findViewById(R.id.spinner_semester_attendance);
        studentsRecyclerView = findViewById(R.id.recycler_view_students_attendance);
        saveAttendanceButton = findViewById(R.id.button_save_attendance); // Changed ID
        chooseAttendanceDateButton = findViewById(R.id.button_choose_attendance_date); // New button
        selectedAttendanceDateTextView = findViewById(R.id.text_view_selected_attendance_date); // New TextView

        // Setup RecyclerView Adapter
        adapter = new StudentAttendanceAdapter(this); // Pass context to adapter
        studentsRecyclerView.setLayoutManager(new LinearLayoutManager(this)); // Set layout manager
        studentsRecyclerView.setAdapter(adapter);

        // Set default date to today's date on creation
        setDefaultAttendanceDateToToday();


        // Observe LiveData for available semesters
        attendanceViewModel.getAllSemesters().observe(this, semesters -> {
            List<String> spinnerItems = new ArrayList<>();
            spinnerItems.add("Select Semester"); // Hint
            if (semesters != null) {
                for (Integer sem : semesters) {
                    spinnerItems.add(String.valueOf(sem));
                }
            }

            ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, spinnerItems);
            semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            semesterSpinner.setAdapter(semesterAdapter);
        });

        // Observe LiveData for students with their attendance status
        attendanceViewModel.getStudentsWithAttendanceStatus().observe(this, studentsWithStatus -> {
            adapter.submitList(studentsWithStatus); // Use submitList for ListAdapter
            if (studentsWithStatus == null || studentsWithStatus.isEmpty()) {
                if (selectedSemester != -1 && selectedAttendanceDateMillis != null) {
                    Toast.makeText(this, "No students found for this semester or date.", Toast.LENGTH_SHORT).show();
                } else if (selectedAttendanceDateMillis == null) {
                    Toast.makeText(this, "Please select an attendance date.", Toast.LENGTH_SHORT).show();
                } else if (selectedSemester == -1) {
                    Toast.makeText(this, "Please select a semester.", Toast.LENGTH_SHORT).show();
                }
            }
        });


        // Spinner OnItemSelectedListener
        semesterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Avoid "Select Semester" item
                    selectedSemester = Integer.parseInt(parent.getItemAtPosition(position).toString());
                } else {
                    selectedSemester = -1; // No semester selected
                }
                // Reload students when semester changes, if a date is already selected
                loadStudentsForSelectedDateAndSemester();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Choose Date Button Listener
        chooseAttendanceDateButton.setOnClickListener(v -> showDatePicker());

        // Save Attendance Button Listener
        saveAttendanceButton.setOnClickListener(v -> handleSaveAttendance()); // Changed method name

        // Load all semesters when activity starts
        attendanceViewModel.loadAllSemesters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh students and their attendance status when activity resumes
        loadStudentsForSelectedDateAndSemester();
    }

    /**
     * Sets the selected attendance date to today's date and updates the TextView.
     */
    private void setDefaultAttendanceDateToToday() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        selectedAttendanceDateMillis = today.getTimeInMillis();
        selectedAttendanceDateTextView.setText(dateFormatter.format(new Date(selectedAttendanceDateMillis)));
    }


    /**
     * Shows a DatePickerDialog to allow the user to choose an attendance date.
     */
    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        if (selectedAttendanceDateMillis != null) {
            c.setTimeInMillis(selectedAttendanceDateMillis); // Set picker to previously selected date
        }
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, 0);
                    selectedCalendar.set(Calendar.MINUTE, 0);
                    selectedCalendar.set(Calendar.SECOND, 0);
                    selectedCalendar.set(Calendar.MILLISECOND, 0);

                    selectedAttendanceDateMillis = selectedCalendar.getTimeInMillis();
                    selectedAttendanceDateTextView.setText(dateFormatter.format(new Date(selectedAttendanceDateMillis)));

                    // Load students for the newly selected date and current semester
                    loadStudentsForSelectedDateAndSemester();
                }, year, month, day);
        datePickerDialog.show();
    }

    /**
     * Loads students based on the currently selected attendance date and semester.
     * Only proceeds if both a date and a semester are selected.
     */
    private void loadStudentsForSelectedDateAndSemester() {
        if (selectedAttendanceDateMillis != null && selectedSemester != -1) {
            attendanceViewModel.loadStudentsForSemesterAndDate(selectedSemester, selectedAttendanceDateMillis);
        } else if (selectedAttendanceDateMillis == null) {
            adapter.submitList(new ArrayList<>()); // Clear list if no date selected
            Toast.makeText(this, "Please select an attendance date.", Toast.LENGTH_SHORT).show();
        } else { // selectedSemester == -1
            adapter.submitList(new ArrayList<>()); // Clear list if no semester selected
            Toast.makeText(this, "Please select a semester.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles saving the attendance for selected students.
     */
    private void handleSaveAttendance() { // Changed method name
        if (selectedAttendanceDateMillis == null || selectedSemester == -1) {
            Toast.makeText(this, "Please select both a date and a semester first.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the current list of students with their attendance status from the adapter
        Map<Long, Boolean> attendanceStatusMap = adapter.getAttendanceStatusMap();

        if (attendanceStatusMap.isEmpty()) {
            Toast.makeText(this, "No students to save attendance for.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Attendance Save")
                .setMessage("Are you sure you want to save attendance for " + dateFormatter.format(new Date(selectedAttendanceDateMillis)) + "?")
                .setPositiveButton("Yes, Save", (dialog, which) -> {
                    attendanceViewModel.saveAttendanceForDate(selectedAttendanceDateMillis, attendanceStatusMap);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
