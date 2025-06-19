package com.aquaa.markly.ui.notifications; // New package for notifications UI

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aquaa.markly.data.database.Notification;
import com.aquaa.markly.data.repository.StudentRepository; // StudentRepository now handles notifications

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationViewModel extends AndroidViewModel {

    private StudentRepository mRepository;
    private LiveData<List<Notification>> allNotifications;
    private MutableLiveData<String> operationResult = new MutableLiveData<>(); // For UI feedback on actions

    // Executor for database operations (can reuse StudentRepository's if needed, but a dedicated one is fine)
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public NotificationViewModel(Application application) {
        super(application);
        mRepository = new StudentRepository(application);
        allNotifications = mRepository.getAllNotifications(); // Get LiveData of all notifications
    }

    /**
     * Exposes all notifications from the database, ordered newest first.
     * @return LiveData list of Notification objects.
     */
    public LiveData<List<Notification>> getAllNotifications() {
        return allNotifications;
    }

    /**
     * Exposes operation results (e.g., success/failure messages) to the UI.
     * @return LiveData holding a message string.
     */
    public LiveData<String> getOperationResult() {
        return operationResult;
    }

    /**
     * Marks a specific notification as read.
     * @param notificationId The ID of the notification to mark as read.
     */
    public void markNotificationAsRead(long notificationId) {
        dbExecutor.execute(() -> {
            try {
                mRepository.markNotificationAsRead(notificationId);
                operationResult.postValue("Notification marked as read.");
            } catch (Exception e) {
                operationResult.postValue("Failed to mark notification as read: " + e.getMessage());
            }
        });
    }

    /**
     * Marks all notifications as read.
     */
    public void markAllNotificationsAsRead() {
        dbExecutor.execute(() -> {
            try {
                mRepository.markAllNotificationsAsRead();
                operationResult.postValue("All notifications marked as read.");
            } catch (Exception e) {
                operationResult.postValue("Failed to mark all notifications as read: " + e.getMessage());
            }
        });
    }

    /**
     * Deletes a specific notification.
     * @param notificationId The ID of the notification to delete.
     */
    public void deleteNotification(long notificationId) {
        dbExecutor.execute(() -> {
            try {
                mRepository.deleteNotification(notificationId);
                operationResult.postValue("Notification deleted.");
            } catch (Exception e) {
                operationResult.postValue("Failed to delete notification: " + e.getMessage());
            }
        });
    }

    /**
     * Deletes all notifications.
     */
    public void deleteAllNotifications() {
        dbExecutor.execute(() -> {
            try {
                mRepository.deleteAllNotifications();
                operationResult.postValue("All notifications deleted.");
            } catch (Exception e) {
                operationResult.postValue("Failed to delete all notifications: " + e.getMessage());
            }
        });
    }

    /**
     * Inserts a new notification. This method might be called by other ViewModels indirectly
     * through the repository, but could also be used directly for testing or internal notifications.
     * @param title The title of the notification.
     * @param message The message content.
     * @param type The type of notification (e.g., "SUCCESS", "ERROR").
     */
    public void insertNotification(String title, String message, String type) {
        dbExecutor.execute(() -> {
            try {
                Notification notification = new Notification(title, message, System.currentTimeMillis(), false, type);
                mRepository.insertNotification(notification);
            } catch (Exception e) {
                // Log error, but don't notify UI for a notification about a notification failure
                System.err.println("Failed to insert notification: " + e.getMessage());
            }
        });
    }
}
