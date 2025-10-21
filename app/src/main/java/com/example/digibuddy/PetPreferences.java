package com.example.digibuddy;

import android.content.Context;
import android.content.SharedPreferences;

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
        editor.apply();
    }

    public Pet loadPet() {
        if (!sharedPreferences.contains(KEY_HUNGER)) {
            return new Pet();
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

        return pet;
    }

    public void resetPet() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}