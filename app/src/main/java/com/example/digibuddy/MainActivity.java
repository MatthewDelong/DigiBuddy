package com.example.digibuddy;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private Pet pet;
    private PetPreferences petPreferences;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private ImageView petImage;
    private ProgressBar hungerBar, happinessBar, energyBar, cleanlinessBar;
    private TextView hungerText, happinessText, energyText, cleanlinessText, ageText, messageText, starInfoText;
    private Button feedButton, playButton, sleepButton, cleanButton, resetButton;
    private LinearLayout starsContainer;

    private final Handler uiHandler = new Handler();
    private Runnable uiUpdateRunnable;

    // NEW: Notification constants
    private static final int ENERGY_WARNING_ID = 1001;
    private static final int ENERGY_EMERGENCY_ID = 1002;

    // Mood enum
    enum PetMood {
        HAPPY, HUNGRY, TIRED, DIRTY, SLEEPING, DEFAULT
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        checkAvailableDrawables();
        checkDrawableProperties();
        petPreferences = new PetPreferences(this);

        try {
            loadPet();
        } catch (Exception e) {
            pet = new Pet();
            petPreferences.savePet(pet);
            showMessage("Welcome to DigiBuddy! A new pet has arrived!");
        }

        setupButtons();
        startUIUpdates();
        requestNotificationPermission();

        // NEW: Initial notification cleanup
        cleanupAllEnergyNotifications();
    }

    private void initializeViews() {
        try {
            petImage = findViewById(R.id.petImage);
            hungerBar = findViewById(R.id.hungerBar);
            happinessBar = findViewById(R.id.happinessBar);
            energyBar = findViewById(R.id.energyBar);
            cleanlinessBar = findViewById(R.id.cleanlinessBar);

            hungerText = findViewById(R.id.hungerText);
            happinessText = findViewById(R.id.happinessText);
            energyText = findViewById(R.id.energyText);
            cleanlinessText = findViewById(R.id.cleanlinessText);
            ageText = findViewById(R.id.ageText);
            messageText = findViewById(R.id.messageText);
            starInfoText = findViewById(R.id.starInfoText);
            starsContainer = findViewById(R.id.starsContainer);

            feedButton = findViewById(R.id.feedButton);
            playButton = findViewById(R.id.playButton);
            sleepButton = findViewById(R.id.sleepButton);
            cleanButton = findViewById(R.id.cleanButton);
            resetButton = findViewById(R.id.resetButton);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_LONG).show();
            throw e;
        }
    }

    // Debug method to check available drawables
    private void checkAvailableDrawables() {
        try {
            int[] drawablesToCheck = {
                    R.drawable.ic_pet_egg,
                    R.drawable.ic_pet_baby,
                    R.drawable.ic_pet_teen,
                    R.drawable.ic_pet_adult,
                    R.drawable.ic_pet_happy,
                    R.drawable.ic_pet_hungry,
                    R.drawable.ic_pet_tired,
                    R.drawable.ic_pet_dirty
            };

            String[] drawableNames = {
                    "ic_pet_egg",
                    "ic_pet_baby",
                    "ic_pet_teen",
                    "ic_pet_adult",
                    "ic_pet_happy",
                    "ic_pet_hungry",
                    "ic_pet_tired",
                    "ic_pet_dirty"
            };

            for (int i = 0; i < drawablesToCheck.length; i++) {
                try {
                    String resourceName = getResources().getResourceName(drawablesToCheck[i]);
                    Log.d("DrawableCheck", "✓ " + drawableNames[i] + " exists: " + resourceName);
                } catch (Exception e) {
                    Log.d("DrawableCheck", "✗ " + drawableNames[i] + " MISSING: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e("DrawableCheck", "Error checking drawables: " + e.getMessage());
        }
    }

    // New method to check drawable properties and sizes
    private void checkDrawableProperties() {
        try {
            int[] drawableIds = {
                    R.drawable.ic_pet_egg,
                    R.drawable.ic_pet_baby,
                    R.drawable.ic_pet_teen,
                    R.drawable.ic_pet_adult,
                    R.drawable.ic_pet_happy,
                    R.drawable.ic_pet_hungry,
                    R.drawable.ic_pet_tired,
                    R.drawable.ic_pet_dirty
            };

            String[] names = {
                    "ic_pet_egg", "ic_pet_baby", "ic_pet_teen", "ic_pet_adult",
                    "ic_pet_happy", "ic_pet_hungry", "ic_pet_tired", "ic_pet_dirty"
            };

            for (int i = 0; i < drawableIds.length; i++) {
                try {
                    android.graphics.drawable.Drawable drawable = ContextCompat.getDrawable(this, drawableIds[i]);
                    if (drawable != null) {
                        int width = drawable.getIntrinsicWidth();
                        int height = drawable.getIntrinsicHeight();
                        Log.d("DrawableProps", names[i] + " - Size: " + width + "x" + height +
                                ", Bounds: " + drawable.getBounds() +
                                ", ConstantState: " + (drawable.getConstantState() != null));

                        // Check if drawable is effectively empty
                        if (width <= 0 || height <= 0) {
                            Log.e("DrawableProps", names[i] + " - EMPTY/INVALID SIZE!");
                        }
                    } else {
                        Log.e("DrawableProps", names[i] + " - NULL DRAWABLE");
                    }
                } catch (Exception e) {
                    Log.e("DrawableProps", names[i] + " - ERROR: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e("DrawableProps", "Error checking drawable properties: " + e.getMessage());
        }
    }

    private void debugStageInfo() {
        String stage = pet.getStage();
        double age = pet.getAge();
        Log.d("StageDebug", "=== STAGE DEBUG ===");
        Log.d("StageDebug", "Age: " + age + " days");
        Log.d("StageDebug", "Stage: " + stage);
        Log.d("StageDebug", "Alive: " + pet.isAlive());
        Log.d("StageDebug", "Sleeping: " + pet.isSleeping());
        Log.d("StageDebug", "Hunger: " + pet.getHunger() + ", Energy: " + pet.getEnergy() + ", Cleanliness: " + pet.getCleanliness());

        // Check stage thresholds
        if (age < 1) Log.d("StageDebug", "Should be: egg");
        else if (age < 3) Log.d("StageDebug", "Should be: baby");
        else if (age < 7) Log.d("StageDebug", "Should be: teen");
        else Log.d("StageDebug", "Should be: adult");
    }

    private void loadPet() {
        try {
            pet = petPreferences.loadPet();

            // Calculate time passed since last update
            long timePassed = System.currentTimeMillis() - pet.getLastUpdate();
            long minutesPassed = timePassed / (1000 * 60);

            // Check if this is a fresh pet (all stats at starting values)
            boolean isFreshPet = pet.getHunger() == 100 &&
                    pet.getHappiness() == 100 &&
                    pet.getEnergy() == 100 &&
                    pet.getAge() == 0 &&
                    pet.getCleanliness() == 100;

            // Apply background degradation ONLY if:
            // 1. More than 1 minute has passed AND
            // 2. Pet is alive AND
            // 3. This is NOT a fresh pet
            if (minutesPassed > 1 && pet.isAlive() && !isFreshPet) {
                // UPDATED: Balanced degradation rates (per minute)
                double hungerLoss = minutesPassed * 0.08;
                double happinessLoss = minutesPassed * 0.04;
                double energyLoss = minutesPassed * 0.04;
                double cleanlinessLoss = minutesPassed * 0.016;

                // Calculate age based on days passed (1440 minutes = 1 day)
                double daysPassed = minutesPassed / 1440.0;
                double previousAge = pet.getAge();
                pet.setAge(pet.getAge() + daysPassed);

                // If sleeping, apply sleep benefits
                if (pet.isSleeping()) {
                    // UPDATED: Balanced sleeping rates (per minute)
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

                pet.checkDeath();
                pet.setLastUpdate(System.currentTimeMillis());
                petPreferences.savePet(pet);

                if (minutesPassed > 10) {
                    if (pet.isSleeping()) {
                        showMessage("Welcome back! Your DigiBuddy is still sleeping... Zzz");
                    } else {
                        showMessage("Welcome back! Your DigiBuddy missed you!");
                    }
                }
            }
            // If it's a fresh pet, update the lastUpdate time to prevent immediate degradation
            else if (isFreshPet) {
                pet.setLastUpdate(System.currentTimeMillis());
                petPreferences.savePet(pet);
            }

            updateUI();
            updateSleepButtonText();

            // DEBUG: Show age calculation info
            long currentTimePassed = System.currentTimeMillis() - pet.getLastUpdate();
            long currentMinutesPassed = currentTimePassed / (1000 * 60);
            double currentDaysPassed = currentMinutesPassed / 1440.0;
            Log.d("AgeDebug", "Minutes passed: " + currentMinutesPassed + ", Days passed: " + currentDaysPassed + ", Current age: " + pet.getAge() + ", Stage: " + pet.getStage());

        } catch (Exception e) {
            // If anything fails, create a fresh pet
            pet = new Pet();
            petPreferences.savePet(pet);
            updateUI();
        }
    }

    private void checkMilestones(double previousAge, double currentAge) {
        try {
            int previousDays = (int) previousAge;
            int currentDays = (int) currentAge;

            // Check if we crossed any 10-day milestone
            if (currentDays > previousDays && currentDays % 10 == 0) {
                String milestoneMessage = "🎉 Milestone reached! Your DigiBuddy is now " + currentDays + " days old!";
                showMessage(milestoneMessage);
                updateStarsDisplay();
            }
        } catch (Exception e) {
            // Ignore milestone errors
        }
    }

    private void updateStarsDisplay() {
        try {
            starsContainer.removeAllViews();

            int totalStars = (int) pet.getAge() / 10;

            // Update star info text
            if (totalStars > 0) {
                if (totalStars == 1) {
                    starInfoText.setText("🌟 1 milestone");
                } else {
                    starInfoText.setText("🌟 " + totalStars + " milestones");
                }
            } else {
                starInfoText.setText("");
            }

            // Add larger, more visible stars
            for (int i = 0; i < totalStars; i++) {
                ImageView star = new ImageView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(36, 36);
                params.setMargins(4, 0, 4, 0);
                star.setLayoutParams(params);
                star.setImageResource(R.drawable.ic_gold_star);
                star.setContentDescription("10-day milestone star");
                starsContainer.addView(star);
            }
        } catch (Exception e) {
            // Ignore star display errors
        }
    }

    private void setupButtons() {
        feedButton.setOnClickListener(v -> feedPet());
        playButton.setOnClickListener(v -> playWithPet());
        sleepButton.setOnClickListener(v -> toggleSleep());
        cleanButton.setOnClickListener(v -> cleanPet());
        resetButton.setOnClickListener(v -> resetPet());

        // TEMPORARY: Debug button to force age progression
        resetButton.setOnLongClickListener(v -> {
            // Long press reset button to debug age
            double previousAge = pet.getAge();
            pet.setAge(pet.getAge() + 1.0);
            checkMilestones(previousAge, pet.getAge());
            saveAndUpdate();
            showMessage("Debug: Age increased to " + (int)pet.getAge() + ", Stage: " + pet.getStage());
            Log.d("StageDebug", "Manual age increase - Age: " + pet.getAge() + ", Stage: " + pet.getStage());
            return true;
        });

        // TEMPORARY: Debug button to check current stage
        feedButton.setOnLongClickListener(v -> {
            // Long press feed button to check stage info
            debugStageInfo();
            String debugInfo = "Age: " + pet.getAge() + ", Stage: " + pet.getStage() +
                    ", Hunger: " + pet.getHunger() + ", Alive: " + pet.isAlive();
            showMessage("Debug: " + debugInfo);
            Log.d("StageDebug", debugInfo);
            return true;
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {

                new AlertDialog.Builder(this)
                        .setTitle("Notification Permission Needed")
                        .setMessage("DigiBuddy needs notification permission to show low stat alerts. This helps you take better care of your pet!")
                        .setPositiveButton("Allow", (dialog, which) -> {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                                    PERMISSION_REQUEST_CODE);
                        })
                        .setNegativeButton("Deny", (dialog, which) -> {
                            Toast.makeText(this, "Low stat alerts disabled. You can enable them in Settings later.", Toast.LENGTH_LONG).show();
                            new Handler().postDelayed(() -> startPetService(), 1000);
                        })
                        .setCancelable(false)
                        .show();
            } else {
                new Handler().postDelayed(() -> startPetService(), 1000);
            }
        } else {
            new Handler().postDelayed(() -> startPetService(), 1000);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted! You'll get low stat alerts.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied. You can enable it in App Settings.", Toast.LENGTH_LONG).show();
            }
            startPetService();
        }
    }

    // NEW: Enhanced service management with notification control
    private void startPetService() {
        try {
            Intent serviceIntent = new Intent(this, PetService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.d("PetService", "Pet service started");
        } catch (Exception e) {
            Log.e("PetService", "Error starting pet service: " + e.getMessage());
        }
    }

    private void stopPetService() {
        try {
            Intent serviceIntent = new Intent(this, PetService.class);
            stopService(serviceIntent);
            Log.d("PetService", "Pet service stopped");
        } catch (Exception e) {
            Log.e("PetService", "Error stopping pet service: " + e.getMessage());
        }
    }

    // NEW: Enhanced sleep toggle with guaranteed notification cleanup
    private void toggleSleep() {
        if (!pet.isAlive()) {
            showMessage("Your DigiBuddy has passed away...");
            return;
        }

        boolean wasSleeping = pet.isSleeping();
        pet.setSleeping(!pet.isSleeping());

        // NEW: Immediate sleep state save for service synchronization
        petPreferences.saveSleepStateImmediately(pet.isSleeping());

        saveAndUpdate();
        updateSleepButtonText();

        // NEW: Force immediate notification cleanup with multiple strategies
        if (pet.isSleeping()) {
            // Pet is going to sleep - CLEAN UP ALL ENERGY NOTIFICATIONS
            forceImmediateEnergyNotificationCleanup();
            showMessage("Your DigiBuddy is now sleeping. Zzz...");
        } else {
            // Pet is waking up
            showMessage("Your DigiBuddy woke up!");
        }

        Log.d("SleepToggle", "Sleep state changed from " + wasSleeping + " to " + pet.isSleeping());
    }

    // NEW: Comprehensive energy notification cleanup
    private void forceImmediateEnergyNotificationCleanup() {
        Log.d("NotificationFix", "Starting comprehensive energy notification cleanup");

        // Strategy 1: Cancel notifications locally
        cancelLocalEnergyNotifications();

        // Strategy 2: Force service synchronization
        forceServiceSleepSync();

        // Strategy 3: Direct service communication
        sendImmediateCleanupToService();

        // Strategy 4: Nuclear option as fallback
        new Handler().postDelayed(this::nuclearNotificationCleanup, 1000);
    }

    // NEW: Cancel notifications locally
    private void cancelLocalEnergyNotifications() {
        try {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.cancel(ENERGY_WARNING_ID);
            nm.cancel(ENERGY_EMERGENCY_ID);
            Log.d("NotificationFix", "Local energy notifications cancelled");
        } catch (Exception e) {
            Log.e("NotificationFix", "Error cancelling local notifications: " + e.getMessage());
        }
    }

    // NEW: Enhanced service synchronization
    private void forceServiceSleepSync() {
        try {
            // Stop service completely
            stopPetService();

            // Wait for service to stop
            new Handler().postDelayed(() -> {
                // Restart service with fresh state
                startPetService();

                // Send immediate cleanup command
                sendImmediateCleanupToService();

            }, 500);

        } catch (Exception e) {
            Log.e("SleepSync", "Error in forceServiceSleepSync: " + e.getMessage());
        }
    }

    // NEW: Direct service communication
    private void sendImmediateCleanupToService() {
        try {
            Intent cleanupIntent = new Intent(this, PetService.class);
            cleanupIntent.setAction("CLEANUP_ENERGY_NOTIFICATIONS");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(cleanupIntent);
            } else {
                startService(cleanupIntent);
            }

            Log.d("NotificationFix", "Immediate cleanup sent to service");
        } catch (Exception e) {
            Log.e("NotificationFix", "Error sending cleanup to service: " + e.getMessage());
        }
    }

    // NEW: Nuclear option - complete notification reset
    private void nuclearNotificationCleanup() {
        try {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.cancelAll(); // Cancel ALL notifications from our app

            // Additional cleanup for any persistent notifications
            new Handler().postDelayed(() -> {
                nm.cancel(ENERGY_WARNING_ID);
                nm.cancel(ENERGY_EMERGENCY_ID);
            }, 200);

            Log.d("NuclearCleanup", "Nuclear notification cleanup executed");
        } catch (Exception e) {
            Log.e("NuclearCleanup", "Nuclear cleanup failed: " + e.getMessage());
        }
    }

    // NEW: Cleanup all energy notifications on startup
    private void cleanupAllEnergyNotifications() {
        new Handler().postDelayed(() -> {
            if (pet != null && pet.isSleeping()) {
                forceImmediateEnergyNotificationCleanup();
            }
        }, 2000);
    }

    private void feedPet() {
        if (!pet.isAlive()) {
            showMessage("Your DigiBuddy has passed away...");
            return;
        }

        if (pet.isSleeping()) {
            showMessage("Your DigiBuddy is sleeping! Wait for it to wake up.");
            return;
        }

        pet.setHunger(Math.min(100, pet.getHunger() + 25));
        pet.setHappiness(Math.min(100, pet.getHappiness() + 5));
        pet.setCleanliness(Math.max(0, pet.getCleanliness() - 5));
        saveAndUpdate();
        showMessage("Yum! Your DigiBuddy enjoyed the meal!");
    }

    private void playWithPet() {
        if (!pet.isAlive()) {
            showMessage("Your DigiBuddy has passed away...");
            return;
        }

        if (pet.isSleeping()) {
            showMessage("Your DigiBuddy is sleeping! Wait for it to wake up.");
            return;
        }

        if (pet.getEnergy() < 20) {
            showMessage("Your DigiBuddy is too tired to play right now.");
            return;
        }

        pet.setHappiness(Math.min(100, pet.getHappiness() + 15));
        pet.setEnergy(Math.max(0, pet.getEnergy() - 8));
        pet.setHunger(Math.max(0, pet.getHunger() - 3));
        pet.setCleanliness(Math.max(0, pet.getCleanliness() - 3));
        saveAndUpdate();
        showMessage("Your DigiBuddy had fun playing!");
    }

    private void cleanPet() {
        if (!pet.isAlive()) {
            showMessage("Your DigiBuddy has passed away...");
            return;
        }

        if (pet.isSleeping()) {
            showMessage("Your DigiBuddy is sleeping! Wait for it to wake up.");
            return;
        }

        pet.setCleanliness(100);
        pet.setHappiness(Math.min(100, pet.getHappiness() + 10));
        saveAndUpdate();
        showMessage("Your DigiBuddy feels fresh and clean!");
    }

    private void resetPet() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset DigiBuddy")
                .setMessage("Are you sure you want to reset your DigiBuddy? This cannot be undone!")
                .setPositiveButton("Reset", (dialog, which) -> {
                    petPreferences.resetPet();
                    pet = new Pet();
                    updateUI();
                    updateSleepButtonText();
                    showMessage("A new DigiBuddy has arrived! Take good care of it.");
                    stopPetService();
                    startPetService();
                    // NEW: Cleanup notifications on reset
                    cleanupAllEnergyNotifications();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void saveAndUpdate() {
        pet.setLastUpdate(System.currentTimeMillis());
        petPreferences.savePet(pet);
        updateUI();
    }

    private void updateUI() {
        try {
            hungerBar.setProgress((int) pet.getHunger());
            happinessBar.setProgress((int) pet.getHappiness());
            energyBar.setProgress((int) pet.getEnergy());
            cleanlinessBar.setProgress((int) pet.getCleanliness());

            hungerText.setText(String.valueOf((int) pet.getHunger()));
            happinessText.setText(String.valueOf((int) pet.getHappiness()));
            energyText.setText(String.valueOf((int) pet.getEnergy()));
            cleanlinessText.setText(String.valueOf((int) pet.getCleanliness()));
            ageText.setText(String.valueOf((int) pet.getAge()));

            updateStarsDisplay();
            updatePetImage();
            updateButtonStates();
            checkLowStats();
        } catch (Exception e) {
            Toast.makeText(this, "UI update error, recovering...", Toast.LENGTH_SHORT).show();
        }
    }

    // Mood detection method
    private PetMood determinePetMood() {
        try {
            if (!pet.isAlive()) {
                return PetMood.DEFAULT;
            }

            // EGG STAGE: Always show egg, no moods
            if ("egg".equals(pet.getStage())) {
                return PetMood.DEFAULT;
            }

            if (pet.isSleeping()) {
                return PetMood.SLEEPING;
            }

            // Check for critical needs first
            if (pet.getHunger() < 20) {
                return PetMood.HUNGRY;
            }
            if (pet.getEnergy() < 20) {
                return PetMood.TIRED;
            }
            if (pet.getCleanliness() < 30) {
                return PetMood.DIRTY;
            }

            // Then check for happiness (only if not in critical state)
            if (pet.getHappiness() > 70 && pet.getEnergy() > 50 && pet.getHunger() > 50) {
                return PetMood.HAPPY;
            }

            return PetMood.DEFAULT;
        } catch (Exception e) {
            return PetMood.DEFAULT;
        }
    }

    private void updatePetImage() {
        try {
            debugStageInfo();

            int drawableId = R.drawable.ic_pet_egg;
            String stage = pet.getStage();

            Log.d("StageDebug", "Age: " + pet.getAge() + ", Stage: " + stage);

            // First set base image by life stage
            if ("baby".equals(stage)) {
                drawableId = R.drawable.ic_pet_baby;
                Log.d("DrawableDebug", "Setting base image: BABY");
            } else if ("teen".equals(stage)) {
                drawableId = R.drawable.ic_pet_teen;
                Log.d("DrawableDebug", "Setting base image: TEEN");
            } else if ("adult".equals(stage)) {
                drawableId = R.drawable.ic_pet_adult;
                Log.d("DrawableDebug", "Setting base image: ADULT");
            } else {
                drawableId = R.drawable.ic_pet_egg;
                Log.d("DrawableDebug", "Setting base image: EGG");
            }

            // APPLY MOOD OVERRIDES (if not egg)
            if (!"egg".equals(stage) && pet.isAlive() && !pet.isSleeping()) {
                PetMood mood = determinePetMood();
                Log.d("MoodDebug", "Current mood: " + mood);

                switch (mood) {
                    case HAPPY:
                        drawableId = R.drawable.ic_pet_happy;
                        Log.d("DrawableDebug", "Override: HAPPY mood");
                        break;
                    case HUNGRY:
                        drawableId = R.drawable.ic_pet_hungry;
                        Log.d("DrawableDebug", "Override: HUNGRY mood");
                        break;
                    case TIRED:
                        drawableId = R.drawable.ic_pet_tired;
                        Log.d("DrawableDebug", "Override: TIRED mood");
                        break;
                    case DIRTY:
                        drawableId = R.drawable.ic_pet_dirty;
                        Log.d("DrawableDebug", "Override: DIRTY mood");
                        break;
                    default:
                        Log.d("DrawableDebug", "No mood override, using life stage image");
                        break;
                }
            }

            // Special case for sleeping - use tired image
            if (pet.isSleeping() && !"egg".equals(stage)) {
                drawableId = R.drawable.ic_pet_tired;
                Log.d("DrawableDebug", "Pet is sleeping - using TIRED image");
            }

            // Set the final image
            Log.d("DrawableDebug", "Final drawable ID: " + drawableId);
            petImage.setImageResource(drawableId);

            // Make sure ImageView is properly configured
            petImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            petImage.setAdjustViewBounds(true);

            // Apply visual effects
            if (!pet.isAlive()) {
                petImage.setAlpha(0.5f);
                showMessage("Your DigiBuddy has passed away... Reset to start over.");
            } else if (pet.isSleeping()) {
                petImage.setAlpha(0.7f);
            } else {
                petImage.setAlpha(1.0f);
            }

            // Update mood message
            PetMood currentMood = determinePetMood();
            updateMoodMessage(currentMood);

        } catch (Exception e) {
            Log.e("StageDebug", "Error in updatePetImage: " + e.getMessage());
            e.printStackTrace();
            try {
                petImage.setImageResource(R.drawable.ic_pet_egg);
                petImage.setAlpha(1.0f);
            } catch (Exception ex) {
                Log.e("StageDebug", "Even egg fallback failed: " + ex.getMessage());
            }
        }
    }

    // Updated mood message method
    private void updateMoodMessage(PetMood mood) {
        try {
            String message;

            if (!pet.isAlive()) {
                message = "Your DigiBuddy has passed away... Reset to start over.";
            } else if ("egg".equals(pet.getStage())) {
                message = "I'm still an egg! Keep taking care of me! 🥚";
            } else {
                switch (mood) {
                    case HAPPY:
                        message = "I'm so happy! Thank you for taking good care of me! 🎉";
                        break;
                    case HUNGRY:
                        message = "I'm really hungry... Can I have some food? 🍕";
                        break;
                    case TIRED:
                        message = "I'm feeling very tired... I need some rest 😴";
                        break;
                    case SLEEPING:
                        message = "Zzz... I'm sleeping peacefully 💤";
                        break;
                    case DIRTY:
                        message = "I feel dirty and uncomfortable... Can you clean me? 🛁";
                        break;
                    default:
                        if (pet.getHappiness() > 70) {
                            message = "I'm having a great day! Thanks for being awesome!";
                        } else if (pet.getEnergy() > 80) {
                            message = "I'm full of energy! Let's do something fun!";
                        } else {
                            message = "Hello! I'm doing okay today!";
                        }
                        break;
                }
            }

            messageText.setText(message);
        } catch (Exception e) {
            // Ignore message errors
        }
    }

    private void checkLowStats() {
        if (!pet.isAlive()) return;

        // Warning alerts at 25%
        if (pet.getHunger() <= 25 && pet.getHunger() > 15) {
            showMessage("🍕 Your DigiBuddy is getting hungry! Consider feeding soon.");
        } else if (pet.getHappiness() <= 25 && pet.getHappiness() > 15) {
            showMessage("😢 Your DigiBuddy is feeling sad! Some playtime would help!");
        } else if (pet.getEnergy() <= 25 && pet.getEnergy() > 15) {
            showMessage("😴 Your DigiBuddy is getting tired! Maybe some rest soon?");
        } else if (pet.getCleanliness() <= 25 && pet.getCleanliness() > 15) {
            showMessage("🛁 Your DigiBuddy is getting dirty! A cleaning would be nice!");
        }

        // Emergency alerts at 15%
        else if (pet.getHunger() <= 15 && pet.getHunger() > 0) {
            showMessage("⚠️ Your DigiBuddy is very hungry! Feed it now!");
        } else if (pet.getHappiness() <= 15 && pet.getHappiness() > 0) {
            showMessage("⚠️ Your DigiBuddy is very sad! Play with it urgently!");
        } else if (pet.getEnergy() <= 15 && pet.getEnergy() > 0) {
            showMessage("⚠️ Your DigiBuddy is exhausted! Let it sleep immediately!");
        } else if (pet.getCleanliness() <= 15 && pet.getCleanliness() > 0) {
            showMessage("⚠️ Your DigiBuddy is very dirty! Clean it right away!");
        }
    }

    private void updateButtonStates() {
        boolean isAlive = pet.isAlive();
        boolean isSleeping = pet.isSleeping();

        feedButton.setEnabled(isAlive && !isSleeping);
        playButton.setEnabled(isAlive && !isSleeping && pet.getEnergy() >= 20);
        sleepButton.setEnabled(isAlive);
        cleanButton.setEnabled(isAlive && !isSleeping);
        resetButton.setEnabled(true);
    }

    private void showMessage(String message) {
        try {
            messageText.setText(message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Ignore message errors
        }
    }

    private void updateSleepButtonText() {
        if (pet.isSleeping()) {
            sleepButton.setText("⏰ WAKE");
        } else {
            sleepButton.setText("😴 SLEEP");
        }
    }

    private void startUIUpdates() {
        uiUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (pet.isAlive()) {
                    double previousAge = pet.getAge();

                    if (pet.isSleeping()) {
                        pet.setEnergy(Math.min(100, pet.getEnergy() + 0.004));
                        pet.setHunger(Math.max(0, pet.getHunger() - 0.0004));
                        pet.setHappiness(Math.max(0, pet.getHappiness() - 0.00027));
                        pet.setCleanliness(Math.max(0, pet.getCleanliness() - 0.00013));
                    } else {
                        pet.setHunger(Math.max(0, pet.getHunger() - 0.00133));
                        pet.setHappiness(Math.max(0, pet.getHappiness() - 0.00067));
                        pet.setEnergy(Math.max(0, pet.getEnergy() - 0.00067));
                        pet.setCleanliness(Math.max(0, pet.getCleanliness() - 0.00027));
                    }

                    pet.setAge(pet.getAge() + 0.00001157);
                    checkMilestones(previousAge, pet.getAge());
                    pet.updateStage();
                    pet.checkDeath();
                    saveAndUpdate();
                }
                uiHandler.postDelayed(this, 1000);
            }
        };
        uiHandler.postDelayed(uiUpdateRunnable, 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPet();
        // NEW: Cleanup notifications when resuming
        if (pet != null && pet.isSleeping()) {
            cleanupAllEnergyNotifications();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (uiHandler != null && uiUpdateRunnable != null) {
            uiHandler.removeCallbacks(uiUpdateRunnable);
        }
    }
}