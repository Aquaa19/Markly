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

public class TrackAttendanceAdapter extends ListAdapter<AttendanceRecordDisplay, TrackAttendanceAdapter.AttendanceRecordViewHolder> {

    public TrackAttendanceAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public AttendanceRecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_record, parent, false); // You'll need to create item_attendance_record.xml
        return new AttendanceRecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceRecordViewHolder holder, int position) {
        AttendanceRecordDisplay currentRecord = getItem(position);
        holder.bind(currentRecord);
    }

    static class AttendanceRecordViewHolder extends RecyclerView.ViewHolder {
        private final TextView studentNameTextView;
        private final TextView presentDaysTextView;
        private final TextView absentDaysTextView;
        private final TextView totalDaysTextView;
        private final TextView percentageTextView;

        public AttendanceRecordViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameTextView = itemView.findViewById(R.id.student_name_text_view);
            presentDaysTextView = itemView.findViewById(R.id.present_days_text_view);
            absentDaysTextView = itemView.findViewById(R.id.absent_days_text_view);
            totalDaysTextView = itemView.findViewById(R.id.total_days_text_view);
            percentageTextView = itemView.findViewById(R.id.percentage_text_view);
        }

        public void bind(AttendanceRecordDisplay record) {
            studentNameTextView.setText(record.getStudentName());
            presentDaysTextView.setText(String.format(Locale.getDefault(), "Present: %d", record.getPresentDays()));
            absentDaysTextView.setText(String.format(Locale.getDefault(), "Absent: %d", record.getAbsentDays()));
            totalDaysTextView.setText(String.format(Locale.getDefault(), "Total Recorded: %d", record.getTotalDays()));
            percentageTextView.setText(String.format(Locale.getDefault(), "Attendance: %.2f%%", record.getAttendancePercentage()));
        }
    }

    private static final DiffUtil.ItemCallback<AttendanceRecordDisplay> DIFF_CALLBACK = new DiffUtil.ItemCallback<AttendanceRecordDisplay>() {
        @Override
        public boolean areItemsTheSame(@NonNull AttendanceRecordDisplay oldItem, @NonNull AttendanceRecordDisplay newItem) {
            return oldItem.getStudentId() == newItem.getStudentId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull AttendanceRecordDisplay oldItem, @NonNull AttendanceRecordDisplay newItem) {
            return oldItem.equals(newItem); // Requires equals() and hashCode() in AttendanceRecordDisplay if not using data class
        }
    };
}
