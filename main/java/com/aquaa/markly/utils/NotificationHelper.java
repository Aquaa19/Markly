package com.aquaa.markly.utils; // You might want a dedicated 'notifications' package

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat; // For backward compatibility
import androidx.core.app.NotificationManagerCompat; // For managing notifications

import com.aquaa.markly.R; // Make sure your R class is accessible

/**
 * Helper class for creating and managing Android system notifications.
 * Handles notification channels for Android O (API 26) and above.
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    // Define unique channel IDs for different types of notifications
    public static final String CHANNEL_ID_GENERAL = "markly_general_channel";
    public static final String CHANNEL_ID_REPORTS = "markly_reports_channel";
    public static final String CHANNEL_ID_ALERTS = "markly_alerts_channel";

    // Define unique notification IDs for different notifications to update or clear them later
    private static final int NOTIFICATION_ID_IMPORT_EXPORT = 1;
    private static final int NOTIFICATION_ID_ATTENDANCE_REPORT = 2;
    private static final int NOTIFICATION_ID_SMS_REPORT = 3;
    private static final int NOTIFICATION_ID_PROMOTE_REPORT = 4;


    /**
     * Creates notification channels. This should be called once when the app starts.
     * For example, in your Application class or MainActivity's onCreate.
     * @param context Application context.
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Android O (API 26) and above
            // General Channel
            NotificationChannel generalChannel = new NotificationChannel(
                    CHANNEL_ID_GENERAL,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT // Default importance for general info
            );
            generalChannel.setDescription("General application notifications.");

            // Reports Channel (e.g., Import/Export, Attendance Reports, SMS Reports)
            NotificationChannel reportsChannel = new NotificationChannel(
                    CHANNEL_ID_REPORTS,
                    "Reports & Summaries",
                    NotificationManager.IMPORTANCE_HIGH // High importance for reports that user should see
            );
            reportsChannel.setDescription("Notifications for data import/export, attendance, and SMS reports.");

            // Alerts Channel (for critical issues like promotion failures etc.)
            NotificationChannel alertsChannel = new NotificationChannel(
                    CHANNEL_ID_ALERTS,
                    "Critical Alerts",
                    NotificationManager.IMPORTANCE_HIGH // High importance for alerts
            );
            alertsChannel.setDescription("Critical alerts and warnings.");


            // Register the channels with the system
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(generalChannel);
                manager.createNotificationChannel(reportsChannel);
                manager.createNotificationChannel(alertsChannel);
                Log.d(TAG, "Notification channels created.");
            }
        }
    }

    /**
     * Sends a general system notification.
     * @param context Application context.
     * @param title Notification title.
     * @param message Notification message.
     * @param notificationId A unique ID for this specific notification.
     * @param channelId The ID of the notification channel to use.
     * @param priority Priority for older Android versions, Importance for newer.
     */
    private static void sendNotification(Context context, String title, String message, int notificationId, String channelId, int priority) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification) // You'll need to create this drawable (e.g., a simple bell icon)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message)) // Allow long messages to expand
                .setPriority(priority)
                .setAutoCancel(true); // Dismisses the notification when the user taps it

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "System notification sent: ID=" + notificationId + ", Title='" + title + "'");
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission denied: " + e.getMessage());
            // This catches cases where POST_NOTIFICATIONS permission might be denied
        } catch (Exception e) {
            Log.e(TAG, "Failed to send system notification: " + e.getMessage(), e);
        }
    }

    // --- Specific Notification Methods ---

    public static void sendImportExportNotification(Context context, String title, String message, String type) {
        String channel = CHANNEL_ID_REPORTS;
        int priority = NotificationCompat.PRIORITY_HIGH;
        if ("ERROR".equalsIgnoreCase(type)) {
            channel = CHANNEL_ID_ALERTS;
            priority = NotificationCompat.PRIORITY_MAX;
        } else if ("WARNING".equalsIgnoreCase(type)) {
            priority = NotificationCompat.PRIORITY_HIGH;
        }
        sendNotification(context, title, message, NOTIFICATION_ID_IMPORT_EXPORT, channel, priority);
    }

    public static void sendAttendanceReportNotification(Context context, String message, String type) {
        String channel = CHANNEL_ID_REPORTS;
        int priority = NotificationCompat.PRIORITY_DEFAULT;
        if ("ERROR".equalsIgnoreCase(type)) {
            channel = CHANNEL_ID_ALERTS;
            priority = NotificationCompat.PRIORITY_HIGH;
        }
        sendNotification(context, "Attendance Report", message, NOTIFICATION_ID_ATTENDANCE_REPORT, channel, priority);
    }

    public static void sendSmsReportNotification(Context context, String message, String type) {
        String channel = CHANNEL_ID_REPORTS;
        int priority = NotificationCompat.PRIORITY_HIGH;
        if ("ERROR".equalsIgnoreCase(type)) {
            channel = CHANNEL_ID_ALERTS;
            priority = NotificationCompat.PRIORITY_MAX;
        } else if ("WARNING".equalsIgnoreCase(type)) {
            priority = NotificationCompat.PRIORITY_HIGH;
        }
        sendNotification(context, "SMS Sending Report", message, NOTIFICATION_ID_SMS_REPORT, channel, priority);
    }

    public static void sendPromoteReportNotification(Context context, String message, String type) {
        String channel = CHANNEL_ID_REPORTS;
        int priority = NotificationCompat.PRIORITY_HIGH;
        if ("ERROR".equalsIgnoreCase(type)) {
            channel = CHANNEL_ID_ALERTS;
            priority = NotificationCompat.PRIORITY_MAX;
        } else if ("WARNING".equalsIgnoreCase(type)) {
            priority = NotificationCompat.PRIORITY_HIGH;
        }
        sendNotification(context, "Student Promotion Report", message, NOTIFICATION_ID_PROMOTE_REPORT, channel, priority);
    }

    // You can add more specific methods for other types of notifications as needed
}
