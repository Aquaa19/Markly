package com.aquaa.markly.ui.attendance;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aquaa.markly.R;
import com.aquaa.markly.ui.attendance.TrackAttendanceViewModel.AttendanceRecordDisplay;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TrackAttendanceActivity extends AppCompatActivity {

    private TrackAttendanceViewModel mViewModel;
    private Spinner semesterSpinner;
    private RecyclerView recyclerView;
    private TrackAttendanceAdapter adapter;
    private TextView totalStudentsText;
    private TextView avgAttendanceText;
    private Button chooseDateButton;
    private TextView selectedDateTextView;
    private Button viewAnalyticsButton;

    private Calendar selectedCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_attendance);

        // Initialize ViewModel
        mViewModel = new ViewModelProvider(this).get(TrackAttendanceViewModel.class);

        // Initialize UI components
        chooseDateButton = findViewById(R.id.button_choose_date);
        selectedDateTextView = findViewById(R.id.selected_date_text_view);
        semesterSpinner = findViewById(R.id.semester_spinner);
        recyclerView = findViewById(R.id.attendance_recycler_view);
        totalStudentsText = findViewById(R.id.total_students_text);
        avgAttendanceText = findViewById(R.id.avg_attendance_text);
        viewAnalyticsButton = findViewById(R.id.button_view_analytics);

        // Set initial date display and update ViewModel's month/year
        updateSelectedDateTextView();
        mViewModel.setSelectedMonth(selectedCalendar.get(Calendar.MONTH));
        mViewModel.setSelectedYear(selectedCalendar.get(Calendar.YEAR));


        // Setup RecyclerView
        adapter = new TrackAttendanceAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Set listener for Choose Date button
        chooseDateButton.setOnClickListener(v -> showDatePickerDialog());

        // Setup Semester Spinner
        mViewModel.getAllAvailableSemesters().observe(this, semesters -> {
            List<String> semesterOptions = new ArrayList<>();
            semesterOptions.add("All Semesters");
            if (semesters != null) {
                Collections.sort(semesters);
                for (int semester : semesters) {
                    semesterOptions.add(String.valueOf(semester));
                }
            }
            ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, semesterOptions);
            semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            semesterSpinner.setAdapter(semesterAdapter);

            mViewModel.getSelectedSemester().observe(this, selectedSem -> {
                int defaultSelectionIndex;
                if (selectedSem == 0) {
                    defaultSelectionIndex = 0;
                } else {
                    defaultSelectionIndex = semesterOptions.indexOf(String.valueOf(selectedSem));
                }

                if (defaultSelectionIndex != -1) {
                    semesterSpinner.setSelection(defaultSelectionIndex);
                } else {
                    semesterSpinner.setSelection(0);
                }
            });
        });

        // Set listener for semester spinner to update ViewModel
        semesterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    mViewModel.setSelectedSemester(0);
                } else {
                    mViewModel.setSelectedSemester(Integer.parseInt(parent.getItemAtPosition(position).toString()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Observe attendance records from ViewModel
        mViewModel.getMonthlyAttendanceRecords().observe(this, attendanceRecords -> {
            adapter.submitList(attendanceRecords);
            updateSummary(attendanceRecords);
        });

        // Set OnClickListener for the new "View Analytics" button
        viewAnalyticsButton.setOnClickListener(v -> {
            Intent intent = new Intent(TrackAttendanceActivity.this, AttendanceAnalyticsActivity.class);
            intent.putExtra("selectedMonth", mViewModel.getSelectedMonth().getValue());
            intent.putExtra("selectedYear", mViewModel.getSelectedYear().getValue());
            intent.putExtra("selectedDay", selectedCalendar.get(Calendar.DAY_OF_MONTH)); // Pass the selected day
            intent.putExtra("selectedSemester", mViewModel.getSelectedSemester().getValue());

            if (mViewModel.getMonthlyAttendanceRecords().getValue() != null) {
                intent.putExtra("attendanceRecords", new ArrayList<>(mViewModel.getMonthlyAttendanceRecords().getValue()));
            } else {
                intent.putExtra("attendanceRecords", new ArrayList<AttendanceRecordDisplay>());
            }
            startActivity(intent);
        });
    }

    private void showDatePickerDialog() {
        new DatePickerDialog(this, (view, year, monthOfYear, dayOfMonth) -> {
            selectedCalendar.set(Calendar.YEAR, year);
            selectedCalendar.set(Calendar.MONTH, monthOfYear);
            selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            selectedCalendar.set(Calendar.HOUR_OF_DAY, 0);
            selectedCalendar.set(Calendar.MINUTE, 0);
            selectedCalendar.set(Calendar.SECOND, 0);
            selectedCalendar.set(Calendar.MILLISECOND, 0);

            mViewModel.setSelectedMonth(monthOfYear);
            mViewModel.setSelectedYear(year);

            // Trigger attendance update based on new date selected
            // mViewModel.updateMonthlyAttendanceRecords() is private, ViewModel observes changes to selectedMonth/Year
            // so simply setting these values will trigger the update.

            updateSelectedDateTextView();
        }, selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateSelectedDateTextView() {
        // Format to display Day Month Year (e.g., "5 June 2025")
        // FIX: Enclose literal text 'Westen' in single quotes
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM 'Westen'", Locale.getDefault());
        selectedDateTextView.setText(String.format(Locale.getDefault(), "Date: %s", sdf.format(selectedCalendar.getTime())));
    }

    private void updateSummary(List<AttendanceRecordDisplay> records) {
        totalStudentsText.setText(String.format(Locale.getDefault(), "Total Students Tracked: %d", records.size()));

        double totalPercentage = 0;
        int studentsWithAttendance = 0;
        for (AttendanceRecordDisplay record : records) {
            if (record.getTotalDays() > 0) {
                totalPercentage += record.getAttendancePercentage();
                studentsWithAttendance++;
            }
        }

        if (studentsWithAttendance > 0) {
            double averagePercentage = totalPercentage / studentsWithAttendance;
            avgAttendanceText.setText(String.format(Locale.getDefault(), "Average Attendance: %.2f%%", averagePercentage));
        } else {
            avgAttendanceText.setText("Average Attendance: N/A");
        }
    }
}
