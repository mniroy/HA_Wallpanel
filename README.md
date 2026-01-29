# HA Wallpanel Browser

A full-screen Android kiosk browser app with camera-based motion detection, proximity sensor control, and screen management - similar to Fully Kiosk Browser.

## Features

### 🌐 Full-Screen Browser
- Immersive full-screen WebView browsing
- Hardware-accelerated rendering
- JavaScript and local storage support
- Progress indicator for page loading
- Support for zoom gestures

### 📷 Motion Detection (Like Fully Kiosk)
- **Screen Wake**: Automatically turns on the screen when motion is detected in front of the camera
- **Auto Screen Off**: Turns off the screen after a configurable period of no motion (5 seconds to 5 minutes)
- **Grid-based Detection**: Uses an 8x8 grid analysis for accurate motion detection
- **Adjustable Sensitivity**: Configure motion sensitivity from 1-50
- Works even when the screen is dimmed

### 👋 Proximity Sensor Control
Three modes available:
1. **Wave to Wake**: Wave your hand near the sensor to turn the screen on
2. **Wave to Toggle**: Wave to toggle the screen on/off
3. **Near = Off, Far = On**: Screen turns off when something is near (like phone call behavior)

- Automatic sensor detection (shows if available)
- Low power consumption
- Works independently of camera

### 🖐️ Camera Cover Detection
- Cover the front camera with your hand to instantly turn off the screen
- Uncover the camera to turn the screen back on
- Uses brightness analysis to detect camera coverage
- Adjustable brightness threshold

### 🔒 Kiosk Mode
- Prevents back button from exiting the app
- Can be set as default home app (launcher replacement)
- Optional device admin for screen lock control
- PIN-protected settings access
- Auto-starts on device boot

## How to Use

### Accessing Settings
Tap **5 times** on the top-left corner of the screen to open settings.

### Settings Options

#### Screen Control
| Setting | Description |
|---------|-------------|
| Motion Detection | Enable/disable motion-based screen wake |
| Proximity Sensor | Enable/disable proximity-based control |
| Proximity Mode | Wave to Wake / Wave to Toggle / Near = Off |
| Camera Cover Detection | Enable/disable camera coverage-based screen off |
| Motion Sensitivity | How sensitive the motion detection is (1-50) |
| Screen Off After | Time with no motion before screen off (5s - 5min) |
| Brightness Threshold | Sensitivity for camera coverage detection |
| Detection Delay | Delay between screen toggle events |

#### Security
| Setting | Description |
|---------|-------------|
| Exit PIN | PIN code to exit kiosk mode |
| Device Admin | Enable for full screen lock control |

### Screen Control Behavior

1. **Motion Detection ON**:
   - Screen wakes up when someone walks in front of the camera
   - Screen turns off automatically after the configured delay if no motion is detected

2. **Proximity Sensor ON**:
   - Wave to Wake: Wave hand near sensor → Screen turns on
   - Wave to Toggle: Wave hand → Screen toggles on/off
   - Near = Off: Object near sensor → Screen off, object removed → Screen on

3. **Camera Cover Detection ON**:
   - Cover the front camera → Screen turns off immediately
   - Uncover the camera → Screen turns on

4. **All Three Enabled**: 
   - Motion or proximity wakes the screen
   - Camera cover or proximity (depending on mode) turns it off
   - No motion for X seconds turns it off

## Building the App

### Requirements
- Android Studio Arctic Fox or newer
- JDK 17 or newer
- Android SDK 34

### Build Steps
1. Open the project in Android Studio
2. Sync Gradle files
3. Build > Build Bundle(s) / APK(s) > Build APK(s)
4. The APK will be in `app/build/outputs/apk/debug/`

### Or via command line:
```bash
./gradlew assembleDebug
```

## Installation

1. Enable "Install from Unknown Sources" on your Android device
2. Transfer the APK to your device
3. Install the APK
4. Grant camera permission when prompted
5. (Optional) Set as default home app for full kiosk experience

## Setting as Default Launcher

For a true kiosk experience:
1. Go to Settings > Apps > Default Apps > Home App
2. Select "HA Kiosk"
3. The device will now boot directly into the kiosk browser

## Permissions

- **INTERNET**: Required for web browsing
- **CAMERA**: Required for motion detection and camera cover detection
- **WAKE_LOCK**: Required to turn screen on
- **RECEIVE_BOOT_COMPLETED**: Required for auto-start on boot

Note: Proximity sensor does NOT require any additional permissions.

## Comparison with Fully Kiosk

| Feature | HA Kiosk | Fully Kiosk |
|---------|----------|-------------|
| Full-screen browser | ✅ | ✅ |
| Motion detection wake | ✅ | ✅ |
| Proximity sensor control | ✅ | ✅ |
| Auto screen off | ✅ | ✅ |
| Camera cover detection | ✅ | ✅ |
| Adjustable sensitivity | ✅ | ✅ |
| PIN protection | ✅ | ✅ |
| Boot on startup | ✅ | ✅ |
| Free & Open Source | ✅ | ❌ |

## Troubleshooting

### Proximity Sensor Not Working
- Check if your device has a proximity sensor (Settings will show "Not Available" if missing)
- Some devices have the sensor near the earpiece/camera
- Try different proximity modes

### Motion Detection Too Sensitive
- Lower the motion sensitivity in settings
- Increase the detection delay

### Screen Doesn't Wake
- Ensure camera permission is granted
- Check that motion detection or proximity sensor is enabled
- Try waving closer to the camera/sensor

## License

MIT License - Free for personal and commercial use.
