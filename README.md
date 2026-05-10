# 🔒 Privacy Monitor — Build Instructions

## Step 1: Create a new GitHub repository

1. Go to https://github.com
2. Click the **+** button → **New repository**
3. Name it: `privacy-monitor`
4. Set it to **Public**
5. Click **Create repository**

---

## Step 2: Upload all files

Upload these files keeping the EXACT folder structure:

```
privacy-monitor/
├── .github/
│   └── workflows/
│       └── build.yml
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── privacymonitor/
│   │       │           └── app/
│   │       │               ├── MainActivity.kt
│   │       │               ├── MonitorService.kt
│   │       │               └── BootReceiver.kt
│   │       └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

You can upload via: **Add file → Upload files** on GitHub

---

## Step 3: Wait for the build

1. After uploading, click the **Actions** tab on GitHub
2. You'll see "Build APK" running automatically
3. Wait about **3–5 minutes**
4. When it shows ✅ green, click on it

---

## Step 4: Download your APK

1. Scroll down to **Artifacts**
2. Click **PrivacyMonitor-APK** to download
3. Unzip the file — you'll find `app-debug.apk`
4. Send it to your Samsung phone (via WhatsApp, email, Google Drive, etc.)

---

## Step 5: Install on Samsung

1. Open the APK file on your phone
2. Allow **Install from unknown sources** if asked
3. Install the app

---

## Step 6: Samsung battery optimization (IMPORTANT)

Samsung kills background apps aggressively. To keep the monitor running:

1. Open the app
2. Tap **"Disable Battery Optimization"** button
3. Select **"Allow"**

This ensures the app keeps running even when your screen is off.

---

## How it works

- Tap **Start Monitoring** — runs silently in the background
- If any app activates your **microphone** → voice alert plays: *"Warning! The microphone has been activated."*
- If any app activates your **camera** → voice alert plays: *"Warning! The camera has been activated."*
- A persistent notification keeps the service alive on Samsung
