# SMS Gateway Android App
Free, open-source SMS gateway — no purchase code required.

## Features
- Sign in with Server / Email / Password (same UI as screenshot)
- Sign in via QR Code
- Sends outgoing SMS by polling your server
- Forwards incoming SMS to your server
- Runs as a persistent foreground service
- Auto-starts on device boot
- Multi-language UI (EN, ES, FR, DE, PT, RU, AR)

## How to Build (Android Studio)

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

### Steps
1. Open Android Studio
2. File → Open → select this `SmsGateway` folder
3. Wait for Gradle sync to finish
4. Connect your Android phone (USB debugging on)
5. Click **Run ▶** OR go to **Build → Build Bundle(s)/APK(s) → Build APK(s)**
6. APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

## Server API Expected Endpoints

Your server needs to expose these REST endpoints:

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/login` | Login with email+password, returns `{"token":"..."}` |
| GET | `/api/sms/pending` | Returns array of SMS to send: `[{"id":"...","to":"...","message":"..."}]` |
| PATCH | `/api/sms/{id}/status` | Update status: `{"status":"delivered"}` or `{"status":"failed","reason":"..."}` |
| POST | `/api/sms/incoming` | Receive SMS: `{"from":"...","message":"...","received_at":timestamp}` |

### QR Code Login Format
```json
{"server":"https://yourserver.com","email":"user@example.com","token":"your-api-token"}
```

## Permissions Required
- `RECEIVE_SMS` / `READ_SMS` / `SEND_SMS` — core gateway function
- `READ_PHONE_STATE` — SIM identification
- `INTERNET` — communicate with server
- `FOREGROUND_SERVICE` — keep running in background
- `RECEIVE_BOOT_COMPLETED` — auto-start after reboot
- `CAMERA` — QR code scanning

## Compatible Servers
This app works with any server that implements the API above.
Open-source compatible backends: Firefly SMS, SMS Gateway for Android API, custom Laravel/Node/Django apps.

## License
MIT — free to use, modify, and distribute.
