package com.aquaa.markly.ui.attendance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.aquaa.markly.R;
import com.aquaa.markly.ui.attendance.TrackAttendanceViewModel.AttendanceRecordDisplay;

import java.util.Locale;

public class TopBottomStudentAdapter extends ListAdapter<AttendanceRecordDisplay, TopBottomStudentAdapter.StudentViewHolder> {

    public TopBottomStudentAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_bottom_student, parent, false); // You'll create this layout
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        AttendanceRecordDisplay currentRecord = getItem(position);
        holder.bind(currentRecord);
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        private final TextView studentNameTextView;
        private final TextView attendancePercentageTextView;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameTextView = itemView.findViewById(R.id.top_bottom_student_name_text_view);
            attendancePercentageTextView = itemView.findViewById(R.id.top_bottom_attendance_percentage_text_view);
        }

        public void bind(AttendanceRecordDisplay record) {
            studentNameTextView.setText(record.getStudentName());
            attendancePercentageTextView.setText(String.format(Locale.getDefault(), "%.2f%%", record.getAttendancePercentage()));
        }
    }

    private static final DiffUtil.ItemCallback<AttendanceRecordDisplay> DIFF_CALLBACK = new DiffUtil.ItemCallback<AttendanceRecordDisplay>() {
        @Override
        public boolean areItemsTheSame(@NonNull AttendanceRecordDisplay oldItem, @NonNull AttendanceRecordDisplay newItem) {
            return oldItem.getStudentId() == newItem.getStudentId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull AttendanceRecordDisplay oldItem, @NonNull AttendanceRecordDisplay newItem) {
            // Compare relevant fields to determine if contents are the same
            return oldItem.getStudentName().equals(newItem.getStudentName()) &&
                    oldItem.getAttendancePercentage() == newItem.getAttendancePercentage();
        }
    };
}
