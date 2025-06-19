// StudentManageAdapter.java
package com.aquaa.markly.ui.addstudent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // Import ImageView
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aquaa.markly.R;
import com.aquaa.markly.data.database.Student;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying a list of students in the Add/Manage Student screen.
 * This adapter is responsible for showing student details and handling delete actions.
 */
public class StudentManageAdapter extends RecyclerView.Adapter<StudentManageAdapter.StudentManageViewHolder> {

    private List<Student> students = new ArrayList<>();
    private OnStudentActionListener listener; // Listener for delete action

    /**
     * Interface for callbacks when a student action (like delete) occurs.
     */
    public interface OnStudentActionListener {
        void onDeleteClick(Student student);
        // void onEditClick(Student student); // Future expansion
    }

    public StudentManageAdapter(OnStudentActionListener listener) {
        this.listener = listener;
    }

    /**
     * Sets the list of students to be displayed in the RecyclerView.
     * @param newStudents The new list of Student objects.
     */
    public void setStudents(List<Student> newStudents) {
        this.students = newStudents != null ? newStudents : new ArrayList<>();
        notifyDataSetChanged(); // Notify the adapter that the data set has changed
    }

    @NonNull
    @Override
    public StudentManageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_manage, parent, false);
        return new StudentManageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentManageViewHolder holder, int position) {
        Student currentStudent = students.get(position);
        holder.studentNameTextView.setText(currentStudent.getName());
        holder.studentMobileTextView.setText("Mobile: " + currentStudent.getMobile());
        holder.studentSemesterSectionTextView.setText(
                "Sem: " + currentStudent.getCurrentSemester() + ", Sec: " + currentStudent.getSection()
        );

        // Set OnClickListener for the delete icon
        holder.deleteIcon.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(currentStudent); // Notify the activity/fragment
            }
        });
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    /**
     * ViewHolder class to hold references to the UI elements of each student item.
     */
    static class StudentManageViewHolder extends RecyclerView.ViewHolder {
        private TextView studentNameTextView;
        private TextView studentMobileTextView;
        private TextView studentSemesterSectionTextView;
        private ImageView deleteIcon; // Declare ImageView

        public StudentManageViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameTextView = itemView.findViewById(R.id.text_view_student_name_manage);
            studentMobileTextView = itemView.findViewById(R.id.text_view_student_mobile_manage);
            studentSemesterSectionTextView = itemView.findViewById(R.id.text_view_student_semester_section_manage);
            deleteIcon = itemView.findViewById(R.id.image_view_delete_student); // Initialize ImageView
        }
    }
}
