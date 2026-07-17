# 한 Hangug Deulama — Android

**A native Android client for discovering, swiping, and tracking Korean dramas — built to feel like the same product as its React web counterpart, not a reskin.**

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-26-blue)
![Target SDK](https://img.shields.io/badge/targetSdk-36-blue)
![Status](https://img.shields.io/badge/status-Phases%200--7%20shipped-brightgreen)

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

**Current build state:** Phases 0–7 of the build plan are shipped
end-to-end (auth, catalog, details, swipe deck, engagement queues,
recommendations, genre statistics, profile management, activity timeline).
Phase 8 (polish) is partially done. See the [Roadmap](#-roadmap) section
for the per-phase checklist.

---

## 📸 Screenshots

> Drop your emulator/device captures into the `screenshots/` folder
> using the filenames shown below — they're already linked from this
> README. A standard phone-density portrait capture (~1080×2400) at
> the device's natural aspect ratio is recommended.

### Discovery & recommendations

|               Home               |           Discover (swipe)            |                   Recommendations                   |
|:--------------------------------:|:-------------------------------------:|:---------------------------------------------------:|
|  ![Home](screenshots/home.png)   | ![Discover](screenshots/discover.png) | ![Recommendations](screenshots/recommendations.png) |
| Spotlight, trending, genre rails |        Tinder-style swipe deck        |              Top-10 personalised picks              |

### Catalog

|               Drama details                |           Activity timeline           |               Profile               |
|:------------------------------------------:|:-------------------------------------:|:-----------------------------------:|
| ![Drama details](screenshots/details.png)  | ![Activity](screenshots/activity.png) | ![Profile](screenshots/profile.png) |
| Banner, synopsis, cast, engagement actions |      Reverse-chronological feed       | Stats, top genres, raw profile data |

### Auth & supporting screens

|                 Login                  |               Register                |                 Edit profile                  |                   Genre breakdown                   |
|:--------------------------------------:|:-------------------------------------:|:---------------------------------------------:|:---------------------------------------------------:|
|    ![Login](screenshots/login.png)     | ![Register](screenshots/register.png) | ![Edit profile](screenshots/edit-profile.png) | ![Genre breakdown](screenshots/genre-breakdown.png) |
| Email + password, enumeration-safe 401 |     Full name + email + password      |         Avatar, name, password change         |            Per-genre preference scoring             |

> **Don't have captures yet?** See [`screenshots/README.md`](screenshots/README.md)
> for a checklist of what to snap and the recommended capture settings.

---

## ✨ Features

### Core experience

- **Swipe deck** — native drag gestures with velocity-based fling
  dismissal, rotation, and a stacked-card preview (not a copy of a CSS
  transform — built from scratch with Compose primitives)
- **Anonymous-first browsing** — swipe, favorite, and queue dramas
  before creating an account; everything syncs the moment you sign up
  or log in via the `EngagementSyncService` replay worker
- **Personalized recommendations** — a Top 10 feed that adapts to your
  swipe / favorite / watch history, with a clear cold-start fallback
  for new users and categorical badges (no leaked scoring formula)
- **Drama catalog with real pagination** — Paging 3 source streams the
  full catalog with sort + order + filter support

### Engagement tracking

- **Favorites, Watch Later, and Watched** with optimistic local UI,
  Room-backed offline queue, and automatic reconciliation on sign-in
- **Genre taste profile** — per-genre preference scoring (`likes +5,
  watched +2, disliked −3`, clamped at 0) computed server-side and
  rendered as an honest ranked breakdown
- **Activity timeline** — reverse-chronological merge of all three
  engagement lists, fetched in parallel and sorted server-side

### Profile & session

- **Full profile screen** — every field the backend returns is
  surfaced verbatim (id, name, email, image, liked_count,
  watched_count, favorite_genres), with loading / error / retry states
- **Edit profile** — name, password change (three-field rule
  short-circuited client-side), and avatar upload via the system photo
  picker with built-in compression (≤ 5 MB, JPEG q85, max edge 1024 px)
- **JWT auth** — HS256 tokens stored in DataStore with an in-memory
  cache for sync interceptor reads; `AuthInterceptor` reacts to 401s
  via an `AuthEventBus` that the nav graph subscribes to

### Design

- **Cinematic dark UI** — a bespoke rose/gold design system (no
  default Material You colors) ported 1:1 from the web app's token set
- **Animated transitions** — `AnimatedContent` cross-fades on Activity,
  spring-physics snap-back on the swipe deck, and LIKE / NOPE stamps
  tied to drag progress
- **Per-context empty states** — every list screen has a tailored
  copy and CTA (Discover / Recommendations / Activity / Genre Stats /
  Profile)

---

## 🛠 Tech Stack

| Layer         | Choice                                                                             |
|---------------|------------------------------------------------------------------------------------|
| Language      | Kotlin (100%)                                                                      |
| UI            | Jetpack Compose + Material 3 (custom theme, dark-only)                             |
| Architecture  | MVVM + Clean Architecture (`data` / `domain` / `presentation`)                     |
| Navigation    | Navigation-Compose, type-safe routes via `kotlinx.serialization`                   |
| DI            | Koin                                                                               |
| Networking    | Retrofit2 + OkHttp3 + kotlinx.serialization-converter                              |
| Async         | Kotlin Coroutines + Flow / StateFlow / SharedFlow                                  |
| Local storage | Room (offline queues) + Jetpack DataStore (session + UX flags)                     |
| Pagination    | Paging 3 (`DramaPagingSource`)                                                     |
| Images        | Coil3                                                                              |
| Backend       | Vanilla PHP 8 REST API, MySQL, JWT (HS256) auth — see [`docs/API.md`](docs/API.md) |

**SDK targets:** `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`
(Android 8.0+). `JVM target = 11`.

---

## 🏗 Architecture

Three layers, mirroring the same separation of concerns as the web app's
`api/` + `context/` split:

```
presentation/   → Composables + ViewModels, one feature per package
domain/         → Domain models + repository interfaces (pure Kotlin)
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

…so every screen, present and future, handles success / validation /
auth errors identically instead of each ViewModel reinventing it. The
`ApiResult` sealed type (`Success` / `ValidationError` / `Error` /
`NetworkError`) translates envelopes and HTTP exceptions into a single
ergonomic shape.

---

## 📁 Project Structure

```
app/src/main/java/com/appriyo/deulama/
├── HangugDeulamaApp.kt           # Application — boots Koin, primes session, starts sync service
├── MainActivity.kt               # Single-activity host, edge-to-edge
│
├── data/
│   ├── remote/                   # Retrofit APIs, DTOs, interceptors, NetworkModule, ApiResult
│   ├── local/datastore/          # SessionManager (JWT + cached user), AppPrefs (UX flags)
│   ├── local/db/                 # Room AppDatabase, DAOs, entities (offline queues)
│   ├── mapper/                   # DTO <-> domain converters
│   ├── repository/               # *RepositoryImpl + EngagementSyncService (replay-on-login)
│   └── util/                     # ImageValidator, ImageCompressor
│
├── domain/
│   ├── model/                    # Drama, User, Session, UserProfile, RecommendationSet,
│   │                             # GenreStatistics, EngagementEntry, SwipeRecord
│   └── repository/               # Auth, Drama, Engagement, GenreStats, Profile,
│                                 # Recommendations, Swipe, Timeline
│
├── presentation/
│   ├── navigation/               # NavGraph + typed Routes
│   ├── ui/theme/                 # Color, Type, Shape, Theme, Gradient, GlassOverlay
│   ├── components/               # DramaCard, DramaEngagementActions, StatusBanner,
│   │                             # SectionHeader, MatchScoreBadge, GenreChip, etc.
│   ├── auth/                     # Login + Register screens, AuthViewModel
│   ├── home/                     # Home + spotlight/trending/genre rails
│   ├── discover/                 # Swipe deck (Discover, SwipeCard, DeckAnimationController)
│   ├── details/                  # Drama details
│   ├── recommendations/          # Top-10 personalised feed
│   ├── activity/                 # Reverse-chronological activity timeline
│   ├── genre/                    # Per-genre preference breakdown
│   ├── profile/                  # Profile + Edit Profile screens
│   └── util/                     # ImageUrls (relative -> absolute URL helper)
│
└── di/                           # AppModules.kt — Koin modules
```

---

## 🚀 Getting Started

### Prerequisites

- Android Studio (latest stable)
- JDK 11+
- A running instance of the [PHP backend](#-backend-api) (XAMPP, Laragon,
  or any local PHP 8 server) with the schema from `database/schema.sql`
  imported

### Setup

1. **Clone the repo**
   ```bash
   git clone https://github.com/shahjalal-mahmud/hangug-deulama-android.git
   cd hangug-deulama-android
   ```

2. **Point the app at your local backend.**
   In `app/build.gradle.kts`, the debug build type sets:
   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.104/hangug-api/public/\"")
   ```
   - `192.168.1.104` is the host machine's LAN IP — fine for physical
     devices on the same Wi-Fi (with PHP bound to `0.0.0.0`).
   - On an emulator, swap it for `http://10.0.2.2/hangug-api/public/`
     (Android's alias for the host's `127.0.0.1`).
   - Adjust the path segment (`/hangug-api/public`) to match wherever
     your PHP project actually lives under your server root.

3. **Run it.** Open in Android Studio, let Gradle sync, hit Run. The
   Home screen shows a live connection banner (🟢/🔴) confirming the
   app can reach your backend before you go further.

4. **Production builds** — set the real API domain in the `release`
   build type's `API_BASE_URL` (the current value is still a
   `TODO-your-production-domain.com` placeholder) and flip
   `isMinifyEnabled = true` before generating a signed release build.

---

## 🔌 Backend API

This app is a pure client against a documented REST API — see
[`docs/API.md`](docs/API.md) for the full 19-endpoint reference
(request/response samples, error codes, JWT auth flow, cURL examples,
Postman collection). Both this Android app and the companion React web
app consume the exact same contract; if you change one client's
expectations, check the other.

---

## 🎨 Design System

Dark-only, built on a warm near-black "cinema" palette with rose
(`#FFB2B7`) as primary and gold (`#F1BF65`) as secondary — no Material
You dynamic color, no light theme. Typography pairs **Sora** (display)
with **Inter** (body). The full token set lives in `presentation/ui/theme/`
and is meant to be the single source of truth — no raw hex values
should appear in a screen composable.

---

## 🗺 Roadmap

See [`docs/BUILD_ROADMAP.md`](docs/BUILD_ROADMAP.md) for the full
phase-by-phase build plan. Current status:

- [x] **Phase 0** — Project scaffolding, theme, navigation, live backend health check
- [x] **Phase 1** — Authentication (register / login / session + JWT interceptor)
- [x] **Phase 2** — Drama catalog + details with Paging 3
- [x] **Phase 3** — Swipe deck (custom drag, rotation, stack depth, fly-off actions)
- [x] **Phase 4** — Favorites / Watch Later / Watched + anonymous-mode sync
- [x] **Phase 5** — Recommendations + genre statistics
- [x] **Phase 6** — Profile management (view + edit + avatar upload)
- [x] **Phase 7** — Activity timeline (favorites + watch-later + watched merged)
- [~] **Phase 8** — Polish pass (motion, accessibility, offline states)
      — partial: per-context empty states ✅, custom timing curves ✅,
      shimmer skeletons on Activity ✅, swipe-gesture TalkBack
      semantics ⏳, reduced-motion respect ⏳, global offline banner ⏳,
      encrypted session store ⏳
- [ ] **Phase 9** — Testing + release prep (unit tests / instrumented
      tests / proguard tuning / release signing) — dependencies wired,
      test code not yet authored

---

## 🧪 Testing

- **Domain / use cases** — plain JUnit5, no Android dependencies
- **Repositories** — JUnit + MockWebServer, fixtures taken directly
  from `docs/API.md`'s JSON samples
- **ViewModels** — JUnit + Turbine for `StateFlow` assertions
- **Compose UI** — `createComposeRule()` + semantics-based assertions
- **Swipe gesture** — instrumented tests via
  `performTouchInput { swipeLeft() / swipeRight() }`

```bash
./gradlew test                  # unit tests
./gradlew connectedAndroidTest  # instrumented tests
```

> **Status:** the dependencies for all of the above are wired into
> `app/build.gradle.kts`, but the actual test source sets
> (`app/src/test/` and `app/src/androidTest/`) have not yet been
> authored — see [`docs/UNIT_TEST_PLAN.md`](docs/UNIT_TEST_PLAN.md)
> and [`docs/INSTRUMENTED_TEST_PLAN.md`](docs/INSTRUMENTED_TEST_PLAN.md)
> for the planned coverage.

---

## 📚 Docs

- [`docs/API.md`](docs/API.md) — canonical REST API reference (19 endpoints)
- [`docs/BUILD_ROADMAP.md`](docs/BUILD_ROADMAP.md) — phase-by-phase build plan
- [`docs/ANDROID_APP_PLAN.md`](docs/ANDROID_APP_PLAN.md) — architecture + UX plan
- [`docs/MANUAL_TEST_PLAN.md`](docs/MANUAL_TEST_PLAN.md) — manual QA checklist
- [`docs/UNIT_TEST_PLAN.md`](docs/UNIT_TEST_PLAN.md) — unit test coverage plan
- [`docs/INSTRUMENTED_TEST_PLAN.md`](docs/INSTRUMENTED_TEST_PLAN.md) — instrumented test plan

---

## 👤 Author

**Shahajalal Mahmud**
Founder & Technical Project Manager, [Appriyo](https://appriyo.com/)
[Portfolio](https://shahajalal.me) · [GitHub](https://github.com/shahajalal-mahmud)

---

## 📄 License

This project is currently unlicensed for public use. All rights reserved
unless a license file is added. *(Swap this section out once you've
picked a license — MIT is the common default for a portfolio project if
you want others to be able to fork / learn from it.)*