package com.example.digibuddy;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class PetPreferences {
    private static final String PREFS_NAME = "DigiBuddyPrefs";
    private SharedPreferences sharedPreferences;

    private static final String KEY_HUNGER = "hunger";
    private static final String KEY_HAPPINESS = "happiness";
    private static final String KEY_ENERGY = "energy";
    private static final String KEY_AGE = "age";
    private static final String KEY_STAGE = "stage";
    private static final String KEY_SLEEPING = "sleeping";
    private static final String KEY_ALIVE = "alive";
    private static final String KEY_CLEANLINESS = "cleanliness";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final String KEY_MILESTONES = "milestones";

    public PetPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void savePet(Pet pet) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(KEY_HUNGER, (float) pet.getHunger());
        editor.putFloat(KEY_HAPPINESS, (float) pet.getHappiness());
        editor.putFloat(KEY_ENERGY, (float) pet.getEnergy());
        editor.putFloat(KEY_AGE, (float) pet.getAge());
        editor.putString(KEY_STAGE, pet.getStage());
        editor.putBoolean(KEY_SLEEPING, pet.isSleeping());
        editor.putBoolean(KEY_ALIVE, pet.isAlive());
        editor.putFloat(KEY_CLEANLINESS, (float) pet.getCleanliness());
        editor.putLong(KEY_LAST_UPDATE, pet.getLastUpdate());
        editor.putInt(KEY_MILESTONES, pet.getMilestonesAchieved());
        editor.apply(); // Use apply() for immediate async write

        Log.d("PetPreferences", "Pet saved - Sleeping: " + pet.isSleeping() + ", Energy: " + pet.getEnergy());
    }

    public Pet loadPet() {
        // Check for fresh install
        if (sharedPreferences.getAll().size() == 0) {
            Pet freshPet = new Pet();
            // Reset lastUpdate to current time for fresh installs
            freshPet.setLastUpdate(System.currentTimeMillis());
            Log.d("PetPreferences", "Fresh pet created");
            return freshPet;
        }

        Pet pet = new Pet();
        pet.setHunger(sharedPreferences.getFloat(KEY_HUNGER, 100));
        pet.setHappiness(sharedPreferences.getFloat(KEY_HAPPINESS, 100));
        pet.setEnergy(sharedPreferences.getFloat(KEY_ENERGY, 100));
        pet.setAge(sharedPreferences.getFloat(KEY_AGE, 0));
        pet.setStage(sharedPreferences.getString(KEY_STAGE, "egg"));
        pet.setSleeping(sharedPreferences.getBoolean(KEY_SLEEPING, false));
        pet.setAlive(sharedPreferences.getBoolean(KEY_ALIVE, true));
        pet.setCleanliness(sharedPreferences.getFloat(KEY_CLEANLINESS, 100));
        pet.setLastUpdate(sharedPreferences.getLong(KEY_LAST_UPDATE, System.currentTimeMillis()));
        pet.setMilestonesAchieved(sharedPreferences.getInt(KEY_MILESTONES, 0));

        Log.d("PetPreferences", "Pet loaded - Sleeping: " + pet.isSleeping() + ", Energy: " + pet.getEnergy());
        return pet;
    }

    public void resetPet() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        Log.d("PetPreferences", "Pet data reset");
    }

    // NEW: Immediate sleep state save for notification synchronization
    public void saveSleepStateImmediately(boolean isSleeping) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_SLEEPING, isSleeping);
        editor.putLong(KEY_LAST_UPDATE, System.currentTimeMillis());
        editor.apply(); // Immediate async save

        Log.d("PetPreferences", "Sleep state immediately saved: " + isSleeping);
    }
}