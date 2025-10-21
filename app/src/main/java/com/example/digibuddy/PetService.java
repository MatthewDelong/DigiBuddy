package com.example.digibuddy;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class PetService extends Service {
    private static final long UPDATE_INTERVAL = 60000;
    private static final int NOTIFICATION_ID = 1;
    private static final String ALERT_CHANNEL_ID = "pet_alert_channel";

    private Handler handler;
    private Runnable updateRunnable;
    private PetPreferences petPreferences;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        petPreferences = new PetPreferences(this);
        notificationManager = getSystemService(NotificationManager.class);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Remove the foreground service - no ongoing notification
        startPetUpdates();
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Only create alert channel for low stat warnings
            NotificationChannel alertChannel = new NotificationChannel(
                    ALERT_CHANNEL_ID,
                    "Pet Alert Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertChannel.setDescription("Channel for pet emergency alerts");
            alertChannel.enableVibration(true);
            alertChannel.setImportance(NotificationManager.IMPORTANCE_HIGH);

            notificationManager.createNotificationChannel(alertChannel);
        }
    }

    private void startPetUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updatePetStats();
                checkLowStatsAndNotify();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
        handler.postDelayed(updateRunnable, UPDATE_INTERVAL);
    }

    private void updatePetStats() {
        Pet pet = petPreferences.loadPet();

        if (!pet.isAlive()) {
            stopSelf();
            return;
        }

        // Check if this is a fresh pet (all stats at starting values)
        boolean isFreshPet = pet.getHunger() == 100 &&
                pet.getHappiness() == 100 &&
                pet.getEnergy() == 100 &&
                pet.getAge() == 0 &&
                pet.getCleanliness() == 100;

        // Only apply degradation if this is NOT a fresh pet
        if (!isFreshPet) {
            // Calculate time passed since last update (in minutes)
            long timePassed = System.currentTimeMillis() - pet.getLastUpdate();
            long minutesPassed = timePassed / (1000 * 60);

            if (minutesPassed > 0) {
                // Apply stat changes based on time passed
                double hungerLoss = minutesPassed * 0.1;
                double happinessLoss = minutesPassed * 0.05;
                double energyLoss = minutesPassed * 0.05;
                double cleanlinessLoss = minutesPassed * 0.02;
                double ageGain = minutesPassed * 0.001;

                // If sleeping, apply sleep benefits
                if (pet.isSleeping()) {
                    // While sleeping: energy restores, other stats decrease slower
                    double energyGain = minutesPassed * 0.5;
                    pet.setEnergy(Math.min(100, pet.getEnergy() + energyGain));
                    hungerLoss *= 0.3;
                    happinessLoss *= 0.5;
                    cleanlinessLoss *= 0.5;
                    energyLoss = 0;
                }

                // Apply stat changes
                pet.setHunger(Math.max(0, pet.getHunger() - hungerLoss));
                pet.setHappiness(Math.max(0, pet.getHappiness() - happinessLoss));
                pet.setEnergy(Math.max(0, pet.getEnergy() - energyLoss));
                pet.setCleanliness(Math.max(0, pet.getCleanliness() - cleanlinessLoss));
                pet.setAge(pet.getAge() + ageGain);

                // Update pet stage and check for death
                pet.updateStage();
                pet.checkDeath();

                // Save updated pet state
                pet.setLastUpdate(System.currentTimeMillis());
                petPreferences.savePet(pet);
            }
        }
    }

    private void checkLowStatsAndNotify() {
        Pet pet = petPreferences.loadPet();

        if (!pet.isAlive()) {
            return;
        }

        // Check for low stats (below 10%) - ONLY show notifications for these
        if (pet.getHunger() <= 10) {
            sendAlertNotification("Hunger Alert!", "Your DigiBuddy is very hungry! Feed it soon!");
        }

        if (pet.getHappiness() <= 10) {
            sendAlertNotification("Happiness Alert!", "Your DigiBuddy is very sad! Play with it!");
        }

        if (pet.getEnergy() <= 10) {
            sendAlertNotification("Energy Alert!", "Your DigiBuddy is very tired! Let it sleep!");
        }

        if (pet.getCleanliness() <= 10) {
            sendAlertNotification("Cleanliness Alert!", "Your DigiBuddy is very dirty! Clean it!");
        }

        // Critical alerts (below 5%)
        if (pet.getHunger() <= 5) {
            sendAlertNotification("CRITICAL: Hunger Emergency!", "Your DigiBuddy is starving! Feed it immediately!");
        }

        if (pet.getHappiness() <= 5) {
            sendAlertNotification("CRITICAL: Happiness Emergency!", "Your DigiBuddy is extremely sad! It needs attention!");
        }
    }

    private void sendAlertNotification(String title, String message) {
        Notification alertNotification = new NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // Allows user to dismiss
                .setDefaults(Notification.DEFAULT_ALL) // Sound, vibration, etc.
                .build();

        notificationManager.notify((int) System.currentTimeMillis(), alertNotification); // Unique ID for each alert
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}