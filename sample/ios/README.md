# Arranger iOS Sample App

This is the iOS sample application for Arranger, built with a standard Xcode project (`.xcodeproj`).
It demonstrates how to verify the common UI and logic written in Kotlin Multiplatform (KMP) and Compose Multiplatform (CMP) on an iOS environment.

## Prerequisites

- Xcode 16+
- JDK (required to run Gradle)

## Setup and Run Instructions

### 1. Open the project in Xcode

Open `ArrangerSample.xcodeproj` located in this directory.

```
sample/ios/ArrangerSample.xcodeproj
```

### 2. Build and Run

Select a target simulator (e.g., iPhone 16) from the Xcode toolbar and press **Run (Cmd + R)**.

The KMP shared module (`sample/shared`) is automatically built via Gradle as part of the Xcode build process using the `embedAndSignAppleFrameworkForXcode` task. No manual Gradle invocation is required.

> **Note**: If you modify the Kotlin source code, Xcode will automatically re-run the Gradle build on the next **Run**.

## Project Structure

```
sample/ios/
├── ArrangerSample.xcodeproj/   # Xcode project file
├── ArrangerSample/             # Swift source files
│   ├── ArrangerSampleApp.swift
│   └── ContentView.swift       # Embeds KMP MainViewController
└── README.md
```
