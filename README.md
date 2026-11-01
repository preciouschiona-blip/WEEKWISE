# WeekWise

A weekly planner for Android. It rings your wake-up and routine alarms even when the app is closed, reminds you before classes and calls, asks you each evening what you want to achieve tomorrow, picks contacts from your phone, and shows your phone calendar events in the week.

Free, no account, no ads. Everything stays on your phone. See [PRIVACY.md](PRIVACY.md).

## Install

1. On your Android phone, open the [latest release](../../releases/latest) and download **WeekWise.apk**.
2. Open the downloaded file. Android will ask you to allow installs from your browser or Files app. Allow it for this install.
3. Open WeekWise, allow notifications, and set your daily routine on the first screen.
4. For alarms that never get skipped, open your phone settings, then Apps, WeekWise, Battery, and choose **Unrestricted**.

## For the developer

- Tag a version such as `v1.0` to build and publish a signed APK to Releases. It needs three repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD` and `KEY_ALIAS`.
- Every push to `main` also builds a debug APK as a workflow artifact.
- Screens live in `app/src/main/assets/index.html`. Alarms, notifications, contacts and calendar are in `app/src/main/java/com/weekwise/app/`.
