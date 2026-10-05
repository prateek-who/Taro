package com.prateek.taro.ui

import com.prateek.taro.ui.components.TileRow
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.DateRow
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.ui.components.SecondaryButton
import com.prateek.taro.ui.components.LocalToast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prateek.taro.ui.theme.TaroMotion
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.R
import com.prateek.taro.achievements.AchievementData
import com.prateek.taro.achievements.AchievementInputs
import com.prateek.taro.achievements.BadgeCategory
import com.prateek.taro.achievements.BadgeResult
import com.prateek.taro.achievements.BadgeUnit
import com.prateek.taro.achievements.Rules
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.components.PillSelector
import com.prateek.taro.ui.components.ScrollingColumn
import com.prateek.taro.ui.components.SectionLabel
import com.prateek.taro.ui.components.StatTile
import com.prateek.taro.ui.components.TaroDialog
import com.prateek.taro.ui.components.TaroScaffold
import com.prateek.taro.ui.components.TintChip
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object AchievementsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        AchievementsContent(onBack = { navigator.pop() })
    }
}

private val WEIGHT_PINK = Color(0xFFFF6FAE)
private val EXPLORER_BLUE = Color(0xFF4FC3F7)
private val MIND_VIOLET = Color(0xFFB388FF)
private val STRENGTH_RED = Color(0xFFFF6E6E)

@Composable
private fun categoryColor(category: BadgeCategory): Color = when (category) {
    BadgeCategory.STEPS -> TaroTheme.colors.goal
    BadgeCategory.GOALS -> TaroTheme.colors.special
    BadgeCategory.CLIMB, BadgeCategory.ENERGY -> TaroTheme.colors.flame
    BadgeCategory.WEIGHT -> WEIGHT_PINK
    BadgeCategory.SLEEP -> TaroTheme.colors.sleep
    BadgeCategory.EXPLORER -> EXPLORER_BLUE
    BadgeCategory.FOOD -> TaroTheme.colors.goal
    BadgeCategory.MIND -> MIND_VIOLET
    BadgeCategory.SKILLS -> TaroTheme.colors.flame
    BadgeCategory.STRENGTH -> STRENGTH_RED
    BadgeCategory.LEGENDS -> TaroTheme.colors.special
}

private data class Records(val bestDay: DayRecord?, val bestWeek: Int, val bestMonth: Int, val longestStreak: Int, val total: Int)

private data class DayRecord(val steps: Int, val date: LocalDate)

private fun records(inputs: AchievementInputs): Records {
    val days = inputs.days
    val best = days.maxByOrNull { it.steps }
    return Records(
        bestDay = best?.let { DayRecord(it.steps, it.date) },
        bestWeek = days.groupBy { it.date.with(TemporalAdjusters.previousOrSame(inputs.firstDayOfWeek)) }.values.maxOfOrNull { week -> week.sumOf { it.steps } } ?: 0,
        bestMonth = days.groupBy { it.date.withDayOfMonth(1) }.values.maxOfOrNull { month -> month.sumOf { it.steps } } ?: 0,
        longestStreak = Rules.streak(Rules.goalDays(inputs), Int.MAX_VALUE).value.toInt(),
        total = days.sumOf { it.steps },
    )
}

private fun compactSteps(value: Double): String = when {
    value >= 1_000_000 -> "%.1fM".format(Locale.getDefault(), value / 1_000_000)
    value >= 10_000 -> "%.0fk".format(Locale.getDefault(), value / 1_000)
    value >= 1_000 -> "%.1fk".format(Locale.getDefault(), value / 1_000)
    else -> "%.0f".format(Locale.getDefault(), value)
}

private fun formatValue(unit: BadgeUnit, value: Double): String = when (unit) {
    BadgeUnit.STEPS -> compactSteps(value)
    BadgeUnit.KM -> "%.1f km".format(Locale.getDefault(), value)
    BadgeUnit.METERS -> "%.0f m".format(Locale.getDefault(), value)
    BadgeUnit.KG -> "%.1f kg".format(Locale.getDefault(), value)
    BadgeUnit.MINUTES -> "%.0f min".format(Locale.getDefault(), value)
    else -> "%.0f".format(Locale.getDefault(), value)
}

private fun descriptionParam(result: BadgeResult): String {
    val target = result.def.target
    return when (result.def.unit) {
        BadgeUnit.STEPS -> Util.formatSteps(target.toInt())
        BadgeUnit.KM -> "%.1f km".format(Locale.getDefault(), target)
        BadgeUnit.METERS -> "%.0f m".format(Locale.getDefault(), target)
        BadgeUnit.KG -> "%.0f kg".format(Locale.getDefault(), target)
        BadgeUnit.MINUTES -> "%.0f".format(Locale.getDefault(), target)
        else -> "%.0f".format(Locale.getDefault(), target)
    }
}

private data class AchievementsState(val inputs: AchievementInputs, val badges: List<BadgeResult>)

@Composable
private fun AchievementsContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val dataVersion = rememberDataVersion()
    var state by remember { mutableStateOf<AchievementsState?>(null) }
    var category by rememberSaveable { mutableStateOf<BadgeCategory?>(null) }
    var open by remember { mutableStateOf<BadgeResult?>(null) }
    val showcaseIds by AppPreferences.showcaseBadgesFlow().collectAsStateWithLifecycle(AppPreferences.showcaseBadges)
    val toast = LocalToast.current
    val dateFormat = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()) }

    LaunchedEffect(dataVersion) {
        state = withContext(Dispatchers.Default) {
            val inputs = AchievementData.inputs(context)
            AchievementsState(inputs, com.prateek.taro.achievements.Badges.evaluate(inputs))
        }
    }

    TaroScaffold(title = stringResource(R.string.achievements_title), onBack = onBack) { padding ->
        ScrollingColumn(padding, modifier = Modifier.padding(horizontal = 16.dp)) {
            val current = state
            if (current == null) {
                Text(
                    text = stringResource(R.string.badges_loading),
                    color = TaroTheme.colors.accent,
                    modifier = Modifier.padding(24.dp),
                )
                return@ScrollingColumn
            }
            val earned = current.badges.filter { it.earned }
            val gold = TaroTheme.colors.special

            Panel(modifier = Modifier.padding(top = 8.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${earned.size}",
                            style = MaterialTheme.typography.displaySmall,
                            color = gold,
                        )
                        Text(
                            text = " / ${current.badges.size}  ${stringResource(R.string.badges_unlocked)}",
                            color = TaroTheme.colors.accent,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                        )
                    }
                    val fill by animateFloatAsState(
                        targetValue = earned.size.toFloat() / current.badges.size.coerceAtLeast(1),
                        animationSpec = TaroMotion.gentle(),
                        label = "badges",
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(TaroTheme.colors.accentOpaque),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fill)
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(gold.copy(alpha = 0.55f), gold))),
                        )
                    }
                    val latest = earned.maxByOrNull { it.progress.earnedOn!! }
                    latest?.let {
                        Text(
                            text = stringResource(R.string.badge_latest, stringResource(it.def.title), it.progress.earnedOn!!.format(dateFormat)),
                            fontSize = 13.sp,
                            color = TaroTheme.colors.accent,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
                val showcase = if (showcaseIds.isEmpty()) {
                    earned.sortedByDescending { it.progress.earnedOn }.take(3)
                } else {
                    showcaseIds.mapNotNull { id -> earned.firstOrNull { it.def.id == id } }
                }
                if (showcase.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.badge_showcase).uppercase(),
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TaroTheme.colors.accent,
                        modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        repeat(3) { slot ->
                            val badge = showcase.getOrNull(slot)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .then(if (badge != null) Modifier.clickable { open = badge } else Modifier)
                                    .padding(vertical = 6.dp),
                            ) {
                                if (badge != null) {
                                    BadgeMedal(badge, categoryColor(badge.def.category), size = 68)
                                    Text(
                                        text = stringResource(badge.def.title),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        modifier = Modifier.padding(top = 8.dp),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(TaroTheme.colors.accentOpaque),
                                    )
                                }
                            }
                        }
                    }
                    if (showcaseIds.isEmpty()) {
                        Text(
                            text = stringResource(R.string.badge_showcase_hint),
                            fontSize = 12.sp,
                            color = TaroTheme.colors.accent,
                            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                        )
                    }
                }
            }

            PillSelector(
                options = listOf<Pair<BadgeCategory?, String>>(null to stringResource(R.string.badge_cat_all)) +
                    BadgeCategory.entries.map { it to stringResource(it.label) },
                selected = category,
                onSelect = { category = it },
                modifier = Modifier.padding(vertical = 16.dp),
            )

            val inCategory = current.badges.filter { badge ->
                if (category == null) !badge.def.claimable || badge.earned else badge.def.category == category
            }
            val groups = if (inCategory.any { it.def.group != null }) {
                inCategory.groupBy { it.def.group }.toList().sortedBy { it.first?.ordinal ?: -1 }
            } else {
                listOf(null to inCategory.sortedWith(compareByDescending<BadgeResult> { it.earned }.thenByDescending { if (it.earned) 0f else it.fraction }))
            }
            groups.forEach { (group, badges) ->
                if (group != null) {
                    SectionLabel(
                        text = "${stringResource(group.label)}  ${badges.count { it.earned }}/${badges.size}",
                        modifier = Modifier.padding(top = 0.dp),
                    )
                }
                badges.chunked(3).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                    ) {
                        row.forEach { badge ->
                            BadgeTile(badge, dateFormat, onClick = { open = badge }, modifier = Modifier.weight(1f))
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            val records = remember(current) { records(current.inputs) }
            SectionLabel(stringResource(R.string.badges_records))
            TileRow(modifier = Modifier.fillMaxWidth()) {
                StatTile(
                    label = stringResource(R.string.badges_best_day),
                    value = records.bestDay?.let { Util.formatSteps(it.steps) } ?: "-",
                    color = TaroTheme.colors.goal,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.best_week),
                    value = Util.formatSteps(records.bestWeek),
                    modifier = Modifier.weight(1f),
                )
            }
            TileRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 24.dp),
            ) {
                StatTile(
                    label = stringResource(R.string.most_walked_month),
                    value = Util.formatSteps(records.bestMonth),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.badges_total),
                    value = compactSteps(records.total.toDouble()),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.badge_cat_goals),
                    value = "${records.longestStreak}d",
                    color = gold,
                    modifier = Modifier.weight(0.8f),
                )
            }
        }
    }

    open?.let { badge ->
        val pinned = badge.def.id in showcaseIds
        BadgeDialog(
            badge = badge,
            dateFormat = dateFormat,
            showcased = pinned,
            onToggleShowcase = {
                when {
                    pinned -> AppPreferences.showcaseBadges = showcaseIds - badge.def.id
                    showcaseIds.size >= 3 -> toast.show(context.getString(R.string.badge_showcase_full), ToastKind.INFO)
                    else -> {
                        AppPreferences.showcaseBadges = showcaseIds + badge.def.id
                        toast.show(context.getString(R.string.badge_showcase_added), ToastKind.SUCCESS)
                    }
                }
                open = null
            },
            onClaim = { date ->
                AppPreferences.setBadgeClaim(badge.def.id, date)
                if (date != null) toast.show(context.getString(R.string.badge_claimed, context.getString(badge.def.title)), ToastKind.SUCCESS)
                open = null
            },
            onDismiss = { open = null },
        )
    }
}

@Composable
private fun BadgeTile(badge: BadgeResult, dateFormat: DateTimeFormatter, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = categoryColor(badge.def.category)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(TaroTheme.colors.accentOpaque)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
    ) {
        BadgeMedal(badge, color, size = 60)
        Text(
            text = stringResource(badge.def.title),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = if (badge.earned) MaterialTheme.colorScheme.onSurface else TaroTheme.colors.accent,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = badge.progress.earnedOn?.format(dateFormat)
                ?: if (badge.def.unit == BadgeUnit.FLAG) stringResource(R.string.badge_locked)
                else "${formatValue(badge.def.unit, badge.progress.value)} / ${formatValue(badge.def.unit, badge.def.target)}",
            fontSize = 11.sp,
            color = if (badge.earned) color else TaroTheme.colors.accent,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun BadgeMedal(badge: BadgeResult, color: Color, size: Int) {
    val track = TaroTheme.colors.accentOpaque
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size.dp)) {
        Canvas(modifier = Modifier.size(size.dp)) {
            val stroke = 3.5.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            if (badge.earned) {
                drawCircle(color.copy(alpha = 0.16f))
                drawArc(color, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            } else {
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                if (badge.fraction > 0f) {
                    drawArc(color.copy(alpha = 0.7f), -90f, 360f * badge.fraction, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
        }
        Icon(
            painter = painterResource(badge.def.icon),
            contentDescription = null,
            tint = if (badge.earned) color else TaroTheme.colors.accent.copy(alpha = 0.55f),
            modifier = Modifier.size((size * 0.42f).dp),
        )
    }
}

@Composable
private fun BadgeDialog(
    badge: BadgeResult,
    dateFormat: DateTimeFormatter,
    showcased: Boolean,
    onToggleShowcase: () -> Unit,
    onClaim: (LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    val color = categoryColor(badge.def.category)
    var claimDate by remember { mutableStateOf(Util.logicalToday()) }
    TaroDialog(
        title = stringResource(badge.def.title),
        onDismiss = onDismiss,
        confirmText = stringResource(android.R.string.ok),
        onConfirm = onDismiss,
        dismissText = null,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            BadgeMedal(badge, color, size = 96)
            Text(
                text = stringResource(badge.def.description, descriptionParam(badge)),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            if (badge.earned) {
                TintChip(
                    text = stringResource(R.string.badge_earned_on, badge.progress.earnedOn!!.format(dateFormat)),
                    color = color,
                    modifier = Modifier.padding(top = 16.dp),
                )
                SecondaryButton(
                    text = stringResource(if (showcased) R.string.badge_showcase_remove else R.string.badge_showcase_add),
                    onClick = onToggleShowcase,
                    icon = R.drawable.ic_badge_star,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                if (badge.def.claimable) {
                    SecondaryButton(
                        text = stringResource(R.string.badge_unclaim),
                        onClick = { onClaim(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            } else if (badge.def.claimable) {
                Text(
                    text = stringResource(R.string.badge_claim_hint),
                    color = TaroTheme.colors.accent,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
                DateRow(
                    label = stringResource(R.string.badge_claimed_on),
                    date = claimDate,
                    today = Util.logicalToday(),
                    onChange = { claimDate = it },
                    modifier = Modifier.padding(top = 12.dp),
                )
                PrimaryButton(
                    text = stringResource(R.string.badge_claim),
                    onClick = { onClaim(claimDate) },
                    tint = color,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                )
            } else if (badge.def.unit != BadgeUnit.FLAG) {
                Box(
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(TaroTheme.colors.accentOpaque),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(badge.fraction)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                }
                Text(
                    text = "${formatValue(badge.def.unit, badge.progress.value)} / ${formatValue(badge.def.unit, badge.def.target)}",
                    color = TaroTheme.colors.accent,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
