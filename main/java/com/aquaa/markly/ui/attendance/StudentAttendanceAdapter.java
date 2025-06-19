package com.aquaa.markly.ui.attendance;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.aquaa.markly.R;
import com.aquaa.markly.ui.attendance.AttendanceViewModel.StudentAttendanceStatus; // Import the nested class

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RecyclerView Adapter for displaying a list of students for attendance taking.
 * Manages the present/absent status of each student.
 */
public class StudentAttendanceAdapter extends ListAdapter<StudentAttendanceStatus, StudentAttendanceAdapter.StudentViewHolder> {

    // Map to keep track of attendance status: Student ID -> isPresent (true/false)
    // This map stores the state of checkboxes as user interacts with them.
    private Map<Long, Boolean> attendanceStatusMap = new HashMap<>();

    public StudentAttendanceAdapter(@NonNull Context context) {
        super(DIFF_CALLBACK);
    }

    @Override
    public void submitList(final List<StudentAttendanceStatus> list) {
        // When a new list is submitted, clear the previous attendance status map
        // and populate it with the new default states from the incoming list.
        attendanceStatusMap.clear();
        if (list != null) {
            for (StudentAttendanceStatus status : list) {
                attendanceStatusMap.put(status.student.getStudentId(), status.isPresent);
            }
        }
        super.submitList(list);
    }


    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_attendance, parent, false); // Assuming this layout exists
        return new StudentViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        StudentAttendanceStatus currentStudentStatus = getItem(position);
        holder.studentNameTextView.setText(currentStudentStatus.student.getName());
        holder.studentSemesterTextView.setText("Semester: " + currentStudentStatus.student.getCurrentSemester());

        // Set checkbox state based on the current attendance status from the map
        // True for present, false for absent.
        // It's crucial to get the status from the map, not directly from currentStudentStatus.isPresent
        // because the map holds user's real-time changes.
        holder.presentCheckBox.setChecked(attendanceStatusMap.getOrDefault(currentStudentStatus.student.getStudentId(), currentStudentStatus.isPresent));


        // Important: Remove previous listener to prevent issues with recycled views
        holder.presentCheckBox.setOnCheckedChangeListener(null);

        // Set new listener for checkbox changes
        holder.presentCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Update the map when checkbox state changes
            attendanceStatusMap.put(currentStudentStatus.student.getStudentId(), isChecked);
        });

        // Optionally, make the whole item clickable to toggle the checkbox
        holder.itemView.setOnClickListener(v -> {
            holder.presentCheckBox.setChecked(!holder.presentCheckBox.isChecked());
        });
    }

    /**
     * Returns the current attendance status for all students in the adapter.
     * This map should be used when saving attendance.
     * @return A map of Student ID to their attendance status (true for present, false for absent).
     */
    public Map<Long, Boolean> getAttendanceStatusMap() {
        return attendanceStatusMap;
    }

    /**
     * DiffUtil.ItemCallback for efficiently calculating differences between two lists of students.
     */
    private static final DiffUtil.ItemCallback<StudentAttendanceStatus> DIFF_CALLBACK = new DiffUtil.ItemCallback<StudentAttendanceStatus>() {
        @Override
        public boolean areItemsTheSame(@NonNull StudentAttendanceStatus oldItem, @NonNull StudentAttendanceStatus newItem) {
            // Compare unique identifiers (student_id)
            return oldItem.student.getStudentId() == newItem.student.getStudentId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull StudentAttendanceStatus oldItem, @NonNull StudentAttendanceStatus newItem) {
            // Compare content fields. This will use the overridden equals() in StudentAttendanceStatus
            return oldItem.equals(newItem);
        }
    };

    /**
     * ViewHolder class to hold references to the UI elements of each student item.
     */
    static class StudentViewHolder extends RecyclerView.ViewHolder {
        private TextView studentNameTextView;
        private TextView studentSemesterTextView; // Assuming you want to display semester
        private CheckBox presentCheckBox; // Checkbox for "Is Present"

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameTextView = itemView.findViewById(R.id.text_view_student_name_attendance); // Assuming these IDs
            studentSemesterTextView = itemView.findViewById(R.id.text_view_student_semester_attendance); // Assuming this ID
            presentCheckBox = itemView.findViewById(R.id.checkbox_present); // Assuming this ID
        }
    }
}
