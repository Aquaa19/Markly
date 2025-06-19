package com.aquaa.markly.ui.notifications;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration; // Import DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aquaa.markly.R;
import com.aquaa.markly.data.database.Notification; // Import Notification entity

import java.util.List;

public class NotificationActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationActionListener {

    private NotificationViewModel notificationViewModel;
    private RecyclerView notificationsRecyclerView;
    private NotificationAdapter notificationAdapter;
    private TextView emptyNotificationsTextView;
    private Button markAllReadButton;
    private Button deleteAllNotificationsButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Initialize ViewModel
        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        // Initialize UI components
        notificationsRecyclerView = findViewById(R.id.recycler_view_notifications);
        emptyNotificationsTextView = findViewById(R.id.empty_notifications_text_view);
        markAllReadButton = findViewById(R.id.button_mark_all_read);
        deleteAllNotificationsButton = findViewById(R.id.button_delete_all_notifications);

        // Setup RecyclerView
        notificationAdapter = new NotificationAdapter(this); // Pass 'this' as listener
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationsRecyclerView.setAdapter(notificationAdapter);
        notificationsRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL)); // Add dividers


        // Observe all notifications
        notificationViewModel.getAllNotifications().observe(this, notifications -> {
            notificationAdapter.submitList(notifications);
            updateEmptyView(notifications);
        });

        // Observe operation results (for Toast messages)
        notificationViewModel.getOperationResult().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(NotificationActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });

        // Set action button listeners
        markAllReadButton.setOnClickListener(v -> showMarkAllReadConfirmation());
        deleteAllNotificationsButton.setOnClickListener(v -> showDeleteAllConfirmation());
    }

    /**
     * Updates the visibility of the empty view based on the notification list.
     * @param notifications The current list of notifications.
     */
    private void updateEmptyView(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            emptyNotificationsTextView.setVisibility(View.VISIBLE);
            notificationsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyNotificationsTextView.setVisibility(View.GONE);
            notificationsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Shows a confirmation dialog for marking all notifications as read.
     */
    private void showMarkAllReadConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Mark All as Read")
                .setMessage("Are you sure you want to mark all notifications as read?")
                .setPositiveButton("Yes", (dialog, which) -> notificationViewModel.markAllNotificationsAsRead())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Shows a confirmation dialog for deleting all notifications.
     */
    private void showDeleteAllConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete All Notifications")
                .setMessage("Are you sure you want to delete all notifications? This action cannot be undone.")
                .setPositiveButton("Yes, Delete", (dialog, which) -> notificationViewModel.deleteAllNotifications())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // --- OnNotificationActionListener implementation ---

    @Override
    public void onMarkReadClick(long notificationId) {
        notificationViewModel.markNotificationAsRead(notificationId);
    }

    @Override
    public void onDeleteClick(long notificationId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Notification")
                .setMessage("Are you sure you want to delete this notification?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> notificationViewModel.deleteNotification(notificationId))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onNotificationClick(long notificationId, boolean isRead) {
        // If the user clicks on an unread notification, mark it as read
        if (!isRead) {
            notificationViewModel.markNotificationAsRead(notificationId);
        }
        // You can add more logic here, e.g., show a dialog with full message, or navigate elsewhere.
        Toast.makeText(this, "Notification clicked. ID: " + notificationId, Toast.LENGTH_SHORT).show();
    }
}
