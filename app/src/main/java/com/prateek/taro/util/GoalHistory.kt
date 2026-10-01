package com.prateek.taro.util

import java.time.LocalDate

class GoalHistory(private val entries: List<Pair<LocalDate, Int>>, private val current: Int) {
    fun on(date: LocalDate): Int = entries.lastOrNull { !it.first.isAfter(date) }?.second ?: current

    fun total(from: LocalDate, to: LocalDate): Int =
        generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.sumOf { on(it) }

    override fun equals(other: Any?) = other is GoalHistory && other.entries == entries && other.current == current

    override fun hashCode() = entries.hashCode() * 31 + current

    companion object {
        private val START: LocalDate = LocalDate.of(2000, 1, 1)
        private const val FIELD = '|'
        private const val RECORD = '\n'

        private fun parse(text: String?): List<Pair<LocalDate, Int>> = text.orEmpty().split(RECORD).mapNotNull { line ->
            val date = runCatching { LocalDate.parse(line.substringBefore(FIELD)) }.getOrNull() ?: return@mapNotNull null
            val goal = line.substringAfter(FIELD, "").toIntOrNull() ?: return@mapNotNull null
            date to goal
        }.sortedBy { it.first }

        fun decode(text: String?, current: Int) = GoalHistory(parse(text), current)

        fun changed(text: String?, old: Int, new: Int, today: LocalDate): String {
            val entries = parse(text).ifEmpty { listOf(START to old) }.filterNot { it.first == today } + (today to new)
            return entries.sortedBy { it.first }.joinToString(RECORD.toString()) { "${it.first}$FIELD${it.second}" }
        }
    }
}
