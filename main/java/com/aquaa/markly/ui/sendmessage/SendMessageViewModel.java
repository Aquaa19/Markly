package com.aquaa.markly.ui.sendmessage;

import android.app.Application;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aquaa.markly.data.database.Notification;
import com.aquaa.markly.data.database.Student;
import com.aquaa.markly.data.database.Attendance;
import com.aquaa.markly.data.repository.StudentRepository;
import com.aquaa.markly.utils.NotificationHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for the SendMessageActivity.
 * Handles fetching students based on semester and absence status.
 */
public class SendMessageViewModel extends AndroidViewModel {

    private static final String TAG = "SendMessageViewModel";
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private StudentRepository studentRepository;
    private MutableLiveData<List<Integer>> allSemesters = new MutableLiveData<>();
    private MutableLiveData<List<Student>> studentsToDisplay = new MutableLiveData<>();
    private MutableLiveData<String> smsResult = new MutableLiveData<>();

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public SendMessageViewModel(Application application) {
        super(application);
        studentRepository = new StudentRepository(application);
    }

    public LiveData<List<Integer>> getAllSemesters() {
        return allSemesters;
    }

    public LiveData<List<Student>> getStudentsToDisplay() {
        return studentsToDisplay;
    }

    public LiveData<String> getSmsResult() {
        return smsResult;
    }

    public void loadAllSemesters() {
        dbExecutor.execute(() -> {
            try {
                List<Integer> semesters = studentRepository.getAllSemestersSync();
                if (semesters != null) {
                    allSemesters.postValue(semesters);
                } else {
                    allSemesters.postValue(new ArrayList<>());
                    smsResult.postValue("No semesters found.");
                    Log.w(TAG, "getAllSemestersSync returned null.");
                }
            } catch (Exception e) {
                allSemesters.postValue(new ArrayList<>());
                String errorMessage = "Error loading semesters: " + e.getMessage();
                smsResult.postValue(errorMessage);
                Log.e(TAG, errorMessage, e);
            }
        });
    }

    public void loadAllStudentsForSemester(int semester) {
        dbExecutor.execute(() -> {
            try {
                List<Student> students = studentRepository.getStudentsBySemesterSync(semester);
                if (students != null) {
                    studentsToDisplay.postValue(students);
                } else {
                    studentsToDisplay.postValue(new ArrayList<>());
                    smsResult.postValue("No students found for semester " + semester + ".");
                    Log.w(TAG, "getStudentsBySemesterSync returned null for semester: " + semester);
                }
            } catch (Exception e) {
                studentsToDisplay.postValue(new ArrayList<>());
                String errorMessage = "Error loading all students for semester: " + semester + ": " + e.getMessage();
                smsResult.postValue(errorMessage);
                Log.e(TAG, errorMessage, e);
            }
        });
    }

    public void loadAbsentStudentsForSpecificDate(long dateMillis, int semester) {
        dbExecutor.execute(() -> {
            try {
                Log.d(TAG, "loadAbsentStudentsForSpecificDate (INIT): Loading for date: " + sdf.format(new Date(dateMillis)) + " (" + dateMillis + ") and semester: " + semester);

                List<Student> absentStudentsOnSelectedDate = studentRepository.getAbsentStudentsOnDate(dateMillis);
                Log.d(TAG, "loadAbsentStudentsForSpecificDate (QUERY RESULT): Students (SMS pending) on selected date " + sdf.format(new Date(dateMillis)) + " (" + dateMillis + ") : " + (absentStudentsOnSelectedDate != null ? absentStudentsOnSelectedDate.size() : 0));


                List<Student> absentStudentsInSemester = new ArrayList<>();
                if (absentStudentsOnSelectedDate != null) {
                    for (Student student : absentStudentsOnSelectedDate) {
                        if (student != null && student.getCurrentSemester() == semester) {
                            absentStudentsInSemester.add(student);
                        }
                    }
                }

                studentsToDisplay.postValue(absentStudentsInSemester);
                if (absentStudentsInSemester.isEmpty()) {
                    smsResult.postValue("No absent students found for semester " + semester + " on " + sdf.format(new Date(dateMillis)) + " for whom SMS is pending.");
                    Log.i(TAG, "No pending absent students found for semester " + semester + " on " + sdf.format(new Date(dateMillis)) + " (" + dateMillis + ")");
                } else {
                    smsResult.postValue("Loaded " + absentStudentsInSemester.size() + " absent students for semester " + semester + " on " + sdf.format(new Date(dateMillis)) + ".");
                    Log.i(TAG, "Loaded " + absentStudentsInSemester.size() + " pending absent students for semester " + semester + " on " + sdf.format(new Date(dateMillis)) + " (" + dateMillis + ")");
                }


            } catch (Exception e) {
                studentsToDisplay.postValue(new ArrayList<>());
                String errorMessage = "Error loading absent students for specific date: " + e.getMessage();
                smsResult.postValue(errorMessage);
                Log.e(TAG, errorMessage, e);
            }
        });
    }

    /**
     * Sends SMS messages to the selected list of students.
     * Successfully messaged students are removed from the `studentsToDisplay` list
     * and their corresponding attendance records are marked as `isSmsSent = true` in the database.
     * Students for whom the message fails remain in the list.
     * An in-app notification IS generated for this action.
     * @param studentsToSendSms The list of Student objects to send SMS to.
     * @param dateForSms The specific date (in milliseconds) for which SMS is being sent.
     * @param customMessage The custom message to send, or null for default message.
     */
    public void sendSmsToStudents(List<Student> studentsToSendSms, long dateForSms, String customMessage) { // Added customMessage parameter
        dbExecutor.execute(() -> {
            String notificationTitle = "SMS Sending Report";
            String notificationType = "SUCCESS";

            if (studentsToSendSms == null || studentsToSendSms.isEmpty()) {
                smsResult.postValue("No students selected to send SMS.");
                Log.w(TAG, "sendSmsToStudents: Attempted to send SMS to an empty or null list of students.");
                return;
            }

            List<Student> currentDisplayedStudents = studentsToDisplay.getValue();
            if (currentDisplayedStudents == null) {
                currentDisplayedStudents = new ArrayList<>();
            }
            List<Student> studentsRemainingAfterSend = new ArrayList<>(currentDisplayedStudents);

            int sentCount = 0;
            List<String> failedRecipients = new ArrayList<>();
            SmsManager smsManager = SmsManager.getDefault();

            Log.d(TAG, "sendSmsToStudents (INIT): SMS will be marked as sent for date: " + sdf.format(new Date(dateForSms)) + " (" + dateForSms + ")");


            for (Student student : studentsToSendSms) {
                if (student == null) {
                    Log.w(TAG, "sendSmsToStudents: Attempted to send SMS to a null student object.");
                    continue;
                }
                String phoneNumber = student.getGuardianMobile();
                // Determine message content: custom or default
                String message;
                if (customMessage != null && !customMessage.isEmpty()) {
                    message = customMessage;
                } else {
                    message = "Dear guardian, your ward " + student.getName() + " was absent on " + sdf.format(new Date(dateForSms)) + "."; // Default message
                }

                boolean smsSentSuccessfully = false;

                if (phoneNumber == null || phoneNumber.isEmpty()) {
                    failedRecipients.add(student.getName() + " (No guardian mobile)");
                    Log.w(TAG, "Skipping SMS for " + student.getName() + ": No guardian mobile number found.");
                    continue;
                }

                try {
                    smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                    smsSentSuccessfully = true;
                    Log.d(TAG, "Attempted to send SMS to " + phoneNumber + " for " + student.getName() + " with message: " + message);

                } catch (Exception e) {
                    failedRecipients.add(student.getName() + " (" + e.getMessage() + ")");
                    Log.e(TAG, "Failed to send SMS to " + student.getName() + " (" + phoneNumber + "): " + e.getMessage(), e);
                }

                if (smsSentSuccessfully) {
                    sentCount++;
                    studentsRemainingAfterSend.remove(student);

                    Log.d(TAG, "sendSmsToStudents (ATTEMPT UPDATE): Student: " + student.getName() + " (ID: " + student.getStudentId() + "), Date: " + sdf.format(new Date(dateForSms)) + " (Value: " + dateForSms + "), Setting isSmsSent to TRUE.");
                    studentRepository.updateAttendanceSmsSentStatus(student.getStudentId(), dateForSms, true);

                    Attendance updatedAttendance = studentRepository.getAttendanceByStudentAndDate(student.getStudentId(), dateForSms);
                    if (updatedAttendance != null) {
                        Log.d(TAG, "sendSmsToStudents (VERIFICATION): Student: " + student.getName() + ", Date: " + sdf.format(new Date(dateForSms)) + ". isSmsSent status AFTER update: " + updatedAttendance.isSmsSent());
                    } else {
                        Log.e(TAG, "sendSmsToStudents (VERIFICATION ERROR): Could not fetch attendance record for student " + student.getName() + " on date " + sdf.format(new Date(dateForSms)) + " immediately after update attempt.");
                    }

                    Log.d(TAG, "SMS sent successfully to: " + student.getName() + " (ID: " + student.getStudentId() + "). Marked attendance as SMS sent for date: " + sdf.format(new Date(dateForSms)));
                } else {
                    Log.w(TAG, "sendSmsToStudents: SMS not sent successfully to: " + student.getName() + " (ID: " + student.getStudentId() + ").");
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.e(TAG, "SMS sending delay interrupted.", e);
                }
            }

            studentsToDisplay.postValue(studentsRemainingAfterSend);

            String messageResult;
            if (sentCount > 0 && failedRecipients.isEmpty()) {
                messageResult = "Successfully sent SMS to " + sentCount + " student(s) for " + sdf.format(new Date(dateForSms)) + ".";
                notificationType = "SUCCESS";
            } else if (sentCount > 0 && !failedRecipients.isEmpty()) {
                messageResult = "Sent SMS to " + sentCount + " student(s) for " + sdf.format(new Date(dateForSms)) + ". Failed for: " + String.join(", ", failedRecipients) + ".";
                notificationType = "WARNING";
            } else if (sentCount == 0 && !failedRecipients.isEmpty()) {
                messageResult = "Failed to send SMS to all selected students for " + sdf.format(new Date(dateForSms)) + ": " + String.join(", ", failedRecipients) + ".";
                notificationType = "ERROR";
            } else {
                messageResult = "No SMS sent to any selected students for " + sdf.format(new Date(dateForSms)) + ".";
                notificationType = "INFO";
            }

            smsResult.postValue(messageResult);
            Log.i(TAG, "sendSmsToStudents: SMS sending operation complete: " + messageResult);

            studentRepository.insertNotification(new Notification(
                    notificationTitle,
                    messageResult,
                    System.currentTimeMillis(),
                    false,
                    notificationType
            ));
            Log.d(TAG, "sendSmsToStudents: In-app notification generated for SMS sending report.");

            NotificationHelper.sendSmsReportNotification(getApplication(), messageResult, notificationType);
            Log.d(TAG, "sendSmsToStudents: System notification triggered for SMS sending report.");
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!dbExecutor.isShutdown()) {
            dbExecutor.shutdown();
            Log.d(TAG, "dbExecutor shutdown initiated in SendMessageViewModel.");
        }
    }
}
