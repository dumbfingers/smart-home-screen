# Smart Home Launcher for Android Tablet

A modern, open-source Android tablet launcher and smart home dashboard designed for wall-mounted or desktop tablet displays. Built with **Kotlin**, **Jetpack Compose**, and **Material 3**.

---

## Features

### 1. Landscape Tablet-Optimized Layout
- Fixed sensor-landscape orientation designed specifically for 10"-14" tablets (such as Samsung Galaxy Tab, Pixel Tablet, Lenovo Tab).
- Prominent digital clock, live date, and glanceable weather header.
- Split screen architecture:
  - **Left column**: Schedule agenda, upcoming events, and two-way calendar sync.
  - **Right column**: Home Assistant smart home entity toggles & Frigate CCTV camera feeds.

### 2. Full Two-Way Calendar Sync (Google & Samsung Calendar)
- Reads, creates, edits, and deletes events directly via Android's native `CalendarContract` ContentProvider.
- Any event modified on the tablet automatically syncs back to Google Calendar, Samsung Calendar, Microsoft Exchange, or Nextcloud through the tablet's account sync adapters.
- Interactive agenda viewer with quick add (+), edit, and delete dialogs.
- Graceful demo mode with sample events prior to granting calendar permissions.

### 3. Home Assistant Integration (Hybrid)
- **Native Quick Cards**: Instant state polling and one-tap toggles for lights, switches, smart plugs, climate controls, and temperature sensors.
- **Full Lovelace Dashboard**: One-tap modal to access your complete Home Assistant web interface directly inside the launcher.
- Simple configuration with your Home Assistant LAN/WAN URL and Long-Lived Access Token.

### 4. Frigate CCTV Integration
- Multi-camera live snapshot grid with automatic 3-second refreshing.
- Tapping any camera opens a full-screen live view.
- Supports auto-discovery via Frigate API or custom comma-separated camera lists.

### 5. Bing Daily Image Screensaver
- Automatically fetches the daily high-resolution Bing wallpaper.
- **Dual Screensaver Support**:
  - **In-App Inactivity Screensaver**: Fades in smoothly after a configurable timeout (defaults to 2 minutes) with digital clock and Bing photo details. Tap anywhere to wake.
  - **Android DreamService**: Registered as `BingDailyDreamService` so the tablet can also use it natively in Android System Settings when docked or charging.

### 6. Clean Home Screen with Hidden App Drawer
- Keeps the home screen focused on smart home information.
- All other installed Android apps can be launched via:
  - Swiping down the system status bar / notification shade.
  - Tapping the discreet app drawer icon in the top header to search and launch installed applications.

### 7. In-App Settings Dialog
- Easily configure:
  - Home Assistant URL & Long-Lived Access Token.
  - Frigate CCTV Server URL & Camera names.
  - Screensaver enable/disable toggle & idle timeout duration in minutes.

---

## Getting Started & Configuration

### Prerequisites
- Android 8.0 (API level 26) or newer.
- Target Android SDK 34 (Android 14+).

### Building the Project
From the repository root:

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew testDebugUnitTest

# Run code analysis
./gradlew check
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Setting as Default Launcher
1. Install the APK on your Android tablet:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
2. Press the tablet's **Home** button.
3. In the "Select a Home app" prompt, choose **Smart Home Screen** and select **Always**.

### Connecting Home Assistant
1. Tap the **Settings** icon (gear in top right).
2. Enter your Home Assistant URL (e.g., `http://192.168.1.100:8123`).
3. In Home Assistant, open your user profile, scroll down to **Long-Lived Access Tokens**, create a new token, and paste it into the launcher.
4. Tap **Save Configuration**.

### Connecting Frigate
1. In the **Settings** dialog, enter your Frigate URL (e.g., `http://192.168.1.100:5000`).
2. List your camera names separated by commas (e.g., `front_door, driveway, backyard`).
3. Tap **Save Configuration**.

---

## Project Structure

```
app/src/main/java/com/smarthome/launcher/
├── SmartHomeApp.kt                  # Application class
├── MainActivity.kt                  # Home launcher activity & layout coordination
├── calendar/
│   ├── CalendarModels.kt            # Calendar and event data structures
│   ├── CalendarRepository.kt        # CalendarContract queries, inserts, updates, deletes
│   ├── CalendarViewModel.kt         # Event state management and permission handling
│   └── CalendarViews.kt             # Agenda widget, event cards, editor dialog
├── homeassistant/
│   ├── HaModels.kt                  # Home Assistant entity models
│   ├── HomeAssistantClient.kt       # REST API client for states and service calls
│   ├── HomeAssistantViewModel.kt    # Polling and optimistic toggle logic
│   └── HomeAssistantViews.kt        # Quick control cards and Lovelace WebView modal
├── frigate/
│   ├── FrigateClient.kt             # Frigate snapshot & stream URL resolver
│   ├── FrigateViewModel.kt          # Camera refresh loop and selection
│   └── FrigateViews.kt              # Camera preview grid and full-screen viewer
├── screensaver/
│   ├── BingImage.kt                 # Bing daily wallpaper data model
│   ├── BingImageClient.kt           # Bing HPImageArchive API fetcher
│   ├── ScreensaverViewModel.kt      # In-app inactivity idle timer
│   ├── ScreensaverViews.kt          # Fade-in overlay with clock and tap-to-wake
│   └── BingDailyDreamService.kt     # System DreamService for charging/docking
├── settings/
│   ├── AppSettings.kt               # SharedPreferences store for credentials/options
│   └── SettingsDialog.kt            # Material 3 configuration dialog
└── ui/
    ├── components/
    │   ├── TabletClockWeatherHeader.kt
    │   └── AppDrawerView.kt         # Installed application launcher modal
    └── theme/
        └── Theme.kt                 # Material 3 Dark theme palette
```

---

## License

This project is open-source and licensed under the [MIT License](LICENSE).
