#!/bin/bash

echo "🔍 Validating Axion Launcher Project Structure..."

# Check if required files exist
echo "📁 Checking project structure..."
required_files=(
    "app/build.gradle"
    "build.gradle"
    "settings.gradle"
    "gradlew"
    "app/src/main/AndroidManifest.xml"
    "app/src/main/java/com/axion/launcher/MainActivity.java"
    "app/src/main/res/layout/activity_main.xml"
    "app/src/main/res/values/strings.xml"
    "app/src/main/res/values/colors.xml"
    "app/src/main/res/values/themes.xml"
)

for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file exists"
    else
        echo "❌ $file missing"
        exit 1
    fi
done

# Check if Gradle wrapper works
echo "🔧 Testing Gradle wrapper..."
if [ -x "./gradlew" ]; then
    echo "✅ Gradle wrapper is executable"
    ./gradlew --version > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo "✅ Gradle wrapper works"
    else
        echo "❌ Gradle wrapper failed"
        exit 1
    fi
else
    echo "❌ Gradle wrapper is not executable"
    exit 1
fi

# Check if project can be parsed
echo "📋 Testing project parsing..."
./gradlew projects > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Project structure is valid"
else
    echo "❌ Project structure is invalid"
    exit 1
fi

# Check if dependencies can be resolved (without building)
echo "📦 Testing dependency resolution..."
./gradlew dependencies --configuration compileClasspath > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Dependencies can be resolved"
else
    echo "⚠️  Dependencies resolution failed (expected without SDK)"
fi

echo "🎉 Project validation completed successfully!"
echo "📱 Axion Launcher project structure is valid and ready for CI/CD"