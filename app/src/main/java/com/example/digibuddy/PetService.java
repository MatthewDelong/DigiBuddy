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
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class PetService extends Service {
    private static final long UPDATE_INTERVAL = 60000;
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "pet_service_channel";
    private static final String ALERT_CHANNEL_ID = "pet_alert_channel";

    // Constants for notification IDs
    private static final int ENERGY_WARNING_ID = 1001;
    private static final int ENERGY_EMERGENCY_ID = 1002;
    private static final int HUNGER_WARNING_ID = 2001;
    private static final int HUNGER_EMERGENCY_ID = 2002;
    private static final int HAPPINESS_WARNING_ID = 3001;
    private static final int HAPPINESS_EMERGENCY_ID = 3002;
    private static final int CLEANLINESS_WARNING_ID = 4001;
    private static final int CLEANLINESS_EMERGENCY_ID = 4002;
    private static final int MILESTONE_ID = 5000;

    private Handler handler;
    private Runnable updateRunnable;
    private PetPreferences petPreferences;
    private NotificationManager notificationManager;

    // Thread-safe alert tracking with atomic operations
    private Map<String, AtomicBoolean> alertSentMap;
    private Map<String, Long> lastAlertTimeMap;

    // Sleep state synchronization
    private final AtomicBoolean isProcessingSleep = new AtomicBoolean(false);

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        petPreferences = new PetPreferences(this);
        notificationManager = getSystemService(NotificationManager.class);
        createNotificationChannels();
        initializeAlertTracking();

        Log.d("PetService", "Service created with enhanced notification control");
    }

    // Completely redesigned alert tracking
    private void initializeAlertTracking() {
        alertSentMap = new HashMap<>();
        lastAlertTimeMap = new HashMap<>();

        // Warning alerts (25% threshold)
        alertSentMap.put("hunger_warning", new AtomicBoolean(false));
        alertSentMap.put("happiness_warning", new AtomicBoolean(false));
        alertSentMap.put("energy_warning", new AtomicBoolean(false));
        alertSentMap.put("cleanliness_warning", new AtomicBoolean(false));

        // Emergency alerts (15% threshold)
        alertSentMap.put("hunger_emergency", new AtomicBoolean(false));
        alertSentMap.put("happiness_emergency", new AtomicBoolean(false));
        alertSentMap.put("energy_emergency", new AtomicBoolean(false));
        alertSentMap.put("cleanliness_emergency", new AtomicBoolean(false));

        long currentTime = System.currentTimeMillis();
        lastAlertTimeMap.put("hunger_warning", currentTime);
        lastAlertTimeMap.put("happiness_warning", currentTime);
        lastAlertTimeMap.put("energy_warning", currentTime);
        lastAlertTimeMap.put("cleanliness_warning", currentTime);
        lastAlertTimeMap.put("hunger_emergency", currentTime);
        lastAlertTimeMap.put("happiness_emergency", currentTime);
        lastAlertTimeMap.put("energy_emergency", currentTime);
        lastAlertTimeMap.put("cleanliness_emergency", currentTime);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("PetService", "Service starting with enhanced notification control");

        // NEW: Handle immediate actions from MainActivity
        if (intent != null) {
            String action = intent.getAction();
            if ("CLEANUP_ENERGY_NOTIFICATIONS".equals(action)) {
                Log.d("PetService", "Immediate energy notification cleanup requested");
                cleanupEnergyNotificationsImmediately();
                return START_STICKY;
            } else if ("FORCE_SLEEP_SYNC".equals(action)) {
                Log.d("PetService", "Force sleep synchronization requested");
                forceSleepStateSync();
                return START_STICKY;
            } else if ("CLEANUP_ALL_NOTIFICATIONS".equals(action)) {
                Log.d("PetService", "Cleanup ALL notifications requested");
                cleanupAllNotificationsImmediately();
                return START_STICKY;
            } else if ("CANCEL_HUNGER_ALERTS".equals(action)) {
                Log.d("PetService", "Cancel hunger alerts requested");
                cancelHungerNotificationsImmediately();
                return START_STICKY;
            }
        }

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
            Log.d("PetService", "Pet is not alive, stopping service");
            cleanupAllNotificationsImmediately();
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
                // BALANCED RATES (per minute)
                double hungerLoss = minutesPassed * 0.08;
                double happinessLoss = minutesPassed * 0.04;
                double energyLoss = minutesPassed * 0.04;
                double cleanlinessLoss = minutesPassed * 0.016;

                double daysPassed = minutesPassed / 1440.0;
                double previousAge = pet.getAge();

                pet.setAge(pet.getAge() + daysPassed);

                if (pet.isSleeping()) {
                    double energyGain = minutesPassed * 0.24;
                    pet.setEnergy(Math.min(100, pet.getEnergy() + energyGain));
                    hungerLoss *= 0.3;
                    happinessLoss *= 0.4;
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

    // NEW: Completely redesigned notification system with state validation
    private void checkLowStatsAndNotify() {
        if (isProcessingSleep.get()) {
            Log.d("PetService", "Sleep processing in progress, skipping notification check");
            return;
        }

        Pet pet = petPreferences.loadPet();

        if (!pet.isAlive()) {
            resetAllAlerts();
            return;
        }

        // NEW: Immediate state validation - skip ALL notifications if conditions don't match
        validateAndCleanupNotifications(pet);

        long currentTime = System.currentTimeMillis();
        long fiveMinutes = 5 * 60 * 1000;

        // Check if we should skip ALL alerts (like when sleeping for energy)
        boolean skipEnergyAlerts = pet.isSleeping();

        if (!skipEnergyAlerts) {
            // WARNING ALERTS - 25% threshold
            checkAndSendStatAlert(pet, "hunger", pet.getHunger(), 25, 15,
                    "Hunger Warning", "Your DigiBuddy is getting hungry! Consider feeding soon.",
                    HUNGER_WARNING_ID, "hunger_warning", currentTime, fiveMinutes);

            checkAndSendStatAlert(pet, "happiness", pet.getHappiness(), 25, 15,
                    "Happiness Warning", "Your DigiBuddy is feeling sad! Some playtime would help!",
                    HAPPINESS_WARNING_ID, "happiness_warning", currentTime, fiveMinutes);

            checkAndSendStatAlert(pet, "energy", pet.getEnergy(), 25, 15,
                    "Energy Warning", "Your DigiBuddy is getting tired! Maybe some rest soon?",
                    ENERGY_WARNING_ID, "energy_warning", currentTime, fiveMinutes);

            checkAndSendStatAlert(pet, "cleanliness", pet.getCleanliness(), 25, 15,
                    "Cleanliness Warning", "Your DigiBuddy is getting dirty! A cleaning would be nice!",
                    CLEANLINESS_WARNING_ID, "cleanliness_warning", currentTime, fiveMinutes);

            // EMERGENCY ALERTS - 15% threshold
            checkAndSendStatAlert(pet, "hunger", pet.getHunger(), 15, 0,
                    "🍕 HUNGER EMERGENCY!", "Your DigiBuddy is very hungry! Feed it immediately!",
                    HUNGER_EMERGENCY_ID, "hunger_emergency", currentTime, fiveMinutes);

            checkAndSendStatAlert(pet, "happiness", pet.getHappiness(), 15, 0,
                    "😢 HAPPINESS EMERGENCY!", "Your DigiBuddy is very sad! Play with it urgently!",
                    HAPPINESS_EMERGENCY_ID, "happiness_emergency", currentTime, fiveMinutes);

            checkAndSendStatAlert(pet, "energy", pet.getEnergy(), 15, 0,
                    "😴 ENERGY EMERGENCY!", "Your DigiBuddy is exhausted! Let it sleep immediately!",
                    ENERGY_EMERGENCY_ID, "energy_emergency", currentTime, fiveMinutes);

            checkAndSendStatAlert(pet, "cleanliness", pet.getCleanliness(), 15, 0,
                    "🛁 CLEANLINESS EMERGENCY!", "Your DigiBuddy is very dirty! Clean it right away!",
                    CLEANLINESS_EMERGENCY_ID, "cleanliness_emergency", currentTime, fiveMinutes);
        } else {
            // If sleeping, cancel any existing energy notifications
            cancelEnergyNotificationsImmediately();
        }
    }

    // NEW: Unified stat alert checking with automatic cleanup
    private void checkAndSendStatAlert(Pet pet, String type, double value, int upperThreshold, int lowerThreshold,
                                       String title, String message, int notificationId, String alertKey,
                                       long currentTime, long cooldown) {

        // Check if alert should be sent
        if (value <= upperThreshold && value > lowerThreshold) {
            if (!alertSentMap.get(alertKey).get() ||
                    (currentTime - lastAlertTimeMap.get(alertKey) > cooldown)) {
                sendAlertNotification(title, message, notificationId);
                alertSentMap.get(alertKey).set(true);
                lastAlertTimeMap.put(alertKey, currentTime);
                Log.d("PetService", "Sent " + alertKey + " notification - Value: " + value);
            }
        } else {
            // NEW: If condition no longer met, immediately cancel notification and reset state
            if (alertSentMap.get(alertKey).get()) {
                alertSentMap.get(alertKey).set(false);
                notificationManager.cancel(notificationId);
                Log.d("PetService", "Cancelled " + alertKey + " notification - Condition no longer met. Value: " + value);
            }
        }
    }

    // NEW: Validate all notifications and cleanup any that shouldn't be showing
    private void validateAndCleanupNotifications(Pet pet) {
        Log.d("PetService", "Validating notifications - Hunger: " + pet.getHunger() +
                ", Energy: " + pet.getEnergy() + ", Sleeping: " + pet.isSleeping());

        // Check hunger notifications
        if (pet.getHunger() > 25) {
            // If hunger is above warning threshold, cancel both warning and emergency
            if (alertSentMap.get("hunger_warning").get()) {
                alertSentMap.get("hunger_warning").set(false);
                notificationManager.cancel(HUNGER_WARNING_ID);
                Log.d("PetService", "Cancelled hunger warning - Hunger level: " + pet.getHunger());
            }
            if (alertSentMap.get("hunger_emergency").get()) {
                alertSentMap.get("hunger_emergency").set(false);
                notificationManager.cancel(HUNGER_EMERGENCY_ID);
                Log.d("PetService", "Cancelled hunger emergency - Hunger level: " + pet.getHunger());
            }
        } else if (pet.getHunger() > 15) {
            // If hunger is between 15-25, only cancel emergency
            if (alertSentMap.get("hunger_emergency").get()) {
                alertSentMap.get("hunger_emergency").set(false);
                notificationManager.cancel(HUNGER_EMERGENCY_ID);
                Log.d("PetService", "Cancelled hunger emergency (now in warning range) - Hunger: " + pet.getHunger());
            }
        }

        // Check energy notifications (respect sleep state)
        if (pet.isSleeping() || pet.getEnergy() > 25) {
            cancelEnergyNotificationsImmediately();
        } else if (pet.getEnergy() > 15) {
            // If energy is between 15-25, only cancel emergency
            if (alertSentMap.get("energy_emergency").get()) {
                alertSentMap.get("energy_emergency").set(false);
                notificationManager.cancel(ENERGY_EMERGENCY_ID);
                Log.d("PetService", "Cancelled energy emergency (now in warning range) - Energy: " + pet.getEnergy());
            }
        }

        // Similar validation for happiness and cleanliness
        if (pet.getHappiness() > 25) {
            if (alertSentMap.get("happiness_warning").get()) {
                alertSentMap.get("happiness_warning").set(false);
                notificationManager.cancel(HAPPINESS_WARNING_ID);
            }
            if (alertSentMap.get("happiness_emergency").get()) {
                alertSentMap.get("happiness_emergency").set(false);
                notificationManager.cancel(HAPPINESS_EMERGENCY_ID);
            }
        }

        if (pet.getCleanliness() > 25) {
            if (alertSentMap.get("cleanliness_warning").get()) {
                alertSentMap.get("cleanliness_warning").set(false);
                notificationManager.cancel(CLEANLINESS_WARNING_ID);
            }
            if (alertSentMap.get("cleanliness_emergency").get()) {
                alertSentMap.get("cleanliness_emergency").set(false);
                notificationManager.cancel(CLEANLINESS_EMERGENCY_ID);
            }
        }
    }

    // NEW: Immediate sleep state handling - ATOMIC OPERATION
    private void handleSleepStateImmediately() {
        if (isProcessingSleep.compareAndSet(false, true)) {
            try {
                Log.d("PetService", "Executing immediate sleep state handling");

                // Cancel ALL energy notifications
                cancelAllEnergyNotificationsImmediately();

                // Reset energy alert states
                alertSentMap.get("energy_warning").set(false);
                alertSentMap.get("energy_emergency").set(false);

                // Update last alert times to prevent immediate re-triggering
                long currentTime = System.currentTimeMillis();
                lastAlertTimeMap.put("energy_warning", currentTime);
                lastAlertTimeMap.put("energy_emergency", currentTime);

                Log.d("PetService", "Sleep state handling completed - energy notifications cleared");
            } finally {
                isProcessingSleep.set(false);
            }
        }
    }

    // NEW: Force sleep state synchronization
    private void forceSleepStateSync() {
        Pet pet = petPreferences.loadPet();
        if (pet.isSleeping()) {
            handleSleepStateImmediately();
        }
    }

    // NEW: Comprehensive notification cleanup methods
    private void cleanupAllNotificationsImmediately() {
        Log.d("PetService", "Executing comprehensive ALL notification cleanup");

        // Cancel all specific notifications
        cancelHungerNotificationsImmediately();
        cancelEnergyNotificationsImmediately();
        cancelHappinessNotificationsImmediately();
        cancelCleanlinessNotificationsImmediately();

        // Reset all alert states
        for (AtomicBoolean sent : alertSentMap.values()) {
            sent.set(false);
        }

        Log.d("PetService", "All notifications cleanup completed");
    }

    private void cancelHungerNotificationsImmediately() {
        try {
            notificationManager.cancel(HUNGER_WARNING_ID);
            notificationManager.cancel(HUNGER_EMERGENCY_ID);
            alertSentMap.get("hunger_warning").set(false);
            alertSentMap.get("hunger_emergency").set(false);
            Log.d("PetService", "Hunger notifications cancelled immediately");
        } catch (Exception e) {
            Log.e("PetService", "Error cancelling hunger notifications: " + e.getMessage());
        }
    }

    private void cancelEnergyNotificationsImmediately() {
        try {
            notificationManager.cancel(ENERGY_WARNING_ID);
            notificationManager.cancel(ENERGY_EMERGENCY_ID);
            alertSentMap.get("energy_warning").set(false);
            alertSentMap.get("energy_emergency").set(false);
            Log.d("PetService", "Energy notifications cancelled immediately");
        } catch (Exception e) {
            Log.e("PetService", "Error cancelling energy notifications: " + e.getMessage());
        }
    }

    private void cancelHappinessNotificationsImmediately() {
        try {
            notificationManager.cancel(HAPPINESS_WARNING_ID);
            notificationManager.cancel(HAPPINESS_EMERGENCY_ID);
            alertSentMap.get("happiness_warning").set(false);
            alertSentMap.get("happiness_emergency").set(false);
        } catch (Exception e) {
            Log.e("PetService", "Error cancelling happiness notifications: " + e.getMessage());
        }
    }

    private void cancelCleanlinessNotificationsImmediately() {
        try {
            notificationManager.cancel(CLEANLINESS_WARNING_ID);
            notificationManager.cancel(CLEANLINESS_EMERGENCY_ID);
            alertSentMap.get("cleanliness_warning").set(false);
            alertSentMap.get("cleanliness_emergency").set(false);
        } catch (Exception e) {
            Log.e("PetService", "Error cancelling cleanliness notifications: " + e.getMessage());
        }
    }

    // NEW: Comprehensive energy notification cleanup
    private void cleanupEnergyNotificationsImmediately() {
        Log.d("PetService", "Executing comprehensive energy notification cleanup");

        // Cancel all energy notifications
        cancelAllEnergyNotificationsImmediately();

        // Reset all energy alert states
        alertSentMap.get("energy_warning").set(false);
        alertSentMap.get("energy_emergency").set(false);

        // Update timestamps
        long currentTime = System.currentTimeMillis();
        lastAlertTimeMap.put("energy_warning", currentTime);
        lastAlertTimeMap.put("energy_emergency", currentTime);

        Log.d("PetService", "Energy notification cleanup completed");
    }

    // NEW: Aggressive energy notification cancellation
    private void cancelAllEnergyNotificationsImmediately() {
        try {
            // Cancel using specific IDs
            notificationManager.cancel(ENERGY_WARNING_ID);
            notificationManager.cancel(ENERGY_EMERGENCY_ID);

            // Additional cancellation for any potential duplicate notifications
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.cancel("energy_warning", ENERGY_WARNING_ID);
                notificationManager.cancel("energy_emergency", ENERGY_EMERGENCY_ID);
            }

            // Nuclear option: cancel all notifications from our app temporarily
            new Handler().postDelayed(() -> {
                notificationManager.cancel(ENERGY_WARNING_ID);
                notificationManager.cancel(ENERGY_EMERGENCY_ID);
            }, 100);

            Log.d("PetService", "All energy notifications cancelled aggressively");
        } catch (Exception e) {
            Log.e("PetService", "Error cancelling energy notifications: " + e.getMessage());
        }
    }

    private void sendAlertNotification(String title, String message, int notificationId) {
        try {
            Notification alertNotification = new NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .build();

            notificationManager.notify(notificationId, alertNotification);
            Log.d("PetService", "Notification sent: " + title + " (ID: " + notificationId + ")");
        } catch (Exception e) {
            Log.e("PetService", "Error sending notification: " + e.getMessage());
        }
    }

    private void resetAllAlerts() {
        Log.d("PetService", "Resetting all alerts");
        for (AtomicBoolean sent : alertSentMap.values()) {
            sent.set(false);
        }
        notificationManager.cancelAll();
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

        notificationManager.notify(MILESTONE_ID, milestoneNotification);
    }

    @Override
    public void onDestroy() {
        Log.d("PetService", "Service destroying");
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