package com.aquaa.markly.ui.sendmessage;

import android.content.Context; // Import Context
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil; // Import DiffUtil
import androidx.recyclerview.widget.ListAdapter; // Import ListAdapter
import androidx.recyclerview.widget.RecyclerView;

import com.aquaa.markly.R;
import com.aquaa.markly.data.database.Student;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying a list of students for sending messages.
 * Manages the selected state of each student.
 */
public class StudentMessageAdapter extends ListAdapter<Student, StudentMessageAdapter.StudentViewHolder> { // Changed to ListAdapter

    private List<Student> selectedStudents = new ArrayList<>(); // To keep track of selected students
    private OnStudentMessageListener listener; // Listener for checkbox events

    // Constructor now accepts Context and the listener
    public StudentMessageAdapter(Context context, OnStudentMessageListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        // The context parameter is typically needed if the adapter needs to do things like
        // inflate layouts that depend on theme or resources, or interact with other services.
        // For this adapter, primarily the listener is the key addition.
    }

    /**
     * Constructor for use when no listener is explicitly provided (less common for this use case,
     * but could be useful for previews or simple displays without interaction).
     */
    public StudentMessageAdapter(Context context) {
        this(context, null); // Call the main constructor with a null listener
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
                .inflate(R.layout.item_student_message, parent, false);
        return new StudentViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student currentStudent = getItem(position); // Use getItem from ListAdapter
        holder.studentNameTextView.setText(currentStudent.getName());
        holder.guardianMobileTextView.setText("Guardian: " + currentStudent.getGuardianMobile());

        // Set checkbox state based on whether the student is in the selectedStudents list
        holder.selectCheckBox.setChecked(selectedStudents.contains(currentStudent));

        // Set listener for checkbox changes
        holder.selectCheckBox.setOnCheckedChangeListener(null); // Clear previous listener to prevent infinite loop

        holder.selectCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedStudents.contains(currentStudent)) {
                    selectedStudents.add(currentStudent);
                }
            } else {
                selectedStudents.remove(currentStudent);
            }
            if (listener != null) {
                listener.onStudentChecked(currentStudent, isChecked);
            }
        });
    }

    /**
     * DiffUtil.ItemCallback for efficiently calculating differences between two lists of students.
     */
    private static final DiffUtil.ItemCallback<Student> DIFF_CALLBACK = new DiffUtil.ItemCallback<Student>() {
        @Override
        public boolean areItemsTheSame(@NonNull Student oldItem, @NonNull Student newItem) {
            // Compare unique identifiers (student_id)
            return oldItem.getStudentId() == newItem.getStudentId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Student oldItem, @NonNull Student newItem) {
            // Compare content fields if items are the same
            return oldItem.equals(newItem); // Requires Student.equals() and hashCode() to be properly implemented
        }
    };

    /**
     * ViewHolder class to hold references to the UI elements of each student item.
     */
    static class StudentViewHolder extends RecyclerView.ViewHolder {
        private TextView studentNameTextView;
        private TextView guardianMobileTextView;
        private CheckBox selectCheckBox;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameTextView = itemView.findViewById(R.id.text_view_student_name_message);
            guardianMobileTextView = itemView.findViewById(R.id.text_view_guardian_mobile_message);
            selectCheckBox = itemView.findViewById(R.id.checkbox_select_student_message);
        }
    }

    /**
     * Interface for communicating student selection changes back to the hosting Activity/Fragment.
     */
    public interface OnStudentMessageListener {
        void onStudentChecked(Student student, boolean isChecked);
    }
}
