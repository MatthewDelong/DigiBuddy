# DigiBuddy - Virtual Pet Care App 🐾

## Overview

DigiBuddy is an Android virtual pet care application where users can nurture and care for their digital companion. The app features realistic stat management, background service updates, milestone achievements with visual rewards, and smart notifications.

## 🖥 Screenshot 

<img src="https://github.com/user-attachments/assets/34c86d50-c772-492c-b0f6-45e58f9d6658" width="300" alt="DigiBuddy-1.0.4-b">


## Features

### 🎯 Core Gameplay

- **Stat Management**: Monitor and maintain your pet's:
  - **Hunger** (0-100)
  - **Happiness** (0-100) 
  - **Energy** (0-100)
  - **Cleanliness** (0-100)
- **Age System**: Pet ages in real days (1 day = 1 age point)
- **Life Stages**: Egg → Baby → Teen → Adult
- **Sleep Mechanics**: Energy restores while sleeping, slower stat decay

### 🎮 Interactions

- **Feed**: Increase hunger +25, happiness +5, decrease cleanliness -5
- **Play**: Increase happiness +15, decrease energy -8, hunger -3, cleanliness -3
- **Sleep**: Toggle sleep/wake states with energy restoration benefits
- **Clean**: Reset cleanliness to 100, increase happiness +10
- **Reset**: Start with a new pet (resets all stats and milestones)

### 🔔 Smart Notifications

- **Low Stat Alerts**: Notifications when stats drop below 25%
- **Critical Alerts**: Emergency notifications below 15%
- **No Spam System**: 5-minute cooldown between duplicate alerts
- **Background Updates**: Pet stats update even when app is closed
- **Foreground Service**: Persistent care with system notifications
- **Milestone Celebrations**: Special notifications for every 10 days achieved

### ⭐ Milestone & Achievement System

- **Day-based Achievements**: Celebrate every 10 days (10, 20, 30...)
- **Visual Star Badges**: Gold stars displayed next to age counter
- **Star Counter**: Shows "🌟 X milestones" with corresponding stars
- **Permanent Rewards**: Stars remain visible as permanent achievements
- **Special Messages**: Encouraging celebration texts for care achievements

## Technical Architecture

### Core Components

- **MainActivity**: Primary UI, user interactions, and real-time stat updates
- **PetService**: Background stat management, notifications, and milestone tracking
- **PetPreferences**: Data persistence using SharedPreferences
- **Pet Model**: Data structure for pet attributes, behaviors, and milestone tracking

### Key Technical Features

- **Foreground Service**: Ensures pet care continues in background
- **Notification Channels**: Separate channels for service (low priority) and alerts (high priority)
- **Smart Alert Tracking**: Prevents duplicate notifications with state management
- **Permission Handling**: Android 13+ notification permission requests with user education
- **Stat Decay System**: Realistic time-based stat degradation with sleep benefits
- **Death Prevention**: Fresh install protection and proper initialization
- **Milestone Persistence**: Achievement data saved across app sessions

## Setup & Installation

### Prerequisites

- Android Studio Arctic Fox or later
- Minimum SDK: 21 (Android 5.0)
- Target SDK: 34 (Android 14)

### Required Permissions

```xml
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```
## Installation Steps
 Clone or download the project repository

- Open in Android Studio

- Build the project (Build → Make Project)

- Run on Android emulator or physical device

- Grant notification permissions when prompted for optimal experience

# Usage Guide
## Getting Started
- Launch the app to create your first DigiBuddy

- All stats start at 100% (except age at 0 days)

- Use interaction buttons to care for your pet

- Monitor the stat bars and keep them above critical levels

- Watch for milestone stars as your pet ages

## Daily Care Routine
- Check stats regularly to prevent critical levels

- Feed when hungry (hunger below 50%)

- Play when energy permits (energy above 20%)

- Let pet sleep to restore energy efficiently

- Clean regularly to maintain happiness and hygiene

- Balance interactions - each action affects multiple stats

## Long-term Play Strategy
- Aim for milestones (10, 20, 30+ days) to earn gold stars

- Maintain balanced stats for healthy growth and longevity

- Watch for life stage changes as pet ages (egg → baby → teen → adult)

- Celebrate achievements with milestone rewards and notifications

- Build your star collection as a visual record of your care-taking success

# Development Notes
## Stat Decay Rates (Per Second)
### While Awake:

- Hunger: -0.1

- Happiness: -0.05

- Energy: -0.05

- Cleanliness: -0.02

### While Sleeping:

- Hunger: -0.03 (70% slower)

- Happiness: -0.02 (60% slower)

- Energy: +0.3 (restores energy)

- Cleanliness: -0.01 (50% slower)

## Interaction Effects
- Feed: Hunger +25, Happiness +5, Cleanliness -5

- Play: Happiness +15, Energy -8, Hunger -3, Cleanliness -3

- Clean: Cleanliness +100, Happiness +10

- Sleep: Toggles sleeping state with significant energy benefits

## Aging System
- 1 real day = 1 age point (1440 minutes of real time)

## Life stage transitions:

- Egg: 0 days

- Baby: 1+ days

- Teen: 3+ days

- Adult: 7+ days

## Milestone triggers: Every 10 days (10, 20, 30, etc.)

## Notification System
- Warning Threshold: Stats below 10%

- Critical Threshold: Stats below 5%

- Cooldown Period: 5 minutes between duplicate alerts

- Death Condition: Any stat reaches 0%

- Smart Reset: Alert states reset when conditions improve

# Troubleshooting
## Common Issues & Solutions
- Pet dies on fresh install: Protected by fresh pet detection logic

- Duplicate notifications: Fixed by smart alert tracking with cooldown periods

- Notifications not working: Check Android 13+ permission grants in app settings

- Stats not updating in background: Verify foreground service is running

- Age progressing too fast: Aging based on real time (1 day = 1440 minutes)

- Missing colors/build errors: Ensure all color resources are defined in colors.xml

## Testing Recommendations
- Use Android emulator for consistent time-based testing

- Test background updates by minimizing app for extended periods

- Verify notification channels work on different Android versions

- Check stat persistence across app restarts and device reboots

- Test milestone system by simulating time progression

# Future Enhancements
## Planned Features
- Multiple pet types/breeds with unique characteristics

- Customizable pet appearances and accessories

- Social features for pet sharing and community

- Mini-games for additional stat boosts and entertainment

- Expanded achievement system with special rewards

- Cloud save and sync across multiple devices

- Seasonal events and limited-time items

- Pet evolution based on care quality and milestones

# Technical Improvements
- Room Database for more robust data persistence

- WorkManager for improved background task scheduling

- Material You dynamic theming support

- Jetpack Compose migration for modern UI

- Comprehensive testing suite with Espresso and JUnit

# License
- This project is licensed under the MIT License - see the LICENSE file for details.

- Enjoy caring for your DigiBuddy! 🐾

- "Every star tells a story of your care and dedication."
