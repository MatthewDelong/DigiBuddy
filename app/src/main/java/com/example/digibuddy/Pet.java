package com.example.digibuddy;

import android.util.Log;

public class Pet {
    private static final String TAG = "Pet";

    private double hunger;
    private double happiness;
    private double energy;
    private double age;
    private String stage;
    private boolean isSleeping;
    private boolean isAlive;
    private double cleanliness;
    private long lastUpdate;
    private int milestonesAchieved;

    // Default constructor with validation
    public Pet() {
        reset(); // Use reset to ensure consistent initialization
        Log.d(TAG, "New pet created with default values");
    }

    // Copy constructor for safe data restoration
    public Pet(Pet other) {
        if (other != null) {
            this.hunger = validateStat(other.hunger, 100);
            this.happiness = validateStat(other.happiness, 100);
            this.energy = validateStat(other.energy, 100);
            this.age = Math.max(0, other.age);
            this.stage = validateStage(other.stage);
            this.isSleeping = other.isSleeping;
            this.isAlive = other.isAlive;
            this.cleanliness = validateStat(other.cleanliness, 100);
            this.lastUpdate = validateTimestamp(other.lastUpdate);
            this.milestonesAchieved = Math.max(0, other.milestonesAchieved);
        } else {
            reset();
        }
        Log.d(TAG, "Pet copy constructed from existing pet");
    }

    // Comprehensive stat validation
    private double validateStat(double value, double defaultValue) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0 || value > 100) {
            Log.w(TAG, "Invalid stat value: " + value + ", resetting to: " + defaultValue);
            return defaultValue;
        }
        return value;
    }

    // Stage validation
    private String validateStage(String stage) {
        if (stage == null) {
            Log.w(TAG, "Null stage, resetting to egg");
            return "egg";
        }

        switch (stage) {
            case "egg":
            case "baby":
            case "teen":
            case "adult":
                return stage;
            default:
                Log.w(TAG, "Invalid stage: " + stage + ", resetting to egg");
                return "egg";
        }
    }

    // Timestamp validation
    private long validateTimestamp(long timestamp) {
        long currentTime = System.currentTimeMillis();
        long oneYearAgo = currentTime - (365L * 24 * 60 * 60 * 1000);
        long oneYearFuture = currentTime + (365L * 24 * 60 * 60 * 1000);

        if (timestamp < oneYearAgo || timestamp > oneYearFuture) {
            Log.w(TAG, "Invalid timestamp: " + timestamp + ", resetting to current time");
            return currentTime;
        }
        return timestamp;
    }

    // Getters with validation
    public double getHunger() {
        return validateStat(hunger, 100);
    }

    public void setHunger(double hunger) {
        this.hunger = validateStat(hunger, 100);
        checkDeath(); // Auto-check death on critical stat changes
    }

    public double getHappiness() {
        return validateStat(happiness, 100);
    }

    public void setHappiness(double happiness) {
        this.happiness = validateStat(happiness, 100);
        checkDeath();
    }

    public double getEnergy() {
        return validateStat(energy, 100);
    }

    public void setEnergy(double energy) {
        this.energy = validateStat(energy, 100);
        checkDeath();
    }

    public double getAge() {
        return Math.max(0, age);
    }

    public void setAge(double age) {
        this.age = Math.max(0, age);
        updateStage(); // Auto-update stage when age changes
        Log.d(TAG, "Age set to: " + age + ", stage: " + stage);
    }

    public String getStage() {
        return validateStage(stage);
    }

    public void setStage(String stage) {
        this.stage = validateStage(stage);
    }

    public boolean isSleeping() {
        return isSleeping;
    }

    public void setSleeping(boolean sleeping) {
        boolean oldState = this.isSleeping;
        this.isSleeping = sleeping;

        if (oldState != sleeping) {
            Log.d(TAG, "Sleep state changed from " + oldState + " to " + sleeping);
        }
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        this.isAlive = alive;
        if (!alive) {
            Log.w(TAG, "Pet has died");
        }
    }

    public double getCleanliness() {
        return validateStat(cleanliness, 100);
    }

    public void setCleanliness(double cleanliness) {
        this.cleanliness = validateStat(cleanliness, 100);
        checkDeath();
    }

    public long getLastUpdate() {
        return validateTimestamp(lastUpdate);
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = validateTimestamp(lastUpdate);
    }

    public int getMilestonesAchieved() {
        return Math.max(0, milestonesAchieved);
    }

    public void setMilestonesAchieved(int milestonesAchieved) {
        this.milestonesAchieved = Math.max(0, milestonesAchieved);
    }

    // Enhanced stage update with validation
    public void updateStage() {
        String oldStage = this.stage;

        if (age >= 7) {
            this.stage = "adult";
        } else if (age >= 3) {
            this.stage = "teen";
        } else if (age >= 1) {
            this.stage = "baby";
        } else {
            this.stage = "egg";
        }

        // Only log if stage actually changed
        if (!this.stage.equals(oldStage)) {
            Log.d(TAG, "Stage updated from " + oldStage + " to " + this.stage + " at age " + age);
        }
    }

    // Enhanced death check with comprehensive validation
    public void checkDeath() {
        boolean wasAlive = this.isAlive;

        // Check if any critical stat reached zero
        boolean shouldDie = getHunger() <= 0 || getHappiness() <= 0 ||
                getEnergy() <= 0 || getCleanliness() <= 0;

        this.isAlive = !shouldDie;

        if (wasAlive && !this.isAlive) {
            Log.w(TAG, "Pet died - Stats: Hunger=" + getHunger() +
                    ", Happiness=" + getHappiness() +
                    ", Energy=" + getEnergy() +
                    ", Cleanliness=" + getCleanliness());
        }
    }

    // Safe stat modification methods
    public void modifyHunger(double delta) {
        setHunger(getHunger() + delta);
    }

    public void modifyHappiness(double delta) {
        setHappiness(getHappiness() + delta);
    }

    public void modifyEnergy(double delta) {
        setEnergy(getEnergy() + delta);
    }

    public void modifyCleanliness(double delta) {
        setCleanliness(getCleanliness() + delta);
    }

    // Comprehensive reset with logging
    public void reset() {
        this.hunger = 100;
        this.happiness = 100;
        this.energy = 100;
        this.age = 0;
        this.stage = "egg";
        this.isSleeping = false;
        this.isAlive = true;
        this.cleanliness = 100;
        this.lastUpdate = System.currentTimeMillis();
        this.milestonesAchieved = 0;

        Log.d(TAG, "Pet completely reset to initial state");
    }

    // Validation method for entire pet state
    public boolean isValid() {
        try {
            return !Double.isNaN(hunger) && hunger >= 0 && hunger <= 100 &&
                    !Double.isNaN(happiness) && happiness >= 0 && happiness <= 100 &&
                    !Double.isNaN(energy) && energy >= 0 && energy <= 100 &&
                    !Double.isNaN(cleanliness) && cleanliness >= 0 && cleanliness <= 100 &&
                    !Double.isNaN(age) && age >= 0 &&
                    stage != null && (stage.equals("egg") || stage.equals("baby") ||
                    stage.equals("teen") || stage.equals("adult")) &&
                    lastUpdate > 0 && lastUpdate <= System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000) &&
                    milestonesAchieved >= 0;
        } catch (Exception e) {
            Log.e(TAG, "Pet validation failed: " + e.getMessage());
            return false;
        }
    }

    // Emergency recovery method
    public void emergencyRecovery() {
        Log.e(TAG, "Emergency pet recovery triggered");

        this.hunger = Math.max(0, Math.min(100, this.hunger));
        this.happiness = Math.max(0, Math.min(100, this.happiness));
        this.energy = Math.max(0, Math.min(100, this.energy));
        this.cleanliness = Math.max(0, Math.min(100, this.cleanliness));
        this.age = Math.max(0, this.age);
        this.stage = validateStage(this.stage);
        this.lastUpdate = Math.min(System.currentTimeMillis(),
                Math.max(System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000), this.lastUpdate));
        this.milestonesAchieved = Math.max(0, this.milestonesAchieved);

        // Ensure pet is alive if stats are reasonable
        if (this.hunger > 10 && this.happiness > 10 && this.energy > 10 && this.cleanliness > 10) {
            this.isAlive = true;
        }

        Log.d(TAG, "Emergency recovery completed");
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.US,
                "Pet[Hunger=%.1f, Happiness=%.1f, Energy=%.1f, Cleanliness=%.1f, " +
                        "Age=%.1f, Stage=%s, Sleeping=%s, Alive=%s, Milestones=%d]",
                hunger, happiness, energy, cleanliness, age, stage, isSleeping, isAlive, milestonesAchieved);
    }
}