package com.prateek.taro.achievements

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.prateek.taro.R
import com.prateek.taro.energy.TrendPoint
import com.prateek.taro.sleep.Night
import com.prateek.taro.sleep.SleepInsights
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class BadgeCategory(@StringRes val label: Int) {
    STEPS(R.string.badge_cat_steps),
    GOALS(R.string.badge_cat_goals),
    CLIMB(R.string.badge_cat_climb),
    ENERGY(R.string.badge_cat_energy),
    WEIGHT(R.string.badge_cat_weight),
    SLEEP(R.string.badge_cat_sleep),
    EXPLORER(R.string.badge_cat_explorer),
}

enum class BadgeUnit { STEPS, KM, METERS, DAYS, KCAL, COUNT, KG, NIGHTS, FLAG }

data class DayStat(
    val date: LocalDate,
    val steps: Int,
    val distanceM: Double = 0.0,
    val activeKcal: Double = 0.0,
    val ascentM: Double = 0.0,
    val earlySteps: Int = 0,
    val lateSteps: Int = 0,
)

data class AchievementInputs(
    val days: List<DayStat> = emptyList(),
    val goal: Int = 0,
    val workouts: List<Pair<LocalDate, Double>> = emptyList(),
    val weights: List<TrendPoint> = emptyList(),
    val losingWeight: Boolean = true,
    val nights: List<Night> = emptyList(),
    val calibratedOn: LocalDate? = null,
    val accuracyCheckedOn: LocalDate? = null,
    val backedUpOn: LocalDate? = null,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
)

data class BadgeProgress(val value: Double, val earnedOn: LocalDate?)

data class BadgeDef(
    val id: String,
    val category: BadgeCategory,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @DrawableRes val icon: Int,
    val target: Double,
    val unit: BadgeUnit,
    val rule: (AchievementInputs) -> BadgeProgress,
)

data class BadgeResult(val def: BadgeDef, val progress: BadgeProgress) {
    val earned: Boolean get() = progress.earnedOn != null
    val fraction: Float get() = (progress.value / def.target).toFloat().coerceIn(0f, 1f)
}

object Rules {
    fun singleDay(days: List<DayStat>, target: Double, value: (DayStat) -> Double): BadgeProgress {
        val sorted = days.sortedBy { it.date }
        return BadgeProgress(
            value = sorted.maxOfOrNull(value) ?: 0.0,
            earnedOn = sorted.firstOrNull { value(it) >= target }?.date,
        )
    }

    fun cumulative(days: List<DayStat>, target: Double, value: (DayStat) -> Double): BadgeProgress {
        var total = 0.0
        var earned: LocalDate? = null
        for (day in days.sortedBy { it.date }) {
            total += value(day)
            if (earned == null && total >= target) earned = day.date
        }
        return BadgeProgress(total, earned)
    }

    fun nth(dates: List<LocalDate>, count: Int): BadgeProgress {
        val sorted = dates.sorted()
        return BadgeProgress(sorted.size.toDouble(), sorted.getOrNull(count - 1))
    }

    fun streak(dates: Collection<LocalDate>, length: Int): BadgeProgress {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        var earned: LocalDate? = null
        for (date in dates.toSortedSet()) {
            run = if (previous != null && date == previous.plusDays(1)) run + 1 else 1
            if (run > best) best = run
            if (earned == null && run >= length) earned = date
            previous = date
        }
        return BadgeProgress(best.toDouble(), earned)
    }

    fun flag(date: LocalDate?) = BadgeProgress(if (date != null) 1.0 else 0.0, date)

    fun goalDays(inputs: AchievementInputs): List<LocalDate> =
        if (inputs.goal <= 0) emptyList() else inputs.days.filter { it.steps >= inputs.goal }.map { it.date }

    fun perfectWeek(inputs: AchievementInputs): BadgeProgress {
        val goalDays = goalDays(inputs).toSet()
        val weeks = goalDays.groupBy { it.with(TemporalAdjusters.previousOrSame(inputs.firstDayOfWeek)) }
        val best = weeks.values.maxOfOrNull { it.size } ?: 0
        val earned = weeks.filterValues { it.size >= 7 }.keys.minOrNull()?.plusDays(6)
        return BadgeProgress(best.toDouble(), earned)
    }

    fun weekendWarrior(inputs: AchievementInputs): BadgeProgress {
        val goalDays = goalDays(inputs).toSet()
        val sunday = goalDays.filter { it.dayOfWeek == DayOfWeek.SUNDAY && it.minusDays(1) in goalDays }.minOrNull()
        return flag(sunday)
    }

    fun weightMoved(inputs: AchievementInputs, kg: Double): BadgeProgress {
        val points = inputs.weights.sortedBy { it.date }
        val start = points.firstOrNull()?.trend ?: return BadgeProgress(0.0, null)
        var best = 0.0
        var earned: LocalDate? = null
        for (point in points) {
            val moved = if (inputs.losingWeight) start - point.trend else point.trend - start
            if (moved > best) best = moved
            if (earned == null && moved >= kg - 1e-9) earned = point.date
        }
        return BadgeProgress(best, earned)
    }

    fun steadyBedtimes(nights: List<Night>, spreadMinutes: Double): BadgeProgress {
        val sorted = nights.sortedBy { it.wakeDate }
        for (end in 6 until sorted.size) {
            val window = sorted.subList(end - 6, end + 1)
            val consecutive = window.zipWithNext().all { (a, b) -> b.wakeDate == a.wakeDate.plusDays(1) }
            val spread = SleepInsights.bedtimeSpreadMinutes(window)
            if (consecutive && spread != null && spread <= spreadMinutes) return flag(window.last().wakeDate)
        }
        return flag(null)
    }
}

object Badges {
    private fun def(
        id: String,
        category: BadgeCategory,
        title: Int,
        description: Int,
        icon: Int,
        target: Double,
        unit: BadgeUnit,
        rule: (AchievementInputs) -> BadgeProgress,
    ) = BadgeDef(id, category, title, description, icon, target, unit, rule)

    private fun daySteps(id: String, title: Int, steps: Int) =
        def(id, BadgeCategory.STEPS, title, R.string.badge_desc_day_steps, R.drawable.ic_steps, steps.toDouble(), BadgeUnit.STEPS) {
            Rules.singleDay(it.days, steps.toDouble()) { day -> day.steps.toDouble() }
        }

    private fun totalSteps(id: String, title: Int, steps: Int) =
        def(id, BadgeCategory.STEPS, title, R.string.badge_desc_total_steps, R.drawable.ic_badge_medal, steps.toDouble(), BadgeUnit.STEPS) {
            Rules.cumulative(it.days, steps.toDouble()) { day -> day.steps.toDouble() }
        }

    private fun goalCount(id: String, title: Int, days: Int) =
        def(id, BadgeCategory.GOALS, title, R.string.badge_desc_goal_days, R.drawable.ic_small_target, days.toDouble(), BadgeUnit.DAYS) {
            Rules.nth(Rules.goalDays(it), days)
        }

    private fun goalStreak(id: String, title: Int, days: Int) =
        def(id, BadgeCategory.GOALS, title, R.string.badge_desc_streak, R.drawable.ic_calories, days.toDouble(), BadgeUnit.DAYS) {
            Rules.streak(Rules.goalDays(it), days)
        }

    private fun climbTotal(id: String, title: Int, meters: Int) =
        def(id, BadgeCategory.CLIMB, title, R.string.badge_desc_climb_total, R.drawable.ic_badge_climb, meters.toDouble(), BadgeUnit.METERS) {
            Rules.cumulative(it.days, meters.toDouble()) { day -> day.ascentM }
        }

    private fun workouts(id: String, title: Int, count: Int) =
        def(id, BadgeCategory.ENERGY, title, R.string.badge_desc_workouts, R.drawable.ic_badge_bolt, count.toDouble(), BadgeUnit.COUNT) {
            Rules.nth(it.workouts.map { workout -> workout.first }, count)
        }

    private fun activeDay(id: String, title: Int, kcal: Int) =
        def(id, BadgeCategory.ENERGY, title, R.string.badge_desc_active_kcal, R.drawable.ic_calories, kcal.toDouble(), BadgeUnit.KCAL) { inputs ->
            val logged = inputs.workouts.groupBy({ it.first }) { it.second }.mapValues { it.value.sum() }
            val days = inputs.days.map { it.copy(activeKcal = it.activeKcal + (logged[it.date] ?: 0.0)) }
            Rules.singleDay(days, kcal.toDouble()) { day -> day.activeKcal }
        }

    private fun weighIns(id: String, title: Int, count: Int) =
        def(id, BadgeCategory.WEIGHT, title, R.string.badge_desc_weigh_ins, R.drawable.ic_weight, count.toDouble(), BadgeUnit.COUNT) {
            Rules.nth(it.weights.map { point -> point.date }, count)
        }

    private fun weightMoved(id: String, title: Int, kg: Double) =
        def(id, BadgeCategory.WEIGHT, title, R.string.badge_desc_weight_moved, R.drawable.ic_badge_hike, kg, BadgeUnit.KG) {
            Rules.weightMoved(it, kg)
        }

    private fun nights(id: String, title: Int, count: Int) =
        def(id, BadgeCategory.SLEEP, title, R.string.badge_desc_nights, R.drawable.ic_sleep, count.toDouble(), BadgeUnit.NIGHTS) {
            Rules.nth(it.nights.map { night -> night.wakeDate }, count)
        }

    private fun healthyStreak(id: String, title: Int, count: Int) =
        def(id, BadgeCategory.SLEEP, title, R.string.badge_desc_healthy_nights, R.drawable.ic_badge_night, count.toDouble(), BadgeUnit.NIGHTS) {
            Rules.streak(it.nights.filter(SleepInsights::inRange).map { night -> night.wakeDate }, count)
        }

    val all: List<BadgeDef> = listOf(
        daySteps("day_10k", R.string.badge_day_10k, 10_000),
        daySteps("day_15k", R.string.badge_day_15k, 15_000),
        daySteps("day_20k", R.string.badge_day_20k, 20_000),
        daySteps("day_25k", R.string.badge_day_25k, 25_000),
        daySteps("day_30k", R.string.badge_day_30k, 30_000),
        def("half_marathon", BadgeCategory.STEPS, R.string.badge_half_marathon, R.string.badge_desc_day_km, R.drawable.ic_badge_hike, 21.1, BadgeUnit.KM) {
            Rules.singleDay(it.days, 21_097.0) { day -> day.distanceM }.let { p -> p.copy(value = p.value / 1000) }
        },
        def("marathon", BadgeCategory.STEPS, R.string.badge_marathon, R.string.badge_desc_day_km, R.drawable.ic_badge_hike, 42.2, BadgeUnit.KM) {
            Rules.singleDay(it.days, 42_195.0) { day -> day.distanceM }.let { p -> p.copy(value = p.value / 1000) }
        },
        totalSteps("total_100k", R.string.badge_total_100k, 100_000),
        totalSteps("total_500k", R.string.badge_total_500k, 500_000),
        totalSteps("total_1m", R.string.badge_total_1m, 1_000_000),
        totalSteps("total_2_5m", R.string.badge_total_2_5m, 2_500_000),
        totalSteps("total_5m", R.string.badge_total_5m, 5_000_000),
        totalSteps("total_10m", R.string.badge_total_10m, 10_000_000),

        goalCount("goal_first", R.string.badge_goal_first, 1),
        goalCount("goal_7", R.string.badge_goal_7, 7),
        goalCount("goal_30", R.string.badge_goal_30, 30),
        goalCount("goal_100", R.string.badge_goal_100, 100),
        goalCount("goal_365", R.string.badge_goal_365, 365),
        goalStreak("streak_3", R.string.badge_streak_3, 3),
        goalStreak("streak_7", R.string.badge_streak_7, 7),
        goalStreak("streak_14", R.string.badge_streak_14, 14),
        goalStreak("streak_30", R.string.badge_streak_30, 30),
        goalStreak("streak_60", R.string.badge_streak_60, 60),
        goalStreak("streak_100", R.string.badge_streak_100, 100),
        def("double_goal", BadgeCategory.GOALS, R.string.badge_double_goal, R.string.badge_desc_multiplier, R.drawable.ic_badge_star, 2.0, BadgeUnit.COUNT) {
            if (it.goal <= 0) BadgeProgress(0.0, null) else Rules.singleDay(it.days, 2.0) { day -> day.steps.toDouble() / it.goal }
        },
        def("triple_goal", BadgeCategory.GOALS, R.string.badge_triple_goal, R.string.badge_desc_multiplier, R.drawable.ic_badge_star, 3.0, BadgeUnit.COUNT) {
            if (it.goal <= 0) BadgeProgress(0.0, null) else Rules.singleDay(it.days, 3.0) { day -> day.steps.toDouble() / it.goal }
        },
        def("perfect_week", BadgeCategory.GOALS, R.string.badge_perfect_week, R.string.badge_desc_perfect_week, R.drawable.ic_badge_week, 7.0, BadgeUnit.DAYS, Rules::perfectWeek),
        def("weekend_warrior", BadgeCategory.GOALS, R.string.badge_weekend_warrior, R.string.badge_desc_weekend, R.drawable.ic_badge_week, 1.0, BadgeUnit.FLAG, Rules::weekendWarrior),

        def("floors_10", BadgeCategory.CLIMB, R.string.badge_floors_10, R.string.badge_desc_floors_day, R.drawable.ic_badge_climb, 30.0, BadgeUnit.METERS) {
            Rules.singleDay(it.days, 30.0) { day -> day.ascentM }
        },
        def("floors_30", BadgeCategory.CLIMB, R.string.badge_floors_30, R.string.badge_desc_floors_day, R.drawable.ic_badge_climb, 90.0, BadgeUnit.METERS) {
            Rules.singleDay(it.days, 90.0) { day -> day.ascentM }
        },
        climbTotal("eiffel", R.string.badge_eiffel, 330),
        climbTotal("burj", R.string.badge_burj, 828),
        climbTotal("kilimanjaro", R.string.badge_kilimanjaro, 5_895),
        climbTotal("everest", R.string.badge_everest, 8_849),

        workouts("workout_first", R.string.badge_workout_first, 1),
        workouts("workout_10", R.string.badge_workout_10, 10),
        workouts("workout_50", R.string.badge_workout_50, 50),
        workouts("workout_100", R.string.badge_workout_100, 100),
        activeDay("active_500", R.string.badge_active_500, 500),
        activeDay("active_1000", R.string.badge_active_1000, 1_000),

        weighIns("weigh_first", R.string.badge_weigh_first, 1),
        weighIns("weigh_30", R.string.badge_weigh_30, 30),
        weighIns("weigh_100", R.string.badge_weigh_100, 100),
        def("weigh_streak_7", BadgeCategory.WEIGHT, R.string.badge_weigh_streak_7, R.string.badge_desc_weigh_streak, R.drawable.ic_weight, 7.0, BadgeUnit.DAYS) {
            Rules.streak(it.weights.map { point -> point.date }, 7)
        },
        weightMoved("moved_1", R.string.badge_moved_1, 1.0),
        weightMoved("moved_3", R.string.badge_moved_3, 3.0),
        weightMoved("moved_5", R.string.badge_moved_5, 5.0),
        weightMoved("moved_10", R.string.badge_moved_10, 10.0),

        nights("night_first", R.string.badge_night_first, 1),
        nights("night_30", R.string.badge_night_30, 30),
        nights("night_100", R.string.badge_night_100, 100),
        healthyStreak("healthy_3", R.string.badge_healthy_3, 3),
        healthyStreak("healthy_7", R.string.badge_healthy_7, 7),
        healthyStreak("healthy_14", R.string.badge_healthy_14, 14),
        def("steady_bedtime", BadgeCategory.SLEEP, R.string.badge_steady_bedtime, R.string.badge_desc_steady_bedtime, R.drawable.ic_badge_night, 1.0, BadgeUnit.FLAG) {
            Rules.steadyBedtimes(it.nights, 30.0)
        },

        def("calibrated", BadgeCategory.EXPLORER, R.string.badge_calibrated, R.string.badge_desc_calibrated, R.drawable.ic_calibrate, 1.0, BadgeUnit.FLAG) {
            Rules.flag(it.calibratedOn)
        },
        def("accuracy", BadgeCategory.EXPLORER, R.string.badge_accuracy, R.string.badge_desc_accuracy, R.drawable.ic_accuracy, 1.0, BadgeUnit.FLAG) {
            Rules.flag(it.accuracyCheckedOn)
        },
        def("backup", BadgeCategory.EXPLORER, R.string.badge_backup, R.string.badge_desc_backup, R.drawable.ic_small_backup, 1.0, BadgeUnit.FLAG) {
            Rules.flag(it.backedUpOn)
        },
        def("early_bird", BadgeCategory.EXPLORER, R.string.badge_early_bird, R.string.badge_desc_early_bird, R.drawable.ic_badge_early, 1_000.0, BadgeUnit.STEPS) {
            Rules.singleDay(it.days, 1_000.0) { day -> day.earlySteps.toDouble() }
        },
        def("night_owl", BadgeCategory.EXPLORER, R.string.badge_night_owl, R.string.badge_desc_night_owl, R.drawable.ic_badge_night, 1_000.0, BadgeUnit.STEPS) {
            Rules.singleDay(it.days, 1_000.0) { day -> day.lateSteps.toDouble() }
        },
        def("explorer_all", BadgeCategory.EXPLORER, R.string.badge_explorer_all, R.string.badge_desc_explorer_all, R.drawable.ic_badge_explore, 3.0, BadgeUnit.COUNT) {
            val dates = listOfNotNull(it.calibratedOn, it.accuracyCheckedOn, it.backedUpOn)
            BadgeProgress(dates.size.toDouble(), if (dates.size == 3) dates.max() else null)
        },
    )

    fun evaluate(inputs: AchievementInputs): List<BadgeResult> = all.map { BadgeResult(it, it.rule(inputs)) }
}
