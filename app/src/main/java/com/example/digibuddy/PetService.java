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
import java.util.HashMap;
import java.util.Map;

public class PetService extends Service {
    private static final long UPDATE_INTERVAL = 60000;
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "pet_service_channel";
    private static final String ALERT_CHANNEL_ID = "pet_alert_channel";

    private Handler handler;
    private Runnable updateRunnable;
    private PetPreferences petPreferences;
    private NotificationManager notificationManager;

    // NEW: Track which alerts have been sent to prevent duplicates
    private Map<String, Boolean> alertSentMap;
    private Map<String, Long> lastAlertTimeMap;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        petPreferences = new PetPreferences(this);
        notificationManager = getSystemService(NotificationManager.class);
        createNotificationChannels();

        // NEW: Initialize alert tracking
        alertSentMap = new HashMap<>();
        lastAlertTimeMap = new HashMap<>();
        initializeAlertTracking();
    }

    // NEW: Initialize alert tracking state
    private void initializeAlertTracking() {
        alertSentMap.put("hunger_low", false);
        alertSentMap.put("happiness_low", false);
        alertSentMap.put("energy_low", false);
        alertSentMap.put("cleanliness_low", false);
        alertSentMap.put("hunger_critical", false);
        alertSentMap.put("happiness_critical", false);

        long currentTime = System.currentTimeMillis();
        lastAlertTimeMap.put("hunger_low", currentTime);
        lastAlertTimeMap.put("happiness_low", currentTime);
        lastAlertTimeMap.put("energy_low", currentTime);
        lastAlertTimeMap.put("cleanliness_low", currentTime);
        lastAlertTimeMap.put("hunger_critical", currentTime);
        lastAlertTimeMap.put("happiness_critical", currentTime);
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
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pet Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Channel for pet service");
            channel.setShowBadge(false);

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

        boolean isFreshPet = pet.getHunger() == 100 &&
                pet.getHappiness() == 100 &&
                pet.getEnergy() == 100 &&
                pet.getAge() == 0 &&
                pet.getCleanliness() == 100;

        if (!isFreshPet) {
            long timePassed = System.currentTimeMillis() - pet.getLastUpdate();
            long minutesPassed = timePassed / (1000 * 60);

            if (minutesPassed > 0) {
                double hungerLoss = minutesPassed * 0.1;
                double happinessLoss = minutesPassed * 0.05;
                double energyLoss = minutesPassed * 0.05;
                double cleanlinessLoss = minutesPassed * 0.02;

                double daysPassed = minutesPassed / 1440.0;
                double previousAge = pet.getAge();
                pet.setAge(pet.getAge() + daysPassed);

                if (pet.isSleeping()) {
                    double energyGain = minutesPassed * 0.5;
                    pet.setEnergy(Math.min(100, pet.getEnergy() + energyGain));
                    hungerLoss *= 0.3;
                    happinessLoss *= 0.5;
                    cleanlinessLoss *= 0.5;
                    energyLoss = 0;
                }

                pet.setHunger(Math.max(0, pet.getHunger() - hungerLoss));
                pet.setHappiness(Math.max(0, pet.getHappiness() - happinessLoss));
                pet.setEnergy(Math.max(0, pet.getEnergy() - energyLoss));
                pet.setCleanliness(Math.max(0, pet.getCleanliness() - cleanlinessLoss));

                pet.updateStage();
                pet.checkDeath();

                pet.setLastUpdate(System.currentTimeMillis());
                petPreferences.savePet(pet);

                checkMilestonesInBackground(previousAge, pet.getAge());
            }
        }
    }

    private void checkLowStatsAndNotify() {
        Pet pet = petPreferences.loadPet();

        if (!pet.isAlive()) {
            // NEW: Reset all alerts if pet is dead
            resetAllAlerts();
            return;
        }

        long currentTime = System.currentTimeMillis();
        long fiveMinutes = 5 * 60 * 1000; // 5 minutes in milliseconds

        // Check for low stats with cooldown and state tracking
        if (pet.getHunger() <= 10 && pet.getHunger() > 0) {
            if (!alertSentMap.get("hunger_low") ||
                    (currentTime - lastAlertTimeMap.get("hunger_low") > fiveMinutes)) {
                sendAlertNotification("Hunger Alert!", "Your DigiBuddy is very hungry! Feed it soon!");
                alertSentMap.put("hunger_low", true);
                lastAlertTimeMap.put("hunger_low", currentTime);
            }
        } else {
            // Reset the alert state when condition is no longer true
            alertSentMap.put("hunger_low", false);
        }

        if (pet.getHappiness() <= 10 && pet.getHappiness() > 0) {
            if (!alertSentMap.get("happiness_low") ||
                    (currentTime - lastAlertTimeMap.get("happiness_low") > fiveMinutes)) {
                sendAlertNotification("Happiness Alert!", "Your DigiBuddy is very sad! Play with it!");
                alertSentMap.put("happiness_low", true);
                lastAlertTimeMap.put("happiness_low", currentTime);
            }
        } else {
            alertSentMap.put("happiness_low", false);
        }

        if (pet.getEnergy() <= 10 && pet.getEnergy() > 0) {
            if (!alertSentMap.get("energy_low") ||
                    (currentTime - lastAlertTimeMap.get("energy_low") > fiveMinutes)) {
                sendAlertNotification("Energy Alert!", "Your DigiBuddy is very tired! Let it sleep!");
                alertSentMap.put("energy_low", true);
                lastAlertTimeMap.put("energy_low", currentTime);
            }
        } else {
            alertSentMap.put("energy_low", false);
        }

        if (pet.getCleanliness() <= 10 && pet.getCleanliness() > 0) {
            if (!alertSentMap.get("cleanliness_low") ||
                    (currentTime - lastAlertTimeMap.get("cleanliness_low") > fiveMinutes)) {
                sendAlertNotification("Cleanliness Alert!", "Your DigiBuddy is very dirty! Clean it!");
                alertSentMap.put("cleanliness_low", true);
                lastAlertTimeMap.put("cleanliness_low", currentTime);
            }
        } else {
            alertSentMap.put("cleanliness_low", false);
        }

        // Critical alerts with separate tracking
        if (pet.getHunger() <= 5 && pet.getHunger() > 0) {
            if (!alertSentMap.get("hunger_critical") ||
                    (currentTime - lastAlertTimeMap.get("hunger_critical") > fiveMinutes)) {
                sendAlertNotification("CRITICAL: Hunger Emergency!", "Your DigiBuddy is starving! Feed it immediately!");
                alertSentMap.put("hunger_critical", true);
                lastAlertTimeMap.put("hunger_critical", currentTime);
            }
        } else {
            alertSentMap.put("hunger_critical", false);
        }

        if (pet.getHappiness() <= 5 && pet.getHappiness() > 0) {
            if (!alertSentMap.get("happiness_critical") ||
                    (currentTime - lastAlertTimeMap.get("happiness_critical") > fiveMinutes)) {
                sendAlertNotification("CRITICAL: Happiness Emergency!", "Your DigiBuddy is extremely sad! It needs attention!");
                alertSentMap.put("happiness_critical", true);
                lastAlertTimeMap.put("happiness_critical", currentTime);
            }
        } else {
            alertSentMap.put("happiness_critical", false);
        }
    }

    // NEW: Reset all alerts (called when pet dies or is reset)
    private void resetAllAlerts() {
        for (String key : alertSentMap.keySet()) {
            alertSentMap.put(key, false);
        }
    }

    private void checkMilestonesInBackground(double previousAge, double currentAge) {
        int previousDays = (int) previousAge;
        int currentDays = (int) currentAge;

        if (currentDays > previousDays && currentDays % 10 == 0) {
            sendMilestoneNotification(currentDays);

            Pet pet = petPreferences.loadPet();
            pet.setMilestonesAchieved(currentDays / 10);
            petPreferences.savePet(pet);
        }
    }

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