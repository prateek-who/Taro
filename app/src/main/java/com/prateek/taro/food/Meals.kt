package com.prateek.taro.food

import android.content.Context
import com.prateek.taro.R
import java.time.LocalDate
import java.time.LocalTime

data class MealSlot(val key: String, val name: String, val daily: Boolean = true, val days: Set<String> = emptySet()) {
    fun plannedOn(date: LocalDate) = daily || date.toString() in days
}

object Meals {
    private const val FIELD = '|'
    private const val RECORD = '\n'
    private const val OCCASIONAL = "0"
    private const val DAY = ','
    const val MAX_NAME = 24

    fun defaults(context: Context) = listOf(MealSlot("food", context.getString(R.string.meal_default)))

    fun cleanName(name: String) = name.filterNot { it == FIELD || it == RECORD }.trim().take(MAX_NAME)

    fun encode(meals: List<MealSlot>): String = meals.joinToString(RECORD.toString()) { meal ->
        val days = if (meal.days.isEmpty()) "" else "$FIELD${meal.days.sorted().joinToString(DAY.toString())}"
        "${meal.key}$FIELD${cleanName(meal.name)}" + if (meal.daily) "" else "$FIELD$OCCASIONAL$days"
    }

    fun decode(text: String?): List<MealSlot>? = text?.split(RECORD)?.mapNotNull { line ->
        val parts = line.split(FIELD)
        val key = parts[0]
        val name = parts.getOrNull(1).orEmpty()
        val daily = parts.getOrNull(2) != OCCASIONAL
        val days = parts.getOrNull(3).orEmpty().split(DAY).filter { it.isNotBlank() }.toSet()
        if (key.isBlank() || name.isBlank()) null else MealSlot(key, name, daily, if (daily) emptySet() else days)
    }?.takeIf { it.isNotEmpty() }

    fun newKey() = "meal_${System.currentTimeMillis()}"

    private val usualTimes = listOf("breakfast" to 8 * 60, "lunch" to 13 * 60, "snack" to 17 * 60, "dinner" to 20 * 60 + 30)

    fun usualMinute(meal: MealSlot?): Int = usualTimes.firstOrNull { (word, _) ->
        meal != null && (meal.key == word || meal.name.contains(word, ignoreCase = true))
    }?.second ?: 12 * 60

    fun forTime(meals: List<MealSlot>, time: LocalTime = LocalTime.now()): String {
        val usual = when (time.hour) {
            in 4..10 -> "breakfast"
            in 11..15 -> "lunch"
            in 16..18 -> "snack"
            else -> "dinner"
        }
        val daily = meals.filter { it.daily }.ifEmpty { meals }
        return daily.firstOrNull { it.key == usual || it.name.contains(usual, ignoreCase = true) }?.key ?: daily.first().key
    }
}

class MealOrders(private val entries: List<Pair<LocalDate, List<String>>>) {
    fun on(date: LocalDate): List<String>? = entries.lastOrNull { !it.first.isAfter(date) }?.second

    fun arrange(meals: List<MealSlot>, date: LocalDate): List<MealSlot> {
        val order = on(date) ?: return meals
        return meals.sortedBy { meal -> order.indexOf(meal.key).let { if (it < 0) Int.MAX_VALUE else it } }
    }

    fun with(date: LocalDate, keys: List<String>, today: LocalDate, fallback: List<String>): MealOrders {
        val map = entries.toMap().toSortedMap()
        val next = date.plusDays(1)
        if (date.isBefore(today) && next !in map) map[next] = on(next) ?: fallback
        map[date] = keys
        return MealOrders(map.toList().takeLast(MAX_DAYS))
    }

    fun encode(): String = entries.joinToString(RECORD.toString()) { "${it.first}$FIELD${it.second.joinToString(KEY.toString())}" }

    override fun equals(other: Any?) = other is MealOrders && other.entries == entries

    override fun hashCode() = entries.hashCode()

    companion object {
        private const val FIELD = '|'
        private const val RECORD = '\n'
        private const val KEY = ','
        private const val MAX_DAYS = 400

        fun decode(text: String?) = MealOrders(
            text.orEmpty().split(RECORD).mapNotNull { line ->
                val date = runCatching { LocalDate.parse(line.substringBefore(FIELD)) }.getOrNull() ?: return@mapNotNull null
                date to line.substringAfter(FIELD, "").split(KEY).filter { it.isNotEmpty() }
            }.sortedBy { it.first }
        )
    }
}
