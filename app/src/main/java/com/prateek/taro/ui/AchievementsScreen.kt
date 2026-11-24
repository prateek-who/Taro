package com.prateek.taro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import com.prateek.taro.R
import com.prateek.taro.achievements.AchievementData
import com.prateek.taro.achievements.AchievementInputs
import com.prateek.taro.achievements.BadgeCategory
import com.prateek.taro.achievements.BadgeResult
import com.prateek.taro.achievements.BadgeUnit
import com.prateek.taro.achievements.Badges
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.ui.components.BentoValue
import com.prateek.taro.ui.components.DateRow
import com.prateek.taro.ui.components.IconDisc
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.MeterBar
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.RowDivider
import com.prateek.taro.ui.components.SecondaryButton
import com.prateek.taro.ui.components.SectionLabel
import com.prateek.taro.ui.components.StackBar
import com.prateek.taro.ui.components.TaroDialog
import com.prateek.taro.ui.components.TintChip
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Util
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object AchievementsScreen : Screen {
    @Composable
    override fun Content() {
        AchievementsContent()
    }
}

private val WEIGHT_PINK = Color(0xFFFF6FAE)
private val EXPLORER_BLUE = Color(0xFF4FC3F7)
private val MIND_VIOLET = Color(0xFFB388FF)
private val STRENGTH_RED = Color(0xFFFF6E6E)

private const val SHOWCASE_SIZE = 3
private const val ALMOST_COUNT = 3
private const val GRID_COLUMNS = 3
private const val SHELF_MEDAL_SHARE = 0.72f
private const val GRID_MEDAL_SHARE = 0.62f
private const val DIALOG_MEDAL_SHARE = 0.38f
private const val ROW_MEDAL_SHARE = 1.05f
private const val MEDAL_STROKE_SHARE = 0.06f
private const val MEDAL_ICON_SHARE = 0.42f
private const val MEDAL_GLOW_INNER = 0.34f
private const val MEDAL_GLOW_OUTER = 0.1f
private const val LOCKED_ICON_ALPHA = 0.55f
private const val LOCKED_ARC_ALPHA = 0.75f
private const val PERCENT = 100

@Composable
private fun categoryColor(category: BadgeCategory): Color = when (category) {
    BadgeCategory.STEPS -> TaroTheme.colors.goal
    BadgeCategory.GOALS -> TaroTheme.colors.special
    BadgeCategory.CLIMB -> TaroTheme.colors.mint
    BadgeCategory.ENERGY -> TaroTheme.colors.flame
    BadgeCategory.WEIGHT -> WEIGHT_PINK
    BadgeCategory.SLEEP -> TaroTheme.colors.sleep
    BadgeCategory.EXPLORER -> EXPLORER_BLUE
    BadgeCategory.FOOD -> TaroTheme.colors.goal
    BadgeCategory.MIND -> MIND_VIOLET
    BadgeCategory.SKILLS -> TaroTheme.colors.flame
    BadgeCategory.STRENGTH -> STRENGTH_RED
    BadgeCategory.LEGENDS -> TaroTheme.colors.special
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

private fun progressText(badge: BadgeResult): String =
    "${formatValue(badge.def.unit, badge.progress.value)} / ${formatValue(badge.def.unit, badge.def.target)}"

private data class AchievementsState(val inputs: AchievementInputs, val badges: List<BadgeResult>)

@Composable
private fun AchievementsContent() {
    val context = LocalContext.current
    val dataVersion = rememberDataVersion()
    val state = rememberInBackground(dataVersion) {
        val inputs = AchievementData.inputs(context)
        AchievementsState(inputs, Badges.evaluate(inputs))
    }?.value
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    var open by remember { mutableStateOf<BadgeResult?>(null) }
    val showcaseIds by AppPreferences.showcaseBadgesFlow().collectAsStateWithLifecycle(AppPreferences.showcaseBadges)
    val toast = LocalToast.current
    val dateFormat = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()) }
    val shortFormat = remember { DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()) }
    val spacing = TaroTheme.spacing
    val colors = TaroTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AppOverflowMenu(back = true, title = stringResource(R.string.achievements_title))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.loose, end = spacing.loose, bottom = spacing.loose),
        ) {
            if (state == null) {
                BentoNote(stringResource(R.string.badges_loading))
                return@Column
            }
            val badges = state.badges.filter { !it.def.claimable || it.earned }
            val earned = state.badges.filter { it.earned }
            val categories = BadgeCategory.entries.mapNotNull { category ->
                state.badges.filter { it.def.category == category }.takeIf { it.isNotEmpty() }?.let { category to it }
            }

            BentoTile(color = colors.special, label = stringResource(R.string.badge_trophy_room), tinted = true, modifier = Modifier.fillMaxWidth()) {
                BentoValue("${earned.size}", colors.special, unit = stringResource(R.string.badge_unlocked_of, badges.size))
                StackBar(
                    parts = categories.map { (category, list) -> list.count { it.earned }.toDouble() to categoryColor(category) } +
                        ((badges.size - earned.size).coerceAtLeast(0).toDouble() to colors.accentOpaque),
                    modifier = Modifier.padding(top = spacing.gap),
                )
                earned.maxByOrNull { it.progress.earnedOn!! }?.let {
                    BentoNote(
                        stringResource(R.string.badge_latest, stringResource(it.def.title), it.progress.earnedOn!!.format(shortFormat)),
                        Modifier.padding(top = spacing.gap),
                    )
                }
                val showcase = if (showcaseIds.isEmpty()) {
                    earned.sortedByDescending { it.progress.earnedOn }.take(SHOWCASE_SIZE)
                } else {
                    showcaseIds.mapNotNull { id -> earned.firstOrNull { it.def.id == id } }
                }
                if (showcase.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = spacing.loose)) {
                        repeat(SHOWCASE_SIZE) { slot ->
                            val badge = showcase.getOrNull(slot)
                            if (badge == null) {
                                Spacer(Modifier.weight(1f))
                            } else {
                                MedalCell(badge, note = null, medalShare = SHELF_MEDAL_SHARE, onClick = { open = badge }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    if (showcaseIds.isEmpty()) {
                        BentoNote(stringResource(R.string.badge_showcase_hint), Modifier.padding(top = spacing.gap))
                    }
                }
            }

            val almost = badges
                .filter { !it.earned && !it.def.claimable && it.def.unit != BadgeUnit.FLAG && it.fraction > 0f }
                .sortedByDescending { it.fraction }
                .take(ALMOST_COUNT)
            if (almost.isNotEmpty()) {
                SectionLabel(stringResource(R.string.badges_almost))
                Panel {
                    almost.forEachIndexed { index, badge ->
                        if (index > 0) RowDivider()
                        AlmostRow(badge) { open = badge }
                    }
                }
            }

            SectionLabel(stringResource(R.string.badges_categories))
            Panel {
                categories.forEachIndexed { index, (category, list) ->
                    if (index > 0) RowDivider()
                    val isOpen = expanded == category.name
                    CategoryRow(category, list, isOpen) { expanded = if (isOpen) null else category.name }
                    if (isOpen) {
                        val groups = if (list.any { it.def.group != null }) {
                            list.groupBy { it.def.group }.toList().sortedBy { it.first?.ordinal ?: -1 }
                        } else {
                            listOf(null to list.sortedWith(compareByDescending<BadgeResult> { it.earned }.thenByDescending { if (it.earned) 0f else it.fraction }))
                        }
                        groups.forEach { (group, members) ->
                            group?.let {
                                BentoNote(
                                    "${stringResource(it.label)}  ${members.count { badge -> badge.earned }}/${members.size}",
                                    Modifier.padding(start = spacing.tight, top = spacing.gap),
                                )
                            }
                            members.chunked(GRID_COLUMNS).forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = spacing.tight)) {
                                    row.forEach { badge ->
                                        val note = badge.progress.earnedOn?.format(shortFormat)
                                            ?: if (badge.def.unit == BadgeUnit.FLAG || badge.def.claimable) stringResource(R.string.badge_locked) else progressText(badge)
                                        MedalCell(badge, note, GRID_MEDAL_SHARE, onClick = { open = badge }, modifier = Modifier.weight(1f))
                                    }
                                    repeat(GRID_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
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
                    showcaseIds.size >= SHOWCASE_SIZE -> toast.show(context.getString(R.string.badge_showcase_full), ToastKind.INFO)
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
private fun rowMedalSize() = with(LocalDensity.current) { (TaroTheme.text.title.lineHeight.toDp() + TaroTheme.text.note.lineHeight.toDp()) * ROW_MEDAL_SHARE }

@Composable
private fun AlmostRow(badge: BadgeResult, onClick: () -> Unit) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val color = categoryColor(badge.def.category)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.gap),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.gap))
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.tight, vertical = spacing.gap),
    ) {
        BadgeMedal(badge, color, Modifier.size(rowMedalSize()))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(badge.def.title), style = type.title, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.badge_to_go, formatValue(badge.def.unit, (badge.def.target - badge.progress.value).coerceAtLeast(0.0))),
                style = type.note,
                color = TaroTheme.colors.accent,
            )
        }
        Text(stringResource(R.string.badge_percent, (badge.fraction * PERCENT).roundToInt()), style = type.meterValue, color = color)
    }
}

@Composable
private fun CategoryRow(category: BadgeCategory, badges: List<BadgeResult>, open: Boolean, onClick: () -> Unit) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val color = categoryColor(category)
    val got = badges.count { it.earned }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.gap),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.gap))
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.tight, vertical = spacing.gap),
    ) {
        IconDisc(badges.first().def.icon, color)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(category.label), style = type.title, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.badge_count, got, badges.size), style = type.meterValue, color = TaroTheme.colors.accent)
                Icon(
                    painterResource(if (open) R.drawable.ic_expand_less else R.drawable.ic_expand_more),
                    contentDescription = null,
                    tint = TaroTheme.colors.accent,
                    modifier = Modifier.padding(start = spacing.hair).size(with(LocalDensity.current) { type.meterValue.lineHeight.toDp() }),
                )
            }
            MeterBar(fraction = got.toFloat() / badges.size, color = color, modifier = Modifier.padding(top = spacing.tight))
        }
    }
}

@Composable
private fun MedalCell(badge: BadgeResult, note: String?, medalShare: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val color = categoryColor(badge.def.category)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(spacing.gap))
            .clickable(onClick = onClick)
            .padding(spacing.tight),
    ) {
        BadgeMedal(badge, color, Modifier.fillMaxWidth(medalShare).aspectRatio(1f))
        Text(
            text = stringResource(badge.def.title),
            style = type.meterLabel,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = if (badge.earned) MaterialTheme.colorScheme.onSurface else TaroTheme.colors.accent,
            modifier = Modifier.padding(top = spacing.tight),
        )
        note?.let {
            Text(
                text = it,
                style = type.note,
                color = if (badge.earned) color else TaroTheme.colors.accent,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun BadgeMedal(badge: BadgeResult, color: Color, modifier: Modifier) {
    val track = TaroTheme.colors.accentOpaque
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * MEDAL_STROKE_SHARE
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            if (badge.earned) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(color.copy(alpha = MEDAL_GLOW_INNER), color.copy(alpha = MEDAL_GLOW_OUTER)),
                        center = Offset(size.width * 0.35f, size.height * 0.3f),
                        radius = size.minDimension,
                    ),
                )
                drawArc(color, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            } else {
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                if (badge.fraction > 0f) {
                    drawArc(color.copy(alpha = LOCKED_ARC_ALPHA), -90f, 360f * badge.fraction, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
        }
        Icon(
            painter = painterResource(badge.def.icon),
            contentDescription = null,
            tint = if (badge.earned) color else TaroTheme.colors.accent.copy(alpha = LOCKED_ICON_ALPHA),
            modifier = Modifier.fillMaxSize(MEDAL_ICON_SHARE),
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
        val spacing = TaroTheme.spacing
        val type = TaroTheme.text
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            BadgeMedal(badge, color, Modifier.fillMaxWidth(DIALOG_MEDAL_SHARE).aspectRatio(1f))
            Text(
                text = stringResource(badge.def.description, descriptionParam(badge)),
                style = type.note,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.loose),
            )
            if (badge.earned) {
                TintChip(
                    text = stringResource(R.string.badge_earned_on, badge.progress.earnedOn!!.format(dateFormat)),
                    color = color,
                    modifier = Modifier.padding(top = spacing.loose),
                )
                SecondaryButton(
                    text = stringResource(if (showcased) R.string.badge_showcase_remove else R.string.badge_showcase_add),
                    onClick = onToggleShowcase,
                    icon = R.drawable.ic_badge_star,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.gap),
                )
                if (badge.def.claimable) {
                    SecondaryButton(
                        text = stringResource(R.string.badge_unclaim),
                        onClick = { onClaim(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = spacing.tight),
                    )
                }
            } else if (badge.def.claimable) {
                Text(
                    text = stringResource(R.string.badge_claim_hint),
                    style = type.note,
                    color = TaroTheme.colors.accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = spacing.gap),
                )
                DateRow(
                    label = stringResource(R.string.badge_claimed_on),
                    date = claimDate,
                    today = Util.logicalToday(),
                    onChange = { claimDate = it },
                    modifier = Modifier.padding(top = spacing.gap),
                )
                PrimaryButton(
                    text = stringResource(R.string.badge_claim),
                    onClick = { onClaim(claimDate) },
                    tint = color,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.gap),
                )
            } else if (badge.def.unit != BadgeUnit.FLAG) {
                MeterBar(fraction = badge.fraction, color = color, modifier = Modifier.padding(top = spacing.loose))
                Text(
                    text = progressText(badge),
                    style = type.note,
                    color = TaroTheme.colors.accent,
                    modifier = Modifier.padding(top = spacing.tight),
                )
            }
        }
    }
}
