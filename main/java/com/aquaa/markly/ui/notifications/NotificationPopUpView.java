package com.aquaa.markly.ui.notifications;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Handler; // Import Handler
import android.os.Looper; // Import Looper for Main thread Handler
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.aquaa.markly.R;
import com.aquaa.markly.data.database.Notification;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Custom View to display an in-app notification pop-up.
 * Supports swipe-to-dismiss functionality without marking the notification as read.
 * Now includes auto-hide feature with a swipe-up animation after a delay.
 */
public class NotificationPopUpView extends FrameLayout {

    private TextView titleTextView;
    private TextView messageTextView;
    private TextView timestampTextView;
    private GestureDetector gestureDetector;
    private Notification currentNotification;
    private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm", Locale.getDefault());

    private static final int SWIPE_THRESHOLD = 100; // Minimum distance for a swipe
    private static final int SWIPE_VELOCITY_THRESHOLD = 100; // Minimum velocity for a swipe

    // Auto-hide feature variables
    private Handler autoHideHandler = new Handler(Looper.getMainLooper()); // Handler for UI thread
    private Runnable autoHideRunnable;
    private static final long AUTO_HIDE_DELAY_MS = 3000; // 3 seconds delay for auto-hide

    public NotificationPopUpView(Context context) {
        super(context);
        init(context);
    }

    public NotificationPopUpView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NotificationPopUpView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.notification_popup_view, this, true);
        titleTextView = findViewById(R.id.text_view_notification_title);
        messageTextView = findViewById(R.id.text_view_notification_message);
        timestampTextView = findViewById(R.id.text_view_notification_timestamp);

        // Initially hide the view
        setVisibility(GONE);

        // Initialize GestureDetector for swipe detection
        gestureDetector = new GestureDetector(context, new GestureListener());

        // Initialize auto-hide runnable
        autoHideRunnable = () -> {
            if (getVisibility() == VISIBLE) { // Only auto-hide if currently visible
                animateDismiss(true, false); // Swipe up animation for auto-hide
            }
        };
    }

    /**
     * Displays a notification in the pop-up view.
     * Starts the auto-hide timer.
     *
     * @param notification The Notification object to display.
     */
    public void showNotification(Notification notification) {
        // Cancel any pending auto-hide actions from previous notifications
        autoHideHandler.removeCallbacks(autoHideRunnable);

        this.currentNotification = notification;
        titleTextView.setText(notification.getTitle());
        messageTextView.setText(notification.getMessage());
        timestampTextView.setText(sdf.format(new Date(notification.getTimestamp())));
        setVisibility(VISIBLE);
        setAlpha(1f); // Ensure it's fully visible
        setTranslationY(0f); // Reset position for animation
        setTranslationX(0f); // Reset position for animation

        // Start the auto-hide timer
        autoHideHandler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY_MS);
    }

    /**
     * Hides the notification pop-up.
     * Cancels any pending auto-hide actions.
     */
    public void hideNotification() {
        autoHideHandler.removeCallbacks(autoHideRunnable); // Crucial: cancel on manual hide
        setVisibility(GONE);
        this.currentNotification = null; // Clear the current notification
    }

    // Override onTouchEvent to pass touch events to GestureDetector
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Also remove auto-hide callbacks if user starts interacting with the pop-up
        autoHideHandler.removeCallbacks(autoHideRunnable);
        return gestureDetector.onTouchEvent(event);
    }

    // Implement onInterceptTouchEvent to ensure touch events are captured by this view
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Intercept all touch events to handle them with our GestureDetector
        // This is crucial to ensure the swipe is detected correctly
        boolean handled = gestureDetector.onTouchEvent(ev);
        return handled || super.onInterceptTouchEvent(ev);
    }


    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            // Must return true to get subsequent events like onFling
            // Also reset animation properties in case view is reused without full hide/show cycle
            setAlpha(1f);
            setTranslationX(0f);
            setTranslationY(0f);
            // On touch down, stop auto-hide timer, as user is interacting
            autoHideHandler.removeCallbacks(autoHideRunnable);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();

            // Detect vertical swipe (up or down)
            if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY < 0) { // Swiped up
                    animateDismiss(true, false); // Slide up
                } else { // Swiped down
                    animateDismiss(true, true); // Slide down
                }
                return true;
            }
            // Detect horizontal swipe (left or right)
            else if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX < 0) { // Swiped left
                    animateDismiss(false, false); // Slide left
                } else { // Swiped right
                    animateDismiss(false, true); // Slide right
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Animates the dismissal of the notification pop-up.
     *
     * @param isVertical True if sliding vertically (up/down), false for horizontally (left/right).
     * @param isPositiveDirection True if sliding down/right, false for up/left.
     */
    private void animateDismiss(boolean isVertical, boolean isPositiveDirection) {
        // Cancel the auto-hide runnable immediately when an animation starts
        autoHideHandler.removeCallbacks(autoHideRunnable);

        int direction = isPositiveDirection ? 1 : -1;
        float targetTranslation = isVertical ? (getHeight() * direction) : (getWidth() * direction);

        // Animate slide-out and fade-out
        animate()
                .translationY(isVertical ? targetTranslation : 0f)
                .translationX(isVertical ? 0f : targetTranslation)
                .alpha(0f)
                .setDuration(300) // Animation duration
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        hideNotification(); // Hide view after animation
                        // Important: DO NOT mark notification as read or delete it here.
                        // The request specifies it should remain unread in the notification window.
                    }
                })
                .start();
    }
}
