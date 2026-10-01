<div align="center">

<img src="images/taro-logo.png" alt="Taro" width="180"/>

# Taro

**Steps, food, energy, weight and sleep in one calm, offline Android app.**

[![Latest release](https://img.shields.io/github/v/release/prateek-who/Taro?label=latest&style=for-the-badge)](https://github.com/prateek-who/Taro/releases/latest)
[![License](https://img.shields.io/github/license/prateek-who/Taro?style=for-the-badge)](LICENSE)

</div>

Taro counts your steps, logs what you eat and weigh, and works out how much you really burn. It learns that from your own data instead of trusting a formula, so the number it tells you to eat gets more accurate the longer you use it. Everything stays on your phone.

## Features

- **Steps and goals**: a live goal ring, streaks, a heatmap calendar, steps by hour and charts for any day, week, month or year. Changing your goal never rewrites past days
- **Accurate distance**: step length from your height and leg length, refined by a known distance walk or guided GPS calibration, plus barometer climb
- **Food logging**: about 7,700 built in foods (USDA and a curated Indian list), grams first with ml, cups and spoons, your own meals, recipes built from ingredients, and a nutrition label scanner that reads the photo on your phone
- **Energy that adapts to you**: resting burn, walking, workouts and digestion through the day, a calorie target for cutting, maintaining or bulking, and a filter that compares your food logs with your weight to learn your real daily burn, shown with an honest plus or minus range
- **Weight**: a trend with water swings filtered out, weekly rate, a four week forecast and feedback on whether your pace matches your goal, with optional weigh-in reminders
- **Sleep**: nights estimated from when your phone was idle, confirmed or edited by you, with averages, bedtime consistency and streaks
- **Badges**: around 160 to earn, from step streaks to food habits, plus calisthenics skill and strength trees you tick off yourself
- **Tap to explain**: tap any number to see what it means and where it comes from
- **Backfill anything**: add or fix past food, weigh-ins, workouts and nights
- **Full backups**: one file holds every step, meal, weigh-in, night, workout and setting, with automatic scheduled backups to a folder you choose
- **Widgets, quick settings tile and notifications** to keep progress at hand

## Privacy

Taro has no internet permission, so it cannot send your data anywhere. There are no accounts, no analytics and no ads.

## Google Play Services

Taro is open source under GPL-3.0. If your phone has Google Play Services, Taro uses it on device for two extras:

- **Vehicle filter**: Activity Recognition spots when you are in a car or on a bus and drops the fake steps that vibration causes
- **Sleep detection**: the Sleep API supplements Taro's own sleep estimate

Without Play Services these two features switch off and everything else works the same. Because the APK includes these Google libraries, Taro is not eligible for F-Droid.

## Download and updates

Get the latest APK from [GitHub Releases](https://github.com/prateek-who/Taro/releases).

Taro never goes online, so it cannot check for updates itself. To get updates automatically, use [Obtainium](https://github.com/ImranR98/Obtainium), a free open source app that watches GitHub releases and installs new versions for you:

1. Install Obtainium from its [releases page](https://github.com/ImranR98/Obtainium/releases).
2. On your phone, open [this link](https://intradeus.github.io/http-protocol-redirector?r=obtainium://add/github.com/prateek-who/Taro) to add Taro, or add the source `https://github.com/prateek-who/Taro` by hand.
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
