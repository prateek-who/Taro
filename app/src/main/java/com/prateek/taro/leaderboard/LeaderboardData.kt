package com.prateek.taro.leaderboard

import android.content.Context
import com.prateek.taro.energy.ActivityEnergy
import com.prateek.taro.energy.EnergyModel
import com.prateek.taro.energy.MinuteSample
import com.prateek.taro.report.ReportData
import com.prateek.taro.ui.WALK_MERGE_MS
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Database
import com.prateek.taro.util.DayScores
import com.prateek.taro.util.Util
import com.prateek.taro.util.Walks
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class BoardData(val days: List<BoardDay>, val today: LocalDate, val weekStart: DayOfWeek) {

    private val cache = HashMap<Pair<Board, BoardRange>, List<Entry>>()

    fun entries(board: Board, range: BoardRange): List<Entry> = cache.getOrPut(board to range) { Leaderboards.entries(days, board, range, today, weekStart) }

    fun standing(board: Board, range: BoardRange): Standing =
        Leaderboards.standing(Leaderboards.current(days, board, range, today, weekStart), entries(board, range))

    fun has(board: Board): Boolean = entries(board, BoardRange.ALL).isNotEmpty()
}

object LeaderboardData {

    fun scoreRanks(context: Context): Map<LocalDate, Int> {
        val database = Database.getInstance(context)
        val first = database.firstEntry?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return emptyMap()
        val yesterday = Util.logicalToday().minusDays(1)
        val minutes = database.getMinutes(first.toString(), yesterday.toString()).groupBy { it.date }
        return ReportData.scores(context, first, yesterday, minutes)
            .filterValues { it.score > 0 }
            .entries.sortedByDescending { it.value.score }
            .mapIndexed { i, (date, _) -> date to i + 1 }
            .toMap()
    }

    fun load(context: Context): BoardData? {
        val database = Database.getInstance(context)
        val today = Util.logicalToday()
        val steps = database.getEntries(database.firstEntry, today.toString())
            .mapNotNull { entry -> runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let { it to entry.steps } }
            .toMap()
        val first = steps.filterValues { it > 0 }.keys.minOrNull() ?: return null
        val from = first.toString()
        val to = today.toString()
        val minutes = database.getMinutes(from, to).groupBy { it.date }
        val scores = ReportData.scores(context, first, today, minutes, withToday = true)
        val body = ActivityEnergy.body()
        val factors = AppPreferences.energyFactors
        val workouts = database.activitiesBetween(from, to).groupBy { it.date }.mapValues { (_, logged) -> logged.sumOf { it.kcal } }
        val sleep = database.sleepsSince(from).filter { !it.nap }.groupBy { it.wakeDate }.mapValues { (_, nights) -> nights.maxOf { it.durationMinutes } }
        val protein = database.foodBetween(from, to).groupBy { it.date }.mapValues { (_, logs) -> logs.sumOf { it.protein } }
        val zone = ZoneId.systemDefault()
        val days = generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.map { date ->
            val key = date.toString()
            val rows = minutes[key].orEmpty()
            val total = steps[date] ?: 0
            val samples = rows.map { MinuteSample(it.steps, it.ascentMeters.toDouble()) }
            val early = date.atTime(DayScores.EARLY_HOUR, 0).atZone(zone).toInstant().toEpochMilli()
            BoardDay(
                date = date,
                values = mapOf(
                    Board.SCORE to (scores[date]?.score ?: 0).toDouble(),
                    Board.STEPS to total.toDouble(),
                    Board.DISTANCE to EnergyModel.dayDistanceM(body, samples, total),
                    Board.MOVING to factors.activity * EnergyModel.dayKcal(body, samples, total) + factors.workout * (workouts[key] ?: 0.0),
                    Board.WALK to (Walks.merged(Walks.all(rows.map { it.minuteStart to it.steps }), WALK_MERGE_MS).maxOfOrNull { it.minutes } ?: 0).toDouble(),
                    Board.SPEED to (rows.maxOfOrNull { it.steps } ?: 0).toDouble(),
                    Board.EARLY to rows.filter { it.minuteStart < early }.sumOf { it.steps }.toDouble(),
                    Board.SLEEP to (sleep[key] ?: 0L).toDouble(),
                    Board.PROTEIN to (protein[key] ?: 0.0),
                ),
                score = scores[date],
            )
        }.toList()
        return BoardData(days, today, ReportData.firstDayOfWeek())
    }
}
