package com.aquaa.markly.ui.sendmessage;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build; // Import Build for version checks
import android.os.Bundle;
import android.text.InputType; // Import for EditText in custom message dialog
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText; // Import for EditText in custom message dialog
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.aquaa.markly.R;
import com.aquaa.markly.data.database.Student;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SendMessageActivity extends AppCompatActivity {

    private static final String TAG = "SendMessageActivity";
    private static final int SMS_PERMISSION_REQUEST_CODE = 101;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 102;

    private SendMessageViewModel sendMessageViewModel;
    private Spinner semesterSpinner;
    private RecyclerView absentStudentsRecyclerView;
    private Button sendMessagesButton;
    private Button chooseDateButton;
    private TextView selectedDateTextView;
    private StudentMessageAdapter adapter;

    private int selectedSemester = -1;
    private Long selectedDateMillis = null;

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMMM 'Markle'", Locale.getDefault()); // Corrected format string

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);

        sendMessageViewModel = new ViewModelProvider(this).get(SendMessageViewModel.class);

        semesterSpinner = findViewById(R.id.spinner_semester_send_message);
        absentStudentsRecyclerView = findViewById(R.id.recycler_view_absent_students);
        sendMessagesButton = findViewById(R.id.button_send_messages);
        chooseDateButton = findViewById(R.id.button_choose_date);
        selectedDateTextView = findViewById(R.id.text_view_selected_date);

        adapter = new StudentMessageAdapter(this);
        absentStudentsRecyclerView.setAdapter(adapter);

        setDefaultDateToToday();

        // Request SMS permission at activity creation
        requestSmsPermission();
        // Removed requestNotificationPermission() from here to avoid conflict
        // It will be requested when the first system notification is attempted.


        sendMessageViewModel.getAllSemesters().observe(this, semesters -> {
            List<String> spinnerItems = new ArrayList<>();
            spinnerItems.add("Select Semester");
            if (semesters != null) {
                for (Integer sem : semesters) {
                    spinnerItems.add(String.valueOf(sem));
                }
            }

            ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, spinnerItems);
            semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            semesterSpinner.setAdapter(semesterAdapter);
        });

        sendMessageViewModel.getStudentsToDisplay().observe(this, students -> {
            adapter.submitList(students);
            if (students == null || students.isEmpty()) {
                if (selectedSemester != -1 && selectedDateMillis != null) {
                    Toast.makeText(this, "No pending absent students found for this selection.", Toast.LENGTH_SHORT).show();
                } else if (selectedDateMillis == null) {
                    Toast.makeText(this, "Please select a date.", Toast.LENGTH_SHORT).show();
                } else if (selectedSemester == -1) {
                    Toast.makeText(this, "Please select a semester.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        sendMessageViewModel.getSmsResult().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(SendMessageActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });

        semesterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedSemester = Integer.parseInt(parent.getItemAtPosition(position).toString());
                } else {
                    selectedSemester = -1;
                }
                loadStudentsForSelectedDateAndSemester();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        chooseDateButton.setOnClickListener(v -> showDatePicker());

        sendMessagesButton.setOnClickListener(v -> showMessageOptionDialog());

        sendMessageViewModel.loadAllSemesters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStudentsForSelectedDateAndSemester();
    }

    private void setDefaultDateToToday() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        selectedDateMillis = today.getTimeInMillis();
        selectedDateTextView.setText(dateFormatter.format(new Date(selectedDateMillis)));
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        if (selectedDateMillis != null) {
            c.setTimeInMillis(selectedDateMillis);
        }
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, 0);
                    selectedCalendar.set(Calendar.MINUTE, 0);
                    selectedCalendar.set(Calendar.SECOND, 0);
                    selectedCalendar.set(Calendar.MILLISECOND, 0);

                    selectedDateMillis = selectedCalendar.getTimeInMillis();
                    selectedDateTextView.setText(dateFormatter.format(new Date(selectedDateMillis)));

                    loadStudentsForSelectedDateAndSemester();
                }, year, month, day);
        datePickerDialog.show();
    }

    private void loadStudentsForSelectedDateAndSemester() {
        if (selectedDateMillis != null && selectedSemester != -1) {
            sendMessageViewModel.loadAbsentStudentsForSpecificDate(selectedDateMillis, selectedSemester);
        } else if (selectedDateMillis == null) {
            adapter.submitList(new ArrayList<>());
            Toast.makeText(this, "Please select a date.", Toast.LENGTH_SHORT).show();
        } else {
            adapter.submitList(new ArrayList<>());
            Toast.makeText(this, "Please select a semester.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Displays a dialog to choose between sending a default or custom message.
     */
    private void showMessageOptionDialog() {
        // Permission checks before showing message options
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "SMS permission not granted. Requesting permission.");
            requestSmsPermission();
            Toast.makeText(this, "SMS permission is required to send messages.", Toast.LENGTH_LONG).show();
            return;
        }

        List<Student> studentsToMessage = adapter.getSelectedStudents();
        if (studentsToMessage.isEmpty()) {
            Toast.makeText(this, "Please select students to send messages to.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDateMillis == null || selectedSemester == -1) {
            Toast.makeText(this, "Please select both a date and a semester first.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Send Message")
                .setMessage("Choose message type:")
                .setPositiveButton("Default Message", (dialog, which) -> {
                    handleSendMessages(null);
                })
                .setNegativeButton("Custom Message", (dialog, which) -> {
                    showCustomMessageDialog();
                })
                .setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }


    /**
     * Shows a dialog to allow the user to input a custom message.
     */
    private void showCustomMessageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Custom Message");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setLines(3);
        input.setHint("e.g., Your ward was absent. Please contact school.");
        builder.setView(input);

        builder.setPositiveButton("Send Custom", (dialog, which) -> {
            String customMessage = input.getText().toString().trim();
            if (customMessage.isEmpty()) {
                Toast.makeText(SendMessageActivity.this, "Custom message cannot be empty.", Toast.LENGTH_SHORT).show();
            } else {
                handleSendMessages(customMessage);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }


    /**
     * Handles sending messages to selected students with the given message content.
     * @param customMessage The custom message to send, or null for default message.
     */
    private void handleSendMessages(String customMessage) {
        // Request Notification permission (for Android 13+) right before sending a system notification
        // This is done here to avoid the "Can request only one set of permissions" conflict
        requestNotificationPermission(); // Moved this call here

        List<Student> studentsToMessage = adapter.getSelectedStudents();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission is required to send messages.", Toast.LENGTH_LONG).show();
            return;
        }
        if (studentsToMessage.isEmpty()) {
            Toast.makeText(this, "No students selected to send messages to.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedDateMillis == null || selectedSemester == -1) {
            Toast.makeText(this, "Please select both a date and a semester first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String confirmationMessage;
        if (customMessage == null) {
            confirmationMessage = "Are you sure you want to send " + studentsToMessage.size() + " default absence message(s) for " + dateFormatter.format(new Date(selectedDateMillis)) + "?";
        } else {
            confirmationMessage = "Are you sure you want to send " + studentsToMessage.size() + " custom absence message(s) for " + dateFormatter.format(new Date(selectedDateMillis)) + "?\n\nMessage: \"" + customMessage + "\"";
        }


        new AlertDialog.Builder(this)
                .setTitle("Confirm SMS Send")
                .setMessage(confirmationMessage)
                .setPositiveButton("Yes, Send", (dialog, which) -> {
                    sendMessageViewModel.sendSmsToStudents(studentsToMessage, selectedDateMillis, customMessage);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST_CODE);
            Log.d(TAG, "Requesting SEND_SMS permission.");
        } else {
            Log.d(TAG, "SEND_SMS permission already granted.");
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
                Log.d(TAG, "Requesting POST_NOTIFICATIONS permission (Android 13+).");
            } else {
                Log.d(TAG, "POST_NOTIFICATIONS permission already granted (Android 13+).");
            }
        } else {
            Log.d(TAG, "POST_NOTIFICATIONS permission not required for this Android version.");
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "SMS permission granted by user.");
            } else {
                Toast.makeText(this, "SMS permission denied. Cannot send messages.", Toast.LENGTH_LONG).show();
                Log.w(TAG, "SMS permission denied by user.");
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Notification permission granted by user (Android 13+).");
            } else {
                Toast.makeText(this, "Notification permission denied. App may not show system notifications.", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Notification permission denied by user (Android 13+).");
            }
        }
    }
}
