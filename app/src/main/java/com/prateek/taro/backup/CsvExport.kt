package com.prateek.taro.backup

import android.content.Context
import com.prateek.taro.energy.WeightJournal
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Database
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExport {
    private fun cell(value: Any?): String {
        val text = value?.toString().orEmpty()
        return if (text.any { it == ',' || it == '"' || it == '\n' }) "\"${text.replace("\"", "\"\"")}\"" else text
    }

    private fun row(vararg values: Any?) = values.joinToString(",") { cell(it) }

    private fun round(value: Double?, places: Int = 1) = value?.let { "%.${places}f".format(Locale.US, it) }

    fun food(context: Context): String {
        val meals = AppPreferences.meals(context).associate { it.key to it.name }
        val time = SimpleDateFormat("HH:mm", Locale.US)
        val logs = Database.getInstance(context).snapshot().foodLogs.sortedWith(compareBy({ it.date }, { it.loggedAt }))
        return buildString {
            appendLine(row("date", "time", "meal", "food", "amount", "quantity", "unit", "kcal", "protein_g", "carbs_g", "fat_g"))
            logs.forEach {
                appendLine(
                    row(
                        it.date, time.format(Date(it.loggedAt)), meals[it.meal] ?: it.meal, it.name, it.amount,
                        round(it.grams), it.unit ?: "g", round(it.kcal, 0), round(it.protein), round(it.carbs), round(it.fat),
                    )
                )
            }
        }
    }

    fun weight(context: Context): String = buildString {
        appendLine(row("date", "weight_kg", "trend_kg"))
        WeightJournal.points(context).forEach { appendLine(row(it.date, round(it.kg, 2), round(it.trend, 2))) }
    }
}
