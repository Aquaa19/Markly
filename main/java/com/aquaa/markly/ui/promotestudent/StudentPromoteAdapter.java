package com.aquaa.markly.ui.promotestudent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aquaa.markly.R;
import com.aquaa.markly.data.database.Student;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying a list of students for promotion.
 * Manages the selected state of each student.
 */
public class StudentPromoteAdapter extends RecyclerView.Adapter<StudentPromoteAdapter.StudentViewHolder> {

    private List<Student> students = new ArrayList<>();
    // Using a List as a Set (with contains/add/remove) can be inefficient for large lists.
    // For smaller lists, it's generally fine. If performance becomes an issue,
    // consider using a HashSet<Student> for selectedStudents, but remember
    // HashSet also relies on correct equals() and hashCode().
    private List<Student> selectedStudents = new ArrayList<>(); // To keep track of selected students

    /**
     * Updates the list of students displayed in the RecyclerView.
     * Clears previous selections when a new list is set.
     * @param newStudents The new list of students.
     */
    public void setStudents(List<Student> newStudents) {
        this.students = newStudents;
        this.selectedStudents.clear(); // Clear selection when list updates
        notifyDataSetChanged();
    }

    /**
     * Gets the list of currently selected students.
     * @return A list of Student objects that are currently selected.
     */
    public List<Student> getSelectedStudents() {
        return selectedStudents;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_promote, parent, false);
        return new StudentViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student currentStudent = students.get(position);
        holder.studentNameTextView.setText(currentStudent.getName());
        holder.currentSemesterTextView.setText("Current Semester: " + currentStudent.getCurrentSemester());

        // Important: Remove previous listener to prevent issues with recycled views
        holder.selectCheckBox.setOnCheckedChangeListener(null);

        // Set checkbox state based on whether the student is in the selectedStudents list
        // This will now work correctly due to implemented equals() and hashCode() in Student class
        holder.selectCheckBox.setChecked(selectedStudents.contains(currentStudent));

        // Set new listener for checkbox changes
        holder.selectCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Only add if not already present (though .contains() now works correctly)
                if (!selectedStudents.contains(currentStudent)) {
                    selectedStudents.add(currentStudent);
                }
            } else {
                selectedStudents.remove(currentStudent);
            }
        });

        // Optionally, make the whole item clickable to toggle the checkbox
        // This provides a larger touch target for users
        holder.itemView.setOnClickListener(v -> {
            holder.selectCheckBox.setChecked(!holder.selectCheckBox.isChecked());
        });
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    /**
     * ViewHolder class to hold references to the UI elements of each student item.
     */
    static class StudentViewHolder extends RecyclerView.ViewHolder {
        private TextView studentNameTextView;
        private TextView currentSemesterTextView;
        private CheckBox selectCheckBox;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameTextView = itemView.findViewById(R.id.text_view_student_name_promote);
            currentSemesterTextView = itemView.findViewById(R.id.text_view_current_semester_promote);
            selectCheckBox = itemView.findViewById(R.id.checkbox_select_student_promote);
        }
    }
}
