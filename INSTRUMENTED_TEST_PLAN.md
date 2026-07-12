# INSTRUMENTED_TEST_PLAN.md

Scope: `androidTest` (`app/src/androidTest/java/...`) only. Anything
genuinely requiring the Android runtime: real Room via
`Room.inMemoryDatabaseBuilder`, DataStore-Preferences on a real `Context`,
the Android `ContentResolver` for image validation, the OS-level photo
picker for `ActivityResultContracts`. Anything testable on the JVM with a
fake is in `UNIT_TEST_PLAN.md` instead — do not duplicate it here.

> **Library note** — currently `app/build.gradle.kts` only declares
> `androidx.test.ext:junit` and `androidx.test.espresso:espresso-core`.
> You will need to add:
> ```kotlin
> androidTestImplementation("androidx.room:room-testing:<ver>")
> androidTestImplementation("androidx.paging:paging-testing:<ver>")
> androidTestImplementation("io.mockk:mockk-android:<ver>")
> androidTestImplementation("com.google.truth:truth:<ver>") // optional
> androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:<ver>")
> ```
> `room-testing` is required for migration tests; `paging-testing` for
> §G; `mockk-android` improves fakes of platform types.

Tagging conventions are the same as `UNIT_TEST_PLAN.md`:

- **REGRESSION TEST — currently FAIL** — exercises a known ❌ in
  `PHASE_0_7_AUDIT.md`.
- **REGRESSION TEST — currently PASS** — exercises a ✅ behind one of the
  18 blocking issues.
- **PROTECTIVE TEST — currently PASS** — exercises a ✅ that does not sit
  directly behind a known bug.

---

## §A. `RoomMigrationTest.kt` (Bug #3, HIGH)

Path: `app/src/androidTest/java/com/appriyo/deulama/data/local/db/RoomMigrationTest.kt`
Backs: `data/local/db/DatabaseModule.kt` (uses
`fallbackToDestructiveMigration()`) and `data/local/db/AppDatabase.kt`
(`version = 1`).

This file **demonstrates Bug #3**. Today, the production configuration
deliberately wipes data on schema bump via
`fallbackToDestructiveMigration()` (`DatabaseModule.kt:30`). The test below
is the exact proof that data is destroyed; it should flip from FAIL to PASS
once real `Migration_1_2` objects replace the destructive fallback.

Use `Room.databaseBuilder(...).addMigrations(...)` with the real migrations
under test (initially none — the test should observe destructive behaviour).

- [ ] **`schema bump from 1 to 2 with fallbackToDestructiveMigration wipes queued rows`**
  (REGRESSION TEST — currently FAIL — Bug #3, HIGH)
  Steps:
    1. Build a v1 in-memory Room database (`DatabaseBuilder` with `version = 1`,
       `createFromAsset` not used since exportSchema is false — fall back to
       programmatic seeding). Seed all four tables with at least one row each.
    2. `db.close()`.
    3. Build a v2 database with `fallbackToDestructiveMigration()` (mirror the
       production config). Trigger the open (Room's internal
       `onDestructiveMigration` callback fires).
    4. Assert the tables are **empty** — i.e. data was destroyed. Currently
       PASS if you're literally mirroring prod behaviour; the test is
       reformulated below as the negative.
- [ ] **`schema bump from 1 to 2 with no migration objects + addMigrations(strict) throws IllegalStateException (data preserved on disk)`**
  (REGRESSION TEST — currently FAIL — Bug #3)
  Replace `fallbackToDestructiveMigration` with `addMigrations(...)` calling
  an empty list. Pre-seed v1 with 1 row per table, close, reopen as v2.
  Expect `IllegalStateException` from Room. After a real `Migration_1_2` is
  added, replace the expectation with: data survives.
- [ ] **`Migration_1_2_PRESENT preserves queued rows across the schema bump (when a real migration exists)`**
  (PROTECTIVE — currently FAIL until a migration is added)
  Once a `Migration_1_2` is authored, this test verifies the queued rows
  round-trip. Today the test is `@Ignore`'d.
- [ ] **`Room throws no error on initial install (no upgrade path)`**
  (PROTECTIVE — currently PASS)
- [ ] **`all four entity tables exist with the names from AppDatabase annotation`**
  (PROTECTIVE — currently PASS — `LocalSwipeEntity`, `LocalFavoriteEntity`,
  `LocalWatchLaterEntity`, `LocalWatchedEntity`)

**Per file: in-memory Room requirement.** `Room.inMemoryDatabaseBuilder`
inside `@Before`. Optional second DB built with `Room.databaseBuilder` on
a temp file for migrations across closed-instance boundaries.

---

## §B. `LocalFavoriteDaoUserIsolationTest.kt` (Bug #2, HIGH)

Path: `app/src/androidTest/java/com/appriyo/deulama/data/local/db/dao/LocalFavoriteDaoUserIsolationTest.kt`
Backs: `data/local/db/dao/LocalFavoriteDao.kt` and
`data/local/db/entity/LocalFavoriteEntity.kt`. Supports Bug #2 from the
instrumented side.

> **Big-picture:** Bug #2 today is "Room state bleeds across accounts"
> because `isFavoritedFlow(dramaId)` does not filter by user. The
> instrumented version of this test uses **real** Room (not a fake DAO) and
> demonstrates the contamination concretely. Mark the failing tests
> `@Ignore("fails until LocalFavoriteEntity gets a userId column AND
> LocalFavoriteDao.isFavoritedFlow gains a userId parameter AND the repo
> filters by userId")`.

Use `Room.inMemoryDatabaseBuilder` per @Before. The DAO today has methods:
`upsert`, `deleteByDrama`, `isFavoritedFlow(dramaId)`, `allOrderedFlow`.

### §B.1 Cross-user contamination (Bug #2)

- [ ] **`isFavoritedFlow returns the same value for two users when the local row was seeded for user A only — BUG #2 manifestation`**
  (REGRESSION TEST — currently FAIL — Bug #2)
  Seed `LocalFavoriteEntity(drama_id = 42, created_at = ...)` for "user A".
  Open a second fake user via a separate `SessionManager`-backed repo and
  call `isFavorited(42)`. Expect `true` today (the bug). After the fix
  (entity gets `userId`, DAO query takes `userId`), expect `false`.
  Mark `@Ignore` with that explanatory message until the fix lands.

- [ ] **`after logout-login as a different user, isFavorited reads true for dramas favorited by the previous user — BUG #2 manifestation`**
  (REGRESSION TEST — currently FAIL — Bug #2)
  Two `SessionManager` instances seeded in alternation. Verifies the
  contamination vector end-to-end. `@Ignore` until the fix.

### §B.2 Behavioural patterns the fix MUST preserve (currently PASS)

- [ ] **`upsert with the same primary key replaces, not duplicates`**
  (PROTECTIVE — currently PASS)
- [ ] **`deleteByDrama removes exactly the row matching that drama_id`**
  (PROTECTIVE — currently PASS)
- [ ] **`allOrderedFlow emits the rows in `created_at ASC` order`**
  (PROTECTIVE — currently PASS)
- [ ] **`isFavoritedFlow emits `true` while a row exists and `false` after deleteByDrama`**
  (PROTECTIVE — currently PASS — protects the Flow-observation pattern even
  after the schema fix)
- [ ] **`isFavoritedFlow emits `false` for a drama with no row`**
  (PROTECTIVE — currently PASS)
- [ ] **`Room callback / InvalidationTracker fires after deleteByDrama`**
  (PROTECTIVE — currently PASS — relevant to the Phase-4 sync-flow flicker
  bug noted in the audit)
- [ ] **`Room callback / InvalidationTracker fires after upsert`**
  (PROTECTIVE — currently PASS)

The same shape repeats for `LocalSwipeDao`, `LocalWatchLaterDao`,
`LocalWatchedDao` — point at the same file naming pattern with the table
prefix adjusted. Cross-cutting items:

- [ ] **`LocalSwipeDao.allOrderedFlow emits rows in `created_at ASC` order`**
  (PROTECTIVE — currently PASS)
- [ ] **`LocalSwipeDao.deleteByDrama removes only the matching row`**
  (PROTECTIVE — currently PASS)
- [ ] **`LocalWatchLaterDao.isQueuedFlow follows the same true/false semantics as Favorites`**
  (PROTECTIVE — currently PASS)
- [ ] **`LocalWatchedDao.isMarkedWatchedFlow follows the same true/false semantics`**
  (PROTECTIVE — currently PASS)
- [ ] **`LocalWatchedDao persists a row whose `created_at` column is a Long, not a String`**
  (PROTECTIVE — currently PASS — guards against a future migration regression)

---

## §C. `SessionManagerPersistenceTest.kt`

Path: `app/src/androidTest/java/com/appriyo/deulama/data/local/datastore/SessionManagerPersistenceTest.kt`
Backs: `data/local/datastore/SessionManager.kt`. Indirectly supports **Bug
#4** (session-expired flow must clear the session, not just null a cached
variable).

Uses a real `Context` (via `androidx.test.core.app.ApplicationProvider`).

### §C.1 DataStore durability across process death

- [ ] **`saveSession persists the JWT across simulated process death`**
  (PROTECTIVE — currently PASS)
  Steps:
    1. `ApplicationProvider.getApplicationContext()`
    2. `SessionManager(ctx).saveSession(Session(User(...), "token-xyz"))`
    3. Close any references to the manager.
    4. Build a fresh `SessionManager(ctx)` on the same context; assert
       `currentSession()?.token == "token-xyz"`.
- [ ] **`saveSession persists the user object including profileImage`**
  (PROTECTIVE — currently PASS — guards the `default.png` contract)
- [ ] **`saveSession persists user fields independently of the in-memory cache prime`**
  (PROTECTIVE — currently PASS)
- [ ] **`prime pre-fills the in-memory cache from DataStore on first call`**
  (PROTECTIVE — currently PASS)
- [ ] **`currentSession() returns null before prime has completed`**
  (PROTECTIVE — currently PASS — `SessionManager.kt:31` notes this)

### §C.2 clear()

- [ ] **`clear() empties the DataStore preferences and the in-memory cache`**
  (PROTECTIVE — currently PASS)
  Asserts that on a fresh `SessionManager(ctx)` after `clear()`, both
  `currentToken()` and `currentSession()` are null AND that re-opening the
  underlying `preferencesDataStore` returns an empty `Preferences`.
- [ ] **`clear() is idempotent (calling twice does not throw)`**
  (PROTECTIVE — currently PASS)
- [ ] **`clear() removes the JWT but leaves other unrelated prefs keys intact (none today, but pin it for the EncryptedSharedPreferences swap)`**
  (PROTECTIVE — currently PASS)

### §C.3 Session-expired path (supports Bug #4)

- [ ] **`after clear(), AuthInterceptor.currentToken() returns null (genuinely zeroed)`**
  (PROTECTIVE — currently PASS — required for Bug #4 fix)
  Calling `clear()` MUST zero the token before `navController.navigateToAuthGraph()`
  fires, so the Login screen doesn't fire an authenticated request on its
  own behalf.
- [ ] **`saveSession then clear, then saveSession again — second session reads correctly from DataStore`**
  (PROTECTIVE — currently PASS)

---

## §D. `EngagementSyncServiceIntegrationTest.kt`

Path: `app/src/androidTest/java/com/appriyo/deulama/data/repository/EngagementSyncServiceIntegrationTest.kt`
Backs: `data/repository/EngagementSyncService.kt` end-to-end with a real
Room database.

The Phase-4 audit mentions a flicker bug ("Room flows don't invalidate
after sync deletes the rows"). This file is where that flicker is pinned.

### §D.1 Replay ordering

- [ ] **`replayAll() processes tables in the documented order: swipes → favorites → watch-later → watched`**
  (PROTECTIVE — currently PASS)
  Use a `LinkedHashSet` of `engagementKind` strings fed through a fake API
  recorder; assert insertion order.

- [ ] **`replayAll() iterates rows per table in `created_at ASC` order`**
  (PROTECTIVE — currently PASS)

### §D.2 Row deletion on success

- [ ] **`after replayAll() with all 4 APIs returning Success, the corresponding local rows are deleted`**
  (PROTECTIVE — currently PASS)
  Steps:
    1. Seed each table with 2 rows.
    2. Fake each API to return `ApiResult.Success`.
    3. Call `replayAll()`.
    4. Query each DAO's `allOrderedFlow().first()` (or `.size()`) and
       assert zero rows remain.
- [ ] **`after replayAll() the local rows are deleted AND any active InvalidationTracker observers re-fire with the new state`**
  (PROTECTIVE — currently PASS — pins the de-flicker half of Phase-4)
- [ ] **`on ApiResult.Error or NetworkError the row is left in place for retry`**
  (PROTECTIVE — currently PASS)
- [ ] **`on ApiResult.Error with HTTP 409 the row is left in place too (treatAlreadyAppliedAsSuccess does NOT fire on a non-2xx returning Error)`**
  (PROTECTIVE — currently PASS — pins the rule)

Actually re-read carefully: `treatAlreadyAppliedAsSuccess` collapses 409/404
into Success; if it does, the row IS deleted. The test above needs to be
inverted for 409: confirm the row IS deleted after a 409 (because the
collapse treats it as success). Two corollaries:

- [ ] **`treatAlreadyAppliedAsSuccess with HTTP 409 deletes the local row (collapse to Success)`**
  (PROTECTIVE — currently PASS)
- [ ] **`treatAlreadyAppliedAsSuccess with HTTP 404 deletes the local row (collapse to Success)`**
  (PROTECTIVE — currently PASS — and pins the no-unwatch contract for
  Watched)

### §D.3 No network calls for anonymous users

- [ ] **`replayAll() makes no API calls when SessionManager.sessionFlow emits null`**
  (PROTECTIVE — currently PASS)
- [ ] **`replayAll() makes exactly one attempt per queued row when SessionManager.sessionFlow emits non-null`**
  (PROTECTIVE — currently PASS)

### §D.4 Concurrent sync / no double-fire

- [ ] **`replayAll() during an in-flight replay is a no-op (isSyncing guard)`**
  (PROTECTIVE — currently PASS — `EngagementSyncService.kt:81`)

---

## §E. `AuthEventSessionExpiredDispatchTest.kt`

Path: `app/src/androidTest/java/com/appriyo/deulama/data/remote/AuthEventSessionExpiredDispatchTest.kt`
Backs: `data/remote/AuthEventBus.kt` + `data/remote/interceptor/AuthInterceptor.kt`.
Supports **Bug #4**.

Use `okhttp3.mockwebserver.MockWebServer` and the real `OkHttpClient`
configured for the auth network module. This is the only place we can
realistically test the entire interceptor → event bus pipeline without
walking Compose UI.

- [ ] **`401 on a protected endpoint emits AuthEvent.SessionExpired on AuthEventBus`**
  (REGRESSION TEST — currently PASS — supports Bug #4)
- [ ] **`401 on /api/auth/login does NOT emit AuthEvent.SessionExpired`**
  (PROTECTIVE — currently PASS — pins the avoid-logout-on-bad-creds rule)
- [ ] **`401 on /api/auth/register does NOT emit AuthEvent.SessionExpired`**
  (PROTECTIVE — currently PASS)
- [ ] **`403 / 404 / 500 do NOT emit AuthEvent.SessionExpired`**
  (PROTECTIVE — currently PASS)
- [ ] **`Authorization header is added even for endpoints that ultimately return 401`**
  (PROTECTIVE — currently PASS)

### §E.1 NavGraph collector indirect test

The `NavGraph` collector is `@Composable` (out of scope for unit / android
tests), but the underlying state transitions ARE testable. The
`AuthViewModel.logout()` half can be covered via a fake VM that mirrors
the same call (already covered in `UNIT_TEST_PLAN.md §H`). The
`navigateToAuthGraph()` half is a Compose navigation call and is exempt.

---

## §F. `ImageValidatorCompressorTest.kt` (Bug #13)

Path: `app/src/androidTest/java/com/appriyo/deulama/data/util/ImageValidatorCompressorTest.kt`
Backs: `data/util/ImageValidator.kt` and `data/util/ImageCompressor.kt`.

This is the only place `BitmapFactory.decodeStream` and
`ContentResolver` are usable. Backed-by `androidx.test.core.app`'s ability
to construct a real `ContentResolver`.

### §F.1 Validator

- [ ] **`validate(<5 MB JPEG>) returns success`**
  (PROTECTIVE — currently PASS)
- [ ] **`validate(>5 MB raw bytes) returns size failure`**
  (PROTECTIVE — currently PASS — guards the ≤5MB cap)
- [ ] **`validate(<1 KB GIF) returns mime failure (only {jpeg, png, webp} allowed)`**
  (PROTECTIVE — currently PASS)
- [ ] **`validate(empty stream) returns failure with empty-bytes message`**
  (PROTECTIVE — currently PASS)

### §F.2 Compressor

- [ ] **`compress returns JPEG bytes ≥ the raw input size after downscale`**
  (PROTECTIVE — currently PASS)
- [ ] **`compress on a 4096×4096 input yields an output whose longest edge is ≤ 1024 px`**
  (PROTECTIVE — currently PASS)
- [ ] **`compress on a 1024×768 input (already under cap) returns bytes within ±10% of input size`**
  (PROTECTIVE — currently PASS)
- [ ] **`compress on a content URI whose second decode returns null surfaces an error, not 0-byte output — BUG #13`**
  (REGRESSION TEST — currently FAIL — Bug #13)
  Steps:
    1. Build a stub `InputStream` that returns non-zero on the first
       `decodeStream` (bounds) and zero-length / null on the second
       (pixels). Mock the URI via
       `androidx.test.core.app.ApplicationProvider.getApplicationContext()`.
    2. Call `ImageCompressor.compress(...)`.
    3. Today: returns an empty `ByteArray` silently. Assert
       `byteArray.isNotEmpty()` is **false** (the bug).
    4. After the fix, assert the compressor throws or returns a tagged
       failure that the VM can map to `imageError`.

### §F.3 EditProfile integration

- [ ] **`EditProfileViewModel.onImagePicked with the failing URI populates imageError rather than uploading 0-byte file — BUG #13`**
  (REGRESSION TEST — currently FAIL — Bug #13; counterpart of F.2 from the
  VM side. Requires `mockk-android` to fake `BitmapFactory`.)

---

## §G. `DramaPagingSourceInstrumentedTest.kt` (no current bug)

Path: `app/src/androidTest/java/com/appriyo/deulama/data/remote/DramaPagingSourceInstrumentedTest.kt`
Backs: `data/remote/DramaPagingSource.kt`.

Uses `androidx.paging:paging-testing` and
`okhttp3.mockwebserver.MockWebServer`. All items here are PROTECTIVE and
do not currently back a known bug:

- [ ] **`paging source issues GET /api/dramas with page=1 on first load`**
  (PROTECTIVE — currently PASS)
- [ ] **`paging source appends page=N+1 after a successful page=N load`**
  (PROTECTIVE — currently PASS)
- [ ] **`paging source reaches endOfPagination when the server returns fewer items than page size`**
  (PROTECTIVE — currently PASS)
- [ ] **`paging source on 422 returns LoadResult.Error with the parsed ValidationError`**
  (PROTECTIVE — currently PASS)
- [ ] **`paging source on HTTP 401 returns LoadResult.Error without crashing`**
  (PROTECTIVE — currently PASS)
- [ ] **`paging source on IOException returns LoadResult.Error (Paging convention)`**
  (PROTECTIVE — currently PASS)

> Note: when Bug #9 lands (search / sort / genre), add tests:
>
> - `paging source sends `q=foo` query param when `searchQuery` is set`
> - `paging source sends `sort=title&order=asc` query params when sort is set`
> - `paging source sends `genre=Romance` query param when genre is set`

Mark these `@Ignore` until Bug #9 is implemented.

---

## §H. `MappersPlaceholderCleanupTest.kt` (cross-cutting)

Path: `app/src/androidTest/java/com/appriyo/deulama/data/remote/MappersPlaceholderCleanupTest.kt`
Backs: `data/remote/MappersPlaceholder.kt` (audit cross-cutting — slated
for deletion).

- [ ] **`data/remote/MappersPlaceholder.kt does not exist on disk`**
  (REGRESSION TEST — currently FAIL — cross-cutting finding)
  This test asserts `File("...src/main/.../MappersPlaceholder.kt").exists()`
  is `false`. Today it returns `true`, so the test fails. After deletion,
  it passes. Catches accidental re-addition in the future.

(If the file is gone at test-run time before this lands, treat the test as
PROTECTIVE — currently PASS.)

---

## §I. Skip list (do NOT test here)

These are explicitly **out of scope** for the instrumented-test plan:

- Any Compose `@Composable` rendering function (covered by `MANUAL_TEST_PLAN.md`).
- Coil image-loader behavior in production (covered by
  `MANUAL_TEST_PLAN.md` Step 26's "Discard" check and Bug #15's eye-test).
- Auth flow end-to-end against the live PHP backend (covered by
  `MANUAL_TEST_PLAN.md`).
- Any logic that doesn't need a real `Context`, real Room, or real
  DataStore — that lives in `UNIT_TEST_PLAN.md`.

Phases / files that genuinely have **nothing** here:

- **Phase 5 (Recommendations / Genre Stats)** — these have no
  Android-runtime-only logic beyond what `UNIT_TEST_PLAN.md §L` already
  covers. The 401 mapping bug (#11) is a VM-level decision, not a Room /
  DataStore decision. Mark this section as "intentionally empty".

---

## "Write In This Order"

Same priority convention as `UNIT_TEST_PLAN.md`: follow the audit's
Blocking Issues numbering (1 → 18) before protective coverage.

1. **`RoomMigrationTest.kt` — schema bump from 1 to 2 with `fallbackToDestructiveMigration` destroys data, demonstrating Bug #3** (Bug #3, HIGH)
2. **`RoomMigrationTest.kt` — schema bump with empty migration list throws IllegalStateException (proves today the only path forward is destructive)** (Bug #3)
3. **`RoomMigrationTest.kt` — Migration_1_2 preserves queued rows** (will be `@Ignore` until a real migration is authored; flips to PASS once Bug #3 is fixed)
4. **`LocalFavoriteDaoUserIsolationTest.kt` — isFavoritedFlow returns true cross-user** (Bug #2, HIGH)
5. **`LocalFavoriteDaoUserIsolationTest.kt` — after logout/login as different user, contamination persists** (Bug #2)
6. **`LocalFavoriteDaoUserIsolationTest.kt` — DAO behavioural patterns the fix must preserve** (✅ verified, protective)
7. **`SessionManagerPersistenceTest.kt` — clear() zeros the DataStore key genuinely** (supports Bug #4 path)
8. **`SessionManagerPersistenceTest.kt` — saveSession survives simulated process death** (✅ verified, protective)
9. **`EngagementSyncServiceIntegrationTest.kt` — replayAll ordering and per-table row deletion** (✅ verified, protective)
10. **`EngagementSyncServiceIntegrationTest.kt` — InvalidationTracker fires after sync deletes rows** (✅ verified, protective — pins the de-flicker half)
11. **`AuthEventSessionExpiredDispatchTest.kt` — 401 emits SessionExpired** (✅ verified, supports Bug #4)
12. **`AuthEventSessionExpiredDispatchTest.kt` — 401 on /api/auth/login does NOT emit SessionExpired** (✅ verified, protective)
13. **`ImageValidatorCompressorTest.kt` — compress surfaces error on null second decode** (Bug #13)
14. **`ImageValidatorCompressorTest.kt` — EditProfileViewModel consumes the failure, populates imageError** (Bug #13)
15. **`ImageValidatorCompressorTest.kt` — MIME whitelist + size cap regression** (✅ verified, protective)
16. **`DramaPagingSourceInstrumentedTest.kt` — paging load + append + end-of-pagination** (✅ verified, protective)
17. **`DramaPagingSourceInstrumentedTest.kt` — 422 / 401 / IOException → LoadResult.Error** (✅ verified, protective)
18. **`DramaPagingSourceInstrumentedTest.kt` — search / sort / genre placeholder tests** (`@Ignore` until Bug #9 ships)
19. **`MappersPlaceholderCleanupTest.kt` — file is absent** (cross-cutting cleanup)
20. **`LocalSwipeDaoUserIsolationTest.kt`, `LocalWatchLaterDaoUserIsolationTest.kt`, `LocalWatchedDaoUserIsolationTest.kt`** — same shape as `LocalFavoriteDaoUserIsolationTest.kt` (Bug #2 repeats across all four tables)

Done when all of the above are either green (PASS) or explicitly
red-by-design (FAIL — `@Ignore`'d with a comment that names the bug they
exist to prove).