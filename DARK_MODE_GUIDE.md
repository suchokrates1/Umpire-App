# Dark Mode - User Guide

## Overview

The Tennis Referee app now supports **Dark Mode** with three theme options:
- 🌞 **Light Theme** - Traditional bright interface
- 🌙 **Dark Theme** - Easy on the eyes in low-light environments
- 🔄 **System Default** - Automatically follows your device's theme setting

## How to Change Theme

1. Open the **Tennis Referee** app
2. On the Court Selection screen, tap the **Settings icon** (⚙️) in the top-right corner
3. In the Settings screen, find the **Theme** section
4. Select your preferred theme:
   - **Light** - Always use light theme
   - **Dark** - Always use dark theme
   - **System Default** - Follow device settings (recommended)

The theme will **change instantly** without restarting the app.

## Theme Colors

### Light Theme
- **Background**: Light gray (#FAFAFA)
- **Cards**: White (#FFFFFF)
- **Text**: Black
- **Primary**: Green (#2E7D32)
- **Accent**: Yellow (#FFC107)

### Dark Theme
- **Background**: Dark gray (#121212)
- **Cards**: Medium dark (#2C2C2C)
- **Text**: White
- **Primary**: Light green (#66BB6A)
- **Accent**: Light yellow (#FFD54F)

### Court Colors (Dark Mode Adjusted)
- **Available**: Lighter green (#66BB6A) for better visibility
- **Occupied**: Lighter red (#EF5350) for better contrast

### Button Colors (Dark Mode Optimized)
- **ACE**: Light green (#66BB6A)
- **FAULT**: Light red (#EF5350)
- **BALL IN PLAY**: Light yellow (#FFD54F)
- **WINNER**: Light green
- **FORCED ERROR**: Light orange (#FFB74D)
- **UNFORCED ERROR**: Light red

## System Default (Recommended)

The **System Default** option is recommended for most users because:
- ✅ Automatically switches between light/dark based on time of day
- ✅ Respects your device's battery-saving preferences
- ✅ Provides consistent experience across all your apps
- ✅ Reduces eye strain in different lighting conditions

### How System Default Works
- If your device is set to Dark Mode → App uses Dark Theme
- If your device is set to Light Mode → App uses Light Theme
- Changes automatically when you change device settings

## Benefits of Dark Mode

### 🔋 Battery Saving
- On OLED/AMOLED screens, dark pixels use less power
- Can extend battery life during long tournaments

### 👁️ Eye Comfort
- Reduces eye strain in low-light environments
- Less harsh glare from the screen
- Better for late-evening matches

### 🎾 Professional Look
- Modern, sleek appearance
- Better contrast for scoreboard display
- Easier to read during bright outdoor conditions

## Troubleshooting

### Theme doesn't change immediately
- Theme should change instantly when selected
- If not, try closing and reopening the app

### Theme resets after app restart
- Your theme preference is saved automatically
- If it resets, check app permissions for storage access

### Colors look wrong
- Ensure you're using the latest version of the app
- Check if your device has any color filters enabled
- Some Android skins may override theme colors

### System Default not working
- Check your device's system settings
- Navigate to: Settings > Display > Dark theme
- Ensure it's enabled/disabled as desired

## Technical Details

The app uses Android's **DayNight** theme system, which:
- Follows Material Design guidelines
- Provides smooth theme transitions
- Respects system-wide theme settings
- Saves preferences locally using SharedPreferences

### Theme Persistence
- Your theme choice is saved immediately
- Persists across app restarts
- Stored in app's private storage
- No internet connection required

## Accessibility

Dark Mode improves accessibility for:
- **Light sensitivity** - Reduces bright light exposure
- **Visual impairments** - Higher contrast ratios
- **Night blindness** - Easier to see in dark environments
- **Migraines** - Less trigger from bright screens

## Tips for Best Experience

1. **Outdoor matches** → Use Light Theme for better sunlight visibility
2. **Indoor evening matches** → Use Dark Theme for comfort
3. **All-day tournaments** → Use System Default for automatic switching
4. **OLED devices** → Dark Theme saves battery significantly

## Feedback

If you experience any issues with Dark Mode or have suggestions for improvement, please contact support through the app or visit our GitHub repository.

---

**Version**: 1.0.0  
**Last Updated**: November 25, 2025  
**Feature**: Dark Mode Support
