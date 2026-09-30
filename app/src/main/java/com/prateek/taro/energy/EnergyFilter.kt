package com.prateek.taro.energy

import java.time.LocalDate
import kotlin.math.sqrt

data class FilterDay(
    val date: LocalDate,
    val intake: Double?,
    val carbs: Double?,
    val sleepMinutes: Long,
    val active: Double?,
    val logged: Double,
    val weight: Double?,
)

data class EnergyFactors(
    val resting: Double,
    val activity: Double,
    val workout: Double,
    val restingSd: Double,
    val activitySd: Double,
    val workoutSd: Double,
    val covariance: Double,
) {
    fun need(restingKcal: Double, activeKcal: Double, workoutKcal: Double = 0.0): Double =
        Metabolism.withDigestion(resting * restingKcal + activity * activeKcal + workout * workoutKcal)

    fun needSd(restingKcal: Double, activeKcal: Double, workoutKcal: Double = 0.0): Double {
        val variance = restingKcal * restingKcal * restingSd * restingSd +
            activeKcal * activeKcal * activitySd * activitySd +
            workoutKcal * workoutKcal * workoutSd * workoutSd +
            2 * restingKcal * activeKcal * covariance
        return Metabolism.withDigestion(sqrt(variance.coerceAtLeast(0.0)))
    }

    fun encode(): String = listOf(resting, activity, workout, restingSd, activitySd, workoutSd, covariance).joinToString(",")

    companion object {
        val NEUTRAL = EnergyFactors(
            1.0, 1.0, 1.0,
            EnergyFilter.PRIOR_RESTING_SD, EnergyFilter.PRIOR_ACTIVITY_SD, EnergyFilter.PRIOR_WORKOUT_SD, 0.0,
        )

        fun decode(text: String?): EnergyFactors {
            val parts = text.orEmpty().split(',').mapNotNull { it.toDoubleOrNull() }
            return if (parts.size == 7) EnergyFactors(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]) else NEUTRAL
        }
    }
}

data class BodyState(val date: LocalDate, val tissue: Double, val water: Double, val tissueSd: Double)

data class BodyModel(
    val factors: EnergyFactors,
    val states: List<BodyState>,
    val loggedDays: Int,
    val weighIns: Int,
    val recentIntake: Double?,
    val recentActive: Double,
    val recentWorkouts: Double,
    val workoutDays: Int,
    val restingAtTissue: Double,
) {
    val latest: BodyState get() = states.last()
    val learning: Boolean get() = loggedDays < EnergyFilter.SETTLED_DAYS || weighIns < EnergyFilter.SETTLED_WEIGH_INS

    fun dailyChangeKg(): Double? = recentIntake?.let {
        ((1 - Metabolism.DIGESTION_SHARE) * it - factors.resting * restingAtTissue - factors.activity * recentActive - factors.workout * recentWorkouts) /
            Metabolism.KCAL_PER_KG
    }

    fun forecastKg(days: Int): Double? = dailyChangeKg()?.let { latest.tissue + it * days }
}

object EnergyFilter {
    const val PRIOR_RESTING_SD = 0.10
    const val PRIOR_ACTIVITY_SD = 0.25
    const val PRIOR_WORKOUT_SD = 0.3
    const val SETTLED_DAYS = 14
    const val SETTLED_WEIGH_INS = 8
    private const val RECENT_DAYS = 14
    private const val MIN_RECENT_LOGGED = 7

    private const val T = 0
    private const val W = 1
    private const val R = 2
    private const val A = 3
    private const val O = 4
    private const val N = 5

    private const val WATER_KEEP = 0.6
    private const val WATER_PER_CARB = 2.0
    private const val CARB_AVERAGE_WEIGHT = 0.1
    private const val MODEL_SD_KG = 0.03
    private const val WATER_SD_KG = 0.2
    private const val RESTING_DRIFT = 0.004
    private const val ACTIVITY_DRIFT = 0.008
    private const val WORKOUT_DRIFT = 0.008
    private const val WORKOUT_ERROR = 0.15
    private const val SCALE_SD_KG = 0.15
    private const val LOGGED_INTAKE_ERROR = 0.08
    private const val UNKNOWN_INTAKE_ERROR = 0.3
    private const val ACTIVE_ERROR = 0.1
    private const val UNKNOWN_ACTIVE_ERROR = 0.5
    private const val OUTLIER_SIGMAS_SQUARED = 25.0

    fun isLogged(intake: Double?, restingKcal: Double) = intake != null && intake >= restingKcal * 0.5

    fun run(days: List<FilterDay>, restingAt: (Double) -> Double): BodyModel? {
        val sorted = days.sortedBy { it.date }
        val startIndex = sorted.indexOfFirst { it.weight != null }
        if (startIndex < 0) return null
        val history = sorted.drop(startIndex)

        val x = doubleArrayOf(history.first().weight!!, 0.0, 1.0, 1.0, 1.0)
        val p = DoubleArray(N * N)
        p[T * N + T] = 0.3 * 0.3
        p[W * N + W] = 0.4 * 0.4
        p[R * N + R] = PRIOR_RESTING_SD * PRIOR_RESTING_SD
        p[A * N + A] = PRIOR_ACTIVITY_SD * PRIOR_ACTIVITY_SD
        p[O * N + O] = PRIOR_WORKOUT_SD * PRIOR_WORKOUT_SD

        val knownActive = history.mapNotNull { it.active }
        val usualActive = if (knownActive.isEmpty()) 0.0 else knownActive.average()
        var carbAverage: Double? = null
        var loggedDays = 0
        var weighIns = 0
        val loggedIntakes = mutableListOf<Double>()
        val states = mutableListOf<BodyState>()

        history.forEachIndexed { index, day ->
            if (index > 0 && day.weight != null) update(x, p, day.weight)
            if (day.weight != null) weighIns++
            states += BodyState(day.date, x[T], x[W], sqrt(p[T * N + T]))
            if (index == history.lastIndex) return@forEachIndexed

            val sleepShare = 1 - Metabolism.SLEEP_REDUCTION * day.sleepMinutes.coerceIn(0, 1440) / 1440.0
            val resting = restingAt(x[T]) * sleepShare
            val restingSlope = (restingAt(x[T] + 1) - restingAt(x[T])) * sleepShare
            val logged = isLogged(day.intake, resting)
            val intake = if (logged) day.intake!! else assumedIntake(loggedIntakes, x, resting, usualActive)
            val intakeSd = intake * if (logged) LOGGED_INTAKE_ERROR else UNKNOWN_INTAKE_ERROR
            val active = day.active ?: usualActive
            val activeSd = active * if (day.active != null) ACTIVE_ERROR else UNKNOWN_ACTIVE_ERROR
            if (logged) {
                loggedDays++
                loggedIntakes += intake
            }

            val carbInput = if (logged && day.carbs != null) {
                val average = carbAverage ?: day.carbs
                carbAverage = average + CARB_AVERAGE_WEIGHT * (day.carbs - average)
                WATER_PER_CARB * (day.carbs - average) / 1000
            } else 0.0

            predict(x, p, intake, intakeSd, resting, restingSlope, active, activeSd, day.logged, carbInput)
        }

        val recent = history.takeLast(RECENT_DAYS)
        val recentLogged = recent.filter { isLogged(it.intake, restingAt(x[T])) }.mapNotNull { it.intake }
        val recentActive = recent.map { it.active ?: usualActive }.average()
        return BodyModel(
            factors = EnergyFactors(
                resting = x[R],
                activity = x[A],
                workout = x[O],
                restingSd = sqrt(p[R * N + R]),
                activitySd = sqrt(p[A * N + A]),
                workoutSd = sqrt(p[O * N + O]),
                covariance = p[R * N + A],
            ),
            states = states,
            loggedDays = loggedDays,
            weighIns = weighIns,
            recentIntake = recentLogged.takeIf { it.size >= MIN_RECENT_LOGGED }?.average(),
            recentActive = recentActive,
            recentWorkouts = recent.map { it.logged }.average(),
            workoutDays = history.count { it.logged > 0 },
            restingAtTissue = restingAt(x[T]),
        )
    }

    private fun assumedIntake(logged: List<Double>, x: DoubleArray, resting: Double, usualActive: Double): Double =
        if (logged.size >= 3) logged.takeLast(RECENT_DAYS).average()
        else Metabolism.withDigestion(x[R] * resting + x[A] * usualActive)

    private fun predict(
        x: DoubleArray,
        p: DoubleArray,
        intake: Double,
        intakeSd: Double,
        resting: Double,
        restingSlope: Double,
        active: Double,
        activeSd: Double,
        workout: Double,
        carbInput: Double,
    ) {
        val k = Metabolism.KCAL_PER_KG
        val keep = 1 - Metabolism.DIGESTION_SHARE
        val f = DoubleArray(N * N)
        for (i in 0 until N) f[i * N + i] = 1.0
        f[T * N + T] = 1 - x[R] * restingSlope / k
        f[T * N + R] = -resting / k
        f[T * N + A] = -active / k
        f[T * N + O] = -workout / k
        f[W * N + W] = WATER_KEEP

        x[T] += (keep * intake - x[R] * resting - x[A] * active - x[O] * workout) / k
        x[W] = WATER_KEEP * x[W] + carbInput

        val next = multiply(multiply(f, p), transpose(f))
        next[T * N + T] += MODEL_SD_KG * MODEL_SD_KG +
            (keep * keep * intakeSd * intakeSd + x[A] * x[A] * activeSd * activeSd + (WORKOUT_ERROR * workout).let { it * it }) / (k * k)
        next[W * N + W] += WATER_SD_KG * WATER_SD_KG
        next[R * N + R] += RESTING_DRIFT * RESTING_DRIFT
        next[A * N + A] += ACTIVITY_DRIFT * ACTIVITY_DRIFT
        next[O * N + O] += WORKOUT_DRIFT * WORKOUT_DRIFT
        next.copyInto(p)
        clamp(x)
    }

    private fun update(x: DoubleArray, p: DoubleArray, weight: Double) {
        val innovation = weight - (x[T] + x[W])
        val s = p[T * N + T] + p[W * N + W] + 2 * p[T * N + W] + SCALE_SD_KG * SCALE_SD_KG
        if (innovation * innovation / s > OUTLIER_SIGMAS_SQUARED) return
        val gain = DoubleArray(N) { (p[it * N + T] + p[it * N + W]) / s }
        for (i in 0 until N) x[i] += gain[i] * innovation
        val ph = DoubleArray(N) { p[T * N + it] + p[W * N + it] }
        for (i in 0 until N) for (j in 0 until N) p[i * N + j] -= gain[i] * ph[j]
        for (i in 0 until N) for (j in i + 1 until N) {
            val mean = (p[i * N + j] + p[j * N + i]) / 2
            p[i * N + j] = mean
            p[j * N + i] = mean
        }
        clamp(x)
    }

    private fun clamp(x: DoubleArray) {
        x[R] = x[R].coerceIn(0.7, 1.3)
        x[A] = x[A].coerceIn(0.4, 2.0)
        x[O] = x[O].coerceIn(0.3, 1.8)
    }

    private fun multiply(a: DoubleArray, b: DoubleArray) = DoubleArray(N * N) { index ->
        val i = index / N
        val j = index % N
        (0 until N).sumOf { a[i * N + it] * b[it * N + j] }
    }

    private fun transpose(a: DoubleArray) = DoubleArray(N * N) { index -> a[(index % N) * N + index / N] }
}
