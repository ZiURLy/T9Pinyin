# ZiUR Phone — test build

This is a standalone Android test application for the ZiUR Phone dialer work.

## Included in this build
- Compose dial-pad UI.
- Local contact reading after explicit runtime permission.
- T9 digit search over contact names using Android ICU Han-Latin transliteration.
- Outgoing calls through Android Telecom after the user grants CALL_PHONE.
- Call-screening service registration and a button to request the Android call-screening role on Android 10+.

## Important limitations
- This is a test milestone, not a finished replacement dialer.
- The original T9PY binary dictionary is retained in the parent repository but is not yet parsed by this standalone app; ICU readings can differ for polyphonic names.
- The original call-block APK's geographic prefix/index table is not fully decoded. Geographic blocking therefore remains fail-open and is not enabled.
- No full incoming/active call UI is implemented. This build does not request the default dialer role.
- The APK is a GitHub Actions debug build signed with the standard Android debug key. It is for installation/testing, not Play Store release.

## Build
The GitHub Actions workflow installs JDK 17, Gradle 8.9 and Android SDK 35, then runs the app assembleDebug task.
