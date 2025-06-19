package com.aquaa.markly.ui.notifications;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.aquaa.markly.R;
import com.aquaa.markly.data.database.Notification;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class NotificationAdapter extends ListAdapter<Notification, NotificationAdapter.NotificationViewHolder> {

    private OnNotificationActionListener listener;
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()); // Corrected SimpleDateFormat

    public NotificationAdapter(OnNotificationActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification currentNotification = getItem(position);
        holder.bind(currentNotification);
    }

    /**
     * ViewHolder class for individual notification items.
     */
    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private TextView messageTextView;
        private TextView timestampTextView;
        private ImageView statusImageView;
        private Button markReadButton;
        private Button deleteButton;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.text_view_notification_title);
            messageTextView = itemView.findViewById(R.id.text_view_notification_message);
            timestampTextView = itemView.findViewById(R.id.text_view_notification_timestamp);
            statusImageView = itemView.findViewById(R.id.image_view_notification_status);
            markReadButton = itemView.findViewById(R.id.button_mark_read);
            deleteButton = itemView.findViewById(R.id.button_delete_notification);
        }

        public void bind(Notification notification) {
            titleTextView.setText(notification.getTitle());
            messageTextView.setText(notification.getMessage());
            timestampTextView.setText(dateTimeFormat.format(notification.getTimestamp()));

            // Resolve theme attributes for dynamic background colors
            TypedValue typedValue = new TypedValue();
            Context context = itemView.getContext();

            // Set styling based on read status
            if (notification.isRead()) {
                statusImageView.setVisibility(View.GONE);
                titleTextView.setTypeface(null, Typeface.NORMAL);
                messageTextView.setTypeface(null, Typeface.NORMAL);
                // Use colorSurfaceContainerLow from theme
                context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerLow, typedValue, true);
                itemView.setBackgroundColor(typedValue.data);
                markReadButton.setVisibility(View.GONE);
            } else {
                statusImageView.setVisibility(View.VISIBLE);
                titleTextView.setTypeface(null, Typeface.BOLD);
                messageTextView.setTypeface(null, Typeface.BOLD);
                // Use colorSurfaceContainer from theme
                context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainer, typedValue, true);
                itemView.setBackgroundColor(typedValue.data);
                markReadButton.setVisibility(View.VISIBLE);
            }

            // Set button click listeners
            markReadButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMarkReadClick(notification.getNotificationId());
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(notification.getNotificationId());
                }
            });

            // Handle item click for marking as read (optional, can be redundant if mark read button exists)
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification.getNotificationId(), notification.isRead());
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<Notification> DIFF_CALLBACK = new DiffUtil.ItemCallback<Notification>() {
        @Override
        public boolean areItemsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
            return oldItem.getNotificationId() == newItem.getNotificationId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getMessage().equals(newItem.getMessage()) &&
                    oldItem.getTimestamp() == newItem.getTimestamp() &&
                    oldItem.isRead() == newItem.isRead() &&
                    oldItem.getType().equals(newItem.getType());
        }
    };

    public interface OnNotificationActionListener {
        void onMarkReadClick(long notificationId);
        void onDeleteClick(long notificationId);
        void onNotificationClick(long notificationId, boolean isRead);
    }
}
