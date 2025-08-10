# Axion Launcher

A simple and clean Android launcher for Minecraft PE with Material Design UI.

## Features

- **Clean Material Design UI**: Beautiful lavender-themed interface following Material Design guidelines
- **Navigation Drawer**: Easy navigation between Dashboard, Settings, and Information sections
- **One-Click Launch**: Launch Minecraft PE with a single button press
- **Version Information**: Display current Minecraft PE version and status
- **Settings Panel**: Configure app preferences and options
- **Information Section**: Learn about the app and its features

## Screenshots

The app features a clean interface with:
- Navigation drawer with Dashboard, Settings, and Info sections
- Main dashboard with version card and launch button
- Material Design cards and components
- Lavender color scheme

## Requirements

- Android API Level 21 (Android 5.0) or higher
- Minecraft PE installed on the device

## Installation

1. Clone this repository
2. Open the project in Android Studio
3. Build and run the app on your device

## Usage

1. Open the Axion Launcher app
2. Navigate through the drawer menu to access different sections
3. On the Dashboard, click the "Launch" button to start Minecraft PE
4. Use the Settings section to configure app preferences
5. Check the Information section for app details

## Project Structure

```
app/
├── src/main/
│   ├── java/com/axion/launcher/
│   │   ├── MainActivity.java          # Main activity with navigation drawer
│   │   ├── DashboardFragment.java     # Dashboard fragment with launch button
│   │   ├── SettingsFragment.java      # Settings fragment
│   │   └── InfoFragment.java          # Information fragment
│   ├── res/
│   │   ├── layout/                    # Layout files
│   │   ├── values/                    # Resources (strings, colors, themes)
│   │   ├── drawable/                  # Vector drawables
│   │   └── menu/                      # Navigation menu
│   └── AndroidManifest.xml           # App manifest
└── build.gradle                      # App-level build configuration
```

## Dependencies

- AndroidX AppCompat
- Material Design Components
- Navigation Components
- ConstraintLayout

## License

This project is open source and available under the MIT License.

## Contributing

Feel free to submit issues and enhancement requests!
