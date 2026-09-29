<div align="center">

<img src="images/taro-logo.png" alt="Taro" width="180"/>

# Taro

**Steps, energy, weight and sleep in one calm, offline Android app.**

[![Latest release](https://img.shields.io/github/v/release/prateek-who/stepsytoo?label=latest&style=for-the-badge)](https://github.com/prateek-who/stepsytoo/releases/latest)
[![License](https://img.shields.io/github/license/prateek-who/stepsytoo?style=for-the-badge)](LICENSE)

</div>

Taro counts your steps with the phone's own step sensor and turns them into distance, calories and daily goals you actually want to hit. It also estimates your total daily energy, tracks your weight trend and logs your sleep. Everything stays on your phone.

## Features

- **Steps and goals**: a live goal ring, streaks, a heatmap calendar and charts for any day, week, month or year
- **Accurate distance**: step length from your height and leg length, refined by guided GPS calibration and barometer climb
- **Energy**: resting burn (Mifflin-St Jeor), active burn from your steps, logged workouts including your own custom activities, and a daily calorie target for cutting, maintaining or bulking
- **Weight**: a smoothed trend line, weekly rate and honest feedback on whether your pace matches your goal, with optional weigh-in reminders
- **Sleep**: nights estimated from when your phone was idle, confirmed or edited by you, with averages, bedtime consistency and streaks
- **Backfill anything**: add or fix past weigh-ins, workouts and nights with a date picker
- **Full backups**: one file holds every step, weigh-in, night, workout and setting, with automatic scheduled backups to a folder you choose
- **Widgets, quick settings tile and notifications** to keep progress at hand

## Privacy

Taro has no internet permission, so it cannot send your data anywhere. There are no accounts, no analytics and no ads.

## Google Play Services

Taro is open source under GPL-3.0. If your phone has Google Play Services, Taro uses it on device for two extras:

- **Vehicle filter**: Activity Recognition spots when you are in a car or on a bus and drops the fake steps that vibration causes
- **Sleep detection**: the Sleep API supplements Taro's own sleep estimate

Without Play Services these two features switch off and everything else works the same. Because the APK includes these Google libraries, Taro is not eligible for F-Droid.

## Download and updates

Get the latest APK from [GitHub Releases](https://github.com/prateek-who/stepsytoo/releases).

Taro never goes online, so it cannot check for updates itself. To get updates automatically, use [Obtainium](https://github.com/ImranR98/Obtainium), a free open source app that watches GitHub releases and installs new versions for you:

1. Install Obtainium from its [releases page](https://github.com/ImranR98/Obtainium/releases).
2. On your phone, open [this link](https://intradeus.github.io/http-protocol-redirector?r=obtainium://add/github.com/prateek-who/stepsytoo) to add Taro, or add the source `https://github.com/prateek-who/Taro` by hand.
3. Obtainium notifies you when a new release is out and installs it. Updates keep your data because every release is signed with the same key.

Coming from Stepsy? Export a backup in Stepsy (Backup, then Back up now), install Taro and tap Restore a backup on the welcome screen (or from the settings).

## Automation

Pause or resume step counting from apps like Tasker with a broadcast intent.

|    Field    |                                Value                                |
|:-----------:|:-------------------------------------------------------------------:|
| Intent type |                              Broadcast                              |
|   Package   |                         `com.prateek.taro`                          |
|   Action    | `com.prateek.taro.action.PAUSE` or `com.prateek.taro.action.RESUME` |

## Building

Taro uses Kotlin, Jetpack Compose and JDK 21.

```
./gradlew assembleDebug        # debug build
./gradlew testDebugUnitTest    # unit tests
./gradlew assembleRelease      # release build, signed when a keystore is configured
```

## Credits

Taro started as a fork of [Stepsy](https://github.com/nvllz/stepsy) by [nvllz](https://github.com/nvllz), which is based on [MotionMate](https://github.com/0xf4b1/motionmate) by [0xf4b1](https://github.com/0xf4b1). Both are GPL-3.0, and so is Taro.
