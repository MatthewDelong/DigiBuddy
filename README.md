# DigiBuddy - Web-Based Digital Pet
# A nostalgic web-based digital pet game inspired by the classic 1990s virtual pets. Take care of your DigiBuddy as it grows from an egg to an adult!

https://via.placeholder.com/500x300/6a11cb/ffffff?text=DigiBudty+Virtual+Pet

## Features
- 🥚 Life Stages: Watch your pet evolve through 4 stages: Egg → Baby → Teen → Adult

- 🎮 Interactive Care: Feed, play, clean, and put your pet to sleep

- 💾 Auto-Save: Your progress is automatically saved locally in your browser

- ⏰ Authentic Timing: Slow-paced gameplay true to the original virtual pets

- 📱 Responsive Design: Works on desktop and mobile devices

- 🎨 Pixel Art Style: Retro-inspired visuals with dynamic pet expressions

## How to Play
# Basic Care
- FEED 🍕 - Increases hunger and slightly increases happiness

- PLAY ⚽ - Increases happiness but consumes energy and hunger

- SLEEP 😴 - Toggle sleep mode to restore energy faster

- CLEAN 🛁 - Increases happiness

## Stats to Monitor
- HUNGER - Decreases over time, feed to restore

- HAPPINESS - Decreases over time, play and clean to restore

- ENERGY - Decreases when active, sleep to restore

- AGE - Increases slowly over time, triggers evolution

## Evolution Stages
- Egg (0-1 day)

- Baby (1-3 days)

- Teen (3-7 days)

- Adult (7+ days)

## Installation
# No installation required! Simply:

- Save the HTML file to your computer

- Open it in any modern web browser

- Start caring for your DigiBuddy!

## Browser Compatibility
- Chrome 60+

- Firefox 55+

- Safari 11+

- Edge 79+

## Technical Details
# Storage
- Uses localStorage to save pet state automatically

- Progress persists between browser sessions

- Time-based stat degradation when you're away

## Technologies
- HTML5 - Game structure and canvas for pet rendering

- CSS3 - Retro styling with gradients and animations

- JavaScript - Game logic, state management, and local storage

## Game Mechanics
- Game loop runs every 3 seconds

- Slow stat degradation for authentic experience

- Dynamic pet appearance based on mood and stage

- Random events and messages

## Customization
# Changing Pet Sprites
- To customize the pet appearance, modify the drawPet() function in the JavaScript section. The function uses Canvas API to dynamically draw the pet based on its current state and mood.

## Adjusting Game Speed
# Modify the GAME_SPEED constants to change:

- Stat decay rates

- Aging speed

- Game loop interval

## Development
# File Structure

```
digibuddy/
├── index.html
├── LICENSE
├── README.md
├── script.js
└── style.css
```

## Local Development
- Clone or download the HTML file

- Open in your preferred code editor

- Modify as needed

- Test in browser

## Contributing
- Feel free to fork this project and submit pull requests for:

- New pet designs

- Additional features

- Bug fixes

- Performance improvements

## License
- This project is open source and available under the MIT License.

## Acknowledgments
- Inspired by the original Tamagotchi virtual pets

- Built with vanilla web technologies for maximum compatibility

- Designed for nostalgia and simple fun

## Support
# If you encounter any issues or have questions:

- Check that your browser supports local storage

- Ensure JavaScript is enabled

- Try refreshing the page if the pet seems unresponsive

## Enjoy caring for your DigiBuddy! 🐣 → 🐥 → 🐔