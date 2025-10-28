package com.example.digibuddy;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
    }

    private void loadPet() {
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
            double hungerLoss = minutesPassed * 0.1;
            double happinessLoss = minutesPassed * 0.05;
            double energyLoss = minutesPassed * 0.05;
            double cleanlinessLoss = minutesPassed * 0.02;

            // Calculate age based on days passed (1440 minutes = 1 day)
            double daysPassed = minutesPassed / 1440.0;
            double previousAge = pet.getAge();
            pet.setAge(pet.getAge() + daysPassed);

            // Check for milestone achievements
            checkMilestones(previousAge, pet.getAge());

            // If sleeping, apply sleep benefits
            if (pet.isSleeping()) {
                // While sleeping: energy restores, hunger decreases slower
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
    }

    private void checkMilestones(double previousAge, double currentAge) {
        int previousDays = (int) previousAge;
        int currentDays = (int) currentAge;

        // Check if we crossed any 10-day milestone
        if (currentDays > previousDays && currentDays % 10 == 0) {
            String milestoneMessage = "🎉 Milestone reached! Your DigiBuddy is now " + currentDays + " days old!";
            showMessage(milestoneMessage);

            // Update stars display
            updateStarsDisplay();
        }
    }

    private void updateStarsDisplay() {
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
            starInfoText.setText(""); // No text when no stars
        }

        // Add larger, more visible stars
        for (int i = 0; i < totalStars; i++) {
            ImageView star = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    36, // Larger stars
                    36
            );
            params.setMargins(4, 0, 4, 0);
            star.setLayoutParams(params);
            star.setImageResource(R.drawable.ic_gold_star);
            star.setContentDescription("10-day milestone star");
            starsContainer.addView(star);
        }
    }

    private void setupButtons() {
        feedButton.setOnClickListener(v -> feedPet());
        playButton.setOnClickListener(v -> playWithPet());
        sleepButton.setOnClickListener(v -> toggleSleep());
        cleanButton.setOnClickListener(v -> cleanPet());
        resetButton.setOnClickListener(v -> resetPet());
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

    private void toggleSleep() {
        if (!pet.isAlive()) {
            showMessage("Your DigiBuddy has passed away...");
            return;
        }

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
    }

    private void checkLowStats() {
        if (!pet.isAlive()) return;

        if (pet.getHunger() <= 10 && pet.getHunger() > 0) {
            showMessage("⚠️ Your DigiBuddy is very hungry! Feed it!");
        } else if (pet.getHappiness() <= 10 && pet.getHappiness() > 0) {
            showMessage("⚠️ Your DigiBuddy is very sad! Play with it!");
        } else if (pet.getEnergy() <= 10 && pet.getEnergy() > 0) {
            showMessage("⚠️ Your DigiBuddy is exhausted! Let it sleep!");
        } else if (pet.getCleanliness() <= 10 && pet.getCleanliness() > 0) {
            showMessage("⚠️ Your DigiBuddy is very dirty! Clean it!");
        }
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
        resetButton.setEnabled(true);
    }

    private void showMessage(String message) {
        messageText.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void startUIUpdates() {
        uiUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (pet.isAlive()) {
                    double previousAge = pet.getAge();

                    if (pet.isSleeping()) {
                        pet.setEnergy(Math.min(100, pet.getEnergy() + 0.3));
                        pet.setHunger(Math.max(0, pet.getHunger() - 0.03));
                        pet.setHappiness(Math.max(0, pet.getHappiness() - 0.02));
                        pet.setCleanliness(Math.max(0, pet.getCleanliness() - 0.01));
                    } else {
                        pet.setHunger(Math.max(0, pet.getHunger() - 0.1));
                        pet.setHappiness(Math.max(0, pet.getHappiness() - 0.05));
                        pet.setEnergy(Math.max(0, pet.getEnergy() - 0.05));
                        pet.setCleanliness(Math.max(0, pet.getCleanliness() - 0.02));
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (uiHandler != null && uiUpdateRunnable != null) {
            uiHandler.removeCallbacks(uiUpdateRunnable);
        }
    }
}