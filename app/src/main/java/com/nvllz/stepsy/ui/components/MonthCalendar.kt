package com.nvllz.stepsy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.nvllz.stepsy.ui.theme.StepsyMotion
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nvllz.stepsy.R
import com.nvllz.stepsy.ui.theme.StepsyTheme
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun MonthCalendar(
    selected: LocalDate,
    minDate: LocalDate,
    maxDate: LocalDate,
    firstDayOfWeek: DayOfWeek,
    jumpKey: Int,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    heat: Map<LocalDate, Float> = emptyMap(),
) {
    val firstMonth = YearMonth.from(minDate)
    val monthCount = ChronoUnit.MONTHS.between(firstMonth, YearMonth.from(maxDate)).toInt() + 1
    fun pageOf(date: LocalDate) =
        ChronoUnit.MONTHS.between(firstMonth, YearMonth.from(date)).toInt().coerceIn(0, monthCount - 1)

    val pagerState = rememberPagerState(initialPage = pageOf(selected)) { monthCount }
    val scope = rememberCoroutineScope()
    val titleFormat = remember { DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()) }
    val weekdays = remember(firstDayOfWeek) { (0L until 7L).map { firstDayOfWeek.plus(it) } }

    LaunchedEffect(selected, jumpKey) {
        val page = pageOf(selected)
        if (pagerState.currentPage != page) pagerState.animateScrollToPage(page)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        val shownMonth = firstMonth.plusMonths(pagerState.currentPage.toLong())
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                enabled = pagerState.currentPage > 0,
            ) {
                Icon(painterResource(R.drawable.ic_chevron_right), null, modifier = Modifier.rotate(180f))
            }
            Text(
                text = shownMonth.format(titleFormat).replaceFirstChar { it.titlecase(Locale.getDefault()) },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                enabled = pagerState.currentPage < monthCount - 1,
            ) {
                Icon(painterResource(R.drawable.ic_chevron_right), null)
            }
        }

        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            weekdays.forEach {
                Text(
                    text = it.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        HorizontalPager(state = pagerState, beyondViewportPageCount = 1) { page ->
            MonthGrid(
                month = firstMonth.plusMonths(page.toLong()),
                firstDayOfWeek = firstDayOfWeek,
                selected = selected,
                minDate = minDate,
                maxDate = maxDate,
                heat = heat,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
    selected: LocalDate,
    minDate: LocalDate,
    maxDate: LocalDate,
    heat: Map<LocalDate, Float>,
    onSelect: (LocalDate) -> Unit,
) {
    val leading = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val today = maxDate

    Column {
        repeat(6) { week ->
            Row {
                repeat(7) { weekday ->
                    val day = week * 7 + weekday - leading + 1
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                    ) {
                        if (day in 1..month.lengthOfMonth()) {
                            val date = month.atDay(day)
                            DayCell(
                                day = day,
                                selected = date == selected,
                                today = date == today,
                                enabled = date in minDate..maxDate,
                                heat = heat[date] ?: 0f,
                                onClick = { onSelect(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, selected: Boolean, today: Boolean, enabled: Boolean, heat: Float, onClick: () -> Unit) {
    val goal = StepsyTheme.colors.goal
    val met = heat >= 1f
    val fill by animateColorAsState(
        when {
            met -> goal
            heat > 0f -> goal.copy(alpha = 0.1f + 0.45f * heat)
            else -> Color.Transparent
        },
        label = "heat",
    )
    val ring by animateDpAsState(if (selected) 2.dp else 0.dp, StepsyMotion.snappy(), label = "selected ring")
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            .then(if (ring > 0.dp) Modifier.border(ring, onSurface, RoundedCornerShape(12.dp)) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.3f),
    ) {
        Text(
            text = day.toString(),
            fontSize = 14.sp,
            fontWeight = if (today || selected || met) FontWeight.Bold else FontWeight.Normal,
            color = if (met) StepsyTheme.colors.background else onSurface,
        )
        if (today) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(4.dp)
                    .background(if (met) StepsyTheme.colors.background else StepsyTheme.colors.special, CircleShape),
            )
        }
    }
}
