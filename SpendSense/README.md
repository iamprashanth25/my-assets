# SpendSense

Android app that reads bank SMS on-device and categorizes your spending. No internet permission, no server.

## Build
1. Open this folder in Android Studio (Koala or newer), let Gradle sync.
2. Plug in your phone (USB debugging on) and press Run, or Build > Build APK(s).
3. Grant SMS permission when prompted.

## How it works
- `data/SmsParser.kt` pulls amount, debit/credit, merchant and account from an SMS; skips OTPs, due reminders, promos, failed txns.
- `data/Categorizer.kt` maps merchants to categories by keyword (Indian merchants). Edit `RULES` to add your own.
- Tap any transaction to recategorize; the choice applies to every transaction from that merchant and is remembered.
- Duplicate alerts for the same payment (within 3 minutes) are merged.

Run parser tests: `./gradlew test`
