# Extension Browser

Android 8.0 (API 26) compatible starter browser project.

## Current features
- Lightweight WebView browser
- JavaScript + DOM storage
- URL bar and navigation
- Extension file picker for `.crx`/`.zip`
- Shortcut to Android installed-app settings
- GitHub Actions APK build

## Important limitation
Android WebView does NOT execute Chrome extensions. The extension picker is included as the project UI, but actual `.crx`/Manifest V2/V3 execution requires integrating an extension-capable Chromium engine/browser core. This project deliberately does not pretend WebView can run Chrome extensions.

## Build with GitHub Actions
1. Create a new GitHub repository.
2. Upload all files from this project.
3. Push to `main`, or open Actions → Build APK → Run workflow.
4. Download the `ExtensionBrowser-debug` artifact.

Minimum Android version: API 26 (Android 8.0).
