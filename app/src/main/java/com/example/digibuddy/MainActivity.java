package com.example.digibuddy;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
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
    private ProgressBar hungerBar, happinessBar, energyBar, ageBar;
    private TextView hungerText, happinessText, energyText, ageText, messageText;
    private Button feedButton, playButton, sleepButton, cleanButton, resetButton;

    private final Handler uiHandler = new Handler();
    private Runnable uiUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        petPreferences = new PetPreferences(this);
        loadPet();

        setupButtons();
        startUIUpdates();

        // Request notification permission first, then start service
        requestNotificationPermission();
    }

    private void initializeViews() {
        petImage = findViewById(R.id.petImage);
        hungerBar = findViewById(R.id.hungerBar);
        happinessBar = findViewById(R.id.happinessBar);
        energyBar = findViewById(R.id.energyBar);
        ageBar = findViewById(R.id.ageBar);

        hungerText = findViewById(R.id.hungerText);
        happinessText = findViewById(R.id.happinessText);
        energyText = findViewById(R.id.energyText);
        ageText = findViewById(R.id.ageText);
        messageText = findViewById(R.id.messageText);

        feedButton = findViewById(R.id.feedButton);
        playButton = findViewById(R.id.playButton);
        sleepButton = findViewById(R.id.sleepButton);
        cleanButton = findViewById(R.id.cleanButton);
        resetButton = findViewById(R.id.resetButton);
    }

    private void loadPet() {
        pet = petPreferences.loadPet();

        // Calculate time passed since last update
        long timePassed = System.currentTimeMillis() - pet.getLastUpdate();
        long minutesPassed = timePassed / (1000 * 60);

        // Apply background degradation if significant time passed
        if (minutesPassed > 1 && pet.isAlive()) {
            double hungerLoss = minutesPassed * 0.1;
            double happinessLoss = minutesPassed * 0.05;
            double energyLoss = minutesPassed * 0.05;
            double ageGain = minutesPassed * 0.001;
            double cleanlinessLoss = minutesPassed * 0.02;

            // If sleeping, apply sleep benefits
            if (pet.isSleeping()) {
                // While sleeping: energy restores, hunger decreases slower
                double energyGain = minutesPassed * 0.5; // Energy restores while sleeping
                pet.setEnergy(Math.min(100, pet.getEnergy() + energyGain));
                hungerLoss *= 0.3; // Hunger decreases much slower while sleeping
                happinessLoss *= 0.5; // Happiness decreases slower while sleeping
                cleanlinessLoss *= 0.5; // Cleanliness decreases slower while sleeping
                energyLoss = 0; // No energy loss while sleeping
            }

            pet.setHunger(Math.max(0, pet.getHunger() - hungerLoss));
            pet.setHappiness(Math.max(0, pet.getHappiness() - happinessLoss));
            pet.setEnergy(Math.max(0, pet.getEnergy() - energyLoss));
            pet.setCleanliness(Math.max(0, pet.getCleanliness() - cleanlinessLoss));
            pet.setAge(pet.getAge() + ageGain);

            pet.updateStage();
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

        updateUI();
        updateSleepButtonText();
    }

    private void setupButtons() {
        feedButton.setOnClickListener(v -> feedPet());
        playButton.setOnClickListener(v -> playWithPet());
        sleepButton.setOnClickListener(v -> toggleSleep());
        cleanButton.setOnClickListener(v -> cleanPet());
        resetButton.setOnClickListener(v -> resetPet());
    }

    private void requestNotificationPermission() {
        // Only required for Android 13 (API 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {

                // Show explanation before requesting
                new AlertDialog.Builder(this)
                        .setTitle("Notification Permission Needed")
                        .setMessage("DigiBuddy needs notification permission to show pet status updates and reminders. This helps you take better care of your pet!")
                        .setPositiveButton("Allow", (dialog, which) -> {
                            // Request the permission
                            ActivityCompat.requestPermissions(this,
                                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                                    PERMISSION_REQUEST_CODE);
                        })
                        .setNegativeButton("Deny", (dialog, which) -> {
                            Toast.makeText(this, "Notifications disabled. You can enable them in Settings later.", Toast.LENGTH_LONG).show();
                            startPetService(); // Start service even without permission
                        })
                        .setCancelable(false)
                        .show();
            } else {
                // Permission already granted
                startPetService();
            }
        } else {
            // Below Android 13, no permission needed
            startPetService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted! You'll get pet updates.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied. You can enable it in App Settings.", Toast.LENGTH_LONG).show();
            }
            // Start service regardless of permission result
            startPetService();
        }
    }

    private void startPetService() {
        android.content.Intent serviceIntent = new android.content.Intent(this, PetService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopPetService() {
        android.content.Intent serviceIntent = new android.content.Intent(this, PetService.class);
        stopService(serviceIntent);
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
        pet.setCleanliness(Math.max(0, pet.getCleanliness() - 5)); // Feeding makes pet a bit messy
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
        pet.setCleanliness(Math.max(0, pet.getCleanliness() - 3)); // Playing makes pet messy
        saveAndUpdate();
        showMessage("Your DigiBuddy had fun playing!");
    }

    private void toggleSleep() {
        if (!pet.isAlive()) return;

        pet.setSleeping(!pet.isSleeping());
        saveAndUpdate();
        updateSleepButtonText();

        if (pet.isSleeping()) {
            showMessage("Your DigiBuddy is now sleeping. Zzz...");
        } else {
            showMessage("Your DigiBuddy woke up!");
        }
    }

    private void updateSleepButtonText() {
        if (pet.isSleeping()) {
            sleepButton.setText("⏰ WAKE");
        } else {
            sleepButton.setText("😴 SLEEP");
        }
    }

    private void cleanPet() {
        if (!pet.isAlive()) return;

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
        hungerBar.setProgress((int) pet.getHunger());
        happinessBar.setProgress((int) pet.getHappiness());
        energyBar.setProgress((int) pet.getEnergy());
        ageBar.setProgress((int) Math.min(100, pet.getAge() * 14.28)); // Adjusted for 0-7 age range

        hungerText.setText(String.valueOf((int) pet.getHunger()));
        happinessText.setText(String.valueOf((int) pet.getHappiness()));
        energyText.setText(String.valueOf((int) pet.getEnergy()));
        ageText.setText(String.format("%.1f", pet.getAge()));

        updatePetImage();
        updateButtonStates();
    }

    private void updatePetImage() {
        int drawableId = R.drawable.ic_pet_egg;

        if ("baby".equals(pet.getStage())) {
            drawableId = R.drawable.ic_pet_baby;
        } else if ("teen".equals(pet.getStage())) {
            drawableId = R.drawable.ic_pet_teen;
        } else if ("adult".equals(pet.getStage())) {
            drawableId = R.drawable.ic_pet_adult;
        }

        petImage.setImageResource(drawableId);

        if (!pet.isAlive()) {
            petImage.setAlpha(0.5f);
            showMessage("Your DigiBuddy has passed away... Reset to start over.");
        } else if (pet.isSleeping()) {
            petImage.setAlpha(0.7f);
        } else {
            petImage.setAlpha(1.0f);
        }
    }

    private void updateButtonStates() {
        boolean isAlive = pet.isAlive();
        boolean isSleeping = pet.isSleeping();

        feedButton.setEnabled(isAlive && !isSleeping);
        playButton.setEnabled(isAlive && !isSleeping && pet.getEnergy() >= 20);
        sleepButton.setEnabled(isAlive);
        cleanButton.setEnabled(isAlive && !isSleeping);
        resetButton.setEnabled(true); // Always enabled
    }

    private void showMessage(String message) {
        messageText.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void startUIUpdates() {
        uiUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                // Simulate stat decay over time (only when app is open)
                if (pet.isAlive()) {
                    if (pet.isSleeping()) {
                        // While sleeping: energy restores, other stats decrease slower
                        pet.setEnergy(Math.min(100, pet.getEnergy() + 0.3));
                        pet.setHunger(Math.max(0, pet.getHunger() - 0.03));
                        pet.setHappiness(Math.max(0, pet.getHappiness() - 0.02));
                        pet.setCleanliness(Math.max(0, pet.getCleanliness() - 0.01));
                    } else {
                        // While awake: normal stat decay
                        pet.setHunger(Math.max(0, pet.getHunger() - 0.1));
                        pet.setHappiness(Math.max(0, pet.getHappiness() - 0.05));
                        pet.setEnergy(Math.max(0, pet.getEnergy() - 0.05));
                        pet.setCleanliness(Math.max(0, pet.getCleanliness() - 0.02));
                    }

                    pet.setAge(pet.getAge() + 0.001);
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
        // Reload pet data when app comes to foreground
        loadPet();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (uiHandler != null && uiUpdateRunnable != null) {
            uiHandler.removeCallbacks(uiUpdateRunnable);
        }
        // Don't stop service here - let it run in background
    }
}