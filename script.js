// Pet state object
const pet = {
  hunger: 100,
  happiness: 100,
  energy: 100,
  age: 0,
  stage: "egg",
  isSleeping: false,
  isAlive: true,
  cleanliness: 100,
  lastUpdate: Date.now(),
};

// DOM elements
const hungerValue = document.getElementById("hunger-value");
const happinessValue = document.getElementById("happiness-value");
const energyValue = document.getElementById("energy-value");
const ageValue = document.getElementById("age-value");

const hungerBar = document.getElementById("hunger-bar");
const happinessBar = document.getElementById("happiness-bar");
const energyBar = document.getElementById("energy-bar");
const ageBar = document.getElementById("age-bar");

const feedBtn = document.getElementById("feed-btn");
const playBtn = document.getElementById("play-btn");
const sleepBtn = document.getElementById("sleep-btn");
const cleanBtn = document.getElementById("clean-btn");
const resetBtn = document.getElementById("reset-btn");

const messageBox = document.getElementById("message-box");
const petCanvas = document.getElementById("petCanvas");
const ctx = petCanvas.getContext("2d");

// More authentic timing - much slower like the original
const GAME_SPEED = {
  HUNGER_DECAY: 0.3, // Was 1.5 - much slower
  HAPPINESS_DECAY: 0.2, // Was 1 - much slower
  ENERGY_DECAY: 0.2, // Was 1 - much slower
  ENERGY_RESTORE: 1, // Was 3 - slower restoration
  AGE_RATE: 0.02, // Was 0.1 - much slower aging
  GAME_LOOP_INTERVAL: 3000, // Check every 3 seconds instead of 1
};

// Pet evolution stages - adjusted for slower aging
const stages = {
  egg: { minAge: 0, maxAge: 1 },
  baby: { minAge: 1, maxAge: 3 },
  teen: { minAge: 3, maxAge: 7 },
  adult: { minAge: 7, maxAge: 15 },
};

// Save pet state to localStorage
function savePetState() {
  const petState = {
    ...pet,
    lastUpdate: Date.now(),
  };
  localStorage.setItem("digiBuddyPet", JSON.stringify(petState));
}

// Load pet state from localStorage
function loadPetState() {
  const saved = localStorage.getItem("digiBuddyPet");
  if (saved) {
    const savedPet = JSON.parse(saved);

    // Calculate time passed since last save
    const timePassed = Date.now() - savedPet.lastUpdate;
    const minutesPassed = Math.floor(timePassed / (1000 * 60));

    // Update stats based on time passed (much slower degradation)
    if (minutesPassed > 0 && savedPet.isAlive) {
      // Calculate degradation based on time passed - much slower
      const hungerLoss = minutesPassed * 0.5; // Much slower rate
      const happinessLoss = minutesPassed * 0.3;
      const energyLoss = minutesPassed * 0.3;
      const ageGain = minutesPassed * 0.01;

      // Apply changes
      savedPet.hunger = Math.max(0, savedPet.hunger - hungerLoss);
      savedPet.happiness = Math.max(0, savedPet.happiness - happinessLoss);
      savedPet.energy = Math.max(0, savedPet.energy - energyLoss);
      savedPet.age += ageGain;

      // Check if pet died while away
      if (
        savedPet.hunger <= 0 ||
        savedPet.happiness <= 0 ||
        savedPet.energy <= 0
      ) {
        savedPet.isAlive = false;
      }
    }

    // Update the pet object
    Object.assign(pet, savedPet);
    return true;
  }
  return false;
}

// Reset pet to initial state
function resetPet() {
  if (
    confirm(
      "Are you sure you want to reset your DigiBuddy? This cannot be undone!"
    )
  ) {
    pet.hunger = 100;
    pet.happiness = 100;
    pet.energy = 100;
    pet.age = 0;
    pet.stage = "egg";
    pet.isSleeping = false;
    pet.isAlive = true;
    pet.cleanliness = 100;
    pet.lastUpdate = Date.now();

    localStorage.removeItem("digiBuddyPet");
    updateUI();
    showMessage("A new DigiBuddy has arrived! Take good care of it.");
  }
}

// Draw the pet based on its current stage
function drawPet() {
  ctx.clearRect(0, 0, petCanvas.width, petCanvas.height);

  // Set color based on pet status
  let petColor = "#4ECDC4"; // Default healthy color

  if (pet.hunger < 30) petColor = "#FF6B6B"; // Hungry - red
  if (pet.happiness < 30) petColor = "#3498DB"; // Sad - blue
  if (pet.energy < 30) petColor = "#F39C12"; // Tired - orange
  if (!pet.isAlive) petColor = "#95A5A6"; // Dead - gray

  ctx.fillStyle = petColor;

  // Draw different shapes based on pet stage
  switch (pet.stage) {
    case "egg":
      // Draw egg shape
      ctx.beginPath();
      ctx.ellipse(80, 80, 40, 50, 0, 0, Math.PI * 2);
      ctx.fill();

      // Draw crack when hatching is near
      if (pet.age > stages.egg.maxAge - 0.2) {
        ctx.strokeStyle = "#8B4513";
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(60, 60);
        ctx.lineTo(75, 75);
        ctx.lineTo(90, 65);
        ctx.stroke();
      }
      break;

    case "baby":
      // Draw simple round body
      ctx.beginPath();
      ctx.arc(80, 90, 30, 0, Math.PI * 2);
      ctx.fill();

      // Draw eyes
      ctx.fillStyle = "#2C3E50";
      ctx.beginPath();
      ctx.arc(70, 80, 5, 0, Math.PI * 2);
      ctx.arc(90, 80, 5, 0, Math.PI * 2);
      ctx.fill();

      // Draw mouth - changes based on happiness
      ctx.strokeStyle = "#2C3E50";
      ctx.lineWidth = 2;
      ctx.beginPath();
      if (pet.happiness > 70) {
        // Happy mouth
        ctx.arc(80, 95, 10, 0, Math.PI);
      } else if (pet.happiness > 30) {
        // Neutral mouth
        ctx.moveTo(70, 95);
        ctx.lineTo(90, 95);
      } else {
        // Sad mouth
        ctx.arc(80, 100, 10, Math.PI, 0, true);
      }
      ctx.stroke();
      break;

    case "teen":
      // Draw oval body
      ctx.beginPath();
      ctx.ellipse(80, 90, 35, 40, 0, 0, Math.PI * 2);
      ctx.fill();

      // Draw eyes
      ctx.fillStyle = "#2C3E50";
      ctx.beginPath();
      ctx.arc(65, 80, 6, 0, Math.PI * 2);
      ctx.arc(95, 80, 6, 0, Math.PI * 2);
      ctx.fill();

      // Draw mouth
      ctx.strokeStyle = "#2C3E50";
      ctx.lineWidth = 2;
      ctx.beginPath();
      if (pet.happiness > 70) {
        ctx.arc(80, 100, 12, 0, Math.PI);
      } else if (pet.happiness > 30) {
        ctx.moveTo(70, 100);
        ctx.lineTo(90, 100);
      } else {
        ctx.arc(80, 105, 12, Math.PI, 0, true);
      }
      ctx.stroke();
      break;

    case "adult":
      // Draw more complex body
      ctx.beginPath();
      ctx.ellipse(80, 85, 40, 45, 0, 0, Math.PI * 2);
      ctx.fill();

      // Draw details
      ctx.fillStyle = "#2C3E50";

      // Eyes
      ctx.beginPath();
      ctx.arc(65, 75, 7, 0, Math.PI * 2);
      ctx.arc(95, 75, 7, 0, Math.PI * 2);
      ctx.fill();

      // Mouth
      ctx.strokeStyle = "#2C3E50";
      ctx.lineWidth = 3;
      ctx.beginPath();
      if (pet.happiness > 70) {
        ctx.arc(80, 100, 15, 0, Math.PI);
      } else if (pet.happiness > 30) {
        ctx.moveTo(70, 100);
        ctx.lineTo(90, 100);
      } else {
        ctx.arc(80, 105, 15, Math.PI, 0, true);
      }
      ctx.stroke();

      // Add some features to show it's an adult
      ctx.fillStyle = petColor;
      ctx.beginPath();
      ctx.arc(50, 70, 10, 0, Math.PI * 2);
      ctx.arc(110, 70, 10, 0, Math.PI * 2);
      ctx.fill();
      break;

    default:
      // Default circle
      ctx.beginPath();
      ctx.arc(80, 80, 40, 0, Math.PI * 2);
      ctx.fill();
  }

  // If sleeping, draw ZZZ
  if (pet.isSleeping) {
    ctx.fillStyle = "#F1C40F";
    ctx.font = "20px Arial";
    ctx.fillText("Z", 60, 50);
    ctx.fillText("Z", 75, 45);
    ctx.fillText("Z", 90, 50);
  }

  // If dead, draw X eyes
  if (!pet.isAlive) {
    ctx.strokeStyle = "#2C3E50";
    ctx.lineWidth = 3;

    // Left eye X
    ctx.beginPath();
    ctx.moveTo(65, 75);
    ctx.lineTo(75, 85);
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(75, 75);
    ctx.lineTo(65, 85);
    ctx.stroke();

    // Right eye X
    ctx.beginPath();
    ctx.moveTo(85, 75);
    ctx.lineTo(95, 85);
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(95, 75);
    ctx.lineTo(85, 85);
    ctx.stroke();
  }
}

// Update the pet's stage based on age
function updateStage() {
  if (pet.age >= stages.adult.minAge) {
    pet.stage = "adult";
  } else if (pet.age >= stages.teen.minAge) {
    pet.stage = "teen";
  } else if (pet.age >= stages.baby.minAge) {
    pet.stage = "baby";
  } else {
    pet.stage = "egg";
  }
}

// Update the UI with current pet stats
function updateUI() {
  hungerValue.textContent = Math.max(0, Math.round(pet.hunger));
  happinessValue.textContent = Math.max(0, Math.round(pet.happiness));
  energyValue.textContent = Math.max(0, Math.round(pet.energy));
  ageValue.textContent = Math.floor(pet.age);

  hungerBar.style.width = `${Math.max(0, pet.hunger)}%`;
  happinessBar.style.width = `${Math.max(0, pet.happiness)}%`;
  energyBar.style.width = `${Math.max(0, pet.energy)}%`;
  ageBar.style.width = `${Math.min(100, pet.age * 6.67)}%`; // Scale age to fit bar

  // Update status indicators
  document.getElementById("health-indicator").style.opacity =
    pet.hunger > 30 && pet.happiness > 30 && pet.energy > 30 ? "1" : "0.3";
  document.getElementById("hunger-indicator").style.opacity =
    pet.hunger < 50 ? "1" : "0.3";
  document.getElementById("energy-indicator").style.opacity =
    pet.energy < 50 ? "1" : "0.3";
  document.getElementById("happiness-indicator").style.opacity =
    pet.happiness < 50 ? "1" : "0.3";

  drawPet();

  // Save state after every UI update
  savePetState();
}

// Show a message in the message box
function showMessage(message) {
  messageBox.textContent = message;
}

// Game loop - runs every 3 seconds (slower)
function gameLoop() {
  if (!pet.isAlive) return;

  // Decrease stats over time - MUCH SLOWER
  if (!pet.isSleeping) {
    pet.hunger = Math.max(0, pet.hunger - GAME_SPEED.HUNGER_DECAY);
    pet.happiness = Math.max(0, pet.happiness - GAME_SPEED.HAPPINESS_DECAY);
    pet.energy = Math.max(0, pet.energy - GAME_SPEED.ENERGY_DECAY);
  } else {
    // Sleeping restores energy - slower
    pet.energy = Math.min(100, pet.energy + GAME_SPEED.ENERGY_RESTORE);
    pet.hunger = Math.max(0, pet.hunger - GAME_SPEED.HUNGER_DECAY * 0.3);
  }

  // Increase age - MUCH SLOWER
  pet.age += GAME_SPEED.AGE_RATE;

  // Update stage if needed
  const oldStage = pet.stage;
  updateStage();
  if (oldStage !== pet.stage) {
    showMessage(`Your DigiBuddy evolved into a ${pet.stage}!`);
  }

  // Check for death
  if (pet.hunger <= 0 || pet.happiness <= 0 || pet.energy <= 0) {
    pet.isAlive = false;
    showMessage(
      "Your DigiBuddy has passed away... Use the reset button to start over."
    );
  }

  // Random events - less frequent
  if (Math.random() < 0.02) {
    // 2% chance each cycle (was 5%)
    const events = [
      "Your DigiBuddy is looking for attention!",
      "Your DigiBuddy seems bored...",
      "Your DigiBuddy is making cute noises!",
      "Your DigiBuddy is exploring its surroundings.",
    ];
    showMessage(events[Math.floor(Math.random() * events.length)]);
  }

  updateUI();
}

// Button event handlers
feedBtn.addEventListener("click", () => {
  if (!pet.isAlive) return;

  if (pet.isSleeping) {
    showMessage("Your DigiBuddy is sleeping! Wait for it to wake up.");
    return;
  }

  pet.hunger = Math.min(100, pet.hunger + 25);
  pet.happiness = Math.min(100, pet.happiness + 5);
  showMessage("Yum! Your DigiBuddy enjoyed the meal!");
  updateUI();
});

playBtn.addEventListener("click", () => {
  if (!pet.isAlive) return;

  if (pet.isSleeping) {
    showMessage("Your DigiBuddy is sleeping! Wait for it to wake up.");
    return;
  }

  if (pet.energy < 20) {
    showMessage("Your DigiBuddy is too tired to play right now.");
    return;
  }

  pet.happiness = Math.min(100, pet.happiness + 15);
  pet.energy = Math.max(0, pet.energy - 8);
  pet.hunger = Math.max(0, pet.hunger - 3);
  showMessage("Your DigiBuddy had fun playing!");
  updateUI();
});

sleepBtn.addEventListener("click", () => {
  if (!pet.isAlive) return;

  pet.isSleeping = !pet.isSleeping;
  if (pet.isSleeping) {
    showMessage("Your DigiBuddy is now sleeping. Zzz...");
  } else {
    showMessage("Your DigiBuddy woke up!");
  }
  updateUI();
});

cleanBtn.addEventListener("click", () => {
  if (!pet.isAlive) return;

  if (pet.isSleeping) {
    showMessage("Your DigiBuddy is sleeping! Wait for it to wake up.");
    return;
  }

  pet.happiness = Math.min(100, pet.happiness + 10);
  showMessage("Your DigiBuddy feels fresh and clean!");
  updateUI();
});

resetBtn.addEventListener("click", resetPet);

// Initialize the game
function init() {
  // Try to load saved pet
  const hasSavedPet = loadPetState();

  if (hasSavedPet) {
    showMessage("Welcome back! Your DigiBuddy missed you!");
  } else {
    showMessage("Welcome to DigiBuddy! Take good care of your new pet.");
  }

  updateUI();
  setInterval(gameLoop, GAME_SPEED.GAME_LOOP_INTERVAL); // Run game loop every 3 seconds

  // Save state when page is about to be closed
  window.addEventListener("beforeunload", savePetState);
}

// Start the game when the page loads
window.onload = init;
