// MainViewModel.java
package com.aquaa.markly.ui.main;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * ViewModel for the MainActivity.
 * Handles user greeting logic, motivational quotes, and provides data to the UI.
 */
public class MainViewModel extends AndroidViewModel {

    private static final String PREFS_NAME = "MarklyPrefs";
    private static final String KEY_USER_NAME = "userName";

    private MutableLiveData<String> greetingMessage = new MutableLiveData<>();
    private MutableLiveData<String> motivationalQuote = new MutableLiveData<>(); // LiveData for the quote
    private SharedPreferences sharedPreferences;
    private Random random = new Random();

    // List of motivational quotes provided by the user
    private final List<String> quotes = Arrays.asList(
            "Success is not final, failure is not fatal: It is the courage to continue that counts.",
            "The only limit to our realization of tomorrow is our doubts of today.",
            "It’s not whether you get knocked down, it’s whether you get up.",
            "Strength does not come from winning. Your struggles develop your strengths.",
            "The harder the struggle, the more glorious the triumph.",
            "Do the thing you fear most, and the death of fear is certain.",
            "You miss 100% of the shots you don’t take.",
            "The only way to do great work is to love what you do.",
            "Don’t watch the clock; do what it does. Keep going.",
            "Action is the foundational key to all success.",
            "Whether you think you can or you think you can’t, you’re right.",
            "Your time is limited, don’t waste it living someone else’s life.",
            "The expert in anything was once a beginner.",
            "Success is stumbling from failure to failure with no loss of enthusiasm.",
            "The only person you are destined to become is the person you decide to be.",
            "Don’t be pushed by your problems. Be led by your dreams.",
            "The future belongs to those who believe in the beauty of their dreams.",
            "Passion is energy. Feel the power that comes from focusing on what excites you.",
            "If you want to live a happy life, tie it to a goal, not to people or things.",
            "Dream big and dare to fail.",
            "You are never too old to set another goal or to dream a new dream.",
            "Believe you can and you’re halfway there.",
            "No one can make you feel inferior without your consent.",
            "The only thing standing between you and your goal is the story you keep telling yourself.",
            "You were born to win, but to be a winner, you must plan to win, prepare to win, and expect to win.",
            "If it doesn’t challenge you, it won’t change you.",
            "The price of discipline is always less than the pain of regret.",
            "When something is important enough, you do it even if the odds are not in your favor.",
            "Success is not for the chosen few, it's for the few who choose it — and never quit.",
            "I’m not talented, I am obsessed.",
            "If people knew how hard I had to work to gain my mastery, it would not seem so wonderful at all.",
            "Success is no accident. It is hard work, perseverance, learning, studying, sacrifice.",
            "What one man can do, another can do.",
            "Everything should be made as simple as possible, but not simpler.",
            "Do not worry about your problems with mathematics. I assure you, mine are far greater.",
            "He who has a why to live can bear almost any how.",
            "A warrior is not born in comfort. He is built through trials.",
            "You are not late. You are just early in your journey."
    );

    public MainViewModel(Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Exposes the greeting message to the UI.
     * @return LiveData holding the greeting string.
     */
    public LiveData<String> getGreetingMessage() {
        return greetingMessage;
    }

    /**
     * Exposes a randomly selected motivational quote to the UI.
     * @return LiveData holding the quote string.
     */
    public LiveData<String> getMotivationalQuote() {
        return motivationalQuote;
    }

    /**
     * Checks if the user's name is already stored.
     * @return True if name exists, false otherwise.
     */
    public boolean isUserNameStored() {
        return sharedPreferences.contains(KEY_USER_NAME);
    }

    /**
     * Saves the user's full name to SharedPreferences.
     * @param fullName The full name entered by the user.
     */
    public void saveUserName(String fullName) {
        sharedPreferences.edit().putString(KEY_USER_NAME, fullName).apply();
        updateGreetingAndQuote(); // Update greeting and quote immediately after saving
    }

    /**
     * Retrieves the user's first name from the stored full name.
     * @return The first name, or "User" if not found or empty.
     */
    private String getFirstName() {
        String fullName = sharedPreferences.getString(KEY_USER_NAME, "User");
        if (fullName == null || fullName.trim().isEmpty()) {
            return "User";
        }
        // Extract the first word as the first name
        String[] parts = fullName.trim().split("\\s+");
        return parts[0];
    }

    /**
     * Generates a time-based greeting (Good Morning/Afternoon/Evening).
     * @return The greeting string.
     */
    private String getTimeBasedGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        if (hourOfDay >= 5 && hourOfDay < 12) {
            return "Good Morning";
        } else if (hourOfDay >= 12 && hourOfDay < 18) {
            return "Good Afternoon";
        } else {
            return "Good Evening";
        }
    }

    /**
     * Selects a random quote from the predefined list.
     * @return A random motivational quote.
     */
    private String getRandomQuote() {
        if (quotes.isEmpty()) {
            return "Stay motivated!"; // Fallback quote
        }
        return quotes.get(random.nextInt(quotes.size()));
    }

    /**
     * Updates both the greeting message and selects a new random quote.
     * This method should be called when the screen is resumed or user name is set.
     */
    public void updateGreetingAndQuote() {
        String firstName = getFirstName();
        String timeGreeting = getTimeBasedGreeting();
        greetingMessage.postValue(firstName + ", " + timeGreeting);

        String selectedQuote = getRandomQuote();
        motivationalQuote.postValue(selectedQuote);
    }
}
