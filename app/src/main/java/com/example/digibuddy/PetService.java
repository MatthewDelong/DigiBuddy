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
    private static final String CHANNEL_ID = "pet_service_channel";
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
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundService();
        startPetUpdates();
        return START_STICKY;
    }

    private void startForegroundService() {
        Notification notification = createNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Main service channel (low priority)
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pet Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Channel for pet service");
            channel.setShowBadge(false);

            // Alert channel (high priority for low stats)
            NotificationChannel alertChannel = new NotificationChannel(
                    ALERT_CHANNEL_ID,
                    "Pet Alert Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertChannel.setDescription("Channel for pet emergency alerts");
            alertChannel.enableVibration(true);

            notificationManager.createNotificationChannel(channel);
            notificationManager.createNotificationChannel(alertChannel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("DigiBuddy")
                .setContentText("Monitoring your pet's stats")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .build();
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

                // NEW: Age increases based on real time (1 day = 1440 minutes)
                double daysPassed = minutesPassed / 1440.0;
                double previousAge = pet.getAge();
                pet.setAge(pet.getAge() + daysPassed);

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

                // Update pet stage and check for death
                pet.updateStage();
                pet.checkDeath();

                // Save updated pet state
                pet.setLastUpdate(System.currentTimeMillis());
                petPreferences.savePet(pet);

                // NEW: Check for milestone achievements in background
                checkMilestonesInBackground(previousAge, pet.getAge());
            }
        }
    }

    // NEW: Check for milestone achievements from background service
    private void checkMilestonesInBackground(double previousAge, double currentAge) {
        int previousDays = (int) previousAge;
        int currentDays = (int) currentAge;

        // Check if we crossed any 10-day milestone
        if (currentDays > previousDays && currentDays % 10 == 0) {
            // Send a special milestone notification
            sendMilestoneNotification(currentDays);
        }
    }

    // NEW: Send milestone achievement notification
    private void sendMilestoneNotification(int days) {
        Notification milestoneNotification = new NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setContentTitle("🎉 Milestone Achieved!")
                .setContentText("Your DigiBuddy is now " + days + " days old! Amazing care!")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .build();

        notificationManager.notify((int) System.currentTimeMillis() + 1, milestoneNotification);
    }

    private void checkLowStatsAndNotify() {
        Pet pet = petPreferences.loadPet();

        if (!pet.isAlive()) {
            return;
        }

        // Check for low stats (below 10%) - ONLY show notifications for these
        if (pet.getHunger() <= 10 && pet.getHunger() > 0) {
            sendAlertNotification("Hunger Alert!", "Your DigiBuddy is very hungry! Feed it soon!");
        }

        if (pet.getHappiness() <= 10 && pet.getHappiness() > 0) {
            sendAlertNotification("Happiness Alert!", "Your DigiBuddy is very sad! Play with it!");
        }

        if (pet.getEnergy() <= 10 && pet.getEnergy() > 0) {
            sendAlertNotification("Energy Alert!", "Your DigiBuddy is very tired! Let it sleep!");
        }

        if (pet.getCleanliness() <= 10 && pet.getCleanliness() > 0) {
            sendAlertNotification("Cleanliness Alert!", "Your DigiBuddy is very dirty! Clean it!");
        }

        // Critical alerts (below 5%)
        if (pet.getHunger() <= 5 && pet.getHunger() > 0) {
            sendAlertNotification("CRITICAL: Hunger Emergency!", "Your DigiBuddy is starving! Feed it immediately!");
        }

        if (pet.getHappiness() <= 5 && pet.getHappiness() > 0) {
            sendAlertNotification("CRITICAL: Happiness Emergency!", "Your DigiBuddy is extremely sad! It needs attention!");
        }
    }

    private void sendAlertNotification(String title, String message) {
        Notification alertNotification = new NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .build();

        notificationManager.notify((int) System.currentTimeMillis(), alertNotification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
        notificationManager.cancel(NOTIFICATION_ID);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}