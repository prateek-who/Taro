package com.prateek.taro.leaderboard

import com.prateek.taro.report.Period
import com.prateek.taro.report.ReportKind
import com.prateek.taro.util.DayScore
import java.time.DayOfWeek
import java.time.LocalDate

enum class Board(val key: String) {
    SCORE("score"), STEPS("steps"), DISTANCE("distance"), MOVING("moving"), WALK("walk"), SPEED("speed"), EARLY("early"), SLEEP("sleep"), PROTEIN("protein")
}

enum class BoardRange(val key: String) {
    ALL("all"), MONTH("month"), WEEKS("weeks"), WEEKDAY("weekday")
}

data class BoardDay(val date: LocalDate, val values: Map<Board, Double>, val score: DayScore? = null) {
    fun value(board: Board): Double = values[board] ?: 0.0
}

data class Entry(val date: LocalDate, val value: Double, val score: DayScore? = null)

data class Standing(val value: Double, val rank: Int, val of: Int, val next: Entry?)

data class Pin(val board: Board, val range: BoardRange)

object Leaderboards {

    val PINNABLE = listOf(BoardRange.ALL, BoardRange.MONTH, BoardRange.WEEKS)

    val DEFAULT_PINS = listOf(Pin(Board.SCORE, BoardRange.ALL), Pin(Board.STEPS, BoardRange.MONTH), Pin(Board.SPEED, BoardRange.ALL))

    fun entries(days: List<BoardDay>, board: Board, range: BoardRange, today: LocalDate, weekStart: DayOfWeek): List<Entry> {
        val finished = days.filter { it.date.isBefore(today) && it.value(board) > 0 }
        fun entry(day: BoardDay) = Entry(day.date, day.value(board), day.score.takeIf { board == Board.SCORE })
        return when (range) {
            BoardRange.ALL -> finished.map(::entry).sortedByDescending { it.value }
            BoardRange.MONTH -> finished.filter { it.date.year == today.year && it.date.month == today.month }.map(::entry).sortedByDescending { it.value }
            BoardRange.WEEKS -> {
                val current = week(today, weekStart)
                finished.groupBy { week(it.date, weekStart) }
                    .filterKeys { it.isBefore(current) }
                    .map { (start, week) -> Entry(start, week.sumOf { it.value(board) }) }
                    .sortedByDescending { it.value }
            }
            BoardRange.WEEKDAY -> finished.groupBy { it.date.dayOfWeek }
                .mapValues { (_, same) -> same.maxBy { it.value(board) } }
                .toSortedMap(compareBy { (it.value - weekStart.value + 7) % 7 })
                .values.map(::entry)
        }
    }

    fun current(days: List<BoardDay>, board: Board, range: BoardRange, today: LocalDate, weekStart: DayOfWeek): Double = when (range) {
        BoardRange.WEEKS -> {
            val start = week(today, weekStart)
            days.filter { !it.date.isBefore(start) && !it.date.isAfter(today) }.sumOf { it.value(board) }
        }
        else -> days.firstOrNull { it.date == today }?.value(board) ?: 0.0
    }

    fun standing(value: Double, ranked: List<Entry>): Standing {
        val rank = ranked.count { it.value > value } + 1
        return Standing(value, rank, ranked.size + 1, ranked.getOrNull(rank - 2))
    }

    fun week(date: LocalDate, weekStart: DayOfWeek): LocalDate = Period.containing(ReportKind.WEEK, date, weekStart).start

    fun encode(pins: List<Pin>): String = pins.joinToString(",") { "${it.board.key}:${it.range.key}" }

    fun decode(text: String?): List<Pin> {
        if (text.isNullOrBlank()) return DEFAULT_PINS
        val pins = text.split(',').mapNotNull { part ->
            val (board, range) = part.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
            Pin(
                Board.entries.firstOrNull { it.key == board } ?: return@mapNotNull null,
                BoardRange.entries.firstOrNull { it.key == range }?.takeIf { it in PINNABLE } ?: BoardRange.ALL,
            )
        }.distinctBy { it.board }
        return pins.ifEmpty { DEFAULT_PINS }
    }
}
