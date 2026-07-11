# Fix: Android Emulator cannot reach local PHP backend

> **Scope**: This document fixes two specific issues that block the Android
> app from calling your local PHP 8 REST API at `hangug-api/public/api/`.
> Nothing here touches CORS, JSON, JWT, or any backend code — the request
> is being killed on-device before it ever leaves the Android process.

---

## The two errors

```
--> POST http://127.0.0.1/hangug-api/public/api/auth/login
<-- HTTP FAILED: java.net.UnknownServiceException:
    CLEARTEXT communication to 127.0.0.1 not permitted by network security policy
```

1. **Loopback routing** — on an Android emulator, `127.0.0.1` points to
   the emulator itself, not to the host machine running XAMPP/Laragon.
2. **Cleartext policy** — since Android 9 (API 28), HTTP traffic is
   blocked by default unless explicitly whitelisted in a network
   security config.

Both are fixed below.

---

## Step 1 — Point the app at the host loopback

### 1a. Emulator (default)

Open **`app/build.gradle.kts`** and update the `debug` build type:

```kotlin
buildTypes {
    debug {
        // 10.0.2.2 is the emulator's alias for the host machine's 127.0.0.1
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2/hangug-api/public/\"")
    }
    release {
        // ...production URL unchanged...
    }
}
```

> **Already-applied diff in this repo:**
> ```
> - buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1/hangug-api/public/\"")
> + buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2/hangug-api/public/\"")
> ```

After editing, run **Build → Make Project** (or just hit Run — Gradle
will regenerate `BuildConfig`).

### 1b. Physical device on the same Wi-Fi

Replace `10.0.2.2` with your host machine's LAN IP, e.g.:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.42/hangug-api/public/\"")
```

How to find your LAN IP:

| OS      | Command                     | Look for                                |
| ------- | --------------------------- | --------------------------------------- |
| Windows | `ipconfig`                  | `IPv4 Address` under your Wi-Fi adapter |
| macOS   | `ifconfig \| grep "inet "`  | `inet 192.168.x.x` under `en0`          |
| Linux   | `ip -4 addr show wlan0`     | `inet 192.168.x.x/...`                  |

Additional requirements for physical-device testing:

1. Phone and laptop on the **same Wi-Fi SSID**.
2. PHP dev server bound to `0.0.0.0`, not `127.0.0.1`:
   ```bash
   # PHP built-in
   php -S 0.0.0.0:8000 -t public/

   # Apache (httpd.conf / httpd-vhosts.conf)
   Listen 0.0.0.0:80

   # Nginx (sites-enabled/*.conf)
   listen 80;   # default already binds 0.0.0.0
   ```
3. Host firewall allows inbound on port 80 (or 8000). On Windows, the
   first time you start XAMPP Apache, accept the "Windows Defender
   Firewall has blocked some features…" prompt and tick **Private
   networks**.

---

## Step 2 — Allow cleartext HTTP for local dev hosts

### 2a. Edit `app/src/main/res/xml/network_security_config.xml`

The file in this repo already allows `10.0.2.2`. The version below also
covers a LAN IP and `localhost` so you don't need a second edit if you
switch to a physical device.

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <!-- Emulator -> host loopback -->
        <domain includeSubdomains="false">10.0.2.2</domain>
        <!-- Edit to match your dev machine's actual LAN IP -->
        <domain includeSubdomains="false">192.168.1.42</domain>
        <!-- Loopback (kept for completeness; usually unnecessary on device) -->
        <domain includeSubdomains="false">127.0.0.1</domain>
        <domain includeSubdomains="false">localhost</domain>
    </domain-config>
    <base-config cleartextTrafficPermitted="false" />
</network-security-config>
```

The trailing `<base-config cleartextTrafficPermitted="false" />` is
deliberate — every host **not** listed above (i.e. the real internet,
your production API, etc.) is still blocked from cleartext traffic.

### 2b. Wire the config into `AndroidManifest.xml`

Open **`app/src/main/AndroidManifest.xml`** and make sure the
`<application>` tag has this attribute (it already does in this repo):

```xml
<application
    android:name=".HangugDeulamaApp"
    android:allowBackup="true"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:networkSecurityConfig="@xml/network_security_config"
    android:theme="@style/Theme.Deulama"
    tools:targetApi="28">

    <activity ...>
        ...
    </activity>
</application>
```

Key attribute:

```xml
android:networkSecurityConfig="@xml/network_security_config"
```

`INTERNET` permission must also be present (it already is on line 5):

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

> **Why no `usesCleartextTraffic`?** The `networkSecurityConfig` attribute
> is the modern, granular mechanism and supersedes the legacy
> `usesCleartextTraffic` flag. You can add `android:usesCleartextTraffic="true"`
> as a belt-and-braces fallback for `minSdk < 24`, but this project targets
> `minSdk = 26` so it isn't required.

---

## Step 3 — Rebuild and verify

1. **Build → Make Project** in Android Studio (regenerates `BuildConfig`).
2. Run the app on an emulator.
3. Trigger the login flow and watch Logcat:

   ```
   --> POST http://10.0.2.2/hangug-api/public/api/auth/login
   <-- 200 OK
   ```

   The previous line:

   ```
   <-- HTTP FAILED: java.net.UnknownServiceException: CLEARTEXT communication...
   ```

   should be gone.

4. The Home screen banner 🟢 / 🔴 will also flip to green once a
   successful round-trip is observed.

### If you still see *connection* errors (not the cleartext one)

That means you've graduated past the two issues in this doc. Next places
to look, in order:

- Host firewall (port 80 / 8000).
- `php -S` bound only to `127.0.0.1` instead of `0.0.0.0`.
- `AllowOverride All` / mod_rewrite not enabled in Apache, so the
  `/hangug-api/public` `.htaccess` isn't routing to `index.php`.
- Backend's own log — is the request even arriving at PHP?

---

## Files changed by this fix

| Path                                                         | Change                                                            |
| ------------------------------------------------------------ | ----------------------------------------------------------------- |
| `app/build.gradle.kts`                                       | `debug.API_BASE_URL`: `127.0.0.1` → `10.0.2.2`                   |
| `app/src/main/res/xml/network_security_config.xml`           | Added LAN IP + `127.0.0.1` / `localhost` entries                  |
| `app/src/main/AndroidManifest.xml`                           | Already wired (no change); reference only                         |

---

## Quick reference — which URL when?

| Scenario                                | Use                       |
| --------------------------------------- | ------------------------- |
| Emulator → host's XAMPP/Laragon         | `http://10.0.2.2/...`     |
| Physical device → host's XAMPP/Laragon  | `http://<host-LAN-IP>/...` |
| Release / Play Store                    | `https://your-prod/...`   |
