# Arranger iOS Sample App

This is the iOS sample application for Arranger, provided as a Swift Playgrounds App (`.swiftpm`).
It demonstrates how to verify the common UI and logic written in Kotlin Multiplatform (KMP) and Compose Multiplatform (CMP) on an iOS environment.

## Setup and Run Instructions

This project uses the `.swiftpm` format, which eliminates the need for complex project management tools like CocoaPods or XcodeGen.
However, you must manually build the dependent KMP `shared.xcframework` via Gradle before opening the app in Xcode.

### 1. Build the KMP (shared) Module

Before opening the iOS app in Xcode, run the following command from the repository root directory.
This command compiles the KMP shared code into `shared.xcframework` and automatically copies the required Compose Multiplatform resources (e.g., images) into the `.swiftpm` directory.

```bash
./gradlew :sample:shared:assembleSharedDebugXCFramework
```

> **Note**: If you modify the Kotlin code or Compose resources (images, fonts, etc.), you must re-run this command to update the Framework.

### 2. Open the Project in Xcode

You can open the project by either:
- Double-clicking the `ArrangerSample.swiftpm` folder in Finder.
- Opening Xcode and selecting **File > Open...**, then choosing `sample/ios/ArrangerSample.swiftpm`.

### 3. Build and Run

Select a target simulator (e.g., iPhone 16) from the Xcode toolbar and press **Run (Cmd + R)**.

---

## Technical Notes

- **120Hz Display Crash Fix**: To prevent crashes related to implicit `Info.plist` checks for 120Hz displays on ProMotion devices, `enforceStrictPlistSanityCheck = false` is explicitly set within `MainViewController.kt`.
- **Resource Loading**: To prevent `MissingResourceException` when loading images, a custom Gradle task automatically copies the `compose-resources` folder after building the XCFramework, and it is explicitly bundled in `Package.swift`.
