# UNIT_TEST_PLAN.md

Scope: pure-JVM unit tests (`app/src/test/java/...`). JUnit4 + MockK + Turbine
+ `kotlinx-coroutines-test` + Room in-memory via `Room.inMemoryDatabaseBuilder`
is **not** used here — Room lives in `INSTRUMENTED_TEST_PLAN.md`. All network
and storage dependencies are faked.

> **Library note** — currently `app/build.gradle.kts` only declares JUnit as
> a `testImplementation`. Before any test below can compile you must add:
> ```kotlin
> testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:<ver>")
> testImplementation("app.cash.turbine:turbine:<ver>")
> testImplementation("io.mockk:mockk:<ver>")
> testImplementation("androidx.arch.core:core-testing:<ver>") // InstantTaskExecutorRule
> ```
> Versions are pinned in `gradle/libs.versions.toml` if you prefer the
> version-catalog convention. This is not a behaviour change to the app; it
> only unblocks the test source set.

Each test below is tagged with one of:

- **REGRESSION TEST — currently FAIL** — exercises a known ❌ from
  `PHASE_0_7_AUDIT.md`. Will turn green once the corresponding bug is fixed.
  Do NOT edit the test when it fails — fix the implementation.
- **REGRESSION TEST — currently PASS** — exercises a ✅ behind one of the
  18 blocking issues. Protects the surrounding code from being broken by the
  eventual fix.
- **PROTECTIVE TEST — currently PASS** — exercises a ✅ that does not sit
  directly behind a known bug. Locks in the current contract so future
  refactors can't silently break it.

File paths mirror `src/main` under `src/test`. Test method names use backtick
descriptions so the test report reads as plain English.

---

## A. `data/remote/ApiResultTest.kt`

Path: `app/src/test/java/com/appriyo/deulama/data/remote/ApiResultTest.kt`
Backs: `data/remote/ApiResult.kt` (`safeApiCall`).

This file does not back any single blocking issue but is the single chokepoint
for **Bug #14 (failure snackbars fall through to blank "Couldn't save that
action.")** — the fix for #14 lives inside `safeApiCall`'s `HttpException`
branch. Without these tests, a fix to #14 cannot be verified without a live
server.

- [ ] `safeApiCall returns Success when envelope is success=true with non-null data`
  (PROTECTIVE — currently PASS)
- [ ] `safeApiCall returns Error with envelope message when envelope is success=false`
  (PROTECTIVE — currently PASS)
- [ ] `safeApiCall returns Error with fallback when envelope success=false and message is blank`
  (PROTECTIVE — currently PASS — pins the existing `ifBlank { "Request failed" }`)
- [ ] `safeApiCall returns Error "Empty response body" when envelope success=true with null data`
  (PROTECTIVE — currently PASS)
- [ ] `safeApiCall returns ValidationError with field map for HTTP 422 with field errors`
  (PROTECTIVE — currently PASS)
- [ ] `safeApiCall returns ValidationError with empty map for HTTP 422 without field errors`
  (PROTECTIVE — currently PASS)
- [ ] **`safeApiCall` returns Error populated from errorBody string when parsed message is blank**
  (REGRESSION TEST — currently FAIL — supports Bug #14 fix)
  Mock `block()` to throw `HttpException` whose `errorBody().string()` is
  `{"message":"","success":false}`; assert returned `ApiResult.Error.message`
  equals `""` today (the bug). After #14 is fixed the assertion flips: message
  should be `""` OR the raw body, depending on which fix direction you pick.
- [ ] `safeApiCall returns Error with HTTP-status fallback when errorBody is empty and unparseable`
  (PROTECTIVE — currently PASS — `ApiResult.kt:79-84` `?: "Request failed (HTTP $status)"`)
- [ ] `safeApiCall wraps IOException as NetworkError`
  (PROTECTIVE — currently PASS)
- [ ] `safeApiCall rethrows CancellationException instead of swallowing it`
  (PROTECTIVE — currently PASS — `ApiResult.kt:86-87`)
- [ ] `safeApiCall wraps generic Throwable as NetworkError`
  (PROTECTIVE — currently PASS)
- [ ] `treatAlreadyAppliedAsSuccess converts Error 409 to Success`
  (PROTECTIVE — currently PASS)
- [ ] `treatAlreadyAppliedAsSuccess converts Error 404 to Success`
  (PROTECTIVE — currently PASS)
- [ ] `treatAlreadyAppliedAsSuccess passes non-409/404 Error through unchanged`
  (PROTECTIVE — currently PASS)
- [ ] `treatAlreadyAppliedAsSuccess leaves Success / ValidationError / NetworkError unchanged`
  (PROTECTIVE — currently PASS)
- [ ] `extractFieldErrors coerces primitive values to single-element lists`
  (PROTECTIVE — currently PASS)
- [ ] `extractFieldErrors skips the "code" key`
  (PROTECTIVE — currently PASS)
- [ ] `extractFieldErrors returns empty map when errors is not a JsonObject`
  (PROTECTIVE — currently PASS)
- [ ] `extractErrorCode returns null when no code field present`
  (PROTECTIVE — currently PASS)

---

## B. `data/util/ImageCompressorTest.kt`

Path: `app/src/test/java/com/appriyo/deulama/data/util/ImageCompressorTest.kt`
Backs: `data/util/ImageCompressor.kt`.

Pure JVM impossible here because `BitmapFactory.decodeStream` requires an
Android runtime. Mark as **DEFERRED TO INSTRUMENTED TEST PLAN**. Listed here
only so it's not silently skipped — see `INSTRUMENTED_TEST_PLAN.md §F`.

No items in this section.

---

## C. `data/util/ImageValidatorTest.kt`

Path: `app/src/test/java/com/appriyo/deulama/data/util/ImageValidatorTest.kt`
Backs: `data/util/ImageValidator.kt`.

Same Android-runtime blocker as `ImageCompressor`. **DEFERRED TO INSTRUMENTED
TEST PLAN §F.** No items here.

---

## D. `data/repository/ProfileRepositoryImplTest.kt`

Path: `app/src/test/java/com/appriyo/deulama/data/repository/ProfileRepositoryImplTest.kt`
Backs: `data/repository/ProfileRepositoryImpl.kt`.

Requires fakes for `ProfileApi` (interface → MockK), `SessionManager`,
`Json`, and the multipart `RequestBody` factory. Note: `MultipartBody.Part` is
constructible on the JVM, so this file is genuinely testable here.

- [ ] **`updateProfile with no image routes to the JSON PUT method and not the multipart one`**
  (REGRESSION TEST — currently PASS)
  Mock `ProfileApi.updateProfileJson` to return success envelope;
  mock `updateProfileMultipart` with `mockk` `verify(exactly = 0)`.
  This protects the routing decision so Bug #1's eventual fix (real
  `imageName`) doesn't accidentally route JSON-path submissions into the
  multipart method.

- [ ] **`updateProfile with image routes to the multipart PUT method`**
  (REGRESSION TEST — currently PASS — sits directly behind Bug #1)
  Provide `imageBytes = byteArrayOf(...)`, `imageMime = "image/png"`,
  `imageName = "test.png"`. Verify `updateProfileMultipart` called once and
  `updateProfileJson` zero times.

- [ ] **`updateProfile multipart call forwards filename from imageName parameter`**
  (REGRESSION TEST — currently FAIL — ties to Bug #1 fix)
  Today `ProfileRepositoryImpl.kt:73` does
  `imageName = imageName ?: "avatar.jpg"`. The fix replaces this with the
  real filename from `OpenableColumns.DISPLAY_NAME`. The test asserts:
  `MultipartBody.Part.createFormData("image", <imageName>, ...)` was built
  with the supplied `imageName`. Today the test FAILS because the call site
  always passes `"avatar.jpg"` regardless of input.

- [ ] **`updateProfile with image and blank mime falls back to image/jpeg`**
  (PROTECTIVE — currently PASS — protects default branch)

- [ ] **`updateProfile propagates ApiResult.ValidationError from API call unchanged`**
  (PROTECTIVE — currently PASS)

- [ ] **`updateProfile propagates ApiResult.NetworkError unchanged`**
  (PROTECTIVE — currently PASS)

- [ ] **`updateProfile maps DTO into UserProfile correctly including favorite_genres and counts`**
  (PROTECTIVE — currently PASS)

- [ ] **`updateProfile maps empty favorite_genres to empty list, never null`**
  (PROTECTIVE — currently PASS — protects the `default.png` contract)

- [ ] **`updateProfile on success calls sessionManager.saveSession with merged user`**
  (REGRESSION TEST — currently PASS — supports Bug #17's location)
  Verify the merged `User` flows into `sessionManager.saveSession`.

- [ ] **`updateProfile on success preserves session token`**
  (REGRESSION TEST — currently PASS — supports Bug #17)

- [ ] **`updateProfile on success merges email from response, falling back to cached only when response omits it`**
  (REGRESSION TEST — currently FAIL — Bug #17)
  Seed session with `User(email = "old@example.com")`. Return a
  `ProfileResponseDto` with `email = "new@example.com"`. Today the test
  FAILS because the code preserves the old email. After #17 is fixed the
  assertion flips to expect `new@example.com`.

- [ ] **`updateProfile on success merges name from response`**
  (REGRESSION TEST — currently PASS — protects correct half of merge)

- [ ] **`updateProfile on success merges image from response`**
  (REGRESSION TEST — currently PASS — protects correct half of merge)

- [ ] **`updateProfile returns the raw updatedFields list in the result wrapper`**
  (PROTECTIVE — currently PASS — `updated_fields` banner contract)

- [ ] **`updateProfile with empty updatedFields returns empty list, not null`**
  (PROTECTIVE — currently PASS)

- [ ] **`updateProfile does NOT touch sessionManager when API returns Error`**
  (PROTECTIVE — currently PASS)

- [ ] **`updateProfile does NOT touch sessionManager when API returns NetworkError`**
  (PROTECTIVE — currently PASS)

- [ ] **`getProfile maps DTO and returns ApiResult.Success`**
  (PROTECTIVE — currently PASS)

- [ ] **`getProfile returns ApiResult.ValidationError on HTTP 422`**
  (PROTECTIVE — currently PASS)

---

## E. `data/repository/SwipeRepositoryImplTest.kt`

Path: `app/src/test/java/com/appriyo/deulama/data/repository/SwipeRepositoryImplTest.kt`
Backs: `data/repository/SwipeRepositoryImpl.kt` AND `presentation/swipe/SwipeViewModel.kt`
via the VM tests in §M.

This is the file that backs **Bugs #6 and #7**. Both tests below are
expected to fail until those bugs are fixed.

- [ ] **`recordSwipe queues to LocalSwipeDao when session is null (anonymous user)`**
  (REGRESSION TEST — currently FAIL — Bug #7)
  Fake `SessionManager` returning `null`. Fake `LocalSwipeDao` (interface) —
  note: `SwipeRepositoryImpl` does NOT currently declare `LocalSwipeDao` in
  its constructor. **This test will not compile today.** That's fine — the
  task is to express the desired behaviour. Mark with `@Ignore` until the
  production constructor is widened to accept `(swipeApi, json, sessionManager, localSwipeDao)`.
  Assert `localSwipeDao.upsert(...)` called once with the drama id, and
  `swipeApi.recordSwipe(...)` zero times.

- [ ] **`recordSwipe hits the API when session is non-null`**
  (PROTECTIVE — currently PASS)

- [ ] **`recordSwipe returns ValidationError when dramaId < MIN_DRAMA_ID (1)`**
  (PROTECTIVE — currently PASS)

- [ ] **`recordSwipe maps the wire DTO into SwipeRecord domain model`**
  (PROTECTIVE — currently PASS)

- [ ] **`recordSwipe normalises swipe_type "like" to SwipeType.LIKE`**
  (PROTECTIVE — currently PASS)

- [ ] **`recordSwipe normalises unknown swipe_type strings to SwipeType.DISLIKE`**
  (PROTECTIVE — currently PASS)

- [ ] **`recordSwipe propagates ApiResult.NetworkError from safeApiCall`**
  (PROTECTIVE — currently PASS)

---

## F. `data/repository/EngagementRepositoryImplsTest.kt`

Path: `app/src/test/java/com/appriyo/deulama/data/repository/EngagementRepositoryImplsTest.kt`
Backs: `data/repository/EngagementRepositoryImpls.kt`. This is the largest
single test file in the plan; it backs **Bugs #2 and #14** and protects the
✅ 409-as-soft-success and optimistic-local-then-reconcile flows.

Constructor deps to fake: `FavoritesApi`, `LocalFavoriteDao`, `SessionManager`,
`Json`, `EngagementFailureBus`. The same shape repeats for
`WatchLaterRepositoryImpl` and `WatchedRepositoryImpl`. Use a shared base
helper inside the test file to keep boilerplate down — but every test name
must be specific enough to identify which repo it covers.

### F.1 Favorites

- [ ] **`add (signed-in, success) upserts local row then calls API and returns Success`**
  (PROTECTIVE — currently PASS)

- [ ] **`add (signed-in, hard API failure) rolls back the local row and emits a SoftFailure`**
  (PROTECTIVE — currently PASS)

- [ ] **`add (signed-in, HTTP 409) treats response as soft-success and KEEPS local row`**
  (PROTECTIVE — currently PASS — locks down the 409 contract)

- [ ] **`add (signed-in, HTTP 404) treats response as soft-success and KEEPS local row`**
  (PROTECTIVE — currently PASS)

- [ ] **`add (anonymous) writes to LocalFavoriteDao and returns Success without touching the API`**
  (PROTECTIVE — currently PASS)

- [ ] **`add with dramaId 0 returns ValidationError without touching DAO or API`**
  (PROTECTIVE — currently PASS)

- [ ] **`remove (signed-in, success) deletes local row and calls API`**
  (PROTECTIVE — currently PASS)

- [ ] **`remove (signed-in, hard API failure) restores the previously-existing local row`**
  (PROTECTIVE — currently PASS — guards the rollback path)

- [ ] **`remove (signed-in, hard API failure) does NOT restore when row was not previously local`**
  (PROTECTIVE — currently PASS — `wasLocal` guard)

- [ ] **`remove (signed-in, HTTP 409) treats response as soft-success and KEEPS the deletion`**
  (PROTECTIVE — currently PASS)

- [ ] **`isFavorited returns the underlying LocalFavoriteDao flow`**
  (REGRESSION TEST — currently FAIL — Bug #2, HIGH)
  Seed `LocalFavoriteDao.isFavoritedFlow` to return `true` when called with
  `(dramaId = 42, userId = user-A)` AND `(dramaId = 42, userId = user-B)`.
  Today the production code calls `localDao.isFavoritedFlow(dramaId)` without
  a `userId`. The test will fail because **the DAO method signature doesn't
  even have a `userId` parameter yet**. Mark `@Ignore` until the schema gets
  a `userId` column on `LocalFavoriteEntity` and the DAO + repository are
  updated. After the fix, the test should pass: `isFavorited` for user A on
  a row seeded by user B must return `false`.

- [ ] **`listFavorites drops rows with null embedded drama`**
  (PROTECTIVE — currently PASS)

- [ ] **`listFavorites uses created_at as the timestamp`**
  (PROTECTIVE — currently PASS)

- [ ] **`listFavorites returns ApiResult.Error unchanged when API returns Error`**
  (PROTECTIVE — currently PASS)

- [ ] **`listFavorites returns ApiResult.NetworkError unchanged when API returns NetworkError`**
  (PROTECTIVE — currently PASS)

### F.2 WatchLater (mirror of F.1, with watch-later specifics)

- [ ] **`add (signed-in, success) upserts local row and calls POST /api/watch-later`**
  (PROTECTIVE — currently PASS)

- [ ] **`add (signed-in, hard failure) rolls back local row`**
  (PROTECTIVE — currently PASS)

- [ ] **`add (signed-in, HTTP 409) treats response as soft-success`**
  (PROTECTIVE — currently PASS)

- [ ] **`add (anonymous) writes to LocalWatchLaterDao without network call`**
  (PROTECTIVE — currently PASS)

- [ ] **`remove (signed-in, success) deletes local row and calls DELETE`**
  (PROTECTIVE — currently PASS)

- [ ] **`remove (signed-in, hard failure) restores the previously-existing local row`**
  (PROTECTIVE — currently PASS)

- [ ] **`isQueued returns the underlying LocalWatchLaterDao flow`**
  (REGRESSION TEST — currently FAIL — Bug #2)
  Same shape as F.1 `isFavorited` — annotated `@Ignore` until the DAO adds
  `userId`.

- [ ] **`listWatchLater drops rows with null embedded drama`**
  (PROTECTIVE — currently PASS)

- [ ] **`listWatchLater uses created_at as the timestamp`**
  (PROTECTIVE — currently PASS)

### F.3 Watched (mirror with `watched_at` instead of `created_at`)

- [ ] **`markWatched (signed-in, success) upserts local row and calls POST /api/watched`**
  (PROTECTIVE — currently PASS)

- [ ] **`markWatched (signed-in, hard failure) rolls back local row`**
  (PROTECTIVE — currently PASS)

- [ ] **`markWatched (signed-in, HTTP 409) treats response as soft-success (Watched cannot be undone)`**
  (PROTECTIVE — currently PASS — pins the no-unwatch contract)

- [ ] **`markWatched (anonymous) writes to LocalWatchedDao without network call`**
  (PROTECTIVE — currently PASS)

- [ ] **`markWatched with dramaId 0 returns ValidationError without touching DAO or API`**
  (PROTECTIVE — currently PASS)

- [ ] **`isMarkedWatched returns the underlying LocalWatchedDao flow`**
  (REGRESSION TEST — currently FAIL — Bug #2 — `@Ignore` until userId added)

- [ ] **`listWatched drops rows with null embedded drama`**
  (PROTECTIVE — currently PASS)

- [ ] **`listWatched uses watched_at when present, falling back to created_at`**
  (PROTECTIVE — currently PASS)

### F.4 messageFor helper (Bug #14)

- [ ] **`messageFor(ValidationError) returns the first non-null message in field order`**
  (PROTECTIVE — currently PASS)

- [ ] **`messageFor(ValidationError) returns fallback string when fieldErrors is empty`**
  (PROTECTIVE — currently PASS — `"Couldn't save that action."`)

- [ ] **`messageFor(NetworkError) returns the offline-friendly copy`**
  (PROTECTIVE — currently PASS — `"Can't reach the server. We'll save this for next time."`)

- [ ] **`messageFor(Error with blank message) returns the fallback string TODAY`**
  (REGRESSION TEST — currently PASS — pins current buggy behaviour)
  Asserts that today, an `ApiResult.Error(message = "")` produces the
  fallback string. This is the test that will become a **REGRESSION TEST —
  currently FAIL** once Bug #14 is fixed; the fix should make
  `messageFor(ApiResult.Error(message=""))` raise or return the body-derived
  copy. After #14 is fixed, flip this test to assert the new contract.

- [ ] **`messageFor(Error with non-blank message) returns the message verbatim`**
  (PROTECTIVE — currently PASS)

---

## G. `data/repository/TimelineRepositoryImplTest.kt`

Path: `app/src/test/java/com/appriyo/deulama/data/repository/TimelineRepositoryImplTest.kt`
Backs: `data/repository/TimelineRepositoryImpl.kt`. Already verified ✅ in
audit — all tests here are PROTECTIVE.

- [ ] **`loadTimeline fires all three list calls concurrently (single coroutineScope, three async)`**
  (PROTECTIVE — currently PASS)
  Use a controlled dispatcher to verify the three deferreds are pending at the
  same suspension point (probe via `progress = deferred::isActive`).

- [ ] **`loadTimeline merges favorites + watch_later + watched rows newest-first by timestamp`**
  (PROTECTIVE — currently PASS)
  Seed each fake repo with rows whose `timestamp` strings are out of order.
  Assert the returned `ApiResult.Success.value` is sorted by `timestamp`
  descending using the documented lexicographic-on-fixed-width convention.

- [ ] **`loadTimeline returns first non-Success subcall (favorites wins over watch-later wins over watched by evaluation order)`**
  (PROTECTIVE — currently PASS)
  Make all three return non-Success; assert the returned error matches the
  "first failure" the code picks (`listOf(...).firstOrNull { ... }`).

- [ ] **`loadTimeline returns ApiResult.Success with empty list when all three subcalls return empty lists`**
  (PROTECTIVE — currently PASS)

- [ ] **`loadTimeline drops any rows whose list subcall returned rows with null embedded drama`**
  (PROTECTIVE — currently PASS — assumes the sub-repos did their filtering)

- [ ] **`loadTimeline propagates CancellationException without swallowing it`**
  (PROTECTIVE — currently PASS)

---

## H. `presentation/auth/AuthViewModelTest.kt`

Path: `app/src/test/java/com/appriyo/deulama/presentation/auth/AuthViewModelTest.kt`
Backs: `presentation/auth/AuthViewModel.kt`. Verified ✅ in audit.

Use a fake `AuthRepository` (interface) returning `Flow<AuthUiState>`. Inject
`kotlinx-coroutines-test`'s `StandardTestDispatcher`.

- [ ] **`state seed reads AuthUiState.Loading`**
  (PROTECTIVE — currently PASS)

- [ ] **`state reflects SignedIn when repo emits SignedIn(user)`**
  (PROTECTIVE — currently PASS)

- [ ] **`state reflects SignedOut after logout() clears session`**
  (PROTECTIVE — currently PASS — supports Bug #4 indirectly: logout is the
  trigger for the session-expired path)

- [ ] **`login dispatches to repo.login and updates state on success`**
  (PROTECTIVE — currently PASS)

- [ ] **`login dispatches to repo.login and surfaces ValidationError field errors`**
  (PROTECTIVE — currently PASS)

- [ ] **`login on Error maps server message into formState.banner`**
  (PROTECTIVE — currently PASS)

- [ ] **`login on NetworkError sets the offline banner`**
  (PROTECTIVE — currently PASS)

- [ ] **`register mirrors login validation rules`**
  (PROTECTIVE — currently PASS)

- [ ] **`logout clears session and emits SignedOut`**
  (PROTECTIVE — currently PASS)

---

## I. `presentation/profile/EditProfileViewModelTest.kt`

Path: `app/src/test/java/com/appriyo/deulama/presentation/profile/EditProfileViewModelTest.kt`
Backs: `presentation/profile/EditProfileViewModel.kt`. Backs **Bugs #1 and #8**.

Fake `ProfileRepository` interface. Use Turbine to assert on the
`StateFlow<EditProfileFormState>` emissions.

- [ ] **`onNameChanged updates formState.name`**
  (PROTECTIVE — currently PASS)

- [ ] **`validate name: blank name sets nameError "Name must be 2-150 characters."`**
  (PROTECTIVE — currently PASS)

- [ ] **`validate name: 1 char sets nameError`**
  (PROTECTIVE — currently PASS)

- [ ] **`validate name: 151 chars sets nameError`**
  (PROTECTIVE — currently PASS)

- [ ] **`validate name: 2 chars passes`**
  (PROTECTIVE — currently PASS)

- [ ] **`validate name: 150 chars passes`**
  (PROTECTIVE — currently PASS)

- [ ] **`onImagePicked calls repository's image validator and stores bytes only on success`**
  (PROTECTIVE — currently PASS — protects image-bytes plumbing)

- [ ] **`onImagePicked with oversized bytes sets imageError without storing bytes`**
  (PROTECTIVE — currently PASS)

- [ ] **`onImagePicked with unsupported MIME sets imageError without storing bytes`**
  (PROTECTIVE — currently PASS)

- [ ] **`onImagePicked stores imageName derived from picker display name, not hardcoded "avatar.jpg"`**
  (REGRESSION TEST — currently FAIL — Bug #1, priority A)
  Drive `onImagePicked(uri)` with a stubbed `ContentResolver` whose
  `OpenableColumns.DISPLAY_NAME` returns `"my-photo.png"`. Today the
  production code ignores that value and pins `imageName = "avatar.jpg"`
  (`EditProfileViewModel.kt:114`). Assert `formState.imageName == "my-photo.png"`.

- [ ] **`submit with empty name (after blank) blocks submission with nameError`**
  (PROTECTIVE — currently PASS)

- [ ] **`submit with only currentPassword filled sets ALL THREE password fields' error to the canonical partial-password message`**
  (REGRESSION TEST — currently FAIL — Bug #8)
  Today the production validator runs the per-field checks first, so
  `currentPasswordError` is empty (the field IS filled) and only
  `newPasswordError` / `confirmPasswordError` get the "all three required"
  message. After #8 is fixed the test should pass: all three fields carry
  the same message and submission is blocked.

- [ ] **`submit with only currentPassword filled never reaches the repository`**
  (PROTECTIVE — currently PASS — already the case; pin it)

- [ ] **`submit with all three passwords filled and not matching sets confirmPasswordError`**
  (PROTECTIVE — currently PASS)

- [ ] **`submit with new password < 8 chars sets newPasswordError`**
  (PROTECTIVE — currently PASS)

- [ ] **`submit with new password > 255 chars sets newPasswordError`**
  (PROTECTIVE — currently PASS)

- [ ] **`submit with valid name + valid password set calls repository.updateProfile exactly once`**
  (PROTECTIVE — currently PASS)

- [ ] **`submit with no image routes repository.updateProfile(imageBytes=null)`**
  (REGRESSION TEST — currently PASS — supports Bug #1 fix)

- [ ] **`submit on Success sets banner with formatted updatedFields list`**
  (PROTECTIVE — currently PASS — `"Saved (name)."` mapping)

- [ ] **`submit on Success with image field maps "image" to "avatar" in the banner`**
  (PROTECTIVE — currently PASS — `formatUpdatedFieldsBanner` rewrite rule)

- [ ] **`submit on Success with empty updatedFields shows generic "Saved." banner`**
  (PROTECTIVE — currently PASS)

- [ ] **`submit on Error propagates server message into banner`**
  (PROTECTIVE — currently PASS)

- [ ] **`submit on NetworkError sets the offline banner`**
  (PROTECTIVE — currently PASS)

- [ ] **`submit on ValidationError maps first field error into the right formState.*Error slot`**
  (PROTECTIVE — currently PASS)

- [ ] **`discard() resets imageBytes/imageMime/imageName/imageError`**
  (PROTECTIVE — currently PASS)

- [ ] **`discard() leaves name and password fields alone`**
  (PROTECTIVE — currently PASS)

---

## J. `presentation/profile/ProfileViewModelTest.kt` (placeholder)

The `ProfileViewModel` reads the live session and re-renders from
`AuthViewModel.state`. There is no per-screen logic worth a dedicated unit
test today. **DEFERRED to MANUAL TEST PLAN step 19 / 20.** No items here.

---

## K. `presentation/activity/ActivityViewModelTest.kt`

Path: `app/src/test/java/com/appriyo/deulama/presentation/activity/ActivityViewModelTest.kt`
Backs: `presentation/activity/ActivityViewModel.kt`. Backs **Bug #5 (race)**.

Use Turbine. Fake `TimelineRepository` whose `loadTimeline()` is a
`CompletableDeferred` you control, plus a fake `SessionManager`.

- [ ] **`load() with no session sets state to SignedOut without calling repository`**
  (PROTECTIVE — currently PASS)

- [ ] **`load() with session sets state to Loading then Success on completion`**
  (PROTECTIVE — currently PASS)

- [ ] **`load() with empty timeline result sets state to Empty`**
  (PROTECTIVE — currently PASS)

- [ ] **`load() with ApiResult.Error sets state to Error with the server message`**
  (PROTECTIVE — currently PASS)

- [ ] **`load() with ApiResult.Error containing blank message sets state to Error with the generic fallback`**
  (PROTECTIVE — currently PASS)

- [ ] **`load() with ApiResult.NetworkError sets state to Error with the offline banner`**
  (PROTECTIVE — currently PASS)

- [ ] **`load() with ApiResult.ValidationError sets state to Error with the parse banner`**
  (PROTECTIVE — currently PASS)

- [ ] **`refresh() is equivalent to load() (idempotent entrypoint)`**
  (PROTECTIVE — currently PASS)

- [ ] **`overlapping load() calls produce non-deterministic final state — first call slower than second`**
  (REGRESSION TEST — currently FAIL — Bug #5)
  Scenario:
    1. Call `load()`. Repository returns a CompletableDeferred that you do
       NOT complete yet. State should be `Loading`.
    2. Call `load()` again. Repository returns a second CompletableDeferred
       that you DO complete with `Success(rows = ...)`.
    3. State should now be `Success(rows)`.
    4. Complete the FIRST deferred with `Error("stale")`.
    5. State should remain `Success(rows)` — but today it flips to
       `Error("stale")` because there's no generation guard. After #5 is
       fixed this test asserts the state stays `Success(rows)`.

- [ ] **`overlapping load() calls — second call slower than first — also flips state incorrectly`**
  (REGRESSION TEST — currently FAIL — Bug #5)
  Same as above but with the timing inverted. Catches the symmetric case.

---

## L. `presentation/recommendations/RecommendationsViewModelTest.kt`

Path: `app/src/test/java/com/appriyo/deulama/presentation/recommendations/RecommendationsViewModelTest.kt`
Backs: `presentation/recommendations/RecommendationsViewModel.kt`. Backs
**Bugs #10 and #11**.

- [ ] **`init / first composition triggers an initial load`**
  (PROTECTIVE — currently PASS)

- [ ] **`Success state renders the recommendation list`**
  (PROTECTIVE — currently PASS)

- [ ] **`Empty state surfaces the cold-start CTA`**
  (PROTECTIVE — currently PASS)

- [ ] **`Error state surfaces the server / network banner`**
  (PROTECTIVE — currently PASS)

- [ ] **`onRetry calls load() again`**
  (PROTECTIVE — currently PASS)

- [ ] **`re-entering the screen DOES NOT refresh the feed today (cache hits)`**
  (REGRESSION TEST — currently FAIL — Bug #10)
  Two `LaunchedEffect(Unit)` simulations separated by advancing virtual
  time; assert the repository is NOT called a second time. Today the
  production VM caches the first response with no `refresh()` on re-entry.
  After #10 is fixed, flip this test to assert the repository IS called.

- [ ] **`a 401 response falls into Error, not SignedOut — today`**
  (REGRESSION TEST — currently FAIL — Bug #11)
  Fake `RecommendationsRepository.load()` to return
  `ApiResult.Error(httpStatus = 401, message = "Unauthenticated.")`.
  Assert that no `SignedOut` state exists and the state is `Error(message=...)`.
  After #11 is fixed, the test flips to assert a `SignedOut` state with a
  login CTA.

---

## M. `presentation/genre/GenreStatsViewModelTest.kt`

Path: `app/src/test/java/com/appriyo/deulama/presentation/genre/GenreStatsViewModelTest.kt`
Backs: `presentation/genre/GenreStatsViewModel.kt` (or the state holder
inside `GenreStatsScreen.kt` if there is no separate VM). Backs **Bug #11
on the Genre Stats side** — listed separately from Recommendations because
the audit marks it as a separate bug for a separate screen.

- [ ] **`initial load fires the repository call`**
  (PROTECTIVE — currently PASS)

- [ ] **`Success renders genre → count bars in descending count order`**
  (PROTECTIVE — currently PASS)

- [ ] **`Empty state surfaces the cold-start CTA`**
  (PROTECTIVE — currently PASS)

- [ ] **`a 401 response falls into Error, not SignedOut — today`**
  (REGRESSION TEST — currently FAIL — Bug #11, second instance)
  Same shape as the Recommendations test above; assert the absence of a
  `SignedOut` state today, and assert its presence after the fix.

- [ ] **`onRetry calls load() again`**
  (PROTECTIVE — currently PASS)

---

## N. `presentation/swipe/SwipeViewModelTest.kt`

Path: `app/src/test/java/com/appriyo/deulama/presentation/swipe/SwipeViewModelTest.kt`
Backs: `presentation/swipe/SwipeViewModel.kt`. Backs **Bug #6 (no rollback
on network failure)**.

Fake `SwipeRepository` and a fake `DramaRepository` for the deck source.

- [ ] **`init seeds the deck from the catalog minus already-swiped drama ids`**
  (PROTECTIVE — currently PASS)

- [ ] **`recordSwipe(like) removes the top card and calls SwipeRepository.recordSwipe(LIKE)`**
  (PROTECTIVE — currently PASS)

- [ ] **`recordSwipe(dislike) removes the top card and calls SwipeRepository.recordSwipe(DISLIKE)`**
  (PROTECTIVE — currently PASS)

- [ ] **`recordSwipe on Error from the repository does NOT reinsert the top card today — bug #6`**
  (REGRESSION TEST — currently FAIL — Bug #6)
  Fake repo returns `ApiResult.Error("Network down")`. After the call,
  assert `deckState.topCard == null` (the card is gone) AND the snackbar
  message is empty. After #6 is fixed, the test flips: the top card must be
  reinserted at position 0 and `snackbar == "Network down"`.

- [ ] **`recordSwipe on ValidationError does NOT reinsert the top card today — bug #6`**
  (REGRESSION TEST — currently FAIL — Bug #6, alternative path)

- [ ] **`recordSwipe on NetworkError does NOT reinsert the top card today — bug #6`**
  (REGRESSION TEST — currently FAIL — Bug #6, alternative path)

- [ ] **`refresh() reloads the deck from the catalog`**
  (PROTECTIVE — currently PASS)

- [ ] **`refresh() excludes dramas already in the swipe history`**
  (PROTECTIVE — currently PASS)

---

## O. `presentation/details/DramaDetailsViewModelTest.kt`

Path: `app/src/test/java/com/appriyo/deulama/presentation/details/DramaDetailsViewModelTest.kt`
Backs: `presentation/details/DramaDetailsViewModel.kt`. Mostly ✅ verified.

- [ ] **`init triggers DramaRepository.getDrama(id) and EngagementFlow wiring`**
  (PROTECTIVE — currently PASS)

- [ ] **`Success state exposes poster, title, year, country, genres, synopsis`**
  (PROTECTIVE — currently PASS)

- [ ] **`Error state with 404 navigates back with a snackbar`**
  (PROTECTIVE — currently PASS)

- [ ] **`toggleFavorite delegates to FavoritesRepository and re-emits isFavorited flow`**
  (PROTECTIVE — currently PASS)

- [ ] **`toggleWatchLater delegates to WatchLaterRepository`**
  (PROTECTIVE — currently PASS)

- [ ] **`markWatched delegates to WatchedRepository (no un-watch affordance)`**
  (PROTECTIVE — currently PASS)

- [ ] **`fromRecommendations=true triggers RecommendationRepository.reasonFor(id)`**
  (PROTECTIVE — currently PASS)

- [ ] **`fromRecommendations=false does NOT call the recommendation reason endpoint`**
  (PROTECTIVE — currently PASS)

---

## P. `presentation/discover/DiscoverViewModelTest.kt`

Path: `app/src/test/java/com/appriyo/deulama/presentation/discover/DiscoverViewModelTest.kt`
Backs: `presentation/discover/DiscoverViewModel.kt` + `DramaPagingSource.kt`.
Backs **Bug #9 (missing search / sort / genre UI)**.

> **TODO test names — write once Bug #9 is implemented.** The production
> VM today does not expose `searchQuery`, `sort`, or `genre` state. Do not
> write a test that asserts the absence of these; that would just pin the
> bug. Instead, sketch the names and leave them out of the "Write In This
> Order" sequence.

- [ ] _(placeholder)_ **`setSearchQuery("foo") triggers a new PagingSource with `q=foo` query parameter`**
  (REGRESSION TEST — currently FAIL — Bug #9; write once `searchQuery` exists)

- [ ] _(placeholder)_ **`clearSearchQuery restores the default unsorted-by-relevance feed`**
  (REGRESSION TEST — currently FAIL — Bug #9)

- [ ] _(placeholder)_ **`setSort(YEAR_ASC) passes `sort=year&order=asc` to DramaPagingSource`**
  (REGRESSION TEST — currently FAIL — Bug #9)

- [ ] _(placeholder)_ **`setGenre("Romance") passes `genre=Romance` to DramaPagingSource`**
  (REGRESSION TEST — currently FAIL — Bug #9)

For now, the file should contain only these TODO-shaped methods (no test
bodies, marked `@Ignore("write once Bug #9 is implemented")`). When the
feature lands, flesh out the bodies.

---

## Q. `presentation/navigation/NavGraphSessionExpiredTest.kt`

Path: `app/src/test/java/com/appriyo/deulama/presentation/navigation/NavGraphSessionExpiredTest.kt`
Backs: `presentation/navigation/NavGraph.kt` `LaunchedEffect` collector for
`AuthEventBus`. Indirectly supports **Bug #4** (no snackbar on
session-expired).

Compose UI is out of scope per the task brief, but the side-effect itself
(`AuthViewModel.logout()` + `navController.navigateToAuthGraph()`) is pure
state and worth a JVM test.

- [ ] **`collecting SessionExpired from AuthEventBus calls AuthViewModel.logout`**
  (PROTECTIVE — currently PASS — pins the underlying event handling
  independent of UI)

- [ ] **`collecting SessionExpired from AuthEventBus triggers a navigateToAuthGraph call on the nav controller`**
  (PROTECTIVE — currently PASS)

- [ ] **`non-SessionExpired events (e.g. Login success) do NOT trigger logout`**
  (PROTECTIVE — currently PASS)

(Once Bug #4 is fixed, add a `pending snackbar message` assertion. For now
the assertion above is the protective baseline.)

---

## R. `data/remote/interceptor/AuthInterceptorTest.kt`

Path: `app/src/test/java/com/appriyo/deulama/data/remote/interceptor/AuthInterceptorTest.kt`
Backs: `data/remote/interceptor/AuthInterceptor.kt`. All ✅ verified.

Use `okhttp3.mockwebserver.MockWebServer` to host a tiny endpoint that
returns 401.

- [ ] **`adds Authorization: Bearer <token> header when session is present`**
  (PROTECTIVE — currently PASS)

- [ ] **`omits Authorization header when session is null (anonymous)`**
  (PROTECTIVE — currently PASS)

- [ ] **`does NOT add Authorization header to /api/auth/login or /api/auth/register (anon endpoints)`**
  (PROTECTIVE — currently PASS)

- [ ] **`emits AuthEvent.SessionExpired on 401 of a non-auth endpoint`**
  (PROTECTIVE — currently PASS — supports Bug #4)

- [ ] **`does NOT emit AuthEvent.SessionExpired on 401 of /api/auth/login`**
  (PROTECTIVE — currently PASS — pins the avoid-logout-on-bad-creds rule)

- [ ] **`passes the response through unchanged after firing SessionExpired`**
  (PROTECTIVE — currently PASS — interceptor is non-failing)

- [ ] **`does NOT block the request when SessionManager throws`**
  (PROTECTIVE — currently PASS)

---

## S. `domain/model/UserProfileTest.kt` (mappers)

Path: `app/src/test/java/com/appriyo/deulama/domain/model/UserProfileTest.kt`
Backs: `domain/model/UserProfile.kt` + the DTO → domain mappers.

- [ ] **`ProfileResponseDto → UserProfile maps every field including favorite_genres`**
  (PROTECTIVE — currently PASS)

- [ ] **`UserProfile is constructed without nullable profileImage, likedCount, watchedCount, favoriteGenres`**
  (PROTECTIVE — currently PASS — locks the `default.png` contract)

- [ ] **`User.toProfileFormSeed() copies fullName and email but never exposes password / token`**
  (PROTECTIVE — currently PASS)

---

## T. Skip list (do NOT test)

These are explicitly **out of scope** for the unit-test plan:

- Compose `@Composable` rendering functions (deferred per task brief).
- `BitmapFactory.decodeStream` paths inside `ImageCompressor.kt` and
  `ImageValidator.kt` — Android-only, see `INSTRUMENTED_TEST_PLAN.md §F`.
- Room schema-level invariants — see `INSTRUMENTED_TEST_PLAN.md §A-§C`.
- Coil image loader — see `MANUAL_TEST_PLAN.md`.
- `MappersPlaceholder.kt` — file slated for deletion (audit cross-cutting
  bug). No tests.

---

## "Write In This Order"

Cross-document priority: bugs #1 and #2 are HIGHEST in the audit (priority A
and privacy). They lead the list. After that, follow the audit's Blocking
Issues numbering (1 → 18), then protective coverage ordered by dependency.

1. **`EditProfileViewModelTest.kt` — onImagePicked stores imageName from picker display name** (Bug #1)
2. **`ProfileRepositoryImplTest.kt` — updateProfile multipart call forwards filename from imageName parameter** (Bug #1)
3. **`ProfileRepositoryImplTest.kt` — updateProfile on success merges email from response** (Bug #17, sits next to #1)
4. **`EngagementRepositoryImplsTest.kt` — isFavorited / isQueued / isMarkedWatched return user-filtered flows** (Bug #2, HIGH)
5. **`ApiResultTest.kt` — safeApiCall populates Error message from errorBody** (Bug #14, supports #2 messaging)
6. **`EngagementRepositoryImplsTest.kt` — messageFor(Error with blank message) contract pin** (Bug #14)
7. **`SwipeRepositoryImplTest.kt` — recordSwipe queues to LocalSwipeDao when session is null** (Bug #7)
8. **`SwipeRepositoryImplTest.kt` — recordSwipe accepts SessionManager + LocalSwipeDao in constructor** (Bug #7; will not compile until constructor widens)
9. **`SwipeViewModelTest.kt` — recordSwipe on Error reinserts the top card** (Bug #6)
10. **`EditProfileViewModelTest.kt` — partial password submit sets ALL THREE error slots** (Bug #8)
11. **`ActivityViewModelTest.kt` — overlapping load() calls preserve first-call result** (Bug #5)
12. **`RecommendationsViewModelTest.kt` — re-entering the screen refreshes the feed** (Bug #10)
13. **`RecommendationsViewModelTest.kt` — 401 maps to SignedOut** (Bug #11, recommendations side)
14. **`GenreStatsViewModelTest.kt` — 401 maps to SignedOut** (Bug #11, genre stats side)
15. **`NavGraphSessionExpiredTest.kt` — SessionExpired collector** (supports Bug #4 indirectly)
16. **`AuthInterceptorTest.kt` — SessionExpired emission on 401** (supports Bug #4)
17. **`EngagementRepositoryImplsTest.kt` — 409-as-soft-success across all three repos** (✅ verified, protective)
18. **`EngagementRepositoryImplsTest.kt` — optimistic local then reconcile + rollback** (✅ verified, protective)
19. **`EngagementRepositoryImplsTest.kt` — list methods drop rows with null embedded drama** (✅ verified, protective)
20. **`ProfileRepositoryImplTest.kt` — full mapping / saveSession preservation / token preservation** (✅ verified, protective)
21. **`ProfileRepositoryImplTest.kt` — image routing JSON vs multipart** (✅ verified, protective)
22. **`TimelineRepositoryImplTest.kt` — concurrent async + first-failure short-circuit + merge order** (✅ verified, protective)
23. **`AuthViewModelTest.kt` — login / register / logout flows** (✅ verified, protective)
24. **`DramaDetailsViewModelTest.kt` — drama load + engagement toggles + reason endpoint** (✅ verified, protective)
25. **`EditProfileViewModelTest.kt` — name / password / image validation** (✅ verified, protective)
26. **`EditProfileViewModelTest.kt` — submit banner formatting incl. image → avatar rename** (✅ verified, protective)
27. **`UserProfileTest.kt` — DTO mapping + `default.png` non-null contract** (✅ verified, protective)
28. **`DiscoverViewModelTest.kt` — placeholder TODO tests for search / sort / genre** (Bug #9 — write once feature lands)
29. **`ApiResultTest.kt` — remaining envelope / cancellation / NetworkError coverage** (✅ verified, protective)
30. **`SwipeRepositoryImplTest.kt` — remaining DTO mapping + MIN_DRAMA_ID guard** (✅ verified, protective)

Done when all of the above are green (for PROTECTIVE / currently PASS) or
documented as red-by-design (for currently FAIL).