# Hangug Deulama — Android App Build Plan

### Native Android Client for the Hangug Deulama K-Drama Recommendation Platform

**Target stack:** Kotlin + Jetpack Compose
**Backend:** Existing vanilla PHP 8 REST API (19 endpoints, JWT/HS256, MySQL)
**Companion clients:** React web app (already built) — this document exists so the Android app looks, feels, and behaves like the *same product*, not a reskin.

---

## Table of Contents

1. [Guiding Principles](#1-guiding-principles)
2. [Tech Stack](#2-tech-stack)
3. [App Architecture](#3-app-architecture)
4. [Project Structure](#4-project-structure)
5. [Networking Layer](#5-networking-layer)
6. [Auth & Session Strategy](#6-auth--session-strategy)
7. [Anonymous Mode & Local Persistence](#7-anonymous-mode--local-persistence)
8. [Screens & Navigation Map](#8-screens--navigation-map)
9. [The Swipe Deck (core interaction)](#9-the-swipe-deck-core-interaction)
10. [Design System — Making It Feel Like One Product](#10-design-system--making-it-feel-like-one-product)
11. [Component Inventory](#11-component-inventory)
12. [Error Handling & Response Envelope](#12-error-handling--response-envelope)
13. [Image Loading & Uploads](#13-image-loading--uploads)
14. [Offline / Loading / Empty States](#14-offline--loading--empty-states)
15. [Testing Strategy](#15-testing-strategy)
16. [Build Variants & Config](#16-build-variants--config)
17. [Suggested Development Phases](#17-suggested-development-phases)
18. [Things Easy to Miss](#18-things-easy-to-miss)

---

## 1. Guiding Principles

- **One backend, one contract.** The Android app talks to the exact same 19-endpoint API as the React app. Do not invent new response shapes — read `docs/api.md` as the single source of truth for both clients.
- **Native interaction, same product language.** Swiping should feel like a proper Android gesture (fling, rotation, velocity-based dismissal) — not a copy of a CSS transform. But colors, type, spacing, and voice must match the web app so a user switching between them doesn't feel like they left the brand.
- **Anonymous-first**, same as web: users can swipe, favorite, and browse before creating an account, then everything syncs on register/login.
- **Offline-tolerant, not offline-first.** Cache what's cheap (catalog pages, images), don't try to fully replicate a sync engine for v1.

---

## 2. Tech Stack

| Concern                      | Choice                                                                                                                           | Notes                                                          |
|------------------------------|----------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| Language                     | Kotlin                                                                                                                           | 100% Kotlin, no Java interop needed                            |
| UI                           | Jetpack Compose (Material 3)                                                                                                     | `androidx.compose.material3`, custom theme (see §10)           |
| Navigation                   | Navigation-Compose (`androidx.navigation:navigation-compose`)                                                                    | Type-safe routes via `kotlinx.serialization`                   |
| Architecture                 | MVVM + Clean Architecture (data / domain / presentation)                                                                         | Mirrors the web's `api/` module + Context split                |
| Async                        | Kotlin Coroutines + Flow / StateFlow                                                                                             | ViewModels expose `StateFlow<UiState>`                         |
| DI                           | KOIN                                                                                                                             | Standard for Compose apps this size                            |
| Networking                   | Retrofit2 + OkHttp3 + kotlinx.serialization (or Moshi)                                                                           | JSON envelope parsing centralized in one place                 |
| Auth storage                 | Jetpack DataStore (Preferences) + EncryptedSharedPreferences for the raw JWT                                                     | Never store JWT in plain SharedPreferences                     |
| Local cache / anonymous mode | DataStore (Proto or Preferences) for small flags; **Room** for swipe/favorite/watch-later queues made while offline or anonymous | Mirrors `localStorage` sync-on-login behavior                  |
| Image loading                | Coil3 (Compose-first, supports Compose Multiplatform if ever needed)                                                             | Handles poster/banner/avatar loading + caching + crossfade     |
| Pagination                   | Jetpack Paging 3 (`Paging3` + `PagingSource`)                                                                                    | For `/api/dramas` catalog paging                               |
| Gestures                     | Compose's own `pointerInput` / `Animatable` / `draggable` (no need for a 3rd-party swipe library — build it, it's ~150 lines)    | See §9                                                         |
| Forms/validation             | Plain Compose state + simple validators, mirrored 1:1 against the PHP `Validator` rules                                          | Keep client + server validation messages consistent            |
| Logging                      | Timber                                                                                                                           | Debug only                                                     |
| Crash/analytics (optional)   | Firebase Crashlytics                                                                                                             | Not required for MVP but recommended before Play Store release |
| Min/Target SDK               | minSdk 26 (Android 8.0), targetSdk latest stable                                                                                 | Matches Compose Material3 baseline comfortably                 |
| Build system                 | Gradle Kotlin DSL, version catalogs (`libs.versions.toml`)                                                                       | Keep dependency versions centralized                           |

---

## 3. App Architecture

Three layers, same separation of concerns as your React `api/` + `context/` split:

```
presentation/  → Composables + ViewModels (one per screen/feature)
domain/        → Use cases, domain models, repository interfaces (pure Kotlin, no Android deps)
data/          → Retrofit services, DTOs, Room DAOs, repository implementations, mappers
```

### Mapping web concepts → Android equivalents

| React / Web                                       | Android equivalent                                                                                                                                                                                  |
|---------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AuthContext.jsx`                                 | `AuthRepository` + `SessionManager` (DataStore-backed) + `AuthViewModel` shared via Hilt singleton scope                                                                                            |
| `DramaContext.jsx`                                | `DramaRepository` (catalog, favorites, watch-later, watched, swipes) — exposed per-screen via ViewModels, not one giant context                                                                     |
| `src/api/client.js` (Axios interceptors, 401 bus) | OkHttp `Authenticator`/`Interceptor` that attaches `Authorization: Bearer <jwt>` and a global `AuthEventBus` (`SharedFlow<AuthEvent.SessionExpired>`) that any screen can collect to force logout   |
| `src/api/*.js` per-resource modules               | Retrofit service interfaces: `AuthApi`, `DramaApi`, `FavoritesApi`, `WatchLaterApi`, `WatchedApi`, `SwipeApi`, `ProfileApi`, `RecommendationsApi`, `HealthApi`                                      |
| Optimistic UI updates                             | Repository updates local Room/StateFlow immediately, reconciles with API response, rolls back on failure with a snackbar                                                                            |
| `localStorage` anonymous sync                     | Room tables flagged `is_synced = false`; on login/register, a `SyncWorker` (or simple suspend function) replays queued swipes/favorites/watch-later/watched calls against the now-authenticated API |

### Recommended repository interfaces (domain layer)

```
AuthRepository        → register(), login(), logout(), currentSession(): Flow<Session?>
DramaRepository        → catalog(page, limit, sort, order): Flow<PagingData<Drama>>, details(id)
SwipeRepository         → swipe(dramaId, type): Result<SwipeResult>
FavoritesRepository    → add(), remove(), list(): Flow<List<Drama>>
WatchLaterRepository    → add(), remove(), list(): Flow<List<Drama>>
WatchedRepository       → add(), list(): Flow<List<Drama>>   // no remove — matches API
ProfileRepository       → get(), update(), genreStatistics()
RecommendationsRepository → topTen(): Flow<RecommendationResult>
```

---

## 4. Project Structure

```
app/
├── src/main/kotlin/com/yourorg/hangugdeulama/
│   ├── HangugDeulamaApp.kt              # @HiltAndroidApp
│   ├── MainActivity.kt                  # single-activity host
│   │
│   ├── data/
│   │   ├── remote/
│   │   │   ├── api/                     # AuthApi, DramaApi, SwipeApi, ...
│   │   │   ├── dto/                     # DramaDto, EnvelopeDto<T>, ErrorDto...
│   │   │   ├── interceptor/             # AuthInterceptor, ErrorInterceptor
│   │   │   └── NetworkModule.kt         # Hilt @Provides Retrofit/OkHttp
│   │   ├── local/
│   │   │   ├── db/                      # Room database, DAOs, entities
│   │   │   ├── datastore/               # SessionManager, AnonPrefsManager
│   │   │   └── paging/                  # DramaPagingSource, RemoteMediator (optional)
│   │   ├── mapper/                      # DTO ↔ domain model mappers
│   │   └── repository/                  # Repository implementations
│   │
│   ├── domain/
│   │   ├── model/                       # Drama, Swipe, Favorite, Profile, Recommendation...
│   │   ├── repository/                  # interfaces only
│   │   └── usecase/                     # e.g. SyncAnonymousActivityUseCase, GetTopTenUseCase
│   │
│   ├── presentation/
│   │   ├── navigation/                  # NavGraph.kt, Routes.kt (sealed/serializable)
│   │   ├── theme/                       # Color.kt, Type.kt, Shape.kt, Theme.kt, Gradient.kt
│   │   ├── components/                  # shared composables (DramaCard, MatchBadge, GenreBadge...)
│   │   ├── auth/                        # LoginScreen, RegisterScreen + ViewModels
│   │   ├── home/                        # HomeScreen + sections
│   │   ├── discover/                    # DiscoverScreen, SwipeDeck, SwipeCard
│   │   ├── details/                     # DramaDetailsScreen
│   │   ├── recommendations/             # RecommendationsScreen
│   │   ├── activity/                    # ActivityScreen
│   │   ├── profile/                     # ProfileScreen, EditProfileSheet
│   │   └── common/                      # LoadingState, ErrorState, EmptyState composables
│   │
│   └── di/                              # Hilt modules
│
├── build.gradle.kts
└── gradle/libs.versions.toml
```

---

## 5. Networking Layer

### Retrofit setup

- Base URL comes from `BuildConfig.API_BASE_URL`, set per build variant (see §16) — mirrors `VITE_API_BASE_URL`.
- One generic envelope wrapper, matching the API doc exactly:

```kotlin
@Serializable
data class Envelope<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val errors: JsonElement? = null   // can be {field: [msgs]} OR {code: "..."}
)
```

- A single `safeApiCall { }` wrapper (suspend inline function) that:
    1. Executes the Retrofit call
    2. Parses the envelope regardless of HTTP status (200/201 vs 4xx/5xx)
    3. Maps `errors.code == "auth.user_not_found"` → triggers global logout
    4. Maps `401` → emits `AuthEvent.SessionExpired` on a shared `SharedFlow`, mirroring the web's "global 401 listener"
    5. Returns a sealed `ApiResult<T>` (`Success`, `ValidationError(fieldMap)`, `Conflict`, `NotFound`, `Unauthorized`, `ServerError`)

### Interceptor chain

```
Request → AuthInterceptor (attaches Bearer token if session exists)
        → HttpLoggingInterceptor (debug builds only)
        → real call
Response → ErrorInterceptor / envelope parsing happens at repository level, not interceptor level
           (interceptors shouldn't throw domain exceptions — keep that in the repository)
```

### Per-endpoint mapping (reference table)

| Feature         | Retrofit interface   | Endpoints                                                        |
|-----------------|----------------------|------------------------------------------------------------------|
| Health          | `HealthApi`          | `GET /api/health`                                                |
| Auth            | `AuthApi`            | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/me` |
| Dramas          | `DramaApi`           | `GET /api/dramas`, `GET /api/dramas/{id}`                        |
| Favorites       | `FavoritesApi`       | `POST/GET /api/favorites`, `DELETE /api/favorites/{id}`          |
| Watch Later     | `WatchLaterApi`      | `POST/GET /api/watch-later`, `DELETE /api/watch-later/{id}`      |
| Watched         | `WatchedApi`         | `POST/GET /api/watched` (no delete)                              |
| Swipe           | `SwipeApi`           | `POST /api/swipe`                                                |
| Profile         | `ProfileApi`         | `GET/PUT /api/profile`, `GET /api/profile/genre-statistics`      |
| Recommendations | `RecommendationsApi` | `GET /api/recommendations`                                       |

`PUT /api/profile` needs **two** Retrofit methods (or one with `@Body Any`): a JSON path for name/password-only updates and a `@Multipart` path when an image is attached — same duality as the web's JSON-vs-multipart branch.

---

## 6. Auth & Session Strategy

- JWT stored via **EncryptedSharedPreferences** (or DataStore + Android Keystore-backed encryption) — never plain text.
- `SessionManager` exposes `Flow<Session?>` (`Session(userId, token, expiresAt)`), decoded from the JWT's own `exp`/`user_id` claims client-side so the app can proactively treat a token as expired without a round trip.
- On app start: read session → if present, hit `GET /api/me` once to confirm the token is still valid server-side (handles server-side revocation edge cases) → route to authenticated shell or anonymous shell accordingly.
- **Global session-expiry handling**: a top-level `LaunchedEffect` in `MainActivity` (or `NavGraph` root) collects `AuthEventBus.events`, and on `SessionExpired` clears the session, shows a snackbar ("Your session expired — please sign in again"), and pops the nav stack back to `Login`. This is the direct Compose equivalent of the web's global 401 listener.
- Login/Register screens reuse the exact validation constraints from the API doc (full_name 2–150, email ≤191, password 8–255, confirm-match) so users get instant feedback instead of waiting on a 422.

---

## 7. Anonymous Mode & Local Persistence

The web app keeps liked/disliked drama IDs mirrored in `localStorage` for anonymous users and syncs on login. Android equivalent:

- **Room** tables: `local_swipe`, `local_favorite`, `local_watch_later`, `local_watched` — each row has `drama_id`, `type`/`value`, `created_at`, `synced: Boolean`.
- While anonymous, all engagement actions write **only** to Room and update UI optimistically. No network calls to protected endpoints.
- On successful login/register:
    1. Read all unsynced local rows.
    2. Replay them against the real endpoints in order (`swipe` → `favorites` → `watch-later` → `watched`), respecting the API's own idempotency (swipe upserts; favorites/watch-later/watched return `409` on duplicates — treat `409` as "already applied, fine" during sync, not an error to surface).
    3. Mark rows `synced = true` (or clear them, since the server is now the source of truth going forward).
- After sync, switch the app's data source for these features from Room-only to Room-as-cache-of-network (repository pattern: network is truth, Room is offline cache/optimistic buffer).

---

## 8. Screens & Navigation Map

Single-Activity + Navigation-Compose graph. Bottom navigation bar mirrors the web's `BottomNav`.

### Navigation graph shape

```
RootNavGraph
├── AuthGraph (no bottom bar)
│   ├── Login
│   └── Register
│
└── MainGraph (bottom bar visible)
    ├── Home                     (bottom tab)
    ├── Discover                 (bottom tab)
    ├── Recommendations          (bottom tab)
    ├── Activity                 (bottom tab, protected → route to Login if anonymous)
    ├── Profile                  (bottom tab, protected → route to Login if anonymous)
    │
    ├── DramaDetails/{id}        (pushed from Home/Discover/Recommendations, no bottom bar or bar dimmed)
    └── EditProfile (modal/bottom sheet, pushed from Profile)
```

### Screen-by-screen breakdown

**1. Login**
- Email + password fields, inline validation matching API constraints.
- "Continue browsing without an account" link → drops straight into `Home` in anonymous mode (parity with web's anonymous-first flow).
- On success: store session, trigger anonymous-activity sync (§7), navigate to `Home`.

**2. Register**
- `full_name`, `email`, `password`, `password_confirmation`.
- Same 422 field-level error mapping as web.
- Auto-login on success (API returns a token on register too).

**3. Home**
- Hero section (rotating spotlight — Compose `HorizontalPager` with auto-advance, mirrors the web's Spotlight rail + progress segments).
- Genre pills row (`LazyRow` of filter chips → deep-links into Discover pre-filtered).
- Continue Watching rail (`LazyRow` of `DramaPosterCard`, sourced from watch-later / partially-engaged items).
- Trending rail (`LazyRow`, cold-start-safe — highest rated).
- Recommendation section (compact preview of Top 10, "See all" → Recommendations tab).

**4. Discover**
- Search bar (debounced, hits `GET /api/dramas` with a client-side title filter, or extend later with a server-side `q` param if you add one to the API).
- Category tabs / genre filter chips.
- Sort dropdown (`title`, `release_year`, `imdb_rating`, `created_at` × `asc`/`desc` — exact same whitelist as the API).
- **Swipe deck** — the core screen. See §9.
- Action buttons row below the deck: Like / Dislike / Favorite / Watch Later / Watched (so swipe isn't the *only* way to act — matches the web's parallel button affordances).
- Match-score badge shown on cards once the user has enough activity to be `is_personalized: true`.
- Swipe progress indicator + keyboard-hint equivalent → on Android this becomes a one-time coach-mark/tooltip on first launch ("Swipe right to like, left to skip") since there's no keyboard.

**5. Drama Details**
- Backdrop hero image (banner_url) with gradient scrim (reuse the web's `glass-overlay` gradient — see §10).
- Action bar: Favorite / Watch Later / Watched / Share.
- Synopsis, info grid (year, rating, genres), cast section (`LazyRow` of cast chips/cards).
- "Similar dramas" rail.
- Recommendation reason text when arriving from Recommendations ("Because you liked X").

**6. Recommendations**
- Top 10 grid/list with match-score badges.
- Cold-start banner when `fallback: true` / `is_personalized: false` ("Swipe a few dramas to unlock personalized picks — showing top-rated for now").

**7. Activity** *(protected — requires login)*
- Reverse-chronological timeline: swipes, favorites, watch-later adds/removes, watched marks.
- Empty state prompts anonymous users to log in if they somehow reach this tab.

**8. Profile** *(protected)*
- Avatar, name, email, `liked_count`, `watched_count`, top-3 favorite genres as chips.
- Genre statistics section (bar chart or ranked list, sourced from `/api/profile/genre-statistics` — `+5` like / `+2` watched / `−3` dislike, clamped at 0, exactly as the API computes it — **do not recompute client-side**, just render what the server returns).
- Edit Profile → bottom sheet or separate screen: name field, avatar picker (camera/gallery via `ActivityResultContracts.PickVisualMedia` or `TakePicture`), password-change fields (current/new/confirm).
- Logout button.

### Protected-route behavior

Mirrors the web's `ProtectedRoute` wrapper: attempting to open `Activity` or `Profile` while anonymous redirects to `Login` with a "sign in to continue" message, then returns to the originally requested destination after successful auth (store the pending destination in the nav back stack or a simple `pendingRoute` state).

---

## 9. The Swipe Deck (core interaction)

This is the one piece of native interaction design that deserves real care — it's the app's signature gesture, and it needs to feel *better* than a scaled-up web swipe, not just equivalent to it.

**Approach:** build it directly with Compose primitives rather than pulling in a third-party Tinder-clone library — the logic is small and you get full control over feel.

```kotlin
Box(
    Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDrag = { change, dragAmount ->
                change.consume()
                offsetX += dragAmount.x
                offsetY += dragAmount.y
            },
            onDragEnd = {
                when {
                    offsetX > swipeThreshold -> animateOffScreen(RIGHT) { onLike(drama) }
                    offsetX < -swipeThreshold -> animateOffScreen(LEFT) { onDislike(drama) }
                    else -> snapBack()
                }
            }
        )
    }
)
```

Key details to get right:

- **Rotation tied to horizontal offset** (`rotationZ = (offsetX / cardWidth) * maxRotationDegrees`), same visual language as most swipe UIs and consistent with the web's own swipe-card tilt.
- **Velocity-based fling detection**, not just distance — a fast flick should dismiss even under the distance threshold (`VelocityTracker`).
- **Color-tinted overlay** that fades in as the card is dragged — green/mint (`tertiary`) tint for like-direction, red/danger tint for dislike-direction — same semantic colors as the web's like/dislike states.
- **Card stack depth**: render the next 2–3 cards behind the active one, scaled down slightly (`scale = 0.95f`, `0.90f`) with reduced alpha, so users see what's coming — richer than a flat single-card web swipe and worth doing since it's cheap in Compose.
- **Optimistic + idempotent**: fire `POST /api/swipe` immediately on release (don't block the animation on the network); the endpoint's own upsert semantics (`200` vs `201`) mean a rapid re-swipe or retry is always safe.
- **Undo affordance** (optional nice-to-have, not in the API scope): keep the last dismissed card + its result in memory for a few seconds with a "Undo" snackbar action that just re-fires `POST /api/swipe` with the opposite type — the upsert makes this trivial.
- Buttons (Like/Dislike/Favorite/Watch Later/Watched) below the deck should trigger the *exact same* animation path as a real swipe, not a separate code path — keeps behavior consistent regardless of input method.

---

## 10. Design System — Making It Feel Like One Product

Your web app's theme lives in CSS custom properties (`@theme` block + `@plugin "daisyui/theme"`). Port these **1:1** into a Compose `ColorScheme` + typography set so the two clients are indistinguishable in spirit. Below is the direct token translation.

### 10.1 Color tokens → Compose

The web is dark-only (`prefersdark: true`, `color-scheme: dark`) — do the same on Android; don't build a light theme unless you plan to add one to the web too. Build a custom `ColorScheme`, not just Material3 defaults:

```kotlin
object HangugColors {
    // Background ramp — monotonic, matches --color-bg-base/elevated/elevated-2
    val BgBase          = Color(0xFF0B0708)
    val BgElevated       = Color(0xFF150F10)
    val BgElevated2      = Color(0xFF1E1516)

    // Surface ramp
    val Surface                 = Color(0xFF1C1011)
    val SurfaceDim               = Color(0xFF170B0C)
    val SurfaceBright            = Color(0xFF453536)
    val SurfaceContainerLowest   = Color(0xFF120A0A)
    val SurfaceContainerLow      = Color(0xFF211516)
    val SurfaceContainer         = Color(0xFF2A1C1D)
    val SurfaceContainerHigh     = Color(0xFF352627)
    val SurfaceContainerHighest  = Color(0xFF403132)

    // Primary — rose
    val Primary            = Color(0xFFFFB2B7)
    val PrimaryContainer    = Color(0xFFF55C6F)
    val OnPrimary            = Color(0xFF67001B)
    val OnPrimaryContainer   = Color(0xFF5B0017)
    val InversePrimary       = Color(0xFFB12941)

    // Secondary — gold
    val Secondary            = Color(0xFFF1BF65)
    val SecondaryContainer   = Color(0xFF7D5800)
    val OnSecondary           = Color(0xFF422D00)
    val OnSecondaryContainer  = Color(0xFFFFD284)

    // Tertiary — mint (watched / success accents)
    val Tertiary            = Color(0xFF6EDBA7)
    val TertiaryContainer    = Color(0xFF30A374)
    val OnTertiary            = Color(0xFF003824)

    // Status
    val Error        = Color(0xFFFFB4AB)
    val ErrorContainer = Color(0xFF93000A)
    val Success        = Color(0xFF4ADE80)
    val Danger          = Color(0xFFF45B69)   // dislike-swipe tint

    // Text — single source of truth, same as the web's derived-alias approach
    val TextPrimary    = Color(0xFFF7EDEE)
    val TextSecondary   = Color(0xFFB39B9C)
    val TextTertiary    = Color(0xFF8A7375)

    // Borders / outline
    val Outline         = Color(0xFFA7898B)
    val OutlineVariant   = Color(0xFF594142)
    val BorderSubtle     = Color(0x40403132)   // ~0.5 alpha of surface-container-highest
    val BorderStrong     = Color(0xA6594142)   // ~0.65 alpha
}

val HangugDarkColorScheme = darkColorScheme(
    primary = HangugColors.Primary,
    onPrimary = HangugColors.OnPrimary,
    primaryContainer = HangugColors.PrimaryContainer,
    onPrimaryContainer = HangugColors.TextPrimary,
    secondary = HangugColors.Secondary,
    onSecondary = HangugColors.OnSecondary,
    secondaryContainer = HangugColors.SecondaryContainer,
    tertiary = HangugColors.Tertiary,
    onTertiary = HangugColors.OnTertiary,
    background = HangugColors.BgBase,
    onBackground = HangugColors.TextPrimary,
    surface = HangugColors.Surface,
    onSurface = HangugColors.TextPrimary,
    surfaceVariant = HangugColors.SurfaceContainerHighest,
    onSurfaceVariant = HangugColors.TextSecondary,
    error = HangugColors.Error,
    errorContainer = HangugColors.ErrorContainer,
    outline = HangugColors.Outline,
    outlineVariant = HangugColors.OutlineVariant,
)
```

> **Rule of thumb:** every color used anywhere in the app should trace back to one of these tokens. If a designer/dev reaches for a raw hex mid-screen, that's a sign a token is missing from this object — add it here first, the same discipline the web CSS file already enforces with its "single source of truth" text tokens.

### 10.2 Typography

Web uses **Sora** (display/headings) + **Inter** (body/metadata/buttons). Bundle both as variable font files under `res/font/` and build a matching `Typography`:

```kotlin
val Sora = FontFamily(Font(R.font.sora_variable))
val Inter = FontFamily(Font(R.font.inter_variable))

val HangugTypography = Typography(
    displayLarge = TextStyle(fontFamily = Sora, fontSize = 40.sp, letterSpacing = (-0.01).em), // → --font-h1
    headlineMedium = TextStyle(fontFamily = Sora, fontSize = 24.sp),                            // → --font-h2
    titleMedium = TextStyle(fontFamily = Sora, fontSize = 18.sp),                                // → --font-card-title
    bodyLarge = TextStyle(fontFamily = Inter, fontSize = 16.sp),                                 // → --font-body-md
    bodyMedium = TextStyle(fontFamily = Inter, fontSize = 14.sp),                                // → --font-body-sm
    labelSmall = TextStyle(fontFamily = Inter, fontSize = 13.sp, letterSpacing = 0.08.em),        // → --font-metadata / eyebrow
    labelLarge = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) // → --font-button
)
```

Hero title (72px / 48px mobile in CSS) doesn't map to a standard Material role — define a custom `heroTitle` / `heroTitleCompact` `TextStyle` alongside the `Typography` object and reference it directly on the Home hero composable.

### 10.3 Shape & radius

```kotlin
val HangugShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // --radius-default (0.25rem)
    small       = RoundedCornerShape(8.dp),   // --radius-lg (0.5rem)
    medium      = RoundedCornerShape(12.dp),  // --radius-xl (0.75rem)
    large        = RoundedCornerShape(12.dp),
    extraLarge   = RoundedCornerShape(999.dp) // --radius-full — chips, pills, avatar
)
```

### 10.4 Signature brand elements to replicate

These are the details that make the two apps feel like the same product rather than "inspired by":

- **Gradient buttons.** The web's `.btn-gradient` is a 135° linear gradient `Primary → PrimaryContainer → Secondary` (rose → deep rose → gold) with a soft rose glow shadow. Recreate with a Compose `Brush.linearGradient` background on a custom `Button`, plus `Modifier.shadow` tuned to a rose-tinted ambient color (Compose shadows are gray by default — you'll want a custom drop-shadow via `graphicsLayer` + `Modifier.drawBehind` or a small elevation-overlay trick to get the tinted glow).
- **Ghost/ subtle button variants** — transparent or low-opacity-gradient backgrounds with a `BorderStrong` outline, used for secondary CTAs (`.btn-gradient-ghost`, `.btn-gradient-subtle`).
- **Match-score badge** — same rose→gold gradient background, used specifically on recommended drama cards. Keep this gradient visually distinct from the primary CTA gradient's direction/stops if you want it to read as a "badge," or reuse verbatim if you want maximum brand consistency — recommend reusing verbatim.
- **Eyebrow labels** — small, uppercase, letter-spaced, gold-colored (`Secondary`) section labels above headings (the web's bilingual "지금 인기 · TRENDING NOW" pattern). Worth keeping the Korean+English pairing on Android too — it's a distinctive brand voice choice, not incidental styling.
- **Glass overlay on hero/backdrop images** — a diagonal gradient scrim from `rgba(103,0,27,0.8)` (rose-black) to `rgba(21,15,16,0.95)` (near-black), used over the details backdrop and home hero so text stays legible over any poster art. Replicate with `Brush.linearGradient` over the `AsyncImage`.
- **Film grain texture** on hero backgrounds — very subtle (5% opacity noise overlay). Optional for v1; skip unless you want full parity, it's a nice-to-have polish detail, not a load-bearing brand element.
- **Motion language:** the web uses a consistent `cubic-bezier(0.4, 0, 0.2, 1)` ("cinematic ease") for nearly everything, plus specific named animations — hero fade/scale on load, floating brand mark, Ken Burns zoom on hero backdrops, story-style progress-bar fills for the spotlight carousel, staggered collage fade-ins. Match the easing curve at minimum: use `CubicBezierEasing(0.4f, 0f, 0.2f, 1f)` as your app-wide default `AnimationSpec` easing so transitions *feel* like the same hand designed them, even where the specific animation differs.
- **Respect reduced motion.** The web disables all animation under `prefers-reduced-motion`. On Android, check `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` (or simpler: honor the system's "Remove animations" accessibility setting) and skip non-essential motion (Ken Burns, floating, stagger) accordingly — keep functional motion like swipe-dismiss and screen transitions, since those aren't purely decorative.

### 10.5 Dark status bar / system UI

Since the app is dark-only, set `enableEdgeToEdge()` with dark icons off (light status/nav bar icons) and match the system bar color to `HangugColors.BgBase` so there's no visible seam at the top/bottom of the screen — a detail the web obviously doesn't need to think about but matters a lot for perceived polish on Android.

---

## 11. Component Inventory

Reusable composables to build once, matching the web's `components/` folders:

| Web component                                              | Compose equivalent                                                                                                                                                                |
|------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DramaCard`, `DramaPosterCard`, `LandscapeDramaCard`       | `DramaCard(drama, variant: Poster / Landscape / Compact)` — one composable, a variant enum, avoids duplicated layout logic                                                        |
| `GenreBadge`                                               | `GenreChip` (Material3 `AssistChip`, custom colors)                                                                                                                               |
| `MatchRing` / match-score badge                            | `MatchScoreBadge(score: Int)`                                                                                                                                                     |
| `SwipeCard`, `SwipeDeck`, `ActionButtons`, `SwipeProgress` | See §9                                                                                                                                                                            |
| `RecommendationBadge`                                      | Folded into `MatchScoreBadge`                                                                                                                                                     |
| `EmptyState`, `ErrorState`, `LoadingState`, `SkeletonCard` | `common/` composables — `LoadingState` uses shimmer brush, not a spinner, to match the web's skeleton-loading approach                                                            |
| `ImageWithSkeleton`                                        | Coil `AsyncImage` + `SubcomposeAsyncImage` placeholder = shimmer box                                                                                                              |
| `SectionHeader`                                            | `SectionHeader(eyebrow, title, seeAllAction)`                                                                                                                                     |
| `RevealSection` (scroll-reveal)                            | `Modifier` + `LaunchedEffect` visibility animation, or skip — scroll-reveal is a web-specific affordance; Android users don't need re-motivating content that's already on-screen |
| `BottomNav`                                                | Material3 `NavigationBar`                                                                                                                                                         |
| `ProfileMenu`                                              | `DropdownMenu` or a profile-tab-driven screen (Android convention favors a dedicated screen over a nav-bar dropdown)                                                              |

---

## 12. Error Handling & Response Envelope

Map every documented status code to a concrete UI behavior — don't leave any as "generic error toast":

| Status                                                                        | Meaning                                | UI behavior                                                                                                                                                                                                                                         |
|-------------------------------------------------------------------------------|----------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `200` / `201`                                                                 | Success                                | Proceed, update state                                                                                                                                                                                                                               |
| `401`                                                                         | Bad/expired/missing token              | Global session-expiry flow (§6)                                                                                                                                                                                                                     |
| `404` on `/api/me` or `/api/profile` with `errors.code = auth.user_not_found` | Account deleted server-side            | Force logout with a specific message ("Your account no longer exists"), not the generic session-expired copy                                                                                                                                        |
| `404` (drama not found, resource not found)                                   | Bad ID / stale cache                   | Inline error state on that screen only, don't force logout                                                                                                                                                                                          |
| `409`                                                                         | Duplicate favorite/watch-later/watched | If user-initiated: show a toast ("Already in your favorites") but treat as a soft success (the desired end state is already true — update UI optimistically as if it succeeded). If from anonymous-sync replay: silently treat as success, no toast |
| `422`                                                                         | Validation failure                     | Map `errors` field-map directly onto the relevant form field's error text — same field names as the API (`email`, `password`, `drama_id`, etc.), so client and server validation copy never drifts                                                  |
| `500`                                                                         | Server error                           | Generic "Something went wrong, please try again" snackbar + retry action where applicable                                                                                                                                                           |
| Network failure (no connectivity)                                             | —                                      | Distinct "You're offline" state, different from a 500 — check via `ConnectivityManager` before/around the call so you're not guessing from exception type alone                                                                                     |

Centralize this in one `ApiResult<T>` sealed class + one `ApiErrorHandler` composable/extension so every screen handles errors identically instead of each ViewModel reinventing it.

---

## 13. Image Loading & Uploads

- **Coil3** for `poster_url`, `banner_url`, `profile_image` — configure a shared `ImageLoader` with disk + memory cache, crossfade enabled (matches the web's `ImageWithSkeleton` fade-in).
- **Avatar upload** (`PUT /api/profile`, multipart): use `ActivityResultContracts.PickVisualMedia()` for gallery selection (modern Photo Picker, no storage permission needed on API 33+; falls back gracefully on older APIs). Client-side pre-validate against the same constraints the API enforces (JPG/JPEG/PNG/WebP, ≤5MB) so users get instant feedback instead of waiting on a 422 — but always let the server be the final authority (it uses `finfo_file()`, not extension/header trust, so don't assume client validation alone is sufficient).
- Compress/resize large images client-side before upload (e.g., via `Bitmap.compress` down to a reasonable max dimension) to avoid users hitting the 5MB cap unnecessarily on modern phone-camera resolutions.

---

## 14. Offline / Loading / Empty States

- **Loading:** shimmer skeleton cards (`SkeletonCard` equivalent), not spinners, for catalog/list screens — matches the web's `DetailsSkeleton` approach and feels faster.
- **Empty:** dedicated `EmptyState` composable per context — empty favorites, empty watch-later, empty activity timeline, cold-start recommendations each get their own copy and (where relevant) a CTA back into Discover.
- **Offline:** a lightweight top banner ("You're offline — showing cached results") rather than blocking the whole screen, when cached Room/Paging data is available; a full-screen `ErrorState` with retry only when there's truly nothing to show.
- **Paging 3 states** (`LoadState.Loading/Error/NotLoading`) wired directly into `LazyColumn`/`LazyRow` footer items for the catalog and any long list.

---

## 15. Testing Strategy

| Layer            | Approach                                                                                                                                                                                                                                                                |
|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Domain/use cases | Pure JUnit5 unit tests, no Android deps needed                                                                                                                                                                                                                          |
| Repositories     | JUnit + MockWebServer (fake the PHP API responses using the exact JSON samples from `docs/api.md` — they're already written for you)                                                                                                                                    |
| ViewModels       | JUnit + Turbine (for testing `StateFlow`/`Flow` emissions)                                                                                                                                                                                                              |
| Compose UI       | `createComposeRule()` + semantics-based assertions; screenshot tests (Paparazzi or Compose Preview screenshot testing) for the design-system components in §10–11 to catch visual drift from the web theme over time                                                    |
| Swipe gesture    | Instrumented UI test simulating drag gestures via `performTouchInput { swipeLeft() / swipeRight() }`, asserting the correct repository call fires                                                                                                                       |
| End-to-end smoke | Reuse the same 10-step walkthrough already documented for the web (`docs/LOCAL_SETUP.md`: register → swipe → favorites → recommendations → profile edit + image upload → logout) as your Android E2E test script too — same backend, same flow, just a different client |

---

## 16. Build Variants & Config

```kotlin
android {
    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2/hangug-api/public\"") // emulator → localhost
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://your-production-domain.com\"")
            isMinifyEnabled = true
            proguardFiles(...)
        }
    }
}
```

- Use `10.0.2.2` (not `localhost`) when pointing the emulator at a locally-running XAMPP/Laragon PHP server.
- For a physical test device on the same network, use your machine's LAN IP and ensure the PHP dev server / Apache is bound to `0.0.0.0`, not `127.0.0.1`.
- Add your production domain to the PHP backend's `config/app.php → cors.allowed_origins` if you ever load anything cross-origin from a WebView; for pure native Retrofit calls CORS doesn't apply, but keep it in mind if you add any web-based auth flow later.
- `network_security_config.xml`: allow cleartext traffic **only** for the debug variant / emulator loopback, never in release.

---

## 17. Suggested Development Phases

Mirrors the web project's own phased plan (§17 in your PROJECT.md) so both teams/timelines can reference the same milestones:

1. **Project scaffolding** — Gradle setup, Hilt, Navigation graph skeleton, theme tokens (§10) fully ported first, before any real screens — get the design system right early so every subsequent screen just consumes it.
2. **Networking + Auth** — Retrofit services, envelope parsing, `AuthRepository`, Login/Register screens, session persistence, global 401 handling.
3. **Catalog + Details** — `DramaRepository`, Paging 3 catalog, Home + Discover (list view first, no swipe yet), Drama Details screen.
4. **Swipe Deck** — the core gesture system (§9), wired to `POST /api/swipe`.
5. **Engagement actions** — Favorites, Watch Later, Watched — repositories, UI, optimistic updates, anonymous-mode Room queue + sync-on-login (§7).
6. **Recommendations + Genre Statistics** — Top 10 screen, cold-start fallback UI, Profile's genre-statistics section.
7. **Profile management** — edit name/password/avatar, image picker + upload + client-side pre-validation.
8. **Activity timeline** — aggregate view across swipes/favorites/watch-later/watched.
9. **Polish pass** — motion (§10.4), reduced-motion handling, empty/error/offline states (§14), accessibility (TalkBack labels on swipe deck especially — it's the one screen that isn't naturally screen-reader-friendly by default, so plan explicit semantic actions there).
10. **Testing + release prep** — full test suite (§15), Proguard/R8 tuning, Play Store listing assets, signing config.

---

## 18. Things Easy to Miss

- **Swipe endpoint is idempotent (upsert)** — the app should *never* treat a re-swipe of the same drama as an error; both `200` and `201` are success paths, differentiate only for analytics/logging, not for UI branching.
- **Watched has no DELETE** — don't build an "un-watch" button; the API doesn't support it and never will per spec. Reflect that intentional constraint in the UI (no delete affordance on watched items) rather than building a UI element that will always 405/404.
- **Login vs Register password rules differ** — login only requires non-empty (1–255 chars), registration requires 8+. Don't apply the 8-char minimum client-side on the login form or you'll block legitimately-created-elsewhere accounts... though in practice all accounts are created via this same registration flow, so this mostly matters for future flexibility (e.g., password resets via another channel).
- **Identical error message for wrong password vs unknown email** — replicate this exactly in copy ("Invalid email or password.") on the login form; don't let the Android app leak which case occurred through different UI treatment (e.g. different icon/color per case would defeat the server's anti-enumeration design).
- **Internal recommendation scores are never exposed** — don't try to reverse-engineer or display a numeric "score" anywhere; only show `is_personalized`/`fallback` flags and the match-score *badge* concept (which is a UI treatment, not the literal internal score).
- **Sort/order whitelist** — if you ever add a sort-picker UI, keep the options hardcoded to exactly `title|release_year|imdb_rating|created_at` × `asc|desc`; sending anything else gets a `422`, so don't build a free-text or dynamic sort field.
- **`default.png` avatar is never deleted server-side** — don't build client logic that assumes a user can ever end up with a null avatar_url; there's always a fallback image URL to load.
- **JWT TTL is 7 days** — decide your own re-auth UX for expiry (silent re-login isn't supported since there's no refresh-token endpoint in this API) — plan for the global-401 → back-to-Login flow being a real, expected occurrence in a 7-day-active user's life, not a rare edge case.