package com.example.digibuddy;

public class Pet {
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

    public Pet() {
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
    }

    public double getHunger() { return hunger; }
    public void setHunger(double hunger) {
        this.hunger = Math.max(0, Math.min(100, hunger));
    }

    public double getHappiness() { return happiness; }
    public void setHappiness(double happiness) {
        this.happiness = Math.max(0, Math.min(100, happiness));
    }

    public double getEnergy() { return energy; }
    public void setEnergy(double energy) {
        this.energy = Math.max(0, Math.min(100, energy));
    }

    public double getAge() { return age; }
    public void setAge(double age) {
        this.age = age;
        // Auto-update stage when age changes
        updateStage();
    }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public boolean isSleeping() { return isSleeping; }
    public void setSleeping(boolean sleeping) { isSleeping = sleeping; }

    public boolean isAlive() { return isAlive; }
    public void setAlive(boolean alive) { isAlive = alive; }

    public double getCleanliness() { return cleanliness; }
    public void setCleanliness(double cleanliness) {
        this.cleanliness = Math.max(0, Math.min(100, cleanliness));
    }

    public long getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(long lastUpdate) { this.lastUpdate = lastUpdate; }

    public int getMilestonesAchieved() { return milestonesAchieved; }
    public void setMilestonesAchieved(int milestonesAchieved) {
        this.milestonesAchieved = milestonesAchieved;
    }

    public void updateStage() {
        if (age >= 7) {
            stage = "adult";
        } else if (age >= 3) {
            stage = "teen";
        } else if (age >= 1) {
            stage = "baby";
        } else {
            stage = "egg";
        }
    }

    public void checkDeath() {
        if (hunger <= 0 || happiness <= 0 || energy <= 0 || cleanliness <= 0) {
            isAlive = false;
        }
    }

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
    }
}