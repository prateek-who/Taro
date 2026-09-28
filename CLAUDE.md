# Stepsy

Fork of Stepsy, an Android step counter. Kotlin, min SDK 26, target SDK 35.

## Style rules (very important)

- Never use semicolons or em dashes. This covers code, comments, docs and commit messages.
- Comment only when strictly necessary. Never write narration style comments.
- UI is Jetpack Compose only. Never add XML layouts. Progress is tracked in `COMPOSE_MIGRATION.md`.
- Build screens from the shared composables in `ui/components`. If a pattern shows up twice, it becomes a shared component.

## Build

- JDK 21. `gradle/gradle-daemon-jvm.properties` pins the Gradle daemon to 21 and `app/build.gradle` sets `jvmToolchain(21)`.
- Flavors: `full` (Play Services activity recognition for the vehicle filter) and `foss` (stub, no Play Services).
- Debug builds use the `.debug` application ID suffix.
- `./gradlew assembleFossDebug` is the quickest compile check.

## How it works

- `service/MotionService` is a foreground service listening to `Sensor.TYPE_STEP_COUNTER`. The sensor reports steps since boot, so the service adds the delta since the last reading. A lower reading means a reboot and only resets the baseline.
- Steps are throttled into DataStore (`util/AppPreferences`) and SQLite (`util/Database`, one row per `YYYY-MM-DD` date).
- Step length is the user value, otherwise `Util.estimateStepLength`: height times 0.415, averaged with an inseam based estimate (inseam times 0.415 / 0.46) when a plausible leg length is set. Distance is steps times step length.
- First launch shows `OnboardingDialog` (height, weight, optional leg length). Permission prompts wait until it closes.
- Calories are steps times weight in kg times 0.0005 (`util/Util.stepsToCalories`).

## UI layout

- `ui/theme`: `StepsyTheme`, colors from `res/values*/colors.xml`, Chivo font family.
- `ui/components`: `StepsyScaffold`, `SettingsCard`, `SectionHeader`, `PreferenceRow`, `SwitchRow`, `StepsySwitch`, `StepsySlider`, `ToggleGroup`, `RangeChip`, `StepsBarChart`, `MonthCalendar`, `PauseDialogs` and the dialogs in `Dialogs.kt`.
- Navigation is Voyager. `MainActivity` hosts the `Navigator` with `HomeScreen` as root. Settings, Backup, Achievements and Daily Goals are Voyager `Screen` objects.
- Remaining activities (`MainActivity`, widget setup, tile dialog) extend `AppCompatActivity`, use `Theme.stepsy` and only handle lifecycle, services and permissions.
- Widgets are Glance (`widget/StepsWidgets.kt`). Receivers in `ui/WidgetProviders.kt` keep their old class names so placed widgets survive updates. `WidgetManager` pushes step counts into Glance state.
- `MainActivity` renders `MainScreen.kt`. Its data comes from plain functions in `MainData.kt`.
- Pause and resume go through `util/PauseController` for both the main screen and the quick settings tile.
- `AppTest` fails on the original code too, because it reads `AppPreferences` before `init`.
