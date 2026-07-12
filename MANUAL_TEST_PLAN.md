# MANUAL_TEST_PLAN.md

Scope: full user flows, gesture feel, real network conditions, cross-restart
state. Anything that cannot currently be automated lives here.

This document is derived from the **33-step "Manual Test Checklist"** at
the bottom of `PHASE_0_7_AUDIT.md` (preserving that document's ordering,
which already respects auth-first → anonymous → dependency), with:

1. Explicit cross-references to numbered audit items (`Bug #N`, `✅ #N`).
2. **Bug-related steps are flagged `Currently FAILS — pending Bug #N`**
   rather than describing the desired end-state as if it already works.
3. Two new steps inserted in their natural positions: one for **Bug #15**
   (Discard doesn't invalidate Coil cache) and one for **Bug #16** (cold-start
   copy drift between Recommendations and Genre Stats). These are added in
   their dependency position, not at the end.
4. A final "Full Regression Walkthrough" — one continuous script meant to be
   run before any release build.

> **Conventions**:
> - "Currently FAILS — pending Bug #N" → exercise this step today and expect
>   it to fail or to behave incorrectly. Once Bug #N is fixed, re-run and
>   expect it to pass.
> - "Currently PASSES" → baseline verification; treat any regression here as
>   a blocker.
> - "✅ protective" → no known bug behind this step; pinning current
>   behaviour.

---

## Phase 0 — Cold Launch (anonymous entry point)

### Step 0.1 — Anonymous CTA lands on Home with bottom bar
- **Goal**: confirm app is bootable and the bottom bar renders.
- **Steps**: Cold-launch the app from a clean install. On the Login
  screen, tap "Continue without account".
- **Expected**: Home populates (catalog rows or empty state). The bottom
  bar shows five tabs: Home / Discover / Recommendations / Activity /
  Profile.
- **Audit link**: ✅ Phase 1 — auth-gated tabs sign-in-prompted.
- **Currently PASSES.**

### Step 0.2 — Email duplicate on register
- **Goal**: confirm a 422 from `/api/auth/register` surfaces as a single
  inline error.
- **Steps**: Tap "Create account" → fill the form with an email that
  already exists in the backend → submit.
- **Expected**: A single inline email error ("Email is already taken." or
  server copy). No full-screen crash.
- **Audit link**: ⚠️ Phase 1 — 422 field error mapping.
- **Currently PASSES.**

### Step 0.3 — Wrong-password login shows generic copy
- **Goal**: confirm login does not enumerate users.
- **Steps**: On Login, supply a known-bad email + bad password → submit.
- **Expected**: Single banner "Invalid email or password." No distinction
  between "email unknown" and "password wrong".
- **Audit link**: ⚠️ Phase 1 — enumeration resistance.
- **Currently PASSES.**

### Step 0.4 — Wrong-password login with a valid email
- **Goal**: same banner copy as 0.3 — proves no enumeration vector.
- **Steps**: On Login, supply a valid email + bad password → submit.
- **Expected**: Same banner, same wording as 0.3.
- **Audit link**: ⚠️ Phase 1 — enumeration resistance (cross-check).
- **Currently PASSES.**

---

## Phase 2 — Catalog (Discover + Details)

### Step 1 — Paging on Discover with no flicker
- **Steps**: Open Discover. Confirm a footer spinner appears mid-scroll and
  the next ~20 rows attach without a jump-cut.
- **Expected**: smooth scroll through ≥30 rows in the catalog; no
  full-screen red banner; spinner only at the footer.
- **Audit link**: ⚠️ Phase 2 — paging scroll.
- **Currently PASSES.**

### Step 2 — Paging on Discover with a network drop mid-page
- **Steps**: Start scrolling Discover. Toggle airplane mode mid-page.
- **Expected**: a non-blocking footer error appears; the existing page
  remains visible; no full-screen red screen.
- **Audit link**: ⚠️ Phase 2 — network drop mid-paging.
- **Currently PASSES.**

### Step 3 — Genre-tag → Discover filter *(currently FAILS — pending Bug #9)*
- **Steps**: Open Drama Details of a drama with a known genre chip → tap
  that chip.
- **Expected (after Bug #9 fix)**: Discover opens filtered to dramas
  containing that genre.
- **Audit link**: ❌ Bug #9 — Discover has no search / sort / genre UI.
- **Currently FAILS — the Discover filter UI is missing entirely. Tap
  the chip → no-op / nothing happens.**

---

## Phase 3 — Swipe Deck

### Step 4 — Re-launching the deck shows no repeats
- **Steps**: Open the Swipe tab. Swipe right on the first 5 dramas. Kill
  the app. Relaunch and reopen the Swipe tab.
- **Expected**: a fresh batch loads; none of the previously-swiped 5
  appears.
- **Audit link**: ⚠️ Phase 3 — re-launch no-repeat.
- **Currently PASSES** (single-device baseline).

### Step 5 — Anonymous swipe queues locally *(currently FAILS — pending Bug #7)*
- **Steps**: Airplane mode ON. Open the Swipe tab. Swipe right on
  two dramas. Toggle airplane mode off. Log in. Reopen the Swipe tab.
- **Expected (after Bug #7 fix)**: the two previously-swiped dramas do
  NOT appear in the fresh deck (they're now persisted server-side) and
  two like-rows appear under Recommendations / Genre Breakdown.
- **Audit link**: ❌ Bug #7 — anonymous swipes don't queue to Room.
- **Currently FAILS — `SwipeRepositoryImpl` has no
  `LocalSwipeDao` injection. Anonymous swipes never reach the queue, so
  the deck still contains those two dramas after a relaunch.**

### Step 6 — Swipe failure rollback *(currently FAILS — pending Bug #6)*
- **Steps**: Sign in. Open the Swipe tab. Toggle airplane mode ON mid-deck.
  Swipe right.
- **Expected (after Bug #6 fix)**: the top card reappears with a small
  snackbar ("Couldn't record your swipe; try again.").
- **Audit link**: ❌ Bug #6 — Swipe deck has no rollback on network failure.
- **Currently FAILS — card disappears visually; no snackbar; no
  server record; swiped drama is silently lost from the local dataset.**

---

## Phase 4 — Engagement + Sync

### Step 7 — Favorite, kill app, re-open Drama Details
- **Steps**: Sign in. Open a Drama Details. Tap the heart icon to
  favorite. Background the app. Force-kill it via dev settings. Relaunch.
  Open the same Drama Details.
- **Expected**: heart icon still filled (Room-backed).
- **Audit link**: ⚠️ Phase 4 — Room-backed favorite survives kill.
- **Currently PASSES.**

### Step 8 — Sync-on-login replays queued favorites
- **Steps**: Airplane mode ON. Sign out. (Optional — restart in
  anonymous mode.) Favorite three dramas. Airplane mode OFF. Sign in.
  Pull-to-refresh the Activity tab.
- **Expected**: the three favorites appear in the Activity timeline with
  timestamps.
- **Audit link**: ⚠️ Phase 4 — sync-on-login replays.
- **Currently PASSES for favorites.**

### Step 9 — 409-as-soft-success on watch-later
- **Steps**: Mark the same drama as queued twice rapidly (two taps in
  <1 s). Confirm via Activity tab refresh that exactly ONE row exists
  server-side.
- **Expected**: ONE row only, no error banner.
- **Audit link**: ⚠️ Phase 4 — 409-as-success; ✅ verified Phase 4.
- **Currently PASSES.**

### Step 10 — Watched has no Unwatch affordance
- **Steps**: Mark a drama as watched. Look at every screen (Details,
  Activity, Profile) for any "Unwatch" / "Mark unwatched" affordance.
- **Expected**: none. Per api.md `POST /api/watched` has no DELETE.
- **Audit link**: ⚠️ Phase 4 — no-unwatch contract.
- **Currently PASSES.**

### Step 11 — Cross-account contamination on Drama Details *(currently FAILS — pending Bug #2, HIGH)*
- **Steps**: Sign in as user A. Open a drama. Tap the heart. Sign out.
  Sign in as user B on the same device. Open the same drama's Details.
- **Expected (after Bug #2 fix)**: heart icon NOT filled (B never
  favorited it).
- **Audit link**: ❌ Bug #2 — Room state bleeds across accounts (privacy,
  HIGH).
- **Currently FAILS — the heart icon is still filled for user B because
  user A's local Room row is read without a userId filter.**

### Step 12 — Unhelpful snackbar on real failure *(currently FAILS — pending Bug #14)*
- **Steps**: Sign in. Open a drama. Tap the heart. While the request is
  in flight, immediately disable wifi.
- **Expected (after Bug #14 fix)**: a snackbar that names what went
  wrong (e.g. contains the server's message or "Network down — saved
  locally.") rather than the generic "Couldn't save that action."
- **Audit link**: ❌ Bug #14 — failure snackbars fall through to blank
  fallback.
- **Currently FAILS — the snackbar reads "Couldn't save that action."
  with no signal as to why.**

---

## Phase 5 — Recommendations + Genre Stats

### Step 13 — Recommendations cold-start CTA
- **Steps**: Log in as a brand-new account with zero swipes. Open the
  Recommendations tab.
- **Expected**: friendly empty-state CTA pointing at the Swipe tab.
- **Audit link**: ⚠️ Phase 5 — Recommendations cold-start.
- **Currently PASSES.**

### Step 14 — Recommendations refresh on re-entry *(currently FAILS — pending Bug #10)*
- **Steps**: Sign in. Swipe five new likes on the Swipe tab. Open
  Recommendations. Note the feed. Switch to a different tab. Wait 30 s.
  Switch back to Recommendations.
- **Expected (after Bug #10 fix)**: the feed has refreshed to include
  the new likes.
- **Audit link**: ❌ Bug #10 — Recommendations feed is not refreshed on
  re-entry to the tab.
- **Currently FAILS — the cached first-load list is shown indefinitely;
  you must force-kill the app to see the new picks.**

### Step 15 — 401 in Recommendations shows SignedOut CTA *(currently FAILS — pending Bug #11, recommendations side)*
- **Steps**: Sign in. Open Recommendations. From dev tools, delete the
  JWT in DataStore (or revoke it server-side). Open Recommendations
  again.
- **Expected (after Bug #11 fix)**: a `SignedOut`-style state with a
  "Go to login" CTA.
- **Audit link**: ❌ Bug #11 — Recommendations lacks a `SignedOut`
  state on 401.
- **Currently FAILS — Recommendations shows a generic Error banner;
  no path back to Login from inside the screen.**

### Step 16 — 401 in Genre Stats shows SignedOut CTA *(currently FAILS — pending Bug #11, genre stats side)*
- **Steps**: Same setup as Step 15 but on the Genre Stats tab (Profile →
  "Genre Stats").
- **Expected (after Bug #11 fix)**: SignedOut CTA.
- **Audit link**: ❌ Bug #11 — Genre Stats also lacks `SignedOut`.
- **Currently FAILS — same shape, separate screen; both need fixing.**

### Step 17 — Genre Stats empty state copy vs Recommendations *(currently FAILS — pending Bug #16)*
- **Steps**: Brand-new account. Open Recommendations and capture the
  exact empty-state headline + subtitle. Open Genre Stats and capture
  the same.
- **Expected (after Bug #16 fix)**: the two screens share one canonical
  copy for the no-data-yet state.
- **Audit link**: ❌ Bug #16 — cold-start banner copy drift between
  Recommendations and Genre Stats.
- **Currently FAILS — Recommendations reads "Take a few minutes to
  swipe — your feed updates instantly." and Genre Stats reads "Make
  your first swipe to see your top genres here." Slight semantic drift
  but no shared source.**

### Step 18 — Genre Stats bars reflect real counts
- **Steps**: With an account that has ≥10 likes spanning ≥3 genres, open
  Profile → Genre Stats.
- **Expected**: vertical bars roughly proportional to the per-genre
  count.
- **Audit link**: ⚠️ Phase 5 — Genre Stats bar heights.
- **Currently PASSES.**

---

## Phase 6 — Profile Management

### Step 19 — JSON name-only submit
- **Steps**: Sign in. Profile → Edit Profile. Edit name; leave password
  blank; leave avatar alone. Tap Save.
- **Expected**: success banner reads `"Saved (name)."` Profile screen's
  header reflects the new name immediately.
- **Audit link**: ⚠️ Phase 6 — JSON name-only submit.
- **Currently PASSES.**

### Step 20 — Password change keeps the existing JWT
- **Steps**: Edit Profile. Fill all three password fields with valid
  values. Tap Save.
- **Expected**: success banner reads `"Saved (password)."` Confirm the
  existing JWT still works (Profile tab still loads — i.e. the JWT was
  not invalidated by the password change).
- **Audit link**: ⚠️ Phase 6 — password change JWT preservation.
- **Currently PASSES.**

### Step 21 — Avatar upload via small PNG *(currently FAILS — pending Bug #1, priority A)*
- **Steps**: Edit Profile. Tap "Change photo". Pick a small PNG (~200
  KB). Tap Save.
- **Expected (after Bug #1 fix)**: success banner reads `"Saved
  (avatar)."`. Avatar visible on Profile screen immediately.
- **Audit link**: ❌ Bug #1 — `imageName` hardcoded to `"avatar.jpg"`.
- **Currently FAILS — even though the upload may succeed against your
  test backend, the code sends `filename="avatar.jpg"` for every MIME
  type. Strict servers that infer content type from filename and reject
  PNG bytes named `.jpg` will return 422 here. Even if your dev backend
  accepts it, the contract is wrong.**

### Step 22 — Oversized avatar rejects client-side
- **Steps**: Edit Profile. Tap "Change photo". Pick a > 5 MB photo.
- **Expected**: inline error "Image must be 5 MB or smaller." No
  network call.
- **Audit link**: ⚠️ Phase 6 — oversized image pre-check.
- **Currently PASSES.**

### Step 23 — Wrong-MIME avatar rejects client-side
- **Steps**: Edit Profile. Tap "Change photo". Pick a `.gif`.
- **Expected**: inline error "Image must be JPG, JPEG, PNG, or WebP."
  No network call.
- **Audit link**: ⚠️ Phase 6 — wrong-MIME pre-check.
- **Currently PASSES.**

### Step 24 — Partial-password submit *(currently FAILS — pending Bug #8)*
- **Steps**: Edit Profile. Fill `current_password` only. Leave new /
  confirm blank. Tap Save.
- **Expected (after Bug #8 fix)**: all three password fields carry the
  same canonical error message ("All three password fields are required
  together."); no PUT is fired.
- **Audit link**: ❌ Bug #8 — partial-password error placement is
  misleading.
- **Currently FAILS — only `new_password` and `confirm_password` flags
  carry the message; `current_password` is silent because the field IS
  filled. The user reads this as "the new password is wrong".**

### Step 25 — Discard clears the picked thumb immediately *(currently FAILS — pending Bug #15)*
- **Steps**: Edit Profile. Tap "Change photo". Pick any image. Confirm
  the new thumbnail appears. Tap "Discard".
- **Expected (after Bug #15 fix)**: the picker thumbnail clears
  immediately. No stale-frame flicker.
- **Audit link**: ❌ Bug #15 — `Discard` doesn't invalidate Coil cache key.
- **Currently FAILS — the AsyncImage holds the previous bytes for one
  composition; a faint stale thumb flickers before the next recomposition
  catches up.**

### Step 26 — Brand-new user default avatar
- **Steps**: Sign in as a brand-new user. Open Edit Profile.
- **Expected**: avatar shows the seeded `default.png`. No null-avatar
  path.
- **Audit link**: ⚠️ Phase 6 — `default.png` non-null contract.
- **Currently PASSES.**

### Step 27 — Logout lands cleanly on Login
- **Steps**: Sign in. Profile → Logout.
- **Expected**: lands on Login screen; bottom bar gone; back-press
  doesn't reveal the protected screens.
- **Audit link**: ✅ verified Phase 1.
- **Currently PASSES.**

### Step 28 — Session-expired snackbar on 401 *(currently FAILS — pending Bug #4)*
- **Steps**: Sign in. From dev tools, manually delete the JWT from
  DataStore while the app is running. Trigger any protected request
  (e.g. open the Activity tab).
- **Expected (after Bug #4 fix)**: a snackbar reads "Your session
  expired. Please sign in again." and the user is navigated to the
  Login screen.
- **Audit link**: ❌ Bug #4 — no "session expired" snackbar.
- **Currently FAILS — logout is silent; user lands on Login with no
  explanation.**

---

## Phase 7 — Activity Timeline

### Step 29 — Timeline reverse-chronological with mixed history
- **Steps**: With an account that has a history of favorites +
  watch-later + watched, open the Activity tab.
- **Expected**: rows render newest-first across all three kinds, with
  the matching chip colour (rose / gold / mint).
- **Audit link**: ⚠️ Phase 7 — believable reverse-chronological feed.
- **Currently PASSES.**

### Step 30 — Timeline pull-to-refresh *(currently FAILS — pending Bug #5 race)*
- **Steps**: Force a slow first load by toggling airplane mode mid-deck.
  Pull-to-refresh Activity tab while the slow load is still in flight.
- **Expected (after Bug #5 fix)**: the second load's result wins, not
  the first's. State is consistent with the user's last action.
- **Audit link**: ❌ Bug #5 — `load()` race on rapid retries.
- **Currently FAILS — depending on which call resolves last, the final
  state can flip between `Success` and `Error` non-deterministically.**

### Step 31 — Anonymous landing on Activity (deep-link)
- **Steps**: Sign out. From a debugger or test intent, deep-link into
  the Activity tab.
- **Expected**: `SignedOut` card with "Go to login" CTA.
- **Audit link**: ⚠️ Phase 7 — anonymous SignedOut card.
- **Currently PASSES.**

### Step 32 — Signed-in empty Activity
- **Steps**: Sign in with a fresh account with zero engagement rows.
  Open the Activity tab.
- **Expected**: `Empty` card with a "Refresh" CTA; tapping Refresh keeps
  the screen in `Empty`.
- **Audit link**: ⚠️ Phase 7 — empty-state CTA.
- **Currently PASSES.**

---

## Cross-cutting

### Step 33 — Slow-network UX (skeletons + banners)
- **Steps**: Throttle to slow-3G (Android dev options → Network
  throttling → "Slow 3G"). Tap Discovery / Recommendations / Activity.
- **Expected**: skeletons appear within ~300 ms; never exceeds ~2 s
  without an inline banner; no jank.
- **Audit link**: ⚠️ Cross-cutting — slow-network UX.
- **Currently PASSES** for basic spinner's, pending
  instrumentation-driven perf checks for true SLA.

### Step 34 — Process death + back-stack restore
- **Steps**: Open Profile → tap Edit Profile → type a new name into the
  text field. Background the app. Force-kill via dev settings.
  Relaunch.
- **Expected**: app lands on the last visible `NavController` destination
  (Edit Profile with the typed name preserved where possible); the
  back-stack is intact.
- **Audit link**: ⚠️ Cross-cutting — process death + restore.
- **Currently PASSES** at the navigation level; the typed-name
  preservation is subject to Compose's `rememberSaveable` choices.

### Step 35 — Token rotation on logout/login
- **Steps**: Sign in. From dev tools, capture the JWT from DataStore.
  Sign out. Sign in again. Capture the new JWT. Compare.
- **Expected**: tokens differ; subsequent network calls succeed with the
  new token.
- **Audit link**: ⚠️ Cross-cutting — token rotation.
- **Currently PASSES.**

---

## Bugs NOT Covered by the Above

This document captures manual verification for the audit's 33-step
checklist plus two insertions (Steps 17, 25). The following bugs in
`PHASE_0_7_AUDIT.md`'s Blocking Issues have no portable manual step
because they are test-framework concerns:

- ❌ Bug #3 — `fallbackToDestructiveMigration()` — covered by
  `INSTRUMENTED_TEST_PLAN.md §A`.
- ❌ Bug #13 — `ImageCompressor` silently uploads 0-byte file — covered by
  `INSTRUMENTED_TEST_PLAN.md §F`.
- ❌ Bug #17 — `ProfileRepositoryImpl` preserves stale email — covered by
  `UNIT_TEST_PLAN.md §D`.
- Cross-cutting: zero `stringResource` use / `fallbackToDestructiveMigration`
  / `MappersPlaceholder.kt` — covered by `INSTRUMENTED_TEST_PLAN.md §H` and
  `UNIT_TEST_PLAN.md §A`.

---

## Full Regression Walkthrough

> **Use this script** before cutting any release build. It is a single
> continuous flow from clean install through every phase in dependency
> order. Run on the same emulator / device you'll ship to. Allow ~30 min.
> Estimate of where bugs are most likely to interrupt:
>
> - During Step 6 (anonymous swipes) you'll likely hit Bug #7 — note
>   the failure here, continue, and report it.
> - During Step 11 (cross-account contamination) you'll likely hit
>   Bug #2 — report and continue.
> - During Step 21 (avatar upload) you'll likely hit Bug #1 — report
>   and continue.

### Pre-flight

- [ ] Uninstall the app to clear DataStore + Room.
- [ ] Confirm a freshly-seeded test PHP backend (or your local XAMPP /
  Laragon) is reachable from the device. Verify `BuildConfig.API_BASE_URL`
  resolves on the emulator (`10.0.2.2`) or the physical device's LAN IP.
- [ ] Two test user accounts available: `user-a@example.com`,
  `user-b@example.com`. Both pre-seeded with one favorited drama
  each. Empty swipe history.

### A. Phase 1 — Auth

1. Cold-launch. **Step 0.1** — tap "Continue without account". Land on
   Home with bottom bar.
2. Sign out (Profile → Logout). Run **Step 0.2** and **Step 0.3**.

### B. Phase 2 — Catalog

3. **Step 1** — paging on Discover scrolls smooth.
4. **Step 2** — airplane mode mid-page → non-blocking footer error.
5. **Step 3** — genre chip tap → flag Bug #9 (likely fail today).

### C. Phase 3 — Swipe

6. **Step 4** — swipe 5 dramas, kill, relaunch → no repeats.
7. **Step 5** — airplane mode ON, swipe 2 likes, login, reopen → flag
   Bug #7 (likely fail today).
8. **Step 6** — airplane mode ON mid-swipe → flag Bug #6 (likely fail).

### D. Phase 4 — Engagement

9. Sign in as `user-a@example.com`.
10. **Step 7** — favorite a drama, force-kill, relaunch → still
    favorited.
11. **Step 9** — double-queue same drama → exactly one row.
12. **Step 10** — confirm no Unwatch affordance.
13. **Step 11** — sign out, sign in as `user-b@example.com`, open same
    drama Details → flag Bug #2 (likely fail today).
14. Sign back in as `user-a@example.com`.
15. **Step 8** — airplane mode OFF after anonymous favorites → all three
    on Activity tab.

### E. Phase 5 — Recommendations / Genre Stats

16. **Step 13** — Recommendations cold-start CTA.
17. **Step 14** — swipe 5 new likes, leave, come back → flag Bug #10.
18. **Step 15** — invalidate JWT, reopen Recommendations → flag Bug #11.
19. **Step 16** — invalidate JWT, reopen Genre Stats → flag Bug #11
    again.
20. **Step 17** — capture both screens' empty-state copy → flag Bug #16.
21. **Step 18** — bars reflect real counts.

### F. Phase 6 — Profile

22. **Step 19** — JSON name-only submit.
23. **Step 20** — password change keeps the JWT.
24. **Step 21** — avatar upload with small PNG → flag Bug #1.
25. **Step 22** — oversized avatar rejects client-side.
26. **Step 23** — GIF rejects client-side.
27. **Step 24** — partial password → flag Bug #8.
28. **Step 25** — Discard clears picker thumb → flag Bug #15.
29. **Step 26** — brand-new user default avatar.
30. **Step 27** — Logout lands cleanly.

### G. Phase 7 — Activity Timeline

31. **Step 29** — reverse-chronological feed.
32. **Step 30** — pull-to-refresh under slow load → flag Bug #5.
33. **Step 31** — anonymous deep-link → SignedOut card.
34. **Step 32** — empty-state with Refresh.

### H. Cross-cutting

35. **Step 28** — delete JWT mid-session → flag Bug #4.
36. **Step 33** — slow-3G skeletons + banners.
37. **Step 34** — process death + restore.
38. **Step 35** — token rotation.

### Sign-off

Once every ✅ step passes and every ❌ step's *expected-after-fix* behaviour
is documented, the build is shippable. Every ❌ step that fails today
must be filed against the corresponding bug number in
`PHASE_0_7_AUDIT.md` §9 before signing off.