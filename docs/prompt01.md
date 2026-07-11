# Kickoff Prompt — Hangug Deulama Android Scaffold

Paste everything below into your coding agent as the first message in an empty Android Studio project (create the project as "Empty Activity" / Compose, minSdk 26, then hand it over).

---

## PROMPT START

I'm scaffolding a new Android app called **Hangug Deulama** — a K-Drama recommendation app with a swipe-based UI. I already have a working PHP 8 REST API backend and a React web frontend; this is the native Android client. I'm attaching my full architecture/design doc (`Hangug-Deulama-Android-App-Plan.md`) — treat it as the source of truth for package structure, tech stack, screen list, and design tokens. Right now I only want **scaffolding**, not real feature logic. Specifically:

### 1. Gradle & dependencies

Set up `libs.versions.toml` + module `build.gradle.kts` with:
- Jetpack Compose (BOM, latest stable) + Material3
- Navigation-Compose with `kotlinx.serialization` for type-safe routes
- KOIN
- Retrofit2 + OkHttp3 (with `HttpLoggingInterceptor` for debug builds) + kotlinx.serialization converter
- Coil3 for image loading
- Jetpack DataStore (Preferences) — for session/token storage for now, just get it wired up, don't worry about encryption yet
- Paging3 (`paging-compose`) — dependency only, no PagingSource yet
- Room (runtime, compiler via KSP) — dependency + empty database class stub
- Kotlin Coroutines
- Two build variants: `debug` (API base URL = `http://10.0.2.2/hangug-api/public` — adjust the path to match my actual local server path, I'll tell you if it's different) and `release` (placeholder production URL, leave as `TODO`)

Use KSP, not kapt, for koin/Room annotation processing.

### 2. Package structure

Create the exact folder/package structure from section 4 of the attached doc (`data/`, `domain/`, `presentation/`, `di/`), with empty placeholder files/objects where needed just so the structure exists and compiles — don't fill in real logic yet except where noted below.

### 3. Theme — port this first, before any screens

Implement `presentation/theme/` fully from section 10 of the doc: `Color.kt` (the complete `HangugColors` object + `HangugDarkColorScheme`), `Type.kt` (Sora + Inter typography — use placeholder system fonts for now if I haven't supplied the actual font files yet, but structure it so swapping in real `.ttf`/variable fonts later is a one-line change), `Shape.kt`, and a `Theme.kt` with a `HangugDeulamaTheme` composable wrapping `MaterialTheme`. Also add a small `Gradient.kt` with the brand gradient brush (rose → deep rose → gold, 135°) as a reusable `Brush`, since I'll need it on buttons and badges everywhere.

Force dark theme only (no light theme, no dynamic color) — set `dynamicColor = false` and always use `HangugDarkColorScheme`. Set up edge-to-edge with the status/nav bar matching `BgBase`.

### 4. Navigation graph with demo/placeholder screens

Build the full nav graph from section 8 of the doc:
- `AuthGraph`: `Login`, `Register` (no bottom bar)
- `MainGraph` with a bottom `NavigationBar`: `Home`, `Discover`, `Recommendations`, `Activity`, `Profile`
- Pushed routes: `DramaDetails/{id}`, `EditProfile`

For **every** screen, create a real Composable file (not just a route entry) with:
- A `Scaffold` using the app theme
- A centered `Text` with the screen name in the hero typography style, so I can visually confirm theming + navigation both work
- Simple nav buttons/links to adjacent screens where it makes sense (e.g. Home → Discover, Login → "Continue without account" → Home) so I can click through the entire flow end to end
- Bottom nav should correctly highlight the active tab and preserve state across tab switches (standard `saveState`/`restoreState` nav-compose pattern)

Don't wire up ViewModels or real state yet — these are pure demo screens. Just get the skeleton navigable and looking on-brand.

### 5. Backend connectivity — one real call, to prove it works

This is the one piece of real logic I want in this scaffold: wire up a minimal `NetworkModule` (koin `@Provides` for OkHttp + Retrofit + a `HealthApi` interface hitting `GET /api/health`), and call it from the `Home` placeholder screen on first composition. Show the result directly on screen — a small status chip or banner reading something like:
- 🟢 "Connected — API says: ok" (parsing the real envelope: `{ success, message, data: { status, time, app } }`)
- 🔴 "Can't reach backend — check your local server is running and the base URL is correct" (on failure)

This gives me an immediate, visible signal that the emulator can talk to my local PHP server before I build anything else. Use the exact `Envelope<T>` wrapper shape described in section 5 of the doc — I want to reuse this pattern for every other endpoint later, so get it right now.

### 6. Manifest & config

- `network_security_config.xml` allowing cleartext traffic for `10.0.2.2` in debug builds only (not release)
- Internet permission
- App name/label = "Hangug Deulama"

### 7. What NOT to do yet

- No koin-injected ViewModels beyond what's needed for the health check
- No Room entities/DAOs beyond an empty `@Database` stub
- No real auth logic, no token storage logic beyond the DataStore dependency being present
- No swipe gesture implementation
- No real API calls other than `/api/health`

### Deliverable

A project that builds and runs on the emulator, shows the themed Login screen first, lets me click through every screen in the app via the demo nav buttons and bottom bar, and shows a live "connected to backend" status on the Home screen. This is my starting point — once this is solid I'll come back and build out each feature phase from section 17 of the doc one at a time.

My local backend is running at: `[FILL IN — e.g. http://localhost/hangug-api/public via XAMPP]`
My actual project package name should be: `[FILL IN — e.g. com.yourname.hangugdeulama]`

## PROMPT END

---

### Before you send it

Fill in the two `[FILL IN]` lines with your real local server path and desired package name — the agent will use these verbatim for the base URL and package declarations, so getting them right up front saves a rename pass later.

### After the scaffold lands

Good next message to send once you've confirmed it builds and the health check goes green: *"Now implement phase 2 from the doc — Auth. Real Retrofit AuthApi, AuthRepository, SessionManager via DataStore, and wire Login/Register to real network calls with the 422 field-error mapping described in section 12."* Keep working through section 17's phases one at a time rather than asking for everything at once — it'll produce cleaner, more reviewable diffs at each step.