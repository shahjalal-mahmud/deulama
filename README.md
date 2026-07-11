# 한 Hangug Deulama — Android

**A native Android client for discovering, swiping, and tracking Korean dramas — built to feel like the same product as its React web counterpart, not a reskin.**

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-26-blue)
![Status](https://img.shields.io/badge/status-in%20development-orange)

---

## Overview

Hangug Deulama is a K-Drama discovery app centered on a Tinder-style swipe
interaction: swipe to like or dislike, and the app learns your taste to
surface personalized recommendations. This repository is the **native
Android client**, built in Kotlin + Jetpack Compose against a shared PHP
REST API — the same backend that powers the [React web app](https://deulama.netlify.app/).

The two clients are treated as one product: same color system, same
typography, same interaction language, same API contract. This README
covers the Android app specifically.

---

## ✨ Features

- **Swipe deck** — native drag gestures with velocity-based fling
  dismissal, rotation, and a stacked-card preview (not a copy of a CSS
  transform — built from scratch with Compose primitives)
- **Anonymous-first browsing** — swipe, favorite, and queue dramas before
  creating an account; everything syncs the moment you sign up or log in
- **Personalized recommendations** — a Top 10 feed that adapts to your
  swipe/favorite/watch history, with a clear cold-start fallback for new
  users
- **Favorites, Watch Later, and Watched tracking** — with optimistic UI
  updates and offline-tolerant local queuing
- **Genre taste profile** — per-genre preference scoring computed
  server-side and rendered as a simple, honest breakdown
- **Cinematic dark UI** — a bespoke rose/gold design system (no default
  Material You colors) ported 1:1 from the web app's token set

---

## 🛠 Tech Stack

| Layer         | Choice                                                                             |
|---------------|------------------------------------------------------------------------------------|
| Language      | Kotlin (100%)                                                                      |
| UI            | Jetpack Compose + Material 3 (custom theme, dark-only)                             |
| Architecture  | MVVM + Clean Architecture (`data` / `domain` / `presentation`)                     |
| Navigation    | Navigation-Compose, type-safe routes via `kotlinx.serialization`                   |
| DI            | Koin                                                                               |
| Networking    | Retrofit2 + OkHttp3 + kotlinx.serialization                                        |
| Async         | Kotlin Coroutines + Flow / StateFlow                                               |
| Local storage | Room (offline queue / anonymous-mode sync) + Jetpack DataStore                     |
| Pagination    | Paging 3                                                                           |
| Images        | Coil3                                                                              |
| Backend       | Vanilla PHP 8 REST API, MySQL, JWT (HS256) auth — see [`docs/api.md`](docs/api.md) |

---

## 🏗 Architecture

Three layers, mirroring the same separation of concerns as the web app's
`api/` + `context/` split:

```
presentation/   → Composables + ViewModels, one feature per package
domain/         → Use cases, domain models, repository interfaces (pure Kotlin)
data/           → Retrofit services, DTOs, Room DAOs, repository implementations, mappers
```

Every API response is parsed through a single generic envelope:

```kotlin
@Serializable
data class Envelope<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val errors: JsonElement? = null,
)
```

...so every screen, present and future, handles success/validation/auth
errors identically instead of each ViewModel reinventing it.

---

## 📁 Project Structure

```
app/src/main/kotlin/com/appriyo/deulama/
├── HangugDeulamaApp.kt          # Application — boots Koin
├── MainActivity.kt              # Single-activity host
│
├── data/
│   ├── remote/                  # Retrofit APIs, DTOs, interceptors, NetworkModule
│   ├── local/                   # Room DB, DataStore-backed SessionManager
│   ├── mapper/                  # DTO <-> domain mappers
│   └── repository/              # Repository implementations
│
├── domain/
│   ├── model/                   # Drama, Swipe, Favorite, Profile, Recommendation...
│   ├── repository/               # Repository interfaces
│   └── usecase/                  # Pure business logic
│
├── presentation/
│   ├── navigation/               # NavGraph, type-safe Routes
│   ├── theme/                    # Color, Type, Shape, Theme, Gradient
│   ├── components/               # Shared composables (DramaCard, StatusBanner...)
│   ├── auth/                     # Login, Register
│   ├── home/                     # Home + spotlight/trending rails
│   ├── discover/                 # Swipe deck
│   ├── details/                  # Drama details
│   ├── recommendations/          # Top 10 feed
│   ├── activity/                 # Activity timeline
│   ├── profile/                  # Profile + Edit Profile
│   └── common/                   # Loading / error / empty states
│
└── di/                            # Koin modules
```

---

## 🚀 Getting Started

### Prerequisites

- Android Studio (latest stable)
- JDK 11+
- A running instance of the [PHP backend](#) (XAMPP, Laragon, or any
  local PHP 8 server) with the schema from `database/schema.sql` imported

### Setup

1. **Clone the repo**
   ```bash
   git clone https://github.com/shahjalal-mahmud/hangug-deulama-android.git
   cd hangug-deulama-android
   ```

2. **Point the app at your local backend.**
   In `app/build.gradle.kts`, the debug build type sets:
   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2/hangug-api/public/\"")
   ```
   `10.0.2.2` is the Android emulator's alias for your machine's
   `localhost`. Adjust the path segment to match wherever your PHP
   project actually lives under your server root. Testing on a physical
   device instead? Swap it for your machine's LAN IP and make sure your
   PHP dev server is bound to `0.0.0.0`, not `127.0.0.1`.

3. **Run it.** Open in Android Studio, let Gradle sync, hit Run. The
   Home screen shows a live connection banner (🟢/🔴) confirming the app
   can reach your backend before you go further.

4. **Production builds** — set the real API domain in the `release`
   build type's `API_BASE_URL` before generating a signed release build.

---

## 🔌 Backend API

This app is a pure client against a documented REST API — see
[`docs/api.md`](docs/api.md) for the full 19-endpoint reference
(request/response samples, error codes, JWT auth flow, cURL examples).
Both this Android app and the companion React web app consume the exact
same contract; if you change one client's expectations, check the other.

---

## 🎨 Design System

Dark-only, built on a warm near-black "cinema" palette with rose
(`#FFB2B7`) as primary and gold (`#F1BF65`) as secondary — no Material
You dynamic color, no light theme. Typography pairs **Sora** (display)
with **Inter** (body). The full token set lives in
`presentation/theme/` and is meant to be the single source of truth —
no raw hex values should appear in a screen composable.

---

## 🗺 Roadmap

See [`Hangug-Deulama-Build-Roadmap.md`](docs/BUILD_ROADMAP.md)
for the full phase-by-phase build plan. Current status:

- [x] Project scaffolding, theme, navigation, live backend health check
- [ ] Authentication (register/login/session)
- [ ] Drama catalog + details
- [ ] Swipe deck
- [ ] Favorites / Watch Later / Watched + anonymous-mode sync
- [ ] Recommendations + genre statistics
- [ ] Profile management
- [ ] Activity timeline
- [ ] Polish pass (motion, accessibility, offline states)
- [ ] Testing + release prep

---

## 🧪 Testing

- **Domain/use cases** — plain JUnit5, no Android dependencies
- **Repositories** — JUnit + MockWebServer, fixtures taken directly from
  `docs/api.md`'s JSON samples
- **ViewModels** — JUnit + Turbine for `StateFlow` assertions
- **Compose UI** — `createComposeRule()` + semantics-based assertions
- **Swipe gesture** — instrumented tests via
  `performTouchInput { swipeLeft() / swipeRight() }`

```bash
./gradlew test               # unit tests
./gradlew connectedAndroidTest # instrumented tests
```

---

## 👤 Author

**Shahajalal Mahmud**
Founder & Technical Project Manager, [Appriyo](https://appriyo.com/)
[Portfolio](https://shahajalal.me) · [GitHub](https://github.com/shahjalal-mahmud)

---

## 📄 License

This project is currently unlicensed for public use. All rights reserved
unless a license file is added. *(Swap this section out once you've
picked a license — MIT is the common default for a portfolio project if
you want others to be able to fork/learn from it.)*