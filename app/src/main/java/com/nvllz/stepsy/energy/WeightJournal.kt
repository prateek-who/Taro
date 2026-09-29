package com.nvllz.stepsy.energy

import android.content.Context
import com.nvllz.stepsy.data.WeightLog
import com.nvllz.stepsy.util.AppPreferences
import com.nvllz.stepsy.util.Database
import com.nvllz.stepsy.util.Util
import java.time.LocalDate
import kotlin.math.roundToInt

object WeightJournal {
    private const val HISTORY_DAYS = 365L

    fun points(context: Context): List<TrendPoint> {
        val since = Util.logicalToday().minusDays(HISTORY_DAYS).toString()
        return WeightTrend.trend(
            Database.getInstance(context).weightsSince(since).map { WeightPoint(LocalDate.parse(it.date), it.kg) }
        )
    }

    fun log(context: Context, date: String, kg: Double) {
        Database.getInstance(context).saveWeight(WeightLog(date, kg, System.currentTimeMillis()))
        syncProfileWeight(context)
    }

    fun delete(context: Context, date: String) {
        Database.getInstance(context).deleteWeight(date)
        syncProfileWeight(context)
    }

    private fun syncProfileWeight(context: Context) {
        points(context).lastOrNull()?.let { AppPreferences.weight = (it.trend * 10).roundToInt() / 10.0 }
    }
}
