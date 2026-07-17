# Deployment Guide — XAMPP → cPanel Shared Hosting

> **Audience:** you have a working XAMPP setup at `C:\xampp\htdocs\hangug-api\`
> and want to push it to a shared-hosting cPanel account so the Android
> app (and the React web app) can talk to it over the public internet.
>
> **Stack at a glance:**
>
> - **Backend:** vanilla PHP 8 (no framework, single front controller at
>   `public/index.php`), MySQL / MariaDB, JWT (HS256) auth.
> - **Server today:** XAMPP at `C:\xampp\htdocs\hangug-api\`.
> - **Server tomorrow:** cPanel shared hosting — public files in
>   `public_html/`, app code outside it, database in cPanel's MySQL.
> - **Client today:** Android app pointed at `http://192.168.1.104/hangug-api/public/`
>   (`app/build.gradle.kts` → `debug` `buildConfigField`).
> - **Client tomorrow:** Android app pointed at
>   `https://yourdomain.com/api/` (release variant of the same file).

This document covers three things:

1. **Get the backend live on cPanel** (upload, MySQL, env, smoke test).
2. **Verify the API matches what the Android client expects** (gotchas
   specific to *this* codebase — auth status codes, multipart shape,
   relative image paths, the works).
3. **Point the Android release build at it** (the one `// TODO` in
   `app/build.gradle.kts`, signing, and a final end-to-end smoke test).

---

## Table of Contents

- [0. Before you start — checklist](#0-before-you-start--checklist)
- [1. Provision the cPanel account](#1-provision-the-cpanel-account)
- [2. Upload the PHP backend](#2-upload-the-php-backend)
- [3. Create the MySQL database](#3-create-the-mysql-database)
- [4. Wire up config and secrets](#4-wire-up-config-and-secrets)
- [5. Smoke-test the API](#5-smoke-test-the-api)
- [6. Verify the API matches the Android client's contract](#6-verify-the-api-matches-the-android-clients-contract)
- [7. Point the Android release build at it](#7-point-the-android-release-build-at-it)
- [8. Final end-to-end test](#8-final-end-to-end-test)
- [9. Common gotchas & FAQ](#9-common-gotchas--faq)

---

## 0. Before you start — checklist

Have these in front of you before you start so you're not juggling
windows while cPanel times out:

- [ ] cPanel login URL, username, password (your hosting provider emails
      these on signup).
- [ ] Domain name pointed at the cPanel server (A record / nameservers
      propagated — `https://yourdomain.com/` should already resolve).
- [ ] SSH or FTP credentials for cPanel **File Manager** access. If you
      don't have SSH, File Manager's Upload + Extract is fine for a
      project this size.
- [ ] A local export of `database/schema.sql` from
      `C:\xampp\htdocs\hangug-api\database\` (see §3).
- [ ] A copy of `C:\xampp\htdocs\hangug-api\config\app.php` (or whatever
      your config file is named) — you'll need to mirror the values in
      cPanel (see §4).
- [ ] Access to the Android project at
      `C:\Users\PC\AndroidStudioProjects\deulama\` — the one `// TODO`
      we have to fix lives in `app/build.gradle.kts`.

---

## 1. Provision the cPanel account

Most of this is done for you by your hosting provider — just confirm:

1. **PHP version is 8.0+.** cPanel → *MultiPHP Manager* (or *Select PHP
   Version*) → set the domain to **PHP 8.1 or newer**. The codebase
   uses PHP 8-only features (`match`, typed properties,
   `hash_equals`, `random_bytes`), so PHP 7.x will fatal.
2. **Required PHP extensions:** PDO + `pdo_mysql`, `mbstring`,
   `openssl`, `json`, `fileinfo` (the latter is used by `finfo_file()`
   during avatar uploads — see `docs/API.md` line 1328). They're all
   stock in cPanel's default extensions, but verify in *MultiPHP INI
   Editor* → *Extensions* if you've disabled any.
3. **SSL is installed and forced to HTTPS.** cPanel → *SSL/TLS Status*
   → enable AutoSSL (Let's Encrypt) for your domain. Then
   *Domains → Redirects* → force HTTPS. The Android release variant
   uses `https://` and the network security config blocks cleartext
   for everything outside dev IPs (see §7.3), so a working TLS cert is
   non-negotiable.
4. **Disk + inode quota:** a small drama catalog + user avatars is well
   under any default cPanel quota, so this is rarely an issue — just
   note where uploads will live (§2.4).

---

## 2. Upload the PHP backend

The directory layout under `C:\xampp\htdocs\hangug-api\` typically looks
like:

```
hangug-api/
├── public/           ← document root. ONLY this folder maps to public_html/
│   ├── index.php     ← single front controller
│   ├── .htaccess     ← pretty URLs / deny dotfiles
│   └── uploads/      ← profile + drama images (writable)
├── app/              ← Controllers, Core, Models — NOT web-accessible
├── config/
├── database/
└── routes/
```

cPanel's web root is `public_html/`. The standard PHP pattern is to
upload the **contents of `public/` to `public_html/`** and the rest of
the project to a folder outside the docroot (e.g. `home/yourcpanel/hangug-api/`,
i.e. one level up from `public_html/`). If your local project already
follows this convention, great — skip to §2.3.

### 2.1 If `index.php` is at the project root (NOT in `public/`)

Many small PHP projects (especially the "single `index.php`" style this
backend uses) put the front controller at the project root. In that
case you have three options:

**Option A — upload everything to `public_html/` directly.** Easiest.
But it exposes `config/`, `app/`, `database/` to the web if you forget
to add a `.htaccess` deny rule. Add this to `public_html/.htaccess` if
you go this route:

```apache
<FilesMatch "\.(env|ini|sql|md|json|lock|log)$">
    Require all denied
</FilesMatch>

# Block direct access to anything that isn't index.php or uploads/
RewriteEngine On
RewriteCond %{REQUEST_URI} !^/index\.php [NC]
RewriteCond %{REQUEST_URI} !^/uploads/ [NC]
RewriteCond %{REQUEST_URI} !^/api/ [NC]
RewriteRule ^.*$ /index.php [L]
```

**Option B — restructure to a `public/`-rooted layout locally, then
upload the `public/` contents to `public_html/`.** Cleaner, matches
modern PHP best practice.

**Option C — upload everything except `public/` to a folder one level
up, and the contents of `public/` to `public_html/`.** Same idea as
Option B but without restructuring locally.

Pick one. This guide assumes Option A or C. Whichever you pick, the
**end result on cPanel must be** that visiting
`https://yourdomain.com/api/health` returns the standard envelope
(see §5.1).

### 2.2 Upload the files

In cPanel **File Manager**:

1. Open `public_html/` (and optionally the folder one level up if you
   went with Option C).
2. Upload the project as a ZIP, then right-click → *Extract*. Drag-and-drop
   also works but is slower over WebDAV. **Do not** upload via plain FTP
   in ASCII mode — it'll mangle line endings on the PHP files.
3. After extraction, check that `public_html/index.php` exists and the
   file sizes match your local copy (File Manager shows this).

If you have SSH access, `rsync` is dramatically faster:

```bash
# from the project root on your local machine
rsync -avz --progress \
  -e ssh \
  ./public/ yourcpanel@yourdomain.com:~/public_html/

# If you went with Option C — also sync the app/ config/ database/
# routes/ directories OUTSIDE public_html/:
rsync -avz --progress \
  -e ssh \
  ./app/ ./config/ ./database/ ./routes/ \
  yourcpanel@yourdomain.com:~/hangug-api/
```

### 2.3 Verify `.htaccess` is there

`public_html/.htaccess` is easy to lose because File Manager hides
dotfiles by default. Toggle *"Show hidden files (dotfiles)"* in the
top-right settings, confirm the file exists, and open it to verify
it's not a zero-byte stub.

### 2.4 The `uploads/` directory

Avatar + drama images need a writable destination. If your project
already has `public/uploads/` (likely), this folder is what
`uploads/profile/default.png` in the API responses refers to. Verify
its permissions:

```bash
# via SSH or File Manager → Permissions:
chmod 755 ~/public_html/uploads
chmod -R 755 ~/public_html/uploads/profile  # if it exists
# The web user (often `nobody` or your cPanel user) must be able to write
# to it during PUT /api/profile image uploads.
```

If the cPanel user can't write (common on locked-down hosts), see §9.

---

## 3. Create the MySQL database

### 3.1 cPanel → MySQL® Databases

1. **Create a database.** Name it something like
   `yourcpanel_hangug` (cPanel prefixes your username — note this prefix
   in every step below; you'll need it for the DSN).
2. **Create a database user.** e.g. `yourcpanel_hangug_app` with a
   **strong random password** (use cPanel's *Password Generator*).
3. **Add the user to the database.** Grant **ALL PRIVILEGES**. Don't
   tick "Create tables" / "Alter" individually — the schema import
   needs the lot.
4. **Note down** (you'll need these in §4):
   - Database name (full, with prefix — e.g. `yourcpanel_hangug`)
   - Username (with prefix — e.g. `yourcpanel_hangug_app`)
   - Password
   - Host (`localhost` is normal; some hosts use a private IP — check
     cPanel → *Databases → Remote MySQL* if you see a different value
     in the dashboard)

### 3.2 Import the schema

Open cPanel → **phpMyAdmin**.

1. Pick the database you just created from the left sidebar.
2. Tab **Import** → *Choose File* →
   `C:\xampp\htdocs\hangug-api\database\schema.sql`.
3. Format: SQL. **Uncheck** "Enable foreign key checks" if the schema
   sets them — keeps the import order flexible.
4. Click *Go*.

You should see green "Import has been successfully finished" with a
row count for each table.

### 3.3 Verify table list

Run this in the phpMyAdmin **SQL** tab:

```sql
SHOW TABLES;
```

Expected tables (cross-check against `docs/API.md` for the data model):

```
users           dramas          favorites
favorites       watch_later     watched
swipes          ...
```

If you don't see them all, re-import — schema imports occasionally
fail silently on a single bad statement.

### 3.4 Seed an admin / test user (optional but recommended)

Don't ship without at least one user you can log in as. Either:

- Run your existing seed script from `database/` (look for
  `seeds.sql` or a `seed.php`).
- Or insert a row manually via phpMyAdmin. If you do, hash the password
  with PHP's `password_hash($pw, PASSWORD_DEFAULT)` — paste the SQL it
  spits out:

  ```sql
  INSERT INTO users (full_name, email, password_hash, created_at)
  VALUES ('Test User', 'you@example.com', '<paste-hash-here>', NOW());
  ```

Don't ship with a real-looking password — change it before going live.

---

## 4. Wire up config and secrets

Open your local `C:\xampp\htdocs\hangug-api\config\app.php` (or
whatever your config file is named — open the actual file to find out).
You need to update it on the server with **production-safe values**.

### 4.1 The five values that must change

| Key                | XAMPP value (typical)                  | cPanel value                                               |
|--------------------|----------------------------------------|------------------------------------------------------------|
| `db.host`          | `127.0.0.1` or `localhost`             | `localhost` (or the private IP from cPanel's DB dashboard) |
| `db.name`          | `hangug` (no prefix)                   | `yourcpanel_hangug` (with cPanel prefix — see §3.1)        |
| `db.user`          | `root`                                 | `yourcpanel_hangug_app` (with prefix)                      |
| `db.pass`          | empty / `""`                           | The strong random password from §3.1                       |
| `jwt.secret`       | A dev-only HMAC key (often short)      | **Generate a new 32+ byte random key** (see below)         |
| `app.url` / `cors.allowed_origins` | `http://localhost`             | `https://yourdomain.com`, `https://www.yourdomain.com`     |

### 4.2 Generating a JWT secret

Open a terminal anywhere with PHP installed:

```bash
php -r 'echo bin2hex(random_bytes(32));'
```

That gives you 64 hex characters — copy it into `jwt.secret` and don't
re-use the dev one. Rotating it invalidates every outstanding token,
which is what you want when moving to production.

### 4.3 CORS allow-list

`docs/API.md` lines 161–162 lists the CORS allow-list as configurable
in `config/app.php → cors.allowed_origins`. The Android app doesn't
need CORS (it's not a browser — see §6.5), but the React web client
**does**. Add at minimum:

```php
'allowed_origins' => [
    'https://yourdomain.com',
    'https://www.yourdomain.com',
],
```

Add the deployed web app's URL too (e.g. `https://deulama.netlify.app`
based on `README.md` line 19) once you know where that lives.

### 4.4 Where to keep config

Two options:

**Option A — keep `config/app.php` outside `public_html/`** (recommended).
Add a `config.local.php` with production values, and have `app.php`
`require` it. Never commit `config.local.php`.

**Option B — keep it inside `public_html/` with a `.htaccess` deny
rule.** Easier but riskier — a one-line Apache misconfiguration leaks
your DB credentials.

Pick A if your project structure allows it; if it doesn't, B is fine
as long as the deny rule in §2.1 is in place.

### 4.5 File permissions

```bash
chmod 644 ~/public_html/index.php
chmod 644 ~/public_html/.htaccess
chmod -R 755 ~/public_html/uploads       # writable by web user
chmod 600 ~/hangug-api/config/app.php    # if outside docroot
```

---

## 5. Smoke-test the API

Before touching the Android app, verify the backend is healthy from
the public internet.

### 5.1 The `/api/health` endpoint

`docs/API.md` line 294 says this endpoint is **unauthenticated** and
returns:

```json
{
  "success": true,
  "message": "",
  "data": {
    "status": "ok",
    "time": "2026-07-06T12:00:00+00:00",
    "app": "hangug-deulama"
  }
}
```

From your laptop:

```bash
curl -i https://yourdomain.com/api/health
```

You should see `HTTP/2 200`, the standard response headers
(`X-Content-Type-Options: nosniff`, `Content-Type: application/json`),
and the JSON body above.

**If you get a 404:** your rewrite rules aren't catching `/api/health`.
Open `public_html/.htaccess` and confirm the rule maps unknown paths
to `index.php`.

**If you get a 500:** check `php_errors.log` (cPanel → *Errors*) — most
commonly a typo in the DB credentials or a missing PHP extension.

### 5.2 Login with your seeded test user

```bash
curl -i -X POST https://yourdomain.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"<your-test-password>"}'
```

You should get back `data.token` (a JWT), `data.user.full_name`,
`data.user.email`, and a `profile_image` field. **Save the token** —
you'll use it in the next checks.

### 5.3 Confirm `created_at` is in the response

Per `docs/API.md` and the Android client's strict DTO contract, the
`user` object MUST include `created_at`. If your login response is
missing it, add it to the login handler — the Android client will
throw `MissingFieldException` and surface a misleading "Can't reach
server" toast. (See the comment at `SwipeDto.kt` lines 28–32 for the
canonical example of this trap.)

### 5.4 Hit a few authenticated endpoints

```bash
TOKEN="<paste-token-from-5.2>"

curl -i https://yourdomain.com/api/me -H "Authorization: Bearer $TOKEN"
curl -i https://yourdomain.com/api/dramas?limit=5
curl -i https://yourdomain.com/api/profile -H "Authorization: Bearer $TOKEN"
curl -i https://yourdomain.com/api/profile/genre-statistics -H "Authorization: Bearer $TOKEN"
curl -i https://yourdomain.com/api/recommendations -H "Authorization: Bearer $TOKEN"
```

Each should return `success: true` and a `data` payload matching
`docs/API.md` for that endpoint.

---

## 6. Verify the API matches the Android client's contract

These are the **gotchas specific to this codebase** — i.e. things
that will compile and run fine but silently break the Android app if
they're wrong on the server. Each one references the exact line in
`docs/API.md` and the Android client that consumes it.

### 6.1 Status codes — return `401`, never `403`

The Android client's `AuthInterceptor` (see the survey note at
`AuthInterceptor.kt` lines 37–39) fires a **global logout** on a `401`
response to a request that carried a Bearer token. If you accidentally
return `403 Forbidden` for an invalid/expired JWT, the client will
never log out — it'll just show a generic error toast and the user
will be stuck.

- ✅ `401 Unauthorized` → invalid/expired JWT, wrong login password.
- ❌ `403 Forbidden` → reserved for future permissions checks. Don't
  emit it for any auth failure.

`docs/API.md` line 178 calls this out: "Missing / malformed / expired
/ signature-invalid JWT; or, on `/api/auth/login`, wrong credentials."

### 6.2 Multipart avatar upload — part name is `image`

`PUT /api/profile` accepts `multipart/form-data` for the avatar upload.
The Android client's `ProfileApi.kt` line 53 sends the file part with
the exact name `image`. Text parts (`name`, `current_password`,
`new_password`, `confirm_password`) are sent as `text/plain;
charset=utf-8`.

If your handler reads `$_FILES['avatar']` instead of `$_FILES['image']`
(or reads `$_POST['name']` instead of `$_POST['name']` for the JSON
path), the upload silently fails. The server-side test:

```bash
# from the curl examples in docs/API.md:
curl -i -X PUT https://yourdomain.com/api/profile \
  -H "Authorization: Bearer $TOKEN" \
  -F "image=@/path/to/avatar.jpg"
```

Expect a 200 with `data.image = "uploads/profile/20260706_..._xxxxx.jpg"`.
Per `docs/API.md` line 1329, the server **replaces** the filename with
a server-generated one — don't try to preserve the original.

### 6.3 Always return `created_at` on `user` and `drama` payloads

Already covered in §5.3 — but worth flagging because the failure mode
is opaque (it looks like a network error, not a parsing error).

### 6.4 Image paths — keep them relative

`docs/API.md` says `profile_image`, `poster_url`, and `banner_url`
are **relative paths** like `uploads/profile/default.png`. The
Android client (`presentation/util/ImageUrls.kt` line 22) prepends
`BuildConfig.API_BASE_URL` to relative paths and passes absolute
URLs through unchanged.

So both work, but **relative is the documented contract** and gives
cleaner URLs in the app. If you switch to a CDN later, return absolute
URLs and the client will pass them through without any code changes.

### 6.5 No CORS needed for Android — only for the web client

The Android app does **not** enforce CORS (it's not a browser). The
React web app does. If you don't see CORS headers (`Access-Control-
Allow-Origin`, `Vary: Origin`) in the response to §5.4, the Android
app will still work fine — but the web app won't. Set the
`cors.allowed_origins` list per §4.3 to cover both.

### 6.6 The 19 endpoints must all exist and respond

There's no harm in cross-checking your deployment against the table in
§4 of `docs/API.md`. As of v1.0 the surface area is:

```
Health           GET  /api/health
Auth             POST /api/auth/register
                 POST /api/auth/login
                 GET  /api/me
Dramas           GET  /api/dramas
                 GET  /api/dramas/{id}
Favorites        POST /api/favorites
                 DELETE /api/favorites/{drama_id}
                 GET  /api/favorites
Watch Later      POST /api/watch-later
                 DELETE /api/watch-later/{drama_id}
                 GET  /api/watch-later
Watched          POST /api/watched
                 GET  /api/watched
Swipe            POST /api/swipe
Profile          GET  /api/profile
                 PUT  /api/profile
Profile Stats    GET  /api/profile/genre-statistics
Recommendations  GET  /api/recommendations
```

If any of these is missing or returns 404, the relevant screen on the
Android app will be stuck on its error/empty state.

### 6.7 Engagement 409-as-success contract

`POST /api/favorites`, `/api/watch-later`, `/api/watched` should
return `201 Created` on first add and `409 Conflict` on duplicate.
The Android client's `safeApiCall`/`treatAlreadyAppliedAsSuccess`
helper (see `ApiResult.kt` line 133) **collapses 409/404 into Success**
on write paths — so a double-tap won't show an error. Make sure the
`/api/swipe` endpoint behaves the same way (200 OK on re-swipe, not
409 Conflict — see `docs/API.md` line 1162).

### 6.8 `POST /api/watched` has no DELETE

`docs/API.md` line 968 calls this out explicitly: "There is intentionally
**no** `DELETE` endpoint (no un-watch)." The Android client never
tries to remove a watched entry, so don't accidentally expose one —
you'd be giving users a way to game their "watched_count" stat in
genre statistics.

---

## 7. Point the Android release build at it

This is the half that lives in the Android project.

### 7.1 The single TODO in `app/build.gradle.kts`

Open `app/build.gradle.kts` line 42 and replace the placeholder:

```kotlin
// BEFORE — placeholder, will not work
buildConfigField("String", "API_BASE_URL", "\"https://TODO-your-production-domain.com/\"")

// AFTER — your real production domain
buildConfigField("String", "API_BASE_URL", "\"https://yourdomain.com/\"")
```

Format requirements:

- **Trailing slash** — both current build variants end with `/` and
  Retrofit expects it. Drop it and every endpoint resolves to
  `https://yourdomain.comapi/health` (404).
- **`https://`** — the `network_security_config.xml` blocks cleartext
  for everything outside dev IPs (see §7.3). A `http://` release URL
  will not work without also editing that file.
- **Path prefix matches your cPanel layout.** If the API is mounted at
  the docroot (i.e. `https://yourdomain.com/api/health` works directly),
  the URL is just `https://yourdomain.com/`. If you put it under
  `/api/`, the URL is `https://yourdomain.com/api/`.

If you're not sure, hit `https://yourdomain.com/api/health` in a
browser — if you see JSON, the path is `/api/`.

### 7.2 Sign the release build

The repo currently has **no `signingConfigs` block** in
`app/build.gradle.kts` (verified — there's nothing between
`buildFeatures` and `dependencies`). That means `gradlew assembleRelease`
will produce an **unsigned** APK that can't be installed on a stock
device.

You have two options:

**Option A — Android Studio wizard.** *Build → Generate Signed App
Bundle / APK…* → *Android App Bundle* → *Create new…* keystore. Save
the keystore somewhere safe (NOT in the repo) and remember the
password. The wizard writes the `signingConfigs.release { … }` block
into `app/build.gradle.kts` automatically.

**Option B — manually edit `app/build.gradle.kts`.** Add this:

```kotlin
android {
    // ... existing config ...

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("DEULAMA_KEYSTORE") ?: "release.keystore")
            storePassword = System.getenv("DEULAMA_STORE_PASSWORD")
            keyAlias = System.getenv("DEULAMA_KEY_ALIAS")
            keyPassword = System.getenv("DEULAMA_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ... existing release config ...
        }
    }
}
```

Then set the env vars in your shell / CI. This keeps the keystore
credentials out of source control — important, because losing the
keystore means you can never push an update to your existing users.

### 7.3 Verify `network_security_config.xml`

`app/src/main/res/xml/network_security_config.xml` already handles
HTTPS correctly out of the box. The current contents:

```xml
<base-config cleartextTrafficPermitted="false" />
```

…which means **cleartext is denied for your production domain**. Since
you're using `https://` (see §7.1), no edit needed.

If you ever ship a cleartext release by mistake, you'll see every
request fail with `CLEARTEXT communication ... not permitted`. Add the
production domain to the `<domain-config cleartextTrafficPermitted="true">`
list as an emergency fix — but really, just fix the URL.

### 7.4 Bump `versionCode` / `versionName`

For the first production release, the existing `versionCode = 1` /
`versionName = "1.0"` is fine. For subsequent releases, bump
`versionCode` (monotonically increasing integer) and `versionName`
(visible to users, e.g. `"1.1"`).

### 7.5 Build the release artifact

```bash
./gradlew :app:bundleRelease      # generates a .aab for Play Store
# or
./gradlew :app:assembleRelease    # generates an .apk for sideloading
```

Output goes to `app/build/outputs/bundle/release/` or
`app/build/outputs/apk/release/` respectively. The `.aab` is what
Google Play wants; the `.apk` is what you sideload to your own test
device.

---

## 8. Final end-to-end test

Before declaring victory, walk through this with a signed release APK
installed on a phone on real Wi-Fi (not an emulator on `10.0.2.2`):

1. **Cold start** → splash → Home. The 🟢 connection banner should
   appear within ~1 s.
2. **Register a new account** → confirm you're logged in and the Home
   tab shows real data, not skeletons forever.
3. **Browse dramas** → tap a card → Details loads with poster + synopsis.
4. **Swipe right** on a card → expect a fly-off + the card below rises.
   Check the server log to confirm a `POST /api/swipe` arrived with
   `swipe_type: "like"`.
5. **Favorite** a drama from the Details screen → the heart icon
   changes immediately. Force-kill the app, reopen, the heart is
   still filled (Room offline queue → server reconciliation).
6. **Mark as watched** a different drama → it should appear at the
   top of the Activity timeline.
7. **Open Profile** → confirm all fields render:
   - `User #<id>`
   - `Liked · <liked_count>` and `Watched · <watched_count>` stat cards
   - Top genres chips (or "No preferences yet" for a brand-new user)
   - The raw `image: uploads/profile/default.png` line
8. **Edit Profile** → upload an avatar → confirm the avatar updates
   everywhere (Home avatar, Profile, next cold start).
9. **Force-stop the backend** (block the domain in `/etc/hosts` on
   your phone temporarily) → confirm the Home screen shows the red
   🔴 banner and not a crash.
10. **Log out** → confirm the auth flow cleanly returns to the login
    screen and the JWT is wiped from DataStore.

If any of these fail, jump back to §5 or §6 for the matching
diagnostic.

---

## 9. Common gotchas & FAQ

### 9.1 "I get 500 from `/api/health`"

Open `php_errors.log` (cPanel → *Errors*). Almost always one of:

- **Database connection refused.** Check `db.host` is `localhost`
  (cPanel, not `127.0.0.1`).
- **Wrong DB credentials.** Test them directly via phpMyAdmin first.
- **Missing PHP extension.** `pdo_mysql` is the usual one. Fix in
  *MultiPHP INI Editor*.

### 9.2 "Avatar uploads return 422"

You're using the wrong field name on the server, OR the image's real
MIME type isn't JPEG/PNG/WebP. Check `$_FILES['image']` (not
`$_FILES['avatar']` / `$_FILES['file']`), and that the handler uses
`finfo_file()` on the uploaded bytes, not `$_FILES['image']['type']`
(which the client sets, but per `docs/API.md` line 1327 the server
should ignore).

### 9.3 "Login works but the app shows 'Account no longer exists' on `/api/me`"

`docs/API.md` line 265 calls this out: the JWT is valid but the user
row is missing from the DB. Either:

- You're pointing at a different DB than the one that issued the JWT.
- The schema import wiped the `users` table.

Re-import the schema and re-seed (§3.4).

### 9.4 "All POSTs fail with CORS in the browser console (but Android works)"

That's actually fine for the Android client — see §6.5. Fix it for
the web client by adding the React app's deployed origin to
`cors.allowed_origins` (§4.3).

### 9.5 "I see 200 OK but the response body is HTML"

Either PHP fataled and the server returned a 500-as-200, or your
rewrite rule is sending every request through `index.php` but
`index.php` isn't routing `/api/*`. Check `routes/api.php` matches
the actual paths in `docs/API.md` § "API Endpoints" (line 285+).

### 9.6 "The release APK won't install on my test device"

- If you used Android Studio's signing wizard, the keystore is
  per-developer — uninstall any old debug build first.
- If you see `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, the signing cert
  doesn't match the previous install. Uninstall first.

### 9.7 "Mixed content warnings in the browser console on uploads"

Your `uploads/` directory is served over `http://` while the rest of
the site is `https://`. Force HTTPS on the whole domain via the
*Domains → Redirects* panel.

### 9.8 "JWT secret leaked — what do I do?"

1. Generate a new 32-byte secret per §4.2.
2. Update `config/app.php` on the server.
3. **All existing tokens are now invalid** — every user gets signed
   out. That's intentional.
4. Search your git history (and any backup) for the old secret and
   treat it as compromised (rotate any other secrets that shared its
   storage path).

### 9.9 "How do I deploy a database migration after going live?"

- Add the migration SQL to `database/migrations/` (or wherever your
  project keeps them).
- Apply via phpMyAdmin's SQL tab, or via CLI if your host provides
  SSH + mysql client.
- **Always back up the live DB first** (`cPanel → Backup → Download
  a MySQL Database Backup`) before running any DDL.

### 9.10 "My uploads dir fills up the disk quota"

cPanel quotas are tight by default. Add a cron job to prune orphans:

```cron
# Nightly — delete avatar files older than 90 days that are no longer
# referenced by any user (requires a quick SELECT against the DB).
0 3 * * * php /home/yourcpanel/hangug-api/scripts/prune-uploads.php
```

(Add `scripts/prune-uploads.php` if it doesn't exist — not in the
default project structure.)

---

## Appendix A — Quick reference: file paths touched

| Action                          | File / location                                                  |
|---------------------------------|------------------------------------------------------------------|
| Set production API URL          | `app/build.gradle.kts` line 42 (release `buildConfigField`)      |
| Allow cleartext for prod (DON'T)| `app/src/main/res/xml/network_security_config.xml` line 22–26    |
| Signing config (if you add it)  | `app/build.gradle.kts` `signingConfigs` + `buildTypes.release`   |
| Bump version                    | `app/build.gradle.kts` `versionCode` / `versionName`             |
| Backend config (DB, JWT, CORS) | `config/app.php` on the server                                  |
| Schema source                   | `C:\xampp\htdocs\hangug-api\database\schema.sql` → phpMyAdmin    |
| Document root (web)             | `public_html/` on cPanel                                         |
| Image uploads                   | `public_html/uploads/` (writable, see §2.4)                     |

## Appendix B — Related docs

- [`docs/API.md`](API.md) — the API contract this guide deploys.
- [`docs/BUILD_ROADMAP.md`](BUILD_ROADMAP.md) — line 227 references
  the production-URL TODO this guide resolves.
- [`README.md`](../README.md) — the project overview; this guide
  slots into the "Production builds" note in §"Getting Started".

---

*Last updated to match the codebase as of the v1.0 API release (see
`docs/API.md` "Changelog" line 1815).*