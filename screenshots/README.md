# Screenshots

Drop your emulator / device captures into this folder using the filenames
below — they're already referenced from the main [README](../README.md)
`Screenshots` section.

## Capture checklist

| Filename              | Screen to capture                                      | Notes                                                                 |
|-----------------------|--------------------------------------------------------|-----------------------------------------------------------------------|
| `home.png`            | **Home** tab                                           | Land on it cold so the spotlight + trending + genre rails are visible |
| `discover.png`        | **Discover** tab (swipe deck)                          | Mid-swipe or after one card dismissed so the stacked preview is visible |
| `recommendations.png` | **Recommendations** tab                                | With at least 2–3 rows visible                                       |
| `details.png`         | **Drama details** screen                               | Scroll to a point where the banner, genres, and synopsis are visible  |
| `activity.png`        | **Activity** tab                                       | With at least a few entries showing different chip colors            |
| `profile.png`         | **Profile** tab                                        | Stat cards + top-genres chips visible                                 |
| `login.png`           | **Login** screen                                       | Empty form, hero banner showing                                      |
| `register.png`        | **Register** screen                                    | Empty form                                                            |
| `edit-profile.png`    | **Edit Profile** screen                                | Avatar + name + password fields visible                               |
| `genre-breakdown.png` | **Genre breakdown** screen                             | Mid-list with bars + the totals strip                                |

## Recommended capture settings

- **Resolution:** capture at the device's natural density (a Pixel 7
  emulator at `1080×2400` is a good baseline). Higher is fine — Markdown
  will scale.
- **Orientation:** portrait.
- **Theme:** Android system in **dark mode**, in-app time set to evening
  so the rose/gold accents read well.
- **Content state:** signed-in account with a handful of completed
  swipes / favorites so lists aren't empty (except Login / Register,
  which should be empty).
- **Tooling:** `adb shell screencap -p > out.png` from a connected
  device, or **Device Screen Capture** in Android Studio
  (`View → Tool Windows → Device Manager → 📷`).

> The README image paths use raw GitHub-relative URLs (e.g.
> `screenshots/home.png`), so once you commit the PNGs the gallery in
> the main README will render automatically — no other changes needed.