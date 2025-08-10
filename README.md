# Axion Launcher

[![Android Build](https://github.com/yourusername/axion-launcher/workflows/Android%20Build/badge.svg)](https://github.com/yourusername/axion-launcher/actions)
[![Test Build](https://github.com/yourusername/axion-launcher/workflows/Test%20Build/badge.svg)](https://github.com/yourusername/axion-launcher/actions)
[![Code Quality](https://github.com/yourusername/axion-launcher/workflows/Code%20Quality/badge.svg)](https://github.com/yourusername/axion-launcher/actions)

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

### Option 1: Build from Source
1. Clone this repository
2. Open the project in Android Studio
3. Build and run the app on your device

### Option 2: Download from GitHub Actions
1. Go to the [Actions](https://github.com/yourusername/axion-launcher/actions) tab
2. Download the latest APK from the "Test Build" workflow
3. Install the APK on your Android device

### Option 3: Release Downloads
1. Go to the [Releases](https://github.com/yourusername/axion-launcher/releases) page
2. Download the latest release APK
3. Install the APK on your Android device

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

## CI/CD

This project uses GitHub Actions for continuous integration and deployment:

### Workflows

1. **Android Build** (`android-build.yml`)
   - Triggers on push to main/master and pull requests
   - Builds the project and runs tests
   - Creates release APK artifacts
   - Uploads build reports and test results

2. **Release** (`release.yml`)
   - Triggers when a new tag is pushed (e.g., `v1.0.0`)
   - Creates a GitHub release with the APK attached
   - Automatically generates release notes

3. **Code Quality** (`code-quality.yml`)
   - Runs Android Lint for code quality checks
   - Performs security analysis with OWASP Dependency Check
   - Uploads lint and security reports

4. **Test Build** (`test-build.yml`)
   - Manual trigger or on push to main/master
   - Validates the build process
   - Creates debug APK for testing

### Building Locally

```bash
# Clone the repository
git clone https://github.com/yourusername/axion-launcher.git
cd axion-launcher

# Build the project
./gradlew build

# Run tests
./gradlew test

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

## License

This project is open source and available under the MIT License.

## Contributing

Feel free to submit issues and enhancement requests!
