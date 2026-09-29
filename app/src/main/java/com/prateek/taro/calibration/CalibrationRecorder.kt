package com.prateek.taro.calibration

data class CalibrationWindow(val steps: Int, val distanceM: Double, val durationS: Int) {
    val cadence: Double get() = steps * 60.0 / durationS
    val stepLengthM: Double get() = distanceM / steps
}

class CalibrationRecorder(
    private val windowSeconds: Int = 30,
    private val minCoverage: Double = 0.7,
    private val minSteps: Int = 20,
) {
    private val stepsPerSecond = sortedMapOf<Long, Int>()
    private val speedPerSecond = sortedMapOf<Long, Double>()
    private var startSecond: Long? = null

    fun addStep(elapsedMs: Long, count: Int = 1) {
        val second = second(elapsedMs)
        stepsPerSecond[second] = (stepsPerSecond[second] ?: 0) + count
    }

    fun addSpeed(elapsedMs: Long, speedMps: Double) {
        speedPerSecond[second(elapsedMs)] = speedMps
    }

    val totalSteps: Int get() = stepsPerSecond.values.sum()

    val gpsDistanceM: Double get() = speedPerSecond.values.sum()

    fun recentCadence(nowMs: Long, seconds: Int = 20): Int? {
        val now = nowMs / 1000
        val start = startSecond ?: return null
        val window = minOf(seconds.toLong(), now - start + 1)
        if (window < 5) return null
        val steps = stepsPerSecond.subMap(now - window + 1, now + 1).values.sum()
        return (steps * 60 / window).toInt()
    }

    fun windows(): List<CalibrationWindow> {
        val first = startSecond ?: return emptyList()
        val last = maxOf(stepsPerSecond.keys.maxOrNull() ?: first, speedPerSecond.keys.maxOrNull() ?: first)
        return (first..last - windowSeconds + 1 step windowSeconds.toLong()).mapNotNull { start ->
            val range = start until start + windowSeconds
            val speeds = range.mapNotNull { speedPerSecond[it] }
            val steps = range.sumOf { stepsPerSecond[it] ?: 0 }
            if (speeds.size < windowSeconds * minCoverage || steps < minSteps) return@mapNotNull null
            val distance = speeds.average() * windowSeconds
            CalibrationWindow(steps, distance, windowSeconds)
        }
    }

    private fun second(elapsedMs: Long): Long {
        val second = elapsedMs / 1000
        if (startSecond == null || second < startSecond!!) startSecond = second
        return second
    }
}
