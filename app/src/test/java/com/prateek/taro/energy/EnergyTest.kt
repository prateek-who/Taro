package com.prateek.taro.energy

import com.prateek.taro.report.DayInput
import com.prateek.taro.report.Highlight
import com.prateek.taro.report.LateNight
import com.prateek.taro.report.Period
import com.prateek.taro.report.ReportInputs
import com.prateek.taro.report.ReportKind
import com.prateek.taro.report.StoryFinder
import com.prateek.taro.report.StoryType
import com.prateek.taro.report.Reports
import com.prateek.taro.report.Totals
import com.prateek.taro.report.WeightVerdict
import com.prateek.taro.util.StreakMark
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EnergyTest {

    private val body = Body(weightKg = 70.0, walkingStepM = 0.75, runningStepM = 1.0)

    @Test
    fun walkingFollowsHeightAndSpeedAndRunningFollowsAcsm() {
        val runKm = EnergyModel.metresPerMinute(body, 160) / 1000.0
        val speed = 100 * 0.75 / 60
        val steadyMinute = (3.85 + 5.97 * speed * speed / 1.7) * 70.0 / 1000 * 5
        assertEquals(steadyMinute, EnergyModel.minuteKcal(body, MinuteSample(steps = 100)), 1e-9)
        assertEquals(EnergyModel.minuteKcal(body, MinuteSample(steps = 80)) / 4, EnergyModel.minuteKcal(body, MinuteSample(steps = 20)), 1e-9)
        assertEquals(1.0, EnergyModel.minuteKcal(body, MinuteSample(steps = 160)) / runKm / body.weightKg, 1e-9)
        assertEquals(EnergyModel.minuteKcal(body, MinuteSample(steps = 80)) * 125, EnergyModel.walkingStepsKcal(body, 10_000), 1e-9)
    }

    @Test
    fun restingBurnNeedAndCalorieTarget() {
        assertEquals(1_648.75, Metabolism.restingKcalPerDay(70.0, 175.0, 30, Sex.MALE), 1e-9)
        assertEquals(1_295.25, Metabolism.restingKcalPerDay(60.0, 165.0, 35, Sex.FEMALE), 1e-9)
        assertEquals(2_000.0, Metabolism.dailyNeed(1_500.0, 300.0), 1e-9)
        assertEquals(1_800.0, Metabolism.calorieTarget(2_300.0, DietGoal.CUT, 500), 1e-9)
        assertEquals(2_600.0, Metabolism.calorieTarget(2_300.0, DietGoal.BULK, 300), 1e-9)
        assertEquals(169.0, Metabolism.digestion(1_650.0, 100.0, 200.0, 50.0), 1e-9)
        assertEquals(50.0, Metabolism.digestion(500.0, 0.0, null, null), 1e-9)

        val week = Period(ReportKind.WEEK, LocalDate.of(2026, 9, 28))
        val days = (0 until 7).map { DayInput(week.start.plusDays(it.toLong()), 6_000, 4_000.0, 150.0, 2_300.0, 2_000.0, 100.0, true, 5_000, true) }
        val report = Reports.build(
            ReportInputs(week, days, Totals(30_000, 21_000.0, 700.0), 70.0, 69.7, emptyList(), null, emptyList(), emptyList(), emptyList(), 100.0)
        )
        assertEquals(-2_100.0 / Metabolism.KCAL_PER_KG, report.expectedKg!!, 1e-9)
        assertEquals(WeightVerdict.MATCHES, report.verdict)
        assertEquals(Highlight.MoreSteps(40), report.highlights.first())

        val dip = days.mapIndexed { i, day -> if (i == 2) day.copy(steps = 2_000) else day }
        val turn = StoryFinder.turn(dip, mapOf(week.start.plusDays(1) to StreakMark.MET, week.start.plusDays(2) to StreakMark.MADE_UP))!!
        assertEquals(week.start.plusDays(3), turn.turn.date)
        assertEquals(true, turn.madeUp && turn.streakBefore)
        val hours = List(24) { if (it == 22) 12_000 else 300 }
        val power = StoryFinder.powerHour(hours, dip)!!
        assertEquals(22 to 6, power.hour to power.units)
        assertEquals(StoryType.NIGHT_SHIFT, StoryFinder.persona(hours, dip, null, null, null)!!.type)
        val nights = (0 until 5).map { week.start.plusDays(it.toLong()) to 23 * 60 + 40 } + (week.start.plusDays(5) to 40)
        assertEquals(LateNight(23 * 60 + 40, week.start.plusDays(5), 40), StoryFinder.lateNight(nights))
    }

    @Test
    fun workoutsAreNetOfRestingAndCustomActivitiesRoundTrip() {
        assertEquals(406.0, Activities.estimateActiveKcal(met = 6.8, weightKg = 70.0, minutes = 60), 1e-9)
        assertEquals(0.0, Activities.estimateActiveKcal(met = 0.8, weightKg = 70.0, minutes = 60), 1e-9)
        val saved = listOf(CustomActivity("Calisthenics", 4.5), CustomActivity("Push|ups", 8.0))
        assertEquals(
            listOf(CustomActivity("Calisthenics", 4.5), CustomActivity("Pushups", 8.0)),
            Activities.decode(Activities.encode(saved)),
        )
    }
}
