package com.darkness.WSafety;

import android.net.Uri;
import java.util.HashSet;
import java.util.Set;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ToggleButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;

public class SmsActivity extends AppCompatActivity {

    private ToggleButton toggleAlert;
    private FusedLocationProviderClient fusedLocationClient;
    private android.os.Handler alertHandler;
    private Runnable alertRunnable;
    private boolean isAlertActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        toggleAlert = findViewById(R.id.toggleAlert);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        alertHandler = new android.os.Handler();

        // Check saved state
        SharedPreferences preferences = getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        isAlertActive = preferences.getBoolean("alert_state", false);
        toggleAlert.setChecked(isAlertActive);
        if (isAlertActive) {
            startAlertService();
        }

        toggleAlert.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startAlertService();
                sendSmsAlert();
                saveAlertState(true);  // Save state as ON
            } else {
                stopAlertService();
                saveAlertState(false); // Save state as OFF
            }
        });

        Button start, stop, helpline;
        stop = findViewById(R.id.stopService);
        start = findViewById(R.id.startService);
        helpline = findViewById(R.id.btn_helpline);
        start.setOnClickListener(this::startServiceV);
        stop.setOnClickListener(this::stopService);
        helpline.setOnClickListener(this::helplines);
    }

    private void startAlertService() {
        isAlertActive = true;
        Log.d("SmsActivity", "Alert service started.");

        alertRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAlertActive) {
                    sendSmsAlert();
                    alertHandler.postDelayed(this, 10 * 60 * 1000);
                }
            }
        };

        alertHandler.post(alertRunnable);

        // Start foreground service
        Intent intent = new Intent(this, AlertForegroundService.class);
        ContextCompat.startForegroundService(this, intent);
    }

    private void stopAlertService() {
        isAlertActive = false;
        alertHandler.removeCallbacks(alertRunnable);
        Log.d("SmsActivity", "Alert service stopped.");

        // Stop the foreground service
        Intent intent = new Intent(this, AlertForegroundService.class);
        stopService(intent);
    }

    private void saveAlertState(boolean isActive) {
        SharedPreferences preferences = getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("alert_state", isActive);
        editor.apply();
    }

    private void sendSmsAlert() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            String currentLocation = "http://maps.google.com/maps?q=loc:" + location.getLatitude() + "," + location.getLongitude();
                            sendSmsToEmergencyContacts(currentLocation);
                            sendWhatsAppToEmergencyContacts(currentLocation);
                        } else {
                            Log.d("SmsActivity", "Location not found.");
                        }
                    }
                });
    }

    private void sendSmsToEmergencyContacts(String location) {
        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        Set<String> emergencyNumbers = sharedPreferences.getStringSet("enumbers", new HashSet<>());
        SmsManager smsManager = SmsManager.getDefault();

        String message = "I am in trouble! Please help! Here is my location: " + location;
        for (String number : emergencyNumbers) {
            smsManager.sendTextMessage(number, null, message, null, null);
        }
        Log.d("SmsActivity", "Alert sent to emergency contacts with location.");
    }

    private void sendWhatsAppToEmergencyContacts(String location) {
        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        Set<String> emergencyNumbers = sharedPreferences.getStringSet("enumbers", new HashSet<>());

        String message = "I am in trouble! Please help! Here is my location: " + location;
        for (String number : emergencyNumbers) {
            sendWhatsAppMessage(number, message);
        }
    }

    private void sendWhatsAppMessage(String phoneNumber, String message) {
        try {
            Intent sendIntent = new Intent(Intent.ACTION_VIEW);
            String url = "https://wa.me/+91" + phoneNumber + "?text=" + Uri.encode(message);
            sendIntent.setPackage("com.whatsapp");
            sendIntent.setData(Uri.parse(url));
            startActivity(sendIntent);
        } catch (Exception e) {
            Log.e("SmsActivity", "WhatsApp not installed or issue with sending message: " + e.getMessage());
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(SmsActivity.this, MainActivity.class));
    }

    public void helplines(View view) {
        startActivity(new Intent(SmsActivity.this, HelplineCall.class));
    }

    public void stopService(View view) {
        Intent notificationIntent = new Intent(this, ServiceMine.class);
        notificationIntent.setAction("stop");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ServiceMine.isRunning) {
                getApplicationContext().startForegroundService(notificationIntent);
                Snackbar.make(findViewById(android.R.id.content), "Service Stopped!", Snackbar.LENGTH_LONG).show();
            }
        } else {
            if (ServiceMine.isRunning) {
                getApplicationContext().startService(notificationIntent);
                Snackbar.make(findViewById(android.R.id.content), "Service Stopped!", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    public void startServiceV(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Intent notificationIntent = new Intent(this, ServiceMine.class);
            notificationIntent.setAction("Start");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplicationContext().startForegroundService(notificationIntent);
                Snackbar.make(findViewById(android.R.id.content), "Service Started!", Snackbar.LENGTH_LONG).show();
            } else {
                getApplicationContext().startService(notificationIntent);
                Snackbar.make(findViewById(android.R.id.content), "Service Started!", Snackbar.LENGTH_LONG).show();
            }
        }
    }
}
