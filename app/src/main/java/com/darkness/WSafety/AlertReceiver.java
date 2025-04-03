package com.darkness.WSafety;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.telephony.SmsManager;
import android.util.Log;
import android.content.pm.PackageManager; // Import PackageManager

import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.HashSet;
import java.util.Set;

public class AlertReceiver extends BroadcastReceiver {
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlertReceiver", "Periodic alert triggered.");
        sendPeriodicAlert(context);
    }

    private void sendPeriodicAlert(Context context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        // Check location permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("AlertReceiver", "Location permission not granted.");
            return; // Handle permission request in your activity
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        String currentLocation = "http://maps.google.com/maps?q=loc:" + location.getLatitude() + "," + location.getLongitude();
                        sendSmsToEmergencyContacts(context, currentLocation);
                    } else {
                        Log.d("AlertReceiver", "Location not found.");
                    }
                });
    }

    private void sendSmsToEmergencyContacts(Context context, String location) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        Set<String> emergencyNumbers = sharedPreferences.getStringSet("enumbers", new HashSet<>());
        SmsManager smsManager = SmsManager.getDefault();

        if (!emergencyNumbers.isEmpty()) {
            for (String number : emergencyNumbers) {
                String message = "I am in trouble! Here is my location: " + location;
                try {
                    smsManager.sendTextMessage(number, null, message, null, null);
                    Log.d("AlertReceiver", "Alert sent to: " + number);
                } catch (Exception e) {
                    Log.e("AlertReceiver", "Failed to send SMS to: " + number + " Error: " + e.getMessage());
                }
            }
        } else {
            Log.d("AlertReceiver", "No emergency contacts found.");
        }
    }
}
