package com.aquaa.markly.ui.attendance;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aquaa.markly.data.database.Attendance;
import com.aquaa.markly.data.database.Student;
import com.aquaa.markly.data.repository.StudentRepository;
import com.aquaa.markly.ui.attendance.TrackAttendanceViewModel.AttendanceRecordDisplay;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for AttendanceAnalyticsActivity.
 * Handles fetching and processing attendance data for analytics.
 */
public class AttendanceAnalyticsViewModel extends AndroidViewModel {

    private static final String TAG = "AttendanceAnalyticsVM";
    private StudentRepository studentRepository;

    private MutableLiveData<Integer> selectedMonth = new MutableLiveData<>();
    private MutableLiveData<Integer> selectedYear = new MutableLiveData<>();
    private MutableLiveData<Integer> selectedDay = new MutableLiveData<>();
    private MutableLiveData<Integer> selectedSemester = new MutableLiveData<>();
    private MutableLiveData<List<AttendanceRecordDisplay>> initialDetailedRecords = new MutableLiveData<>();

    private MutableLiveData<AttendanceSummary> attendanceSummary = new MutableLiveData<>();
    private MutableLiveData<List<AttendanceRecordDisplay>> detailedStudentAttendance = new MutableLiveData<>();

    private MutableLiveData<List<Integer>> allAvailableSemesters = new MutableLiveData<>(); // New LiveData for all semesters

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public AttendanceAnalyticsViewModel(Application application) {
        super(application);
        studentRepository = new StudentRepository(application);
        loadAllSemesters(); // Load semesters on init
    }

    public LiveData<AttendanceSummary> getAttendanceSummary() {
        return attendanceSummary;
    }

    public LiveData<List<AttendanceRecordDisplay>> getDetailedStudentAttendance() {
        return detailedStudentAttendance;
    }

    public LiveData<Integer> getSelectedSemester() {
        return selectedSemester;
    }

    public LiveData<List<Integer>> getAllSemesters() { // New method
        return allAvailableSemesters;
    }

    public void setSelectedMonth(int month) {
        selectedMonth.setValue(month);
        loadAnalyticsData();
    }

    public void setSelectedYear(int year) {
        selectedYear.setValue(year);
        loadAnalyticsData();
    }

    public void setSelectedDay(int day) {
        selectedDay.setValue(day);
        loadAnalyticsData();
    }

    public void setSelectedSemester(int semester) {
        selectedSemester.setValue(semester);
        loadAnalyticsData();
    }


    /**
     * Loads all unique semester numbers from the database.
     * Posts the result to `allAvailableSemesters` LiveData.
     */
    private void loadAllSemesters() {
        dbExecutor.execute(() -> {
            try {
                List<Integer> semesters = studentRepository.getAllSemestersSync(); // Call sync method from repo
                allAvailableSemesters.postValue(semesters);
                Log.d(TAG, "Loaded semesters for analytics spinner: " + (semesters != null ? semesters.size() : 0));
            } catch (Exception e) {
                Log.e(TAG, "Error loading semesters for analytics: " + e.getMessage(), e);
                allAvailableSemesters.postValue(new ArrayList<>());
            }
        });
    }

    /**
     * Sets initial parameters, typically from Intent extras, and triggers data loading.
     */
    public void setSelectionParameters(int monthFromIntent, int yearFromIntent, int dayFromIntent, int semesterFromIntent, ArrayList<AttendanceRecordDisplay> detailedRecords) {
        selectedMonth.setValue(monthFromIntent);
        selectedYear.setValue(yearFromIntent);
        selectedDay.setValue(dayFromIntent);
        selectedSemester.setValue(semesterFromIntent);
        initialDetailedRecords.setValue(detailedRecords);

        loadAnalyticsData();
    }

    /**
     * Loads attendance data based on the currently set month, year, and day,
     * filtered by the selected semester.
     */
    public void loadAnalyticsData() {
        dbExecutor.execute(() -> {
            Integer currentMonth = selectedMonth.getValue();
            Integer currentYear = selectedYear.getValue();
            Integer currentDay = selectedDay.getValue();
            Integer currentSemester = selectedSemester.getValue();

            if (currentYear == null) {
                Log.w(TAG, "Year not selected, cannot load analytics data.");
                return;
            }

            long startDateMillis, endDateMillis;

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (currentDay != -1) { // Specific Day selected
                calendar.set(currentYear, currentMonth, currentDay);
                startDateMillis = calendar.getTimeInMillis();
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                endDateMillis = calendar.getTimeInMillis() - 1;
                Log.d(TAG, "Loading analytics for specific day: " + currentDay + "/" + currentMonth + "/" + currentYear);
            } else if (currentMonth != -1) { // Specific Month selected
                calendar.set(currentYear, currentMonth, 1);
                startDateMillis = calendar.getTimeInMillis();
                calendar.add(Calendar.MONTH, 1);
                calendar.add(Calendar.MILLISECOND, -1); // End of the month
                endDateMillis = calendar.getTimeInMillis();
                Log.d(TAG, "Loading analytics for month: " + currentMonth + "/" + currentYear);
            } else { // Entire Year selected
                calendar.set(currentYear, 0, 1); // January 1st
                startDateMillis = calendar.getTimeInMillis();
                calendar.set(currentYear, 11, 31); // December 31st
                endDateMillis = calendar.getTimeInMillis();
                Log.d(TAG, "Loading analytics for year: " + currentYear);
            }

            // Fetch students based on selected semester
            List<Student> studentsForAnalytics; // Renamed for clarity
            if (currentSemester != null && currentSemester != 0) { // If a specific semester is selected
                studentsForAnalytics = studentRepository.getStudentsBySemesterSync(currentSemester);
                Log.d(TAG, "Filtering students by semester: " + currentSemester + ". Found: " + studentsForAnalytics.size());
            } else { // All semesters
                studentsForAnalytics = studentRepository.getAllStudentsSync();
                Log.d(TAG, "Loading students for all semesters. Found: " + studentsForAnalytics.size());
            }

            // Create a map of student IDs to Student objects for quick lookup
            Map<Long, Student> studentMapForAnalytics = new HashMap<>();
            for (Student student : studentsForAnalytics) {
                studentMapForAnalytics.put(student.getStudentId(), student);
            }


            List<Attendance> allAttendanceRecords = studentRepository.getAllAttendanceSync();

            // Filter attendance records by date range AND by the `studentsForAnalytics` list
            List<Attendance> filteredAttendance = new ArrayList<>();
            for (Attendance attendance : allAttendanceRecords) {
                // Ensure attendance date is within the selected period AND student is in the selected group
                if (attendance.getDate() >= startDateMillis && attendance.getDate() <= endDateMillis &&
                        studentMapForAnalytics.containsKey(attendance.getStudentId())) { // Check if student belongs to the filtered set
                    filteredAttendance.add(attendance);
                }
            }

            calculateSummary(filteredAttendance, studentsForAnalytics); // Pass filtered students
            calculateDetailedAttendance(filteredAttendance, studentsForAnalytics); // Pass filtered students
        });
    }

    private void calculateSummary(List<Attendance> attendanceRecords, List<Student> studentsForAnalytics) {
        int totalStudentsConsidered = studentsForAnalytics.size(); // NOW uses the filtered list of students

        int totalPresentDays = 0;
        int totalAbsentDays = 0;
        int totalRecordedDays = 0;

        for (Attendance attendance : attendanceRecords) {
            totalRecordedDays++;
            if (attendance.isPresent()) {
                totalPresentDays++;
            } else {
                totalAbsentDays++;
            }
        }

        double presentPercentage = (totalRecordedDays == 0) ? 0 : ((double) totalPresentDays / totalRecordedDays) * 100;
        double absentPercentage = (totalRecordedDays == 0) ? 0 : ((double) totalAbsentDays / totalRecordedDays) * 100;

        attendanceSummary.postValue(new AttendanceSummary(
                totalStudentsConsidered, totalPresentDays, totalAbsentDays, presentPercentage, absentPercentage, totalRecordedDays
        ));
        Log.d(TAG, "Calculated attendance summary for selected semester/period. Total students considered: " + totalStudentsConsidered);
    }

    private void calculateDetailedAttendance(List<Attendance> attendanceRecords, List<Student> studentsForAnalytics) {
        // Initialize maps only for students in the selected period
        Map<Long, Integer> studentPresentDays = new HashMap<>();
        Map<Long, Integer> studentAbsentDays = new HashMap<>();
        Map<Long, Integer> studentRecordedDays = new HashMap<>();

        for (Student student : studentsForAnalytics) { // Loop only over students in the selected period
            studentPresentDays.put(student.getStudentId(), 0);
            studentAbsentDays.put(student.getStudentId(), 0);
            studentRecordedDays.put(student.getStudentId(), 0);
        }

        for (Attendance attendance : attendanceRecords) { // attendanceRecords are already filtered
            long studentId = attendance.getStudentId();
            // Ensure the student ID from attendance record is actually in our `studentsForAnalytics` map
            // This is a safety check, as `filteredAttendance` should already ensure this.
            if (studentPresentDays.containsKey(studentId)) {
                studentRecordedDays.put(studentId, studentRecordedDays.get(studentId) + 1);
                if (attendance.isPresent()) {
                    studentPresentDays.put(studentId, studentPresentDays.get(studentId) + 1);
                } else {
                    studentAbsentDays.put(studentId, studentAbsentDays.get(studentId) + 1);
                }
            }
        }

        List<AttendanceRecordDisplay> detailedList = new ArrayList<>();
        for (Student student : studentsForAnalytics) { // Loop only over students in the selected period
            int recorded = studentRecordedDays.getOrDefault(student.getStudentId(), 0);
            int present = studentPresentDays.getOrDefault(student.getStudentId(), 0);
            int absent = studentAbsentDays.getOrDefault(student.getStudentId(), 0);
            double percentage = (recorded == 0) ? 0 : ((double) present / recorded) * 100;

            detailedList.add(new AttendanceRecordDisplay(
                    student.getStudentId(),
                    student.getName(),
                    student.getCurrentSemester(),
                    present,
                    absent
            ));
        }
        detailedStudentAttendance.postValue(detailedList);
        Log.d(TAG, "Calculated detailed student attendance for selected semester/period.");
    }

    /**
     * Data class to hold attendance summary statistics.
     */
    public static class AttendanceSummary {
        private int totalStudents;
        private int totalPresentDays;
        private int totalAbsentDays;
        private double presentPercentage;
        private double absentPercentage;
        private int totalRecordedDays;

        public AttendanceSummary(int totalStudents, int totalPresentDays, int totalAbsentDays,
                                 double presentPercentage, double absentPercentage, int totalRecordedDays) {
            this.totalStudents = totalStudents;
            this.totalPresentDays = totalPresentDays;
            this.totalAbsentDays = totalAbsentDays;
            this.presentPercentage = presentPercentage;
            this.absentPercentage = absentPercentage;
            this.totalRecordedDays = totalRecordedDays;
        }

        public int getTotalStudents() { return totalStudents; }
        public int getTotalPresentDays() { return totalPresentDays; }
        public int getTotalAbsentDays() { return totalAbsentDays; }
        public double getPresentPercentage() { return presentPercentage; }
        public double getAbsentPercentage() { return absentPercentage; }
        public int getTotalRecordedDays() { return totalRecordedDays; }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!dbExecutor.isShutdown()) {
            dbExecutor.shutdown();
        }
        Log.d(TAG, "dbExecutor shutdown initiated in AttendanceAnalyticsViewModel.");
    }
}
