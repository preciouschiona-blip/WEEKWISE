# WeekWise for Android

A weekly planner with real alarms. It rings your 05:00 wake-up even when the app is closed, reminds you about classes and calls, asks you at 20:00 what you want to achieve tomorrow, picks contacts from your phone, and shows your phone calendar events in the week.

This is a complete Android project. It has not been compiled yet, so the first build may show a small error to fix. Send me the error text and I will correct it.

## Get the APK (no Android Studio)

You need a free GitHub account. A computer makes the upload easier.

1. Sign in at github.com and create a new repository (any name, private is fine).
2. Upload everything inside this folder to the repository. Keep the folder structure. The hidden `.github/workflows/build.yml` file must be included. If your upload skips it, choose "Add file", then "Create new file", type `.github/workflows/build.yml` as the name, and paste in the contents of that file.
3. Open the repository's **Actions** tab. Choose **Build WeekWise APK**, then **Run workflow**. It takes about 5 minutes.
4. When it finishes, open the run and download **WeekWise-apk** at the bottom. Unzip it to get `app-debug.apk`.
5. Copy the APK to your phone and open it. Android will ask you to allow installs from this source. Allow it for that one app.

## Get the APK (Android Studio)

1. Install Android Studio and choose **Open**, then select this folder.
2. Wait for the sync, then choose **Build**, **Build Bundle(s) / APK(s)**, **Build APK(s)**.
3. The file is at `app/build/outputs/apk/debug/app-debug.apk`.

## First run on the phone

- Allow **notifications** when asked.
- Open **Settings** in the app and tap **Allow** under Phone calendar to see your calendar events in the week.
- In your phone settings, open Apps, WeekWise, Battery, and choose **Unrestricted**. Many phones stop alarms from ringing without this.
- If alarms do not ring after a restart, open the app once.

## What is inside

- `app/src/main/assets/index.html` the screens (week, today, school, settings, add sheets, 20:00 planning)
- `Schedule.kt`, `AlarmReceiver.kt`, `RingService.kt` alarms that ring, snooze for 5 minutes, and come back after a restart
- `MainActivity.kt`, `Bridge.kt` contact picker, calendar reading, permissions, dialing
