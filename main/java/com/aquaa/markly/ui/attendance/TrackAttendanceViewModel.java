package com.aquaa.markly.ui.attendance;

import android.app.Application;
import android.util.Log; // Import Log

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.aquaa.markly.data.database.Attendance;
import com.aquaa.markly.data.database.Student;
import com.aquaa.markly.data.repository.StudentRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors; // Import Executors

public class TrackAttendanceViewModel extends AndroidViewModel {
    private static final String TAG = "TrackAttendanceVM"; // Tag for logging

    private StudentRepository mRepository;
    private MutableLiveData<Integer> selectedMonth = new MutableLiveData<>();
    private MutableLiveData<Integer> selectedYear = new MutableLiveData<>();
    private MutableLiveData<Integer> selectedSemester = new MutableLiveData<>();

    private LiveData<List<Integer>> allAvailableSemesters;

    private LiveData<List<Student>> filteredStudentsLiveData;

    private MediatorLiveData<List<AttendanceRecordDisplay>> monthlyAttendanceRecords = new MediatorLiveData<>();

    // Executor for background database operations (if needed in this VM)
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();


    public TrackAttendanceViewModel(Application application) {
        super(application);
        mRepository = new StudentRepository(application);

        Calendar calendar = Calendar.getInstance();
        selectedMonth.setValue(calendar.get(Calendar.MONTH));
        selectedYear.setValue(calendar.get(Calendar.YEAR));
        selectedSemester.setValue(0); // Default to 'All Semesters'

        allAvailableSemesters = mRepository.getAllSemesters();

        filteredStudentsLiveData = Transformations.switchMap(selectedSemester, semester -> {
            Log.d(TAG, "Selected Semester changed to: " + semester);
            if (semester == null || semester == 0) {
                return mRepository.getAllStudents(); // LiveData<List<Student>>
            } else {
                return mRepository.getStudentsBySemester(semester); // LiveData<List<Student>>
            }
        });

        // Add sources to trigger updateMonthlyAttendanceRecords when any dependency changes
        monthlyAttendanceRecords.addSource(selectedMonth, month -> updateMonthlyAttendanceRecords());
        monthlyAttendanceRecords.addSource(selectedYear, year -> updateMonthlyAttendanceRecords());
        monthlyAttendanceRecords.addSource(filteredStudentsLiveData, students -> updateMonthlyAttendanceRecords());
    }

    public void setSelectedMonth(int month) {
        if (selectedMonth.getValue() == null || month != selectedMonth.getValue()) {
            selectedMonth.setValue(month);
            Log.d(TAG, "setSelectedMonth: " + month);
        }
    }

    public void setSelectedYear(int year) {
        if (selectedYear.getValue() == null || year != selectedYear.getValue()) {
            selectedYear.setValue(year);
            Log.d(TAG, "setSelectedYear: " + year);
        }
    }

    public void setSelectedSemester(int semester) {
        if (selectedSemester.getValue() == null || semester != selectedSemester.getValue()) {
            selectedSemester.setValue(semester);
            Log.d(TAG, "setSelectedSemester: " + semester);
        }
    }

    public LiveData<Integer> getSelectedMonth() {
        return selectedMonth;
    }

    public LiveData<Integer> getSelectedYear() {
        return selectedYear;
    }

    public LiveData<Integer> getSelectedSemester() {
        return selectedSemester;
    }

    public LiveData<List<Integer>> getAllAvailableSemesters() {
        return allAvailableSemesters;
    }

    public LiveData<List<AttendanceRecordDisplay>> getMonthlyAttendanceRecords() {
        return monthlyAttendanceRecords;
    }

    private void updateMonthlyAttendanceRecords() {
        Integer month = selectedMonth.getValue();
        Integer year = selectedYear.getValue();
        List<Student> students = filteredStudentsLiveData.getValue(); // Get the current list of filtered students

        if (month == null || year == null || students == null) {
            // Log.d(TAG, "updateMonthlyAttendanceRecords: Skipping update, dependencies not ready.");
            monthlyAttendanceRecords.setValue(new ArrayList<>()); // Clear data if dependencies are null
            return;
        }

        Log.d(TAG, "updateMonthlyAttendanceRecords: Fetching attendance for month " + month + ", year " + year + ", for " + students.size() + " filtered students.");


        // Run database operation on a background thread
        dbExecutor.execute(() -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, 1, 0, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startDate = calendar.getTimeInMillis();

            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.MILLISECOND, -1);
            long endDate = calendar.getTimeInMillis();

            // Get all attendance records for the month synchronously
            List<Attendance> allMonthAttendanceSync = mRepository.getAllAttendanceSync(); // Get all attendance first

            // Filter these attendance records based on date range and filtered students
            List<Attendance> filteredAttendanceForMonth = new ArrayList<>();
            Map<Long, Student> currentFilteredStudentMap = new HashMap<>(); // For efficient lookup
            for (Student student : students) { // Populate map from current filtered students
                currentFilteredStudentMap.put(student.getStudentId(), student);
            }

            for (Attendance attendance : allMonthAttendanceSync) {
                if (attendance.getDate() >= startDate && attendance.getDate() <= endDate &&
                        currentFilteredStudentMap.containsKey(attendance.getStudentId())) { // Check if student is in the currently filtered list
                    filteredAttendanceForMonth.add(attendance);
                }
            }


            // Aggregate attendance data per student from filtered records
            Map<Long, AttendanceRecordDisplay> studentAttendanceMap = new HashMap<>();
            for (Student student : students) { // Initialize records for all filtered students
                studentAttendanceMap.put(student.getStudentId(), new AttendanceRecordDisplay(student.getStudentId(), student.getName(), 0, 0, 0.0));
            }

            for (Attendance attendance : filteredAttendanceForMonth) {
                // Ensure attendance record belongs to a student we are currently tracking
                if (studentAttendanceMap.containsKey(attendance.studentId)) {
                    AttendanceRecordDisplay record = studentAttendanceMap.get(attendance.studentId);
                    if (record != null) {
                        record.incrementTotalDays();
                        if (attendance.isPresent) {
                            record.incrementPresentDays();
                        } else {
                            record.incrementAbsentDays();
                        }
                    }
                }
            }

            List<AttendanceRecordDisplay> resultList = new ArrayList<>(studentAttendanceMap.values());
            Collections.sort(resultList, (o1, o2) -> o1.getStudentName().compareToIgnoreCase(o2.getStudentName()));

            for (AttendanceRecordDisplay record : resultList) {
                if (record.getTotalDays() > 0) {
                    double percentage = (double) record.getPresentDays() / record.getTotalDays() * 100;
                    record.setAttendancePercentage(percentage);
                } else {
                    record.setAttendancePercentage(0.0);
                }
            }
            monthlyAttendanceRecords.postValue(resultList); // Use postValue as this is on a background thread
            Log.d(TAG, "updateMonthlyAttendanceRecords: Posted " + resultList.size() + " records.");
        });
    }

    // Data class to hold aggregated attendance information for display
    public static class AttendanceRecordDisplay implements Serializable {
        private long studentId;
        private String studentName;
        private int presentDays;
        private int absentDays;
        private int totalDays;
        private double attendancePercentage;

        // Constructor as per your original definition
        public AttendanceRecordDisplay(long studentId, String studentName, int presentDays, int absentDays, double attendancePercentage) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.presentDays = presentDays;
            this.absentDays = absentDays;
            this.totalDays = presentDays + absentDays; // Total days is sum of present and absent
            this.attendancePercentage = attendancePercentage;
        }

        public long getStudentId() {
            return studentId;
        }

        public String getStudentName() {
            return studentName;
        }

        public int getPresentDays() {
            return presentDays;
        }

        public void incrementPresentDays() {
            this.presentDays++;
        }

        public int getAbsentDays() {
            return absentDays;
        }

        public void incrementAbsentDays() {
            this.absentDays++;
        }

        public int getTotalDays() {
            return totalDays;
        }

        public void incrementTotalDays() {
            this.totalDays++;
        }

        public double getAttendancePercentage() {
            return attendancePercentage;
        }

        public void setAttendancePercentage(double attendancePercentage) {
            this.attendancePercentage = attendancePercentage;
        }

        // Override equals and hashCode for DiffUtil in adapter
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AttendanceRecordDisplay that = (AttendanceRecordDisplay) o;
            return studentId == that.studentId &&
                    presentDays == that.presentDays &&
                    absentDays == that.absentDays &&
                    totalDays == that.totalDays &&
                    Double.compare(that.attendancePercentage, attendancePercentage) == 0 &&
                    studentName.equals(that.studentName);
        }

        @Override
        public int hashCode() {
            return (int) (studentId * 31 + presentDays * 17 + absentDays * 13 + totalDays * 7 + Double.valueOf(attendancePercentage).hashCode() + studentName.hashCode());
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!dbExecutor.isShutdown()) {
            dbExecutor.shutdown();
            Log.d(TAG, "dbExecutor shutdown initiated in TrackAttendanceViewModel.");
        }
    }
}
