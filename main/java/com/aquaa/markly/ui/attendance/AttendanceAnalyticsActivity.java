package com.aquaa.markly.ui.attendance;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aquaa.markly.R;
import com.aquaa.markly.ui.attendance.AttendanceAnalyticsViewModel.AttendanceSummary;
import com.aquaa.markly.ui.attendance.TrackAttendanceViewModel.AttendanceRecordDisplay;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceAnalyticsActivity extends AppCompatActivity {

    private AttendanceAnalyticsViewModel mViewModel;
    private PieChart attendancePieChart;
    private Button analyticsPeriodButton;
    private TextView totalStudentsAnalyticsText;
    private TextView totalPresentDaysText;
    private TextView totalAbsentDaysText;
    private TextView totalRecordedDaysText;
    private RecyclerView topBottomStudentsRecyclerView;
    private TopBottomStudentAdapter topBottomStudentAdapter;

    private RadioGroup periodSelectionRadioGroup;
    private Spinner semesterSpinnerAnalytics;
    private ImageButton prevPeriodButton;
    private ImageButton nextPeriodButton;

    private Calendar currentPeriodCalendar;
    private int currentSelectionMode = PERIOD_MODE_MONTH;
    private int currentSelectedSemester = 0;

    private static final int PERIOD_MODE_DAY = 0;
    private static final int PERIOD_MODE_MONTH = 1;
    private static final int PERIOD_MODE_YEAR = 2;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_analytics);

        mViewModel = new ViewModelProvider(this).get(AttendanceAnalyticsViewModel.class);

        analyticsPeriodButton = findViewById(R.id.button_select_period_date);
        totalStudentsAnalyticsText = findViewById(R.id.total_students_analytics_text);
        totalPresentDaysText = findViewById(R.id.total_present_days_text);
        totalAbsentDaysText = findViewById(R.id.total_absent_days_text);
        totalRecordedDaysText = findViewById(R.id.total_recorded_days_text);
        attendancePieChart = findViewById(R.id.attendance_pie_chart);
        topBottomStudentsRecyclerView = findViewById(R.id.top_bottom_students_recycler_view);

        periodSelectionRadioGroup = findViewById(R.id.radio_group_period_selection);
        semesterSpinnerAnalytics = findViewById(R.id.spinner_semester_analytics);
        prevPeriodButton = findViewById(R.id.button_prev_period);
        nextPeriodButton = findViewById(R.id.button_next_period);

        topBottomStudentAdapter = new TopBottomStudentAdapter();
        topBottomStudentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        topBottomStudentsRecyclerView.setAdapter(topBottomStudentAdapter);
        topBottomStudentsRecyclerView.setNestedScrollingEnabled(false);

        currentPeriodCalendar = Calendar.getInstance();

        // Get initial data from Intent extras
        int monthFromIntent = getIntent().getIntExtra("selectedMonth", -1);
        int yearFromIntent = getIntent().getIntExtra("selectedYear", -1);
        int dayFromIntent = getIntent().getIntExtra("selectedDay", -1);
        int semesterFromIntent = getIntent().getIntExtra("selectedSemester", 0);

        // FIX: Set initial semester for ViewModel immediately from Intent
        mViewModel.setSelectedSemester(semesterFromIntent);
        currentSelectedSemester = semesterFromIntent; // Update local state


        // FIX: Initialize ViewModel's date parameters immediately from Intent or defaults
        // This ensures they are not null when loadAnalyticsData() is first called.
        if (dayFromIntent != -1 && monthFromIntent != -1 && yearFromIntent != -1) {
            currentPeriodCalendar.set(yearFromIntent, monthFromIntent, dayFromIntent);
            currentSelectionMode = PERIOD_MODE_DAY;
            ((RadioButton) findViewById(R.id.radio_day)).setChecked(true);
            mViewModel.setSelectedDay(dayFromIntent);
            mViewModel.setSelectedMonth(monthFromIntent);
            mViewModel.setSelectedYear(yearFromIntent);
        } else if (monthFromIntent != -1 && yearFromIntent != -1) {
            currentPeriodCalendar.set(yearFromIntent, monthFromIntent, 1);
            currentSelectionMode = PERIOD_MODE_MONTH;
            ((RadioButton) findViewById(R.id.radio_month)).setChecked(true);
            mViewModel.setSelectedMonth(monthFromIntent);
            mViewModel.setSelectedYear(yearFromIntent);
            mViewModel.setSelectedDay(-1); // Ensure day is reset if coming from specific day
        } else if (yearFromIntent != -1) {
            currentPeriodCalendar.set(yearFromIntent, 0, 1);
            currentSelectionMode = PERIOD_MODE_YEAR;
            ((RadioButton) findViewById(R.id.radio_year)).setChecked(true);
            mViewModel.setSelectedYear(yearFromIntent);
            mViewModel.setSelectedMonth(-1); // Ensure month is reset
            mViewModel.setSelectedDay(-1); // Ensure day is reset
        } else {
            // Default to current month/year if no intent data
            currentPeriodCalendar = Calendar.getInstance();
            mViewModel.setSelectedMonth(currentPeriodCalendar.get(Calendar.MONTH));
            mViewModel.setSelectedYear(currentPeriodCalendar.get(Calendar.YEAR));
            mViewModel.setSelectedDay(-1); // Default to -1 (no specific day)
            periodSelectionRadioGroup.check(R.id.radio_month); // This will trigger loadAnalyticsDataFromViewModel
        }


        // Setup Semester Spinner
        mViewModel.getAllSemesters().observe(this, semesters -> {
            List<String> semesterOptions = new ArrayList<>();
            semesterOptions.add("All Semesters");
            if (semesters != null) {
                Collections.sort(semesters);
                for (int sem : semesters) {
                    semesterOptions.add(String.valueOf(sem));
                }
            }
            ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, semesterOptions);
            semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            semesterSpinnerAnalytics.setAdapter(semesterAdapter);

            int defaultSelectionIndex = semesterOptions.indexOf(String.valueOf(currentSelectedSemester));
            if (currentSelectedSemester == 0) {
                semesterSpinnerAnalytics.setSelection(0);
            } else if (defaultSelectionIndex != -1) {
                semesterSpinnerAnalytics.setSelection(defaultSelectionIndex);
            } else {
                semesterSpinnerAnalytics.setSelection(0);
            }
        });

        // Set listener for semester spinner to update ViewModel
        semesterSpinnerAnalytics.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    currentSelectedSemester = 0;
                } else {
                    currentSelectedSemester = Integer.parseInt(parent.getItemAtPosition(position).toString());
                }
                mViewModel.setSelectedSemester(currentSelectedSemester);
                // loadAnalyticsDataFromViewModel() will be triggered by ViewModel's selectedSemester observer if needed
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });


        // Setup radio group listener for period selection
        periodSelectionRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_day) {
                currentSelectionMode = PERIOD_MODE_DAY;
                mViewModel.setSelectedDay(currentPeriodCalendar.get(Calendar.DAY_OF_MONTH));
                mViewModel.setSelectedMonth(currentPeriodCalendar.get(Calendar.MONTH)); // Ensure month/year is also set
                mViewModel.setSelectedYear(currentPeriodCalendar.get(Calendar.YEAR));
            } else if (checkedId == R.id.radio_month) {
                currentSelectionMode = PERIOD_MODE_MONTH;
                mViewModel.setSelectedMonth(currentPeriodCalendar.get(Calendar.MONTH));
                mViewModel.setSelectedYear(currentPeriodCalendar.get(Calendar.YEAR));
                mViewModel.setSelectedDay(-1); // Reset day
            } else if (checkedId == R.id.radio_year) {
                currentSelectionMode = PERIOD_MODE_YEAR;
                mViewModel.setSelectedYear(currentPeriodCalendar.get(Calendar.YEAR));
                mViewModel.setSelectedMonth(-1); // Reset month
                mViewModel.setSelectedDay(-1); // Reset day
            }
            updatePeriodDisplayAndLoad();
        });

        // Set listener for the new date selection button
        analyticsPeriodButton.setOnClickListener(v -> showDatePickerDialog());

        // Initial load for period display and data
        updatePeriodDisplayAndLoad();


        prevPeriodButton.setOnClickListener(v -> navigatePeriod(-1));
        nextPeriodButton.setOnClickListener(v -> navigatePeriod(1));

        mViewModel.getAttendanceSummary().observe(this, summary -> {
            updateAnalyticsDisplay(summary);
            updatePieChart(summary);
        });

        mViewModel.getDetailedStudentAttendance().observe(this, records -> {
            List<AttendanceRecordDisplay> sortedRecords = new ArrayList<>(records);
            Collections.sort(sortedRecords, (o1, o2) -> Double.compare(o2.getAttendancePercentage(), o1.getAttendancePercentage()));

            List<AttendanceRecordDisplay> displayRecords = new ArrayList<>();
            if (sortedRecords.size() > 10) {
                for (int i = 0; i < 5; i++) {
                    displayRecords.add(sortedRecords.get(i));
                }
                int startBottom = Math.max(5, sortedRecords.size() - 5);
                for (int i = startBottom; i < sortedRecords.size(); i++) {
                    displayRecords.add(sortedRecords.get(i));
                }
            } else {
                displayRecords.addAll(sortedRecords);
            }
            topBottomStudentAdapter.submitList(displayRecords);
        });

        setupPieChart();
    }

    /**
     * Consolidates updating period display and loading data.
     */
    private void updatePeriodDisplayAndLoad() {
        updatePeriodButtonText();
        loadAnalyticsDataFromViewModel();
    }

    /**
     * Updates the text of the period selection button based on the current calendar and selection mode.
     */
    private void updatePeriodButtonText() {
        String format;
        if (currentSelectionMode == PERIOD_MODE_DAY) {
            format = "dd MMMM 'Westen'"; // FIX: Enclosed 'Westen' in quotes
        } else if (currentSelectionMode == PERIOD_MODE_MONTH) {
            format = "MMMM 'Westen'"; // FIX: Enclosed 'Westen' in quotes
        } else { // PERIOD_MODE_YEAR
            format = "yyyy";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        analyticsPeriodButton.setText("For: " + sdf.format(currentPeriodCalendar.getTime()));
    }


    /**
     * Shows a DatePickerDialog to allow the user to choose a date when in DAY mode.
     */
    private void showDatePickerDialog() {
        if (currentSelectionMode != PERIOD_MODE_DAY) {
            Toast.makeText(this, "Date selection is only available in 'Day' mode.", Toast.LENGTH_SHORT).show();
            return;
        }

        final Calendar c = Calendar.getInstance();
        c.setTime(currentPeriodCalendar.getTime());

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    currentPeriodCalendar.set(year, monthOfYear, dayOfMonth);
                    mViewModel.setSelectedDay(dayOfMonth); // Update ViewModel's day
                    mViewModel.setSelectedMonth(monthOfYear); // Update ViewModel's month
                    mViewModel.setSelectedYear(year); // Update ViewModel's year
                    updatePeriodDisplayAndLoad();
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }


    /**
     * Navigates the period (day, month, or year) backward or forward.
     * @param direction -1 for previous, 1 for next.
     */
    private void navigatePeriod(int direction) {
        if (currentSelectionMode == PERIOD_MODE_DAY) {
            currentPeriodCalendar.add(Calendar.DAY_OF_MONTH, direction);
            mViewModel.setSelectedDay(currentPeriodCalendar.get(Calendar.DAY_OF_MONTH));
            mViewModel.setSelectedMonth(currentPeriodCalendar.get(Calendar.MONTH));
            mViewModel.setSelectedYear(currentPeriodCalendar.get(Calendar.YEAR));
        } else if (currentSelectionMode == PERIOD_MODE_MONTH) {
            currentPeriodCalendar.add(Calendar.MONTH, direction);
            mViewModel.setSelectedMonth(currentPeriodCalendar.get(Calendar.MONTH));
            mViewModel.setSelectedYear(currentPeriodCalendar.get(Calendar.YEAR));
            mViewModel.setSelectedDay(-1); // Reset day
        } else if (currentSelectionMode == PERIOD_MODE_YEAR) {
            currentPeriodCalendar.add(Calendar.YEAR, direction);
            mViewModel.setSelectedYear(currentPeriodCalendar.get(Calendar.YEAR));
            mViewModel.setSelectedMonth(-1); // Reset month
            mViewModel.setSelectedDay(-1); // Reset day
        }
        updatePeriodDisplayAndLoad();
    }

    /**
     * Triggers data loading in the ViewModel based on the current selection mode and date.
     */
    private void loadAnalyticsDataFromViewModel() {
        int month = currentPeriodCalendar.get(Calendar.MONTH);
        int year = currentPeriodCalendar.get(Calendar.YEAR);
        int day = currentPeriodCalendar.get(Calendar.DAY_OF_MONTH);

        int viewModelDay = (currentSelectionMode == PERIOD_MODE_DAY) ? day : -1;
        int viewModelMonth = (currentSelectionMode == PERIOD_MODE_YEAR) ? -1 : month;

        // Use currentSelectedSemester (local state) which is updated by spinner listener
        mViewModel.setSelectionParameters(viewModelMonth, year, viewModelDay, currentSelectedSemester, null);
    }


    private void updateAnalyticsDisplay(AttendanceSummary summary) {
        totalStudentsAnalyticsText.setText(String.format(Locale.getDefault(), "Total Students: %d", summary.getTotalStudents()));
        totalPresentDaysText.setText(String.format(Locale.getDefault(), "Total Present Days: %d (%.2f%%)", summary.getTotalPresentDays(), summary.getPresentPercentage()));
        totalAbsentDaysText.setText(String.format(Locale.getDefault(), "Total Absent Days: %d (%.2f%%)", summary.getTotalAbsentDays(), summary.getAbsentPercentage()));
        totalRecordedDaysText.setText(String.format(Locale.getDefault(), "Total Recorded Attendance Days: %d", summary.getTotalRecordedDays()));
    }


    private void setupPieChart() {
        attendancePieChart.setUsePercentValues(true);
        attendancePieChart.getDescription().setEnabled(false);
        attendancePieChart.setExtraOffsets(5f, 10f, 5f, 5f);

        attendancePieChart.setDragDecelerationFrictionCoef(0.95f);

        attendancePieChart.setDrawHoleEnabled(true);
        attendancePieChart.setHoleColor(Color.TRANSPARENT);
        attendancePieChart.setTransparentCircleColor(Color.WHITE);
        attendancePieChart.setTransparentCircleAlpha(110);
        attendancePieChart.setHoleRadius(58f);
        attendancePieChart.setTransparentCircleRadius(61f);

        attendancePieChart.setDrawCenterText(true);
        attendancePieChart.setRotationAngle(0);
        attendancePieChart.setRotationEnabled(true);
        attendancePieChart.setHighlightPerTapEnabled(true);
        attendancePieChart.animateY(1400, Easing.EaseInOutQuad);

        attendancePieChart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP);
        attendancePieChart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT);
        attendancePieChart.getLegend().setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.VERTICAL);
        attendancePieChart.getLegend().setDrawInside(false);
        attendancePieChart.getLegend().setXEntrySpace(7f);
        attendancePieChart.getLegend().setYEntrySpace(0f);
        attendancePieChart.getLegend().setYOffset(0f);

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
        int onSurfaceColor = typedValue.data;

        attendancePieChart.getLegend().setTextColor(onSurfaceColor);
        attendancePieChart.setEntryLabelColor(onSurfaceColor);
        attendancePieChart.setEntryLabelTextSize(12f);
    }

    private void updatePieChart(AttendanceSummary summary) {
        ArrayList<PieEntry> entries = new ArrayList<>();

        if (summary.getTotalPresentDays() == 0 && summary.getTotalAbsentDays() == 0) {
            attendancePieChart.clear();
            attendancePieChart.setNoDataText("No attendance data for this period.");
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true);
            int onSurfaceVariantColor = typedValue.data;
            attendancePieChart.setNoDataTextColor(onSurfaceVariantColor);
            attendancePieChart.invalidate();
            return;
        }

        if (summary.getPresentPercentage() > 0) {
            entries.add(new PieEntry((float) summary.getPresentPercentage(), "Present"));
        }
        if (summary.getAbsentPercentage() > 0) {
            entries.add(new PieEntry((float) summary.getAbsentPercentage(), "Absent"));
        }


        PieDataSet dataSet = new PieDataSet(entries, "Attendance Breakdown");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(getResources().getColor(R.color.md_theme_light_secondary, getTheme()));
        colors.add(getResources().getColor(R.color.md_theme_light_error, getTheme()));

        dataSet.setColors(colors);

        dataSet.setValueLinePart1OffsetPercentage(80.f);
        dataSet.setValueLinePart1Length(0.2f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueFormatter(new PercentFormatter(attendancePieChart));
        dataSet.setValueTextSize(14f);

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
        int onSurfaceColor = typedValue.data;
        dataSet.setValueTextColor(onSurfaceColor);

        PieData data = new PieData(dataSet);
        attendancePieChart.setData(data);

        attendancePieChart.animateY(800, Easing.EaseInOutQuad);

        attendancePieChart.invalidate();

        String centerText = String.format(Locale.getDefault(), "%.2f%%\nPresent", summary.getPresentPercentage());
        attendancePieChart.setCenterText(centerText);
        attendancePieChart.setCenterTextSize(16f);
        attendancePieChart.setCenterTextColor(onSurfaceColor);
    }
}