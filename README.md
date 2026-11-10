# SMS Expense Tracker

Offline Android expense tracker that reads SMS messages, detects banking/transaction SMS, extracts fields, and lets the user categorize each transaction. All data stays on the device — the app has no internet permission.

## Tech stack
- Kotlin, Jetpack Compose + Material 3
- SQLiteOpenHelper, Jetpack DataStore Preferences, WorkManager, Kotlin Coroutines
- Min SDK 26, target/compile SDK 36
- Package: `com.example.smsexpense`

## Build & install
Requires JDK 17+ and Android SDK (compileSdk 36). The Gradle wrapper uses Gradle 8.13.

```bash
# Debug build
./gradlew assembleDebug

# Release build (R8 + resource shrink enabled)
./gradlew assembleRelease

# Install debug on a connected device/emulator
./gradlew installDebug
```

## Permissions
- `READ_SMS`, `RECEIVE_SMS`, `BROADCAST_SMS`, `POST_NOTIFICATIONS`
- Optional `SYSTEM_ALERT_WINDOW` special access for the transaction review overlay. On first launch, the app explains this access and offers Enable or Not now; Android then opens its settings page for the user to grant or deny it. It can also be enabled later from Settings. Only newly detected bank transaction SMS trigger the overlay.
- No `INTERNET` permission (strictly offline)
- `allowBackup="false"` + data-extraction rules exclude all data from backup

## Known limitations
- SMS/Call logging permission is restricted on Google Play; intended for personal/sideload use.
- Aggressive battery managers (Xiaomi, Oppo, Vivo, Samsung) may block the SMS receiver; allow "run in background / autostart" in device settings.
