# Hangug Deulama — Android Build Roadmap

A phase-by-phase guide to take the scaffold to a shippable app, ordered for
speed: each phase produces something you can actually see/click/test before
moving on, so you're never debugging three unfinished features at once.

**How to use this file:** work top to bottom. Don't skip ahead — later
phases assume earlier ones are done and tested. Each phase has a "Definition
of Done" — don't move on until you can check every box.

---

## ✅ Phase 0 — Scaffolding (done)

- [x] Gradle deps, Koin, Compose theme, nav graph with demo screens
- [x] `Envelope<T>` + `HealthApi` + live connection banner on Home
- [x] AAR metadata / compileSdk fixed
- [x] Room `@Database` stub compiles

You're here. Everything below builds on top of this.

---

## Phase 1 — Auth (real login/register, real session)

**Goal:** tap "Log In" and actually get a JWT back from your PHP API,
stored on-device, with the Home screen reflecting real signed-in state.

### Tasks
1. **Domain model** — `domain/model/User.kt` (`userId`, `fullName`, `email`, `profileImage`, `createdAt`).
2. **DTOs** — `AuthResponseDto` (`user`, `token`), reuse `Envelope<AuthResponseDto>`.
3. **Retrofit** — `AuthApi`: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/me`.
4. **`AuthRepository`** (interface in `domain/repository`, impl in `data/repository`):
    - `register(fullName, email, password, confirmation): Result<User>`
    - `login(email, password): Result<User>`
    - `logout()`
    - `currentSession(): Flow<Session?>` — reads from `SessionManager`
5. **Wire `AuthInterceptor`** for real: read the token out of `SessionManager` synchronously (use `runBlocking` on a `first()` read, or cache the token in a plain in-memory var updated whenever `SessionManager` writes — the in-memory cache is faster and avoids blocking the OkHttp thread).
6. **Global 401 handling** — add a `SharedFlow<AuthEvent>` (`AuthEventBus`), emit `SessionExpired` from wherever you detect a 401, collect it once at the nav-graph root to force back to Login.
7. **Replace demo Login/Register screens** with real forms:
    - Client-side validation matching the API exactly (see gotchas below)
    - Loading state on submit, disable button while in-flight
    - Map `422` → per-field error text, `401`/`409` → banner message
8. **`AuthViewModel`** shared via Koin `single` or scoped per-graph — exposes session state so `HangugNavGraph` can decide `startDestination` (Login vs Home) on app launch instead of always starting at Login.

### Gotchas from the API doc
- Login password validation is **1–255 chars, no minimum** — don't apply the 8-char register rule to the login form.
- Login error message is identical for wrong password vs unknown email (`"Invalid email or password."`) — don't leak which case occurred.
- Register auto-logs-in (token comes back on `201`) — no separate login call needed after registering.
- `full_name` 2–150, `email` ≤191, `password` 8–255 on register.

### Definition of Done
- [ ] Register a real account against your local API → lands on Home signed in.
- [ ] Kill and reopen the app → still signed in (session persisted).
- [ ] Log out → back to Login.
- [ ] Manually expire/corrupt the stored token → next protected call triggers the global-401 flow, not a crash.

**Time estimate:** 1–2 days.

---

## Phase 2 — Catalog + Drama Details (read-only, no auth needed)

**Goal:** real drama data on Home and Discover, tappable into a real
Details screen. This is public-endpoint-only, so it's a good next step —
no session complexity yet.

### Tasks
1. **`Drama` domain model** matching `Drama::publicItem()` shape exactly (see API doc `GET /api/dramas`).
2. **`DramaApi`** — `GET /api/dramas` (page, limit, sort, order), `GET /api/dramas/{id}`.
3. **Paging 3** — `DramaPagingSource` wired to `GET /api/dramas`; expose `Flow<PagingData<Drama>>` from `DramaRepository`.
4. **Replace Home's static content** with real rails: Trending (highest `imdb_rating`), placeholder Continue-Watching rail can stay static until Phase 4.
5. **Replace Discover** with a `LazyVerticalGrid`/list view of the catalog (list view only — no swipe yet, that's Phase 3).
6. **Real Drama Details screen** — poster/banner via Coil3, synopsis, genres, cast, info grid. Use `glass-overlay`-equivalent gradient scrim over the banner (already have `HangugGlassOverlay` in `theme/Gradient.kt`).
7. **`DramaCard` component** — one composable, `variant: Poster | Landscape | Compact` enum, so Home/Discover/Recommendations all reuse it later.

### Gotchas
- `sort`/`order` are a strict whitelist (`title|release_year|imdb_rating|created_at` × `asc|desc`) — sending anything else is a `422`. Hardcode the options, don't build a free-text sort field.
- `id` must be `>= 1` — guard against `0`/negative before calling.

### Definition of Done
- [ ] Home shows real dramas from your DB, scrollable, no crashes on empty/short lists.
- [ ] Tapping any poster opens Details with real data.
- [ ] Pull-to-refresh or scroll-to-load-more works via Paging 3.

**Time estimate:** 1–2 days.

---

## Phase 3 — The Swipe Deck (signature interaction)

**Goal:** the one screen worth spending real design time on.

### Tasks
1. Build the drag gesture directly with `pointerInput` + `detectDragGestures` (see plan doc §9) — don't pull in a third-party swipe library.
2. Rotation tied to horizontal offset, velocity-based fling dismissal (`VelocityTracker`), color-tinted overlay (mint = like, danger-red = dislike).
3. Card stack depth: render 2–3 cards behind the active one, scaled down + faded.
4. `SwipeApi` — `POST /api/swipe` with `{ drama_id, swipe_type }`.
5. Fire the API call **optimistically** on release — don't block the dismiss animation on the network response.
6. Action buttons row below the deck (Like/Dislike/Favorite/Watch Later/Watched) triggering the *same* animation path as a real swipe.
7. One-time coach-mark on first launch ("Swipe right to like, left to skip").

### Gotchas
- `POST /api/swipe` is a genuine upsert — `200` and `201` are **both success**, don't treat a re-swipe as an error.
- `swipe_type` is case-sensitive, exactly `like` or `dislike`.

### Definition of Done
- [ ] Swiping left/right visually feels good (rotation + fling both work, not just a snap).
- [ ] Re-swiping the same drama with the opposite type updates cleanly, no error toast.
- [ ] Rapid swiping doesn't crash or double-fire calls in a way that breaks state.

**Time estimate:** 1–2 days (budget extra here — this is the screen users will judge the app by).

---

## Phase 4 — Favorites, Watch Later, Watched (+ anonymous-mode Room queue)

**Goal:** engagement actions that work for both signed-in and anonymous
users, syncing on login.

### Tasks
1. `FavoritesApi`, `WatchLaterApi`, `WatchedApi` — straightforward CRUD-ish per the doc (remember: **no DELETE on Watched**).
2. Repositories: optimistic local update → reconcile with API response → rollback + snackbar on failure.
3. Room tables: `local_swipe`, `local_favorite`, `local_watch_later`, `local_watched` (this is where your `AppDatabase` placeholder entity gets replaced with real ones).
4. Anonymous-mode: while logged out, engagement actions write **only** to Room, no network calls to protected endpoints.
5. Sync-on-login: replay queued rows in order (`swipe` → `favorites` → `watch-later` → `watched`); treat `409` during replay as "already applied, fine" — not an error.
6. Wire these into Discover's action-button row and Drama Details' action bar.

### Gotchas
- `409` when the user *manually* re-favorites something they already favorited → toast + treat as soft success (desired end state is already true).
- Watched has **no un-watch** — don't build a delete affordance for it, ever.

### Definition of Done
- [ ] Anonymous: favorite a few dramas, log in → they appear as real favorites server-side.
- [ ] Signed-in: favoriting/un-favoriting updates instantly (optimistic), survives app restart.
- [ ] No duplicate-favorite crashes or confusing error toasts.

**Time estimate:** 1–2 days.

---

## Phase 5 — Recommendations + Genre Statistics

**Goal:** the personalization payoff — makes Phases 3–4 feel worth it.

### Tasks
1. `RecommendationsApi` — `GET /api/recommendations`, render up to 10 items with `MatchScoreBadge`.
2. Cold-start banner when `fallback: true` ("Swipe a few dramas to unlock personalized picks").
3. `GET /api/profile/genre-statistics` → render server-computed scores as-is (bar chart or ranked list) — **do not recompute the scoring formula client-side**, just display what's returned.
4. "Because you liked X" reasoning text on Details when arrived-from-Recommendations (simple: pass a flag/param through nav args).

### Gotchas
- Internal recommendation scores are **never exposed** by the API — don't try to reverse-engineer or display a raw number; `is_personalized`/`fallback` flags + the badge *concept* is all you get.

### Definition of Done
- [ ] Brand-new account sees `fallback: true` + top-rated dramas + the cold-start banner.
- [ ] After a handful of swipes/favorites, `is_personalized` flips to `true` and the list changes.

**Time estimate:** 0.5–1 day (mostly UI — the hard logic lives server-side already).

---

## Phase 6 — Profile Management

**Goal:** name/password/avatar editing, matching the web app's rules
exactly.

### Tasks
1. `ProfileApi` — `GET /api/profile`, `PUT /api/profile` (JSON path *and* multipart path — two Retrofit methods or one flexible one).
2. Real `EditProfileScreen`: name field, avatar picker (`ActivityResultContracts.PickVisualMedia`), password-change fields (current/new/confirm).
3. Client-side pre-validate image (JPG/JPEG/PNG/WebP, ≤5MB) before upload — but the server is final authority (`finfo_file()`, not extension trust).
4. Compress large images client-side (`Bitmap.compress`) before upload.
5. Reflect `updated_fields` from the response so you know what actually changed.

### Gotchas
- Partial password fields (e.g. only `current_password` sent) → `422`. All three or none.
- `default.png` avatar is never null — don't code a null-avatar fallback path.

### Definition of Done
- [ ] Change name only → works, `updated_fields: ["name"]`.
- [ ] Change password → old session's next protected call still needs the *same* token (token isn't invalidated by password change in this API) — confirm that's actually true in your build, and handle whichever way it behaves.
- [ ] Upload an oversized/wrong-type image → clean client-side rejection, not a raw 422 dump.

**Time estimate:** 1 day.

---

## Phase 7 — Activity Timeline

**Goal:** the easiest phase — mostly aggregation + list UI.

### Tasks
1. Combine swipes/favorites/watch-later/watched into one reverse-chronological feed (client-side merge of the four "list" endpoints you already built repositories for, sorted by their respective timestamp fields).
2. Empty-state prompting anonymous users to log in if they somehow land here (shouldn't happen given protected-route redirect, but handle it defensively).

### Definition of Done
- [ ] Timeline shows a believable mix of your test account's recent actions, newest first.

**Time estimate:** 0.5 day.

---

## Phase 8 — Polish Pass

**Goal:** the difference between "working" and "feels premium."

### Tasks
- [ ] Motion: apply `CubicBezierEasing(0.4f, 0f, 0.2f, 1f)` as the default easing everywhere.
- [ ] Respect reduced-motion (`Settings.Global.ANIMATOR_DURATION_SCALE == 0f`) — skip Ken Burns/floating/stagger, keep functional motion (swipe-dismiss, screen transitions).
- [ ] Shimmer loading skeletons instead of spinners on all list/catalog screens.
- [ ] Per-context empty states (empty favorites ≠ empty watch-later ≠ empty activity — different copy + CTA each).
- [ ] Offline banner (top, non-blocking) when cached data exists; full-screen retry only when there's truly nothing.
- [ ] TalkBack semantic actions on the swipe deck specifically — it's the one screen that isn't screen-reader-friendly by default.
- [ ] Sanity-pass every screen against the design tokens — no stray hex colors snuck in during feature work.

**Time estimate:** 1–2 days.

---

## Phase 9 — Testing + Release Prep

### Tasks
- [ ] Unit tests for repositories against MockWebServer using the exact JSON samples from the API doc.
- [ ] Instrumented test for the swipe gesture (`performTouchInput { swipeLeft() / swipeRight() }`).
- [ ] Run the same 10-step E2E walkthrough you used for the web app (register → swipe → favorites → recommendations → profile edit+upload → logout) manually at minimum, automate if time allows.
- [ ] ProGuard/R8 tuning for release — test a signed release build actually runs (multipart uploads + JWT parsing are common casualties of aggressive minification).
- [ ] Point `API_BASE_URL` release variant at your real production domain.
- [ ] Play Store listing assets (screenshots of the swipe deck + hero — that's your best marketing image).

**Time estimate:** 1–2 days.

---

## Suggested Timeline (solo, focused)

| Phase     | What                          | Days           |
|-----------|-------------------------------|----------------|
| 1         | Auth                          | 1–2            |
| 2         | Catalog + Details             | 1–2            |
| 3         | Swipe Deck                    | 1–2            |
| 4         | Engagement + anon sync        | 1–2            |
| 5         | Recommendations + Genre Stats | 0.5–1          |
| 6         | Profile Management            | 1              |
| 7         | Activity Timeline             | 0.5            |
| 8         | Polish                        | 1–2            |
| 9         | Testing + Release             | 1–2            |
| **Total** |                               | **~9–14 days** |

That's a realistic solo estimate working focused sessions, not 9-to-5
grinding — could compress to a week if you lean on AI pair-programming
for repository/DTO boilerplate (which is the most mechanical, lowest-risk
part of each phase) and spend your own attention on Phase 3 (swipe feel)
and Phase 8 (polish), which are the two phases users will actually notice.

---

## How to move fast without cutting corners

1. **One phase, one branch.** `feature/auth`, `feature/catalog`, etc. Merge to `main` only when that phase's Definition of Done is fully checked — keeps `main` always demo-able.
2. **Boilerplate first, by hand or AI, then verify against the API doc line-by-line** — DTOs/Retrofit interfaces are the highest-risk-of-silent-bug code (a typo'd field name just silently nulls out instead of erroring), so paste the actual JSON sample next to your DTO and check every field name once.
3. **Test against your real local PHP server continuously**, not mocks, until Phase 9 — you already have `HealthApi` proving connectivity; keep hitting real endpoints as you build each repository so DTO mismatches surface immediately instead of at integration time.
4. **Don't build Phase 5 (Recommendations) before Phase 3/4 have real data** — cold-start vs. personalized is meaningless to test without swipes/favorites already existing.
5. **Reuse `DramaCard` and `Envelope<T>` aggressively.** They're the two things every single later phase touches — get them right once in Phase 2, don't rebuild variants of them per screen.
6. **Skip Firebase Crashlytics until you're near Phase 9** — nice to have, zero value while you're still churning through core features.