package com.aquaa.markly.data.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for storing in-app notifications.
 */
@Entity(tableName = "notifications")
public class Notification {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "notification_id")
    private long notificationId;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "message")
    private String message;

    @ColumnInfo(name = "timestamp")
    private long timestamp; // Unix timestamp in milliseconds

    @ColumnInfo(name = "is_read")
    private boolean isRead;

    @ColumnInfo(name = "type") // e.g., "SUCCESS", "ERROR", "WARNING", "INFO"
    private String type;

    /**
     * Constructor for the Notification entity.
     * @param title The title of the notification.
     * @param message The main message content of the notification.
     * @param timestamp The Unix timestamp (in milliseconds) when the notification was created.
     * @param isRead Whether the notification has been read by the user.
     * @param type The type of notification (e.g., "SUCCESS", "ERROR").
     */
    public Notification(String title, String message, long timestamp, boolean isRead, String type) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.type = type;
    }

    // --- Getters and Setters ---

    public long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(long notificationId) {
        this.notificationId = notificationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
