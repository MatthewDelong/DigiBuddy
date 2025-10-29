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
        // Warning alerts (25% threshold)
        alertSentMap.put("hunger_warning", false);
        alertSentMap.put("happiness_warning", false);
        alertSentMap.put("energy_warning", false);
        alertSentMap.put("cleanliness_warning", false);

        // Emergency alerts (15% threshold)
        alertSentMap.put("hunger_emergency", false);
        alertSentMap.put("happiness_emergency", false);
        alertSentMap.put("energy_emergency", false);
        alertSentMap.put("cleanliness_emergency", false);

        long currentTime = System.currentTimeMillis();

        // Warning timestamps
        lastAlertTimeMap.put("hunger_warning", currentTime);
        lastAlertTimeMap.put("happiness_warning", currentTime);
        lastAlertTimeMap.put("energy_warning", currentTime);
        lastAlertTimeMap.put("cleanliness_warning", currentTime);

        // Emergency timestamps
        lastAlertTimeMap.put("hunger_emergency", currentTime);
        lastAlertTimeMap.put("happiness_emergency", currentTime);
        lastAlertTimeMap.put("energy_emergency", currentTime);
        lastAlertTimeMap.put("cleanliness_emergency", currentTime);
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

                // FIX: Calculate age based on days passed (1440 minutes = 1 day)
                double daysPassed = minutesPassed / 1440.0;
                double previousAge = pet.getAge();

                // FIX: Add full days passed to age
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

        // WARNING ALERTS - 25% threshold
        if (pet.getHunger() <= 25 && pet.getHunger() > 15) {
            if (!alertSentMap.get("hunger_warning") ||
                    (currentTime - lastAlertTimeMap.get("hunger_warning") > fiveMinutes)) {
                sendAlertNotification("Hunger Warning", "Your DigiBuddy is getting hungry! Consider feeding soon.");
                alertSentMap.put("hunger_warning", true);
                lastAlertTimeMap.put("hunger_warning", currentTime);
            }
        } else {
            // Reset the alert state when condition is no longer true
            alertSentMap.put("hunger_warning", false);
        }

        if (pet.getHappiness() <= 25 && pet.getHappiness() > 15) {
            if (!alertSentMap.get("happiness_warning") ||
                    (currentTime - lastAlertTimeMap.get("happiness_warning") > fiveMinutes)) {
                sendAlertNotification("Happiness Warning", "Your DigiBuddy is feeling sad! Some playtime would help!");
                alertSentMap.put("happiness_warning", true);
                lastAlertTimeMap.put("happiness_warning", currentTime);
            }
        } else {
            alertSentMap.put("happiness_warning", false);
        }

        if (pet.getEnergy() <= 25 && pet.getEnergy() > 15) {
            if (!alertSentMap.get("energy_warning") ||
                    (currentTime - lastAlertTimeMap.get("energy_warning") > fiveMinutes)) {
                sendAlertNotification("Energy Warning", "Your DigiBuddy is getting tired! Maybe some rest soon?");
                alertSentMap.put("energy_warning", true);
                lastAlertTimeMap.put("energy_warning", currentTime);
            }
        } else {
            alertSentMap.put("energy_warning", false);
        }

        if (pet.getCleanliness() <= 25 && pet.getCleanliness() > 15) {
            if (!alertSentMap.get("cleanliness_warning") ||
                    (currentTime - lastAlertTimeMap.get("cleanliness_warning") > fiveMinutes)) {
                sendAlertNotification("Cleanliness Warning", "Your DigiBuddy is getting dirty! A cleaning would be nice!");
                alertSentMap.put("cleanliness_warning", true);
                lastAlertTimeMap.put("cleanliness_warning", currentTime);
            }
        } else {
            alertSentMap.put("cleanliness_warning", false);
        }

        // EMERGENCY ALERTS - 15% threshold
        if (pet.getHunger() <= 15 && pet.getHunger() > 0) {
            if (!alertSentMap.get("hunger_emergency") ||
                    (currentTime - lastAlertTimeMap.get("hunger_emergency") > fiveMinutes)) {
                sendAlertNotification("🍕 HUNGER EMERGENCY!", "Your DigiBuddy is very hungry! Feed it immediately!");
                alertSentMap.put("hunger_emergency", true);
                lastAlertTimeMap.put("hunger_emergency", currentTime);
            }
        } else {
            alertSentMap.put("hunger_emergency", false);
        }

        if (pet.getHappiness() <= 15 && pet.getHappiness() > 0) {
            if (!alertSentMap.get("happiness_emergency") ||
                    (currentTime - lastAlertTimeMap.get("happiness_emergency") > fiveMinutes)) {
                sendAlertNotification("😢 HAPPINESS EMERGENCY!", "Your DigiBuddy is very sad! Play with it urgently!");
                alertSentMap.put("happiness_emergency", true);
                lastAlertTimeMap.put("happiness_emergency", currentTime);
            }
        } else {
            alertSentMap.put("happiness_emergency", false);
        }

        if (pet.getEnergy() <= 15 && pet.getEnergy() > 0) {
            if (!alertSentMap.get("energy_emergency") ||
                    (currentTime - lastAlertTimeMap.get("energy_emergency") > fiveMinutes)) {
                sendAlertNotification("😴 ENERGY EMERGENCY!", "Your DigiBuddy is exhausted! Let it sleep immediately!");
                alertSentMap.put("energy_emergency", true);
                lastAlertTimeMap.put("energy_emergency", currentTime);
            }
        } else {
            alertSentMap.put("energy_emergency", false);
        }

        if (pet.getCleanliness() <= 15 && pet.getCleanliness() > 0) {
            if (!alertSentMap.get("cleanliness_emergency") ||
                    (currentTime - lastAlertTimeMap.get("cleanliness_emergency") > fiveMinutes)) {
                sendAlertNotification("🛁 CLEANLINESS EMERGENCY!", "Your DigiBuddy is very dirty! Clean it right away!");
                alertSentMap.put("cleanliness_emergency", true);
                lastAlertTimeMap.put("cleanliness_emergency", currentTime);
            }
        } else {
            alertSentMap.put("cleanliness_emergency", false);
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