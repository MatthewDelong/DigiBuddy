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
    private static final long UPDATE_INTERVAL = 60000; // 1 minute
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "pet_service_channel";

    private Handler handler;
    private Runnable updateRunnable;
    private PetPreferences petPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        petPreferences = new PetPreferences(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start as foreground service
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pet Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Channel for pet status updates");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        // Update notification with current pet status
        Pet currentPet = petPreferences.loadPet();
        String notificationText;

        if (!currentPet.isAlive()) {
            notificationText = "Your DigiBuddy has passed away";
        } else if (currentPet.isSleeping()) {
            notificationText = "Your DigiBuddy is sleeping... Zzz";
        } else {
            notificationText = "Your DigiBuddy is being cared for";
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("DigiBuddy")
                .setContentText(notificationText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void startPetUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updatePetStats();
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
                hungerLoss *= 0.3; // Hunger decreases much slower while sleeping
                happinessLoss *= 0.5; // Happiness decreases slower while sleeping
                cleanlinessLoss *= 0.5; // Cleanliness decreases slower while sleeping
                energyLoss = 0; // No energy loss while sleeping
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

            // Update notification with new status
            updateNotification();
        }
    }

    private void updateNotification() {
        Notification notification = createNotification();
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, notification);
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