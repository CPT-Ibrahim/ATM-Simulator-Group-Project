# ATM NFC Reader — Android App

Reads NFC card UIDs and sends them to your Java ATM application over a USB ADB tunnel.

---

## IDE — Use Android Studio (NOT IntelliJ IDEA)

Download **Android Studio** from: https://developer.android.com/studio

Android Studio is the official IDE for Android. It is built on top of IntelliJ IDEA,
so everything feels identical — same keyboard shortcuts, same UI — but it includes:
- Android SDK built-in
- AVD (emulator) manager
- Gradle integration pre-configured
- NFC testing tools

IntelliJ IDEA *can* technically do Android with a plugin, but Android Studio is
always recommended and is free.

---

## Setup Steps

### 1. Open Project in Android Studio
- File → Open → select the `NFCReaderATM` folder
- Android Studio will detect it as a Gradle project
- Let it sync (first sync downloads Gradle and Android SDK — may take a few minutes)

### 2. Connect Your Phone
- Plug in via USB
- Enable **Developer Options** on your phone:
  Settings → About Phone → tap "Build Number" 7 times
- Enable **USB Debugging** in Developer Options
- Accept the RSA fingerprint prompt on your phone

### 3. Set Up the ADB Tunnel (run this in terminal EVERY time you reconnect)
```bash
adb reverse tcp:8080 tcp:8080
```
This makes your Android app's `localhost:8080` point to your PC's port `8080`.

### 4. Start Your Java ATM Server
Make sure `NFCServer` is running and listening on port `8080` before tapping cards.

### 5. Build & Run the App
- In Android Studio, select your phone from the device dropdown
- Click ▶ Run
- App installs and launches on your phone

### 6. Test It
- Start your Java ATM
- Tap an NFC card/fob/tag on the back of your phone
- The app shows the UID and sends it to the ATM
- The ATM console logs: `[NFCServer] Card tapped — UID: XXXXXXXX`

---

## Configuration

Only one thing to change if you use a different port:

In `MainActivity.java`, line ~30:
```java
private static final String ATM_SERVER_URL = "http://localhost:8080/nfc";
```
Change `8080` to match your Java server port, and update `adb reverse` accordingly.

---

## Adding Cards to the Database

When you tap a card, the app shows its UID (e.g., `A1B2C3D4`).
Copy that UID and assign it to an account:

```sql
UPDATE accounts SET nfc_uid = 'A1B2C3D4' WHERE account_number = '123456';
```

---

## Troubleshooting

| Problem | Fix |
|---|---|
| "Connection refused" | Run `adb reverse tcp:8080 tcp:8080` and make sure Java ATM is running |
| App doesn't detect card | Make sure NFC is ON in phone settings; hold card flat on the back |
| Card detected but wrong UID | Different card types — try again; UID is always the same for the same card |
| ADB not found | Install Android SDK Platform Tools: https://developer.android.com/tools/releases/platform-tools |
| Phone not detected by Android Studio | Enable USB Debugging in Developer Options |
