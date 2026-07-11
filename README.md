# Hangug Deulama — Android Scaffold

This mirrors your existing Android Studio project (`com.appriyo.deulama`).
Merge these files into your real project — don't just drop the folder in
next to it.

## How to merge

1. **`gradle/libs.versions.toml`** → replace your existing one (or merge
   entries in if you've added anything else since).
2. **`app/build.gradle.kts`** → replace yours. It now includes the
   `kotlin-serialization` and `ksp` plugins plus every new dependency,
   and defines `API_BASE_URL` per build type via `BuildConfig`.
3. **`app/src/main/AndroidManifest.xml`** → replace yours. Two things
   changed: `android:name=".HangugDeulamaApp"` (the new Application
   class that boots Koin) and `android:networkSecurityConfig`.
   Your existing `data_extraction_rules.xml` / `backup_rules.xml` /
   `mipmap` icons / `Theme.Deulama` style are untouched and still
   referenced — don't delete those.
4. **`app/src/main/res/xml/network_security_config.xml`** (strict,
   no cleartext — used by release) **and**
   **`app/src/debug/res/xml/network_security_config.xml`** (permits
   cleartext to `10.0.2.2` — used by debug builds only, via Gradle
   source-set precedence) → add both.
5. **`app/src/main/res/values/strings.xml`** → merge the `app_name`
   string into your existing file (don't overwrite if you have other
   strings already).
6. **`app/src/main/kotlin/com/appriyo/deulama/...`** → copy the whole
   tree in. This replaces the template `MainActivity.kt` with one that
   just hosts the theme + nav graph.

## First run checklist

- **Adjust the API path.** `app/build.gradle.kts` debug `API_BASE_URL`
  is currently `http://10.0.2.2/hangug-api/public/`. Change the
  `/hangug-api/public/` part to wherever your PHP project actually
  lives under your local server root (XAMPP `htdocs/`, Laragon `www/`,
  etc). Make sure the trailing slash stays — Retrofit's `baseUrl` +
  `@GET("api/health")` requires it.
- **Start your local PHP server** (XAMPP/Laragon/`php -S`) before
  running the app, then launch on the emulator. The Login screen shows
  first; tap **"Continue browsing without an account"** to land on
  Home, where you'll see a banner: 🟢 connected / 🔴 can't reach it.
- **Physical device instead of emulator?** Swap `10.2.2.2` for your
  machine's LAN IP in both `build.gradle.kts` and
  `app/src/debug/res/xml/network_security_config.xml`, and make sure
  your PHP dev server is bound to `0.0.0.0`, not `127.0.0.1`.
- Click through the whole app: Login → Continue without account → Home
  → Discover → Drama Details → back → bottom tabs (Recommendations,
  Activity, Profile → Edit Profile → back) → Activity's "sign out"
  link takes you back to Login.

## What's real vs. placeholder

**Real:** theme (Color/Type/Shape/Theme/Gradient), nav graph with
type-safe routes, bottom bar with save/restore state, the
`Envelope<T>` wrapper, `HealthApi` + `NetworkModule`, and
`HomeViewModel` actually calling `GET /api/health` through Koin.

**Placeholder only** (empty files with comments, matching the package
structure from section 4 of the plan doc — fill in per feature phase):
`domain/model`, `domain/repository`, `domain/usecase`, `data/mapper`,
`data/repository`, `data/remote/interceptor/AuthInterceptor`,
`data/local/db/AppDatabase` (Room, zero entities), `SessionManager`
(DataStore, unencrypted for now), `presentation/common`.

Every other screen (Login, Register, Discover, Recommendations,
Activity, Profile, Edit Profile, Drama Details) is a themed demo shell
with no ViewModel — just enough to navigate and confirm the design
system end-to-end, per your Phase 1 scope.