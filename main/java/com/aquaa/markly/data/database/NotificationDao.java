package com.aquaa.markly.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface NotificationDao {

    @Insert
    void insertNotification(Notification notification);

    @Update
    void updateNotification(Notification notification);

    // Get all notifications, ordered by timestamp descending (newest first)
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    LiveData<List<Notification>> getAllNotifications();

    // Get unread notifications count
    @Query("SELECT COUNT(*) FROM notifications WHERE is_read = 0")
    LiveData<Integer> getUnreadNotificationCount();

    // Mark a specific notification as read
    @Query("UPDATE notifications SET is_read = 1 WHERE notification_id = :notificationId")
    void markNotificationAsRead(long notificationId);

    // Mark all notifications as read
    @Query("UPDATE notifications SET is_read = 1")
    void markAllNotificationsAsRead();

    // Delete a specific notification
    @Query("DELETE FROM notifications WHERE notification_id = :notificationId")
    void deleteNotification(long notificationId);

    // Delete all notifications
    @Query("DELETE FROM notifications")
    void deleteAllNotifications();
}
