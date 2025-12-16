package com.greg.eventTracker;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


public class NotifWorker extends Worker {
    private static final String TAG = "NotificationWorker";
    private static final String CHANNEL_ID = "EVENT_REMINDERS";

    public NotifWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Worker Started.");

        int userID = getInputData().getInt("USER_ID", -1);
        if (userID == -1) {
            Log.e(TAG, "Invalid User ID");
            return Result.failure();
        }
        getNotifEvents(userID);
        return Result.success();
    }

    //Defines the content of the events
    //Creates log message if no events are found
    private void getNotifEvents(int userID) {
        Context context = getApplicationContext();
        Events db = new Events(context);

        EventData[] tomorrowsEvents = db.tomorrowEvents(userID);

        if (tomorrowsEvents == null || tomorrowsEvents.length == 0) {
            Log.d(TAG, "No events found for Tomorrow.");
            return; // No work to do
        }

        Log.d(TAG, "Found " + tomorrowsEvents.length + " events for tomorrow. Creating notifications.");

        createNotificationChannel(context);

        for (int i = 0; i < tomorrowsEvents.length; i++) {
            EventData event = tomorrowsEvents[i];
            String notificationTitle = "Tomorrows Events: " + event.getName();
            String notificationText = "Location: " + event.getLocation() + " at " + event.getTime();

            int notificationId = (int) event.getId();

            showNotification(context, notificationTitle, notificationText, notificationId);
        }
        int eventCount = 1;

    }

    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is new and not in the support library
    // Register the channel with the system
    private void createNotificationChannel(Context context) {

        CharSequence name = "Event Reminders";
        String description = "Channel for upcoming event notifications";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        Log.d(TAG, "Notification channel created.");
    }

    // Defines how the notification is displayed
    // Checks for permission before attempting to notify
    private void showNotification(Context context, String title, String text, int notificationId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);


        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Cannot show notification: POST_NOTIFICATIONS permission not granted.");
            return;
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        Log.d(TAG, "Notification sent with ID: " + notificationId);
    }
}

