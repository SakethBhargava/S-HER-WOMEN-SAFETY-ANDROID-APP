package com.darkness.WSafety;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.util.Log;

public class AlertForegroundService extends Service {

    private static final String CHANNEL_ID = "AlertServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        // Create a persistent notification to run the service in the foreground
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("S-HER Alert Service")
                .setContentText("Monitoring your safety...")
                .setSmallIcon(R.drawable.ic_alert) // Ensure an icon named ic_alert is present in drawable
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true) // Ensures the notification cannot be dismissed by the user
                .build();

        // Start the service in the foreground with the created notification
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Logic to execute when the service is started
        Log.d("AlertForegroundService", "Foreground service is running...");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("AlertForegroundService", "Foreground service has been stopped.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not using bound service, so return null
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Define the channel properties
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "S-HER Alert Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            serviceChannel.setDescription("Channel for S-HER Alert Service");

            // Register the channel with the system
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
