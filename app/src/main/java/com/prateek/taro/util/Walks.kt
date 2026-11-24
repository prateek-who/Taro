package com.prateek.taro.util

data class Walk(val start: Long, val minutes: Int, val steps: Int) {
    val end: Long get() = start + minutes * 60_000L
}

object Walks {
    private const val MINUTE = 60_000L
    private const val WALKING_STEPS = 40
    private const val MAX_GAP_MINUTES = 2
    private const val MIN_MINUTES = 5

    fun all(minutes: List<Pair<Long, Int>>): List<Walk> {
        val walks = mutableListOf<Walk>()
        var start = 0L
        var last = Long.MIN_VALUE
        var steps = 0
        fun close() {
            if (last == Long.MIN_VALUE) return
            val length = ((last - start) / MINUTE).toInt() + 1
            if (length >= MIN_MINUTES) walks += Walk(start, length, steps)
        }
        minutes.filter { it.second >= WALKING_STEPS }.sortedBy { it.first }.forEach { (at, count) ->
            if (last == Long.MIN_VALUE || at - last > (MAX_GAP_MINUTES + 1) * MINUTE) {
                close()
                start = at
                steps = 0
            }
            last = at
            steps += count
        }
        close()
        return walks
    }

    fun merged(walks: List<Walk>, gapMs: Long): List<Walk> = walks.sortedBy { it.start }.fold(listOf()) { kept, walk ->
        val last = kept.lastOrNull()
        if (last != null && walk.start - last.end < gapMs) {
            kept.dropLast(1) + Walk(last.start, ((walk.end - last.start) / MINUTE).toInt(), last.steps + walk.steps)
        } else {
            kept + walk
        }
    }
}
