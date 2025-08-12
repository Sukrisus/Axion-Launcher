# Axion Launcher

[![Build Origin App](https://github.com/yourusername/axion-launcher/workflows/Build%20Origin%20App/badge.svg)](https://github.com/yourusername/axion-launcher/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A modern and feature-rich Android launcher for Minecraft PE with Material Design UI, version management, and customizable themes.

## ✨ Features

### 🎨 **Modern UI & Themes**
- **Material Design 3**: Clean, modern interface following Google's latest design guidelines
- **Multiple Themes**: Choose from various color themes and appearance options
- **Customizable Appearance**: Personalize your launcher experience
- **Navigation Drawer**: Intuitive navigation between different sections

### 🚀 **Version Management**
- **Multiple MCPE Versions**: Manage and switch between different Minecraft PE versions
- **Version Information**: Display detailed version information and status
- **Easy Version Switching**: Quick access to different game versions
- **Version Validation**: Automatic verification of installed versions

### 🎮 **Launch Features**
- **One-Click Launch**: Launch Minecraft PE with a single button press
- **Quick Access**: Fast access to your favorite game versions
- **Launch History**: Keep track of recently launched versions

### ⚙️ **Settings & Configuration**
- **Comprehensive Settings**: Configure app preferences and options
- **Theme Management**: Switch between different visual themes
- **Appearance Customization**: Adjust colors, layouts, and visual elements
- **User Preferences**: Personalize your launcher experience

## 📱 Requirements

- **Android Version**: API Level 21 (Android 5.0 Lollipop) or higher
- **Target SDK**: Android 13 (API Level 33)
- **Java Version**: Java 17
- **Minecraft PE**: Must be installed on the device
- **Permissions**: 
  - Storage access for version management
  - Package installation permissions

## 🚀 Installation

### Option 1: Download from GitHub Actions
1. Go to the [Actions](https://github.com/yourusername/axion-launcher/actions) tab
2. Select the latest "Build Origin App" workflow run
3. Download the `app-debug-apk` or `app-release-apk` artifact
4. Extract and install the APK on your Android device

### Option 2: Build from Source
```bash
# Clone the repository
git clone https://github.com/yourusername/axion-launcher.git
cd axion-launcher

# Grant execute permission
chmod +x ./gradlew

# Build debug APK
./gradlew assembleDebug

# Build release APK  
./gradlew assembleRelease
```

### Option 3: Android Studio
1. Clone this repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Build and run the app on your device or emulator

## 📖 Usage

1. **Open Axion Launcher** - Launch the app from your device
2. **Navigate Sections** - Use the drawer menu to access:
   - 🏠 **Dashboard** - Main launch interface
   - 📱 **Version Manager** - Manage MCPE versions
   - 🎨 **Appearance** - Customize themes and UI
   - ⚙️ **Settings** - Configure app preferences
   - ℹ️ **Info** - App information and details
3. **Launch Games** - Select a version and tap launch
4. **Customize** - Personalize themes and appearance
5. **Manage Versions** - Add, remove, or switch between MCPE versions

## 🏗️ Project Structure

```
app/
├── src/main/
│   ├── java/com/axion/launcher/
│   │   ├── MainActivity.java              # Main activity with navigation
│   │   ├── DashboardFragment.java         # Main dashboard interface
│   │   ├── VersionManagerFragment.java    # Version management system
│   │   ├── AppearanceFragment.java        # Theme and appearance settings
│   │   ├── SettingsFragment.java          # App configuration
│   │   ├── InfoFragment.java              # Information section
│   │   ├── MCPEVersion.java               # Version data model
│   │   ├── VersionAdapter.java            # Version list adapter
│   │   └── ThemeManager.java              # Theme management system
│   ├── res/
│   │   ├── layout/                        # UI layout files
│   │   ├── values/                        # Resources (strings, colors, themes)
│   │   ├── drawable/                      # Vector drawables and icons
│   │   └── menu/                          # Navigation menu definitions
│   └── AndroidManifest.xml               # App manifest and permissions
├── build.gradle                          # App-level build configuration
└── proguard-rules.pro                    # ProGuard configuration
```

## 🔧 Dependencies

### Core Android Libraries
- **AndroidX AppCompat** `1.6.1` - Backward compatibility
- **Material Design Components** `1.9.0` - Modern UI components
- **ConstraintLayout** `2.1.4` - Flexible layouts

### Navigation & Architecture
- **Navigation Fragment** `2.5.3` - Fragment navigation
- **Navigation UI** `2.5.3` - Navigation UI components
- **Lifecycle LiveData** `2.6.1` - Lifecycle-aware data
- **Lifecycle ViewModel** `2.6.1` - UI state management

### Testing
- **JUnit** `4.13.2` - Unit testing framework
- **AndroidX Test** `1.1.5` - Android testing
- **Espresso** `3.5.1` - UI testing

## 🔄 CI/CD Pipeline

This project uses **GitHub Actions** for continuous integration and deployment:

### Build Workflow (`main.yml`)
- **Triggers**: Push to main/master, Pull Requests
- **Environment**: Ubuntu Latest with JDK 17
- **Android NDK**: Version 27.1.12297006
- **Caching**: Gradle dependencies and wrapper
- **Artifacts**: Both debug and release APKs
- **Build Commands**:
  ```bash
  ./gradlew assembleDebug --stacktrace
  ./gradlew assembleRelease --stacktrace
  ```

### Development Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run lint checks
./gradlew lint

# Clean build
./gradlew clean

# Build specific variants
./gradlew assembleDebug
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

## 🎨 Theming & Customization

The app supports multiple themes and appearance customization:

- **Material Design 3** color system
- **Dynamic theming** based on user preferences
- **Custom color schemes** for different visual styles
- **Theme persistence** across app sessions
- **Appearance settings** for fine-tuning the UI

## 🔒 Permissions

The app requires the following permissions:

- `READ_EXTERNAL_STORAGE` - Access game files and versions
- `WRITE_EXTERNAL_STORAGE` - Manage version installations
- `REQUEST_INSTALL_PACKAGES` - Install APK packages

## 📄 License

This project is licensed under the **Apache License 2.0** - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

We welcome contributions! Here's how you can help:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add some amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Development Guidelines
- Follow **Material Design** guidelines
- Write **clean, documented code**
- Add **tests** for new features
- Ensure **compatibility** with Android 5.0+
- Test on **multiple devices** and screen sizes

## 🐛 Issues & Support

- **Bug Reports**: [Create an issue](https://github.com/yourusername/axion-launcher/issues/new?template=bug_report.md)
- **Feature Requests**: [Request a feature](https://github.com/yourusername/axion-launcher/issues/new?template=feature_request.md)
- **Questions**: [Start a discussion](https://github.com/yourusername/axion-launcher/discussions)

## 🏷️ Version History

- **v1.0** - Initial release with basic launcher functionality
- **Current** - Enhanced version management and theming system

---

<div align="center">
  <p>Made with ❤️ for the Minecraft PE community</p>
  <p>
    <a href="#axion-launcher">Back to Top</a> •
    <a href="https://github.com/yourusername/axion-launcher/issues">Report Bug</a> •
    <a href="https://github.com/yourusername/axion-launcher/discussions">Request Feature</a>
  </p>
</div>
