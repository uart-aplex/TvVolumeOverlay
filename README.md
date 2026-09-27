# TV Volume Overlay

Local Android TV diagnostic and volume overlay app. Package: `com.uart.tvvolume`.
Independent of SmartTube; no SmartTube files were changed.

## What this APK is for

Some Android TVs stop showing their native volume indicator when an HDMI
ARC/CEC receiver or soundbar is connected, even though the TV still receives
the receiver's volume reports. This app supplies a temporary, system-wide volume
indicator over TV apps. It is not a music/video player or an audio amplifier.

Download the APK from this repository's GitHub Releases, install it on the TV,
open **TV 音量提示**, grant display-over-other-apps permission, and select
**全域音量條** (global overlay). Enable **開機自動啟動** for boot startup.
**測試顯示** previews the indicator; **關閉** stops the monitor.
Keep HDMI-CEC enabled. The native experimental mode is not the recommended mode.

If this TV does not expose the overlay permission screen, an authorized ADB
connection can grant the permission:

```text
adb install -r TvVolumeOverlay-0.1.apk
adb shell appops set com.uart.tvvolume SYSTEM_ALERT_WINDOW allow
adb shell pm grant com.uart.tvvolume android.permission.POST_NOTIFICATIONS
```

The notification grant applies to Android 13 or later. No root is required.
Only authorize ADB on trusted computers. Some protected screens can hide overlays.

## Modes

- Off: stop the foreground service and remove the overlay.
- Native (experimental): on a measured volume/mute change, request native UI
  using `ADJUST_SAME`, `FLAG_SHOW_UI` and the AOSP internal absolute-volume flag
  `0x2000`. This is not a firmware patch or a supported portable API contract.
  It does not set a volume index. No native popup was observed on the test TV.
- Global overlay: show the current Android music-stream index/max and mute state
  for three seconds. No key interception, audio processing or volume changes.

Both modes use the same service and are mutually exclusive. Startup is opt-in.
The app uses system volume broadcasts plus a 500 ms read-only polling fallback
while interactive; polling drops to 3 seconds while asleep and no wake lock is
held. Wake-up resets the baseline to avoid stale popups. Repeated key presses
that do not change the reported value will not retrigger the overlay.

## Permissions and limitations

Overlay permission is required for the custom display. Foreground-service and
boot permissions support the monitor. There is no internet, microphone or
accessibility permission. Diagnostics use the `TvVolume` logcat tag and contain
only mode, volume/mute state and errors. Other apps may hide overlays; OEM
background restrictions, force-stop and future Android versions can prevent
automatic restart. Actual cold-boot persistence has not yet been verified.

The displayed number is Android's reported scale, not receiver dB. On this TV,
HDMI ARC music volume matched CEC receiver reports. That equivalence is not
guaranteed on other TVs or output routes.

## Build

Use the existing local JDK 17/SDK and the copied Gradle 7.5 wrapper:

```powershell
. E:\work\tools\bin\android-build-env.ps1
.\gradlew.bat :app:assembleRelease :app:lintRelease --offline
```

AGP 7.4.2, compile/target API 34, min API 26. No external runtime dependencies.
The release variant is non-debuggable, but uses the author's existing local debug
certificate to allow upgrades over the installed test APK. This is a personal
sideload release, not a Play Store production signing setup. The keystore is not
included. Other builders need their own local debug keystore and will not produce
an APK capable of updating the author's signed package without uninstalling it.
APK: `app/build/outputs/apk/release/app-release.apk`.

## TV findings (2026-09-27)

The Xiaomi Android 14 TV received HDMI ARC/CEC receiver volume reports and
SystemUI's controller tracked the matching music volume. Its volume UI was
enabled, but remained hidden. The user confirmed the native display returns
when the receiver is powered off. Pausing Button Mapper TV and Auto Volume
accessibility services did not restore the UI according to the user, although
no new key events were captured in that test window; this is not a definitive
exclusion. Both accessibility services were restored and verified bound.

An ADB `ADJUST_SAME --show` request and this app's experimental absolute-volume
native request left SystemUI `mShowing=false`, `mActiveStream=-1`; the measured
volume remained unchanged. OEM CEC/display handling remains the likely area,
not a proven exact firmware root cause. No CEC/device-volume settings or firmware
were modified. Do not claim native UI restoration.

Custom overlay preview was captured on-device at 42/100 and visually checked,
including above SmartTube after exiting this app. A later window snapshot showed
the overlay removed. Final installed preferences are global overlay (mode 2)
and boot enabled; the monitor was verified running as a foreground service.
Build and lint pass (nonblocking target/backup/localization warnings remain).
Real remote-key updates, mute, receiver power transitions and
cold boot still require additional verification. No full automated test suite.

## Disable and remove

Select Off in the app to stop monitoring; turn off the startup switch if desired.
Uninstall `com.uart.tvvolume` to remove it and its overlay permission. No changes
to the TV firmware or SmartTube need reverting.

Reference: AOSP Android 14 AudioService
https://github.com/aosp-mirror/platform_frameworks_base/blob/android14-release/services/core/java/com/android/server/audio/AudioService.java
