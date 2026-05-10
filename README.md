# PrivacyGuard-Android

**Your screen. Yours alone. (Android Edition)**

A proof-of-concept Android application that uses the device's front camera to detect multiple faces in real time and activates an in-app privacy shield overlay. Built with CameraX for camera access and Google ML Kit for on-device face detection. Optimized for Samsung Galaxy devices and compatible with other modern Android phones.

## The Problem

Shoulder-surfing poses a significant privacy risk in public environments such as transit, cafés, and shared workspaces.

- **73%** of professionals report experiencing shoulder-surfing.
- **1 in 4** data breaches involve visual or in-person credential exposure.
- Estimated **$4.9B** in annual losses from visual hacking incidents.

## The Solution

PrivacyGuard-Android provides an in-app demonstration of real-time, on-device second-face detection. When the app's camera detects more than one face, it immediately overlays a privacy shield on the screen. All processing occurs locally with no data transmitted or stored.

### How It Works

1. **Sense** — Captures live frames from the front camera using CameraX.
2. **Detect** — Applies ML Kit's on-device Face Detection to count faces in real time.
3. **Shield** — Displays a full-screen privacy overlay if multiple faces are detected (response time under 150ms).
4. **Restore** — Removes the overlay instantly once only a single face remains in view.

## Current Scope

This is a standalone in-app demo. The shield protects the screen within this application only. 

System-wide shielding (e.g., overlaying other apps or using accessibility services) is planned for Phase 2 and would require additional permissions and development.

## Why Android and Samsung

| Aspect                  | Benefit |
|-------------------------|---------|
| Camera Access           | CameraX delivers consistent, reliable front-camera performance across Android versions and devices. |
| On-Device ML            | ML Kit runs efficiently on Snapdragon and Exynos processors with no cloud dependency. |
| Security Foundation     | Samsung Knox provides hardware-backed security options for future enhancements. |
| Extensibility           | Architecture supports expansion to system-level privacy features. |

## Implementation Roadmap

- **Phase 1 (Complete)**: In-app demo featuring CameraX integration, ML Kit face detection, and dynamic shield overlay.
- **Phase 2**: Samsung Knox integration and prototype system-wide shielding capabilities.
- **Future**: Potential for OEM-level, native system integration.

## Quick Start

1. Clone the repository:
   ```bash
   git clone https://github.com/ar-fullsend/PrivacyGuard-Android.git
   cd PrivacyGuard-Android
   ```

2. Open the project in Android Studio (latest stable version recommended).

3. Connect a physical Android device (API 29 / Android 10 or higher). Emulators have limited front-camera support for testing.

4. Sync the project and run the app.

5. Grant the **Camera** permission when prompted.

6. Tap **Start Monitoring**. Position yourself and a second person (or object simulating a face) in front of the camera to trigger the shield.

**Test Tip**: The shield activates only when the app's camera view detects two or more faces.

## Project Structure

- `app/` — Core Android application module
  - Camera preview and real-time ML Kit analysis
  - Dynamic privacy shield overlay UI
  - Permission handling and status indicators
- `README.md` — Project documentation and usage guide

## Status

**POC Ready** — Core features fully implemented and validated conceptually on Samsung Galaxy S23/S24 series. Ready for device testing and further development.

## Required Permissions

- `android.permission.CAMERA` — Essential for front-camera access and face detection.

No additional permissions (such as SYSTEM_ALERT_WINDOW) are needed for the current in-app implementation.

## Contributing

We welcome issues, pull requests, and feedback to improve privacy tools on Android. 

## License

MIT License — see the [LICENSE](LICENSE) file for details.

---

*Built with a focus on on-device privacy and practical usability.*