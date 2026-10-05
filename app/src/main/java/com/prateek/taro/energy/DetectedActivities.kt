package com.prateek.taro.energy

data class DetectedSession(val start: Long, val end: Long) {
    val minutes: Int get() = ((end - start) / 60_000L).toInt()
}

object DetectedActivities {
    private const val FIELD = '|'
    private const val RECORD = '\n'
    private const val MAX = 20
    const val MIN_MINUTES = 10
    const val GAP_MS = 3 * 60_000L

    fun decode(text: String?): List<DetectedSession> = text.orEmpty().split(RECORD).mapNotNull { line ->
        val start = line.substringBefore(FIELD).toLongOrNull() ?: return@mapNotNull null
        val end = line.substringAfter(FIELD, "").toLongOrNull() ?: return@mapNotNull null
        DetectedSession(start, end)
    }

    fun encode(sessions: List<DetectedSession>): String =
        sessions.sortedBy { it.start }.takeLast(MAX).joinToString(RECORD.toString()) { "${it.start}$FIELD${it.end}" }
}
