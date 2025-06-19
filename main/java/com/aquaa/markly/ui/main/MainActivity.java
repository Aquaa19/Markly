package com.aquaa.markly.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log; // Added for debugging logs
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer; // Explicitly import Observer
import androidx.lifecycle.ViewModelProvider;

import com.aquaa.markly.R;
import com.aquaa.markly.data.database.Notification; // Import Notification entity
import com.aquaa.markly.data.repository.StudentRepository; // Import StudentRepository
import com.aquaa.markly.ui.addstudent.AddStudentActivity;
import com.aquaa.markly.ui.attendance.AttendanceActivity;
import com.aquaa.markly.ui.attendance.TrackAttendanceActivity;
import com.aquaa.markly.ui.notifications.NotificationActivity;
import com.aquaa.markly.ui.notifications.NotificationPopUpView; // Import your custom view
import com.aquaa.markly.ui.promotestudent.PromoteStudentActivity;
import com.aquaa.markly.ui.sendmessage.SendMessageActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity"; // Define TAG for logging

    private MainViewModel mainViewModel;
    private TextView greetingTextView;
    private TextView quoteTextView;

    // Notification Pop-up related fields
    private NotificationPopUpView notificationPopUpView;
    private StudentRepository studentRepository;
    private Notification currentlyDisplayedPopUpNotification = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ViewModel
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Initialize UI components
        greetingTextView = findViewById(R.id.text_view_greeting);
        quoteTextView = findViewById(R.id.text_view_quote);
        Button buttonAddStudent = findViewById(R.id.button_add_student);
        Button buttonAttendanceCheck = findViewById(R.id.button_attendance_check);
        Button buttonSendMessage = findViewById(R.id.button_send_message);
        Button buttonPromoteStudent = findViewById(R.id.button_promote_student);
        Button buttonTrackAttendanceReport = findViewById(R.id.button_track_attendance_report);
        Button buttonViewNotifications = findViewById(R.id.button_view_notifications); // Initialize new button

        // Initialize Notification Pop-up View and Repository
        notificationPopUpView = findViewById(R.id.notification_pop_up_view);
        studentRepository = new StudentRepository(getApplication());

        // Observe the greeting message LiveData
        mainViewModel.getGreetingMessage().observe(this, greeting -> {
            greetingTextView.setText(greeting);
        });

        // Observe the motivational quote LiveData
        mainViewModel.getMotivationalQuote().observe(this, quote -> {
            quoteTextView.setText(quote);
        });

        // Observe all notifications from the repository to display pop-ups
        studentRepository.getAllNotifications().observe(this, new Observer<List<Notification>>() {
            @Override
            public void onChanged(List<Notification> notifications) {
                if (notifications != null && !notifications.isEmpty()) {
                    Notification latestUnreadNotification = null;
                    for (Notification notif : notifications) {
                        if (!notif.isRead()) { // Check if it's unread
                            // Prioritize the latest unread notification
                            if (latestUnreadNotification == null || notif.getTimestamp() > latestUnreadNotification.getTimestamp()) {
                                latestUnreadNotification = notif;
                            }
                        }
                    }

                    if (latestUnreadNotification != null) {
                        // Check if the currently displayed notification is different from the new latest unread one
                        // This prevents constantly re-showing the same notification if it's already visible
                        if (currentlyDisplayedPopUpNotification == null ||
                                latestUnreadNotification.getNotificationId() != currentlyDisplayedPopUpNotification.getNotificationId()) {

                            Log.d(TAG, "Displaying new in-app notification: " + latestUnreadNotification.getTitle());
                            notificationPopUpView.showNotification(latestUnreadNotification);
                            currentlyDisplayedPopUpNotification = latestUnreadNotification; // Track the currently displayed notification
                        } else {
                            Log.d(TAG, "New unread notification available, but the same one is already showing as pop-up or no newer unread found.");
                        }
                    } else {
                        // No unread notifications to display, hide the pop-up
                        Log.d(TAG, "No unread notifications to display as pop-up, hiding it.");
                        notificationPopUpView.hideNotification();
                        currentlyDisplayedPopUpNotification = null; // Clear tracking as nothing is displayed
                    }
                } else {
                    // No notifications in the database at all, hide the pop-up
                    Log.d(TAG, "No notifications in database, hiding pop-up.");
                    notificationPopUpView.hideNotification();
                    currentlyDisplayedPopUpNotification = null; // Clear tracking
                }
            }
        });


        // Check if user name is stored, if not, prompt for it
        if (!mainViewModel.isUserNameStored()) {
            promptForUserName();
        } else {
            mainViewModel.updateGreetingAndQuote();
        }

        // Set OnClickListener for each button to navigate to respective activities
        buttonAddStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddStudentActivity.class));
            }
        });

        buttonAttendanceCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AttendanceActivity.class));
            }
        });

        buttonSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SendMessageActivity.class));
            }
        });

        buttonPromoteStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PromoteStudentActivity.class));
            }
        });

        buttonTrackAttendanceReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TrackAttendanceActivity.class));
            }
        });

        // Set OnClickListener for the new View Notifications button
        buttonViewNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, NotificationActivity.class));
            }
        });
    }

    /**
     * Displays an AlertDialog to prompt the user for their name.
     */
    private void promptForUserName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Welcome to Markly!");
        builder.setMessage("Please enter your full name:");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            String userName = input.getText().toString().trim();
            if (!userName.isEmpty()) {
                mainViewModel.saveUserName(userName);
            } else {
                Toast.makeText(MainActivity.this, "Name cannot be empty. Please restart app to enter.", Toast.LENGTH_LONG).show();
                mainViewModel.saveUserName("User");
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
            Toast.makeText(MainActivity.this, "Name not saved. Some features might be limited.", Toast.LENGTH_LONG).show();
            mainViewModel.saveUserName("User");
        });

        builder.setCancelable(false);
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mainViewModel.updateGreetingAndQuote();
        // The LiveData observer for notifications will automatically refresh when data changes,
        // so no explicit call to load notifications here is needed.
    }
}
