# TV Volume Overlay 0.1

Initial personal Android TV sideload release.

## Purpose

Display a temporary volume number and bar when an HDMI ARC/CEC receiver is
connected and the TV's native volume popup is missing. Independent of SmartTube;
works as an overlay rather than modifying the TV firmware or controlling the AVR.

## Included

- Global volume/mute overlay, dismissed after three seconds.
- Read-only monitoring of the Android music-stream volume.
- Foreground monitoring and optional boot startup.
- Off switch, overlay permission shortcut, and display preview.
- Optional experimental native-UI request mode.
- No internet, microphone, accessibility service or root requirement.

## Installation

Install `TvVolumeOverlay-0.1.apk`, open **TV 音量提示**, grant overlay permission,
and select **全域音量條**. Enable **開機自動啟動** to request boot startup.
See the README for ADB permission commands if the TV lacks the permission screen.

## Validation and limits

Tested on a Xiaomi Android 14 TV with an HDMI ARC receiver. A preview showed
42/100, including over SmartTube, and dismissed afterward. Foreground service
and boot preference were verified; an actual reboot has not been tested.
Remote-key updates, mute and receiver power transitions need further validation.

Native-UI compatibility requests did not restore the TV's original volume popup;
that mode remains experimental. This release does not claim a firmware fix.
Volume is the Android-reported scale, not necessarily the AVR's displayed dB.
OEM background limits or protected windows may limit operation.

The release APK is non-debuggable and signed with the same personal certificate
as the installed test build. SHA-256 checksums are included with the release.
