# Compose Migration

Goal: every screen in Stepsy is Jetpack Compose. No new XML layouts get added. Resources such as strings, colors, drawables, fonts and the manifest stay as XML.

## Ground rules

- `MainActivity` hosts a Voyager `Navigator`. Screens are Voyager `Screen` objects.
- App screens extend `AppCompatActivity` so the in-app theme and per-app language settings keep working.
- Every activity uses the AppCompat based `Theme.stepsy` (the tile dialog uses `Theme.stepsy.Transparent`) and wraps content in `StepsyTheme`.
- Shared building blocks live in `ui/components` and every screen is assembled from them. New patterns get added there first. Theme, colors and typography live in `ui/theme`.
- No semicolons, no em dashes, comments only when strictly necessary and never narration style.

## Checklist

### 1. Setup
- [x] Compose BOM, Material 3, activity-compose, lifecycle-runtime-compose
- [x] Kotlin Compose compiler plugin
- [x] `StepsyTheme` built from the existing color resources, Chivo typography
- [x] Shared components: scaffold, settings card, switch row, slider, toggle group
- [x] Preference rows, section headers and shared dialogs (single choice, number input, feet and inches, message, HTML)

### 2. Small screens
- [x] `DailyGoalsActivity`
- [x] `TileDialogActivity` (Compose dialog and time picker)
- [x] Widget setup screens, merged into one `WidgetConfigureActivity.kt`

### 3. Medium screens
- [x] `AchievementsActivity` (`activity_achievements`, `item_milestone_achievement`, `item_milestone_achievement_alt`)
- [x] `BackupActivity` (`activity_backup`)

### 4. Settings
- [x] `SettingsActivity`
- [x] Dialogs built in code inside `SettingsActivity`
- [x] Dialogs built in code inside `AppPreferences` (the version dialog was dead code and got removed)

### 5. Main screen
- [x] `MainActivity` (`activity_main`, `content_main`, `cardview_text`, `calendar`, `year_button`)
- [x] Replace MPAndroidChart `Chart` with a Compose `Canvas` bar chart
- [x] Replace `CalendarViewScrollable` with a Compose calendar
- [x] Remove the unused `TextItemAdapter` and `RecyclerView`

### 6. Cleanup
- [x] Remove `viewBinding`, MPAndroidChart, Flexbox and the JitPack repository
- [x] Trim `themes.xml` down to `Theme.stepsy` and `Theme.stepsy.Transparent`
- [x] Drop Material Components

### 7. Optional
- [x] Home screen widgets to Jetpack Glance (`widget/StepsWidgets.kt`), no XML layouts left
- [x] Single activity navigation with Voyager (widget setup screens and the tile dialog stay activities because the system launches them)
