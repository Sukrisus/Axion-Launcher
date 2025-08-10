# Axion Launcher - Build Artifacts

This folder contains the built APK files for the Axion Launcher app.

## Available APKs

### Debug Version
- **File**: `axion-launcher-debug.apk`
- **Size**: ~5.9 MB
- **Type**: Debug build (unsigned)
- **Use**: For testing and development
- **Installation**: Can be installed directly on Android devices with "Unknown sources" enabled

### Release Version
- **File**: `axion-launcher-release-unsigned.apk`
- **Size**: ~4.8 MB
- **Type**: Release build (unsigned)
- **Use**: For distribution (needs to be signed for Play Store)
- **Installation**: Can be installed directly on Android devices with "Unknown sources" enabled

## Installation Instructions

1. **Enable Unknown Sources**:
   - Go to Settings → Security → Unknown Sources
   - Enable "Allow installation of apps from unknown sources"

2. **Install APK**:
   - Download the APK file to your Android device
   - Open the file manager and tap on the APK file
   - Follow the installation prompts

## Build Information

- **App Name**: Axion Launcher
- **Package**: com.axion.launcher
- **Version**: 1.0.0
- **Target SDK**: Android 13 (API 33)
- **Minimum SDK**: Android 5.0 (API 21)

## Features

- Clean Material Design UI with lavender theme
- Navigation drawer with Dashboard, Settings, and Info sections
- One-click Minecraft PE launcher
- Version information display
- Logs section for debugging

## Notes

- These are unsigned APK files
- For Play Store distribution, the release APK needs to be signed with a keystore
- Debug APK is larger due to debug information and symbols