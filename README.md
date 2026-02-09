# HA Wallpanel

A full-screen Android kiosk browser application designed for home automation dashboards. It features camera-based motion detection, proximity sensor integration, and advanced display management to ensure your dashboard is always ready when you approach it.

## Key Features

### Full-Screen Browser
Immersive WebView browsing with hardware acceleration, JavaScript support, and local storage. Includes a progress indicator and zoom gesture support.

### Motion Detection
Automatically turns on the display when motion is detected via the front-facing camera. The screen will turn off after a configurable period of inactivity, overriding system-level sleep settings. Detection intensity is fully adjustable.

### Proximity Sensor Hardware
Integrates with physical proximity sensors if available on the device. Supports multiple modes:
- Wave to Wake: Trigger display on via gesture.
- Wave to Toggle: Manually switch display state.
- Dynamic Occupancy: Near/far logic for automatic control.

### Network Camera Stream
Broadcasts a high-quality MJPEG stream from the device's front camera to the local network. This allows you to integrate the kiosk as a security camera in Home Assistant or other NVR software simultaneously with motion detection.

### Kiosk Mode and Security
- Prevents accidental exit via system navigation.
- Can be configured as the default Home application (Launcher).
- Optional Device Administration for advanced screen lock control.
- PIN-protected settings menu and termination logic.
- Automatic application start upon device boot.

## How to Configure

### Accessing System Settings
Swipe from the left edge of the screen to the right to open the configuration menu.

### Core Settings

#### Application URL
Define the destination address (e.g., Home Assistant dashboard) for the browser.

#### Display and Responsiveness
- Screen Orientation: Lock to portrait or landscape, or use dynamic sensor-based rotation.
- Turn screen off after: Set the duration of inactivity before the display is terminated.
- Sensor Response Delay: Adjust the latency between detection events and system actions.

#### Motion Detection
- Camera Wake: Toggle camera-based analysis.
- Detection Intensity: Adjust how significant movement must be to trigger a wake event.
- Luminance Threshold: Configure the light level requirements for detection.

#### Network Camera Stream
- **Enable MJPEG Stream**: Toggle the stream on or off.
- **Stream URL**: Displays the local network address (e.g. `http://192.168.1.50:2971/`) to use in Home Assistant's Generic Camera integration.

#### Proximity Hardware
- Sensor Availability: View the real-time status of your device's proximity hardware.
- Operating Mode: Select how physical sensor triggers are handled.

#### Security Verification
- Termination PIN: Set the code required to close the application.
- Administration: Grand device management authority to allow the app to lock the screen.

## Installation and Setup

1. Enable installation from unknown sources on your Android device.
2. Install the provided APK file.
3. Grant camera authorization when prompted for motion detection features.
4. Optional: Set HA Wallpanel as the default Home app under System Settings.

## Technical Requirements

- Android SDK 34 or newer.
- Front-facing camera for motion detection features.
- Device Administration privileges for advanced power management.

## License

This project is released under the MIT License.
