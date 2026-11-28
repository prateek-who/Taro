package com.prateek.taro.ui

import com.prateek.taro.ui.components.BoneText
import com.prateek.taro.ui.components.Skeleton
import com.prateek.taro.ui.components.LetterTab
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.prateek.taro.BuildConfig
import com.prateek.taro.R
import com.prateek.taro.achievements.AchievementData
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.ui.components.BentoValue
import com.prateek.taro.ui.components.EditRow
import com.prateek.taro.ui.components.HtmlDialog
import com.prateek.taro.ui.components.LinkText
import com.prateek.taro.ui.components.NavRow
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.components.RowDivider
import com.prateek.taro.ui.components.SectionLabel
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Util.UnitSystem
import com.prateek.taro.util.Util
import kotlin.math.roundToInt

object YouTab : Tab, LetterTab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 2u,
            title = stringResource(R.string.tab_you),
            icon = painterResource(R.drawable.ic_you),
        )

    @Composable
    override fun letter(): String? {
        val name by AppPreferences.nameFlow().collectAsStateWithLifecycle(AppPreferences.name)
        return name.firstOrNull()?.uppercase()
    }

    @Composable
    override fun Content() {
        YouContent()
    }
}

private enum class YouDialog { CALORIES, PROTEIN, ABOUT }

private const val AVATAR_SHARE = 2.2f
private const val AVATAR_ALPHA = 0.18f
private const val BADGE_SHARE = 1.6f
private const val BADGE_ALPHA = 0.2f
private const val RECENT_BADGES = 3

@Composable
private fun YouContent() {
    val context = LocalContext.current
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val colors = TaroTheme.colors
    val navigator = LocalNavigator.currentOrThrow
    val root = navigator.parent ?: navigator
    val dataVersion = rememberDataVersion()
    var dialog by remember { mutableStateOf<YouDialog?>(null) }

    val name by AppPreferences.nameFlow().collectAsStateWithLifecycle(AppPreferences.name)
    val height by AppPreferences.heightFlow().collectAsStateWithLifecycle(AppPreferences.height)
    val weight by AppPreferences.weightFlow().collectAsStateWithLifecycle(AppPreferences.weight)
    val age by AppPreferences.ageFlow().collectAsStateWithLifecycle(AppPreferences.age)
    val sex by AppPreferences.sexFlow().collectAsStateWithLifecycle(AppPreferences.sex)
    val unitSystem by AppPreferences.unitSystemFlow().collectAsStateWithLifecycle(AppPreferences.unitSystem)
    val goal by AppPreferences.dailyGoalTargetFlow().collectAsStateWithLifecycle(AppPreferences.dailyGoalTarget)
    val calorieGoal by AppPreferences.calorieGoalFlow().collectAsStateWithLifecycle(AppPreferences.calorieGoal)
    val proteinPerKg by AppPreferences.proteinPerKgFlow().collectAsStateWithLifecycle(AppPreferences.proteinPerKg)
    val stepLength by AppPreferences.stepLengthFlow().collectAsStateWithLifecycle(AppPreferences.stepLength)
    val calibration by AppPreferences.stepCalibrationFlow().collectAsStateWithLifecycle(AppPreferences.stepCalibration)
    val badges = rememberInBackground(dataVersion) { AchievementData.evaluate(context) }?.value
    val imperial = unitSystem == UnitSystem.IMPERIAL

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        AppOverflowMenu(title = stringResource(R.string.tab_you))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.loose, end = spacing.loose, bottom = spacing.loose),
        ) {
            val avatar = with(LocalDensity.current) { type.fact.lineHeight.toDp() } * AVATAR_SHARE
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.loose),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(spacing.loose))
                    .clickable { root.push(SettingsScreen) }
                    .padding(spacing.tight),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(avatar)
                        .clip(CircleShape)
                        .background(colors.goal.copy(alpha = AVATAR_ALPHA)),
                ) {
                    Text(name.firstOrNull()?.uppercase() ?: "", style = type.fact, color = colors.goal)
                    if (name.isBlank()) {
                        Icon(painterResource(R.drawable.ic_you), contentDescription = null, tint = colors.goal, modifier = Modifier.size(avatar / 2))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(name.ifBlank { stringResource(R.string.you_add_name) }, style = type.fact, color = if (name.isBlank()) colors.accent else MaterialTheme.colorScheme.onSurface)
                    BentoNote(
                        listOfNotNull(
                            heightLabel(height, imperial),
                            weightLabel(weight, imperial),
                            age?.toString(),
                            sex?.let { sexLabel(it) },
                        ).joinToString(" · "),
                    )
                }
                Icon(
                    painterResource(R.drawable.ic_edit),
                    contentDescription = stringResource(R.string.you_edit_profile),
                    tint = colors.accent,
                    modifier = Modifier.size(with(LocalDensity.current) { type.meterValue.lineHeight.toDp() }),
                )
            }

            LeaderboardTile(Modifier.padding(top = spacing.gap)) { root.push(LeaderboardScreen) }
            BadgesTile(badges, modifier = Modifier.padding(top = spacing.loose)) { root.push(AchievementsScreen) }

            SectionLabel(stringResource(R.string.you_goals))
            Panel {
                Column(modifier = Modifier.padding(vertical = spacing.hair)) {
                    EditRow(R.drawable.ic_steps, colors.goal, stringResource(R.string.tab_steps), stringResource(R.string.you_steps_goal, Util.formatSteps(goal)), onClick = { root.push(DailyGoalsScreen) })
                    RowDivider()
                    EditRow(
                        icon = R.drawable.ic_calorie_goal,
                        color = colors.special,
                        label = stringResource(R.string.you_calories),
                        value = calorieGoalSummary(calorieGoal),
                        onClick = { dialog = YouDialog.CALORIES },
                        valueColor = if (calorieGoal != null) colors.goal else colors.accent,
                    )
                    RowDivider()
                    EditRow(
                        icon = R.drawable.ic_badge_bolt,
                        color = PROTEIN_BLUE,
                        label = stringResource(R.string.food_protein),
                        value = stringResource(R.string.you_protein_goal, Util.formatMeasure(proteinPerKg.toDouble()), (proteinPerKg * weight).roundToInt()),
                        onClick = { dialog = YouDialog.PROTEIN },
                    )
                }
            }

            SectionLabel(stringResource(R.string.pref_step_length))
            Panel {
                Column(modifier = Modifier.padding(spacing.tight)) {
                    Text(stepLengthLabel(stepLength, imperial), style = type.value, color = MaterialTheme.colorScheme.onSurface)
                    BentoNote(
                        stringResource(
                            when {
                                AppPreferences.manualStepLength != null -> R.string.you_step_manual
                                calibration != null -> R.string.you_step_calibrated
                                else -> R.string.you_step_estimated
                            },
                        ),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.loose), modifier = Modifier.padding(top = spacing.tight)) {
                        LinkText(stringResource(R.string.you_calibrate), colors.goal, onClick = { root.push(CalibrationScreen) }, icon = R.drawable.ic_calibrate)
                        LinkText(stringResource(R.string.you_accuracy), MaterialTheme.colorScheme.onSurface, onClick = { root.push(AccuracyScreen) }, icon = R.drawable.ic_accuracy)
                    }
                }
            }

            SectionLabel(stringResource(R.string.you_app))
            Panel {
                Column(modifier = Modifier.padding(vertical = spacing.hair)) {
                    NavRow(R.drawable.ic_schedule, colors.goal, stringResource(R.string.report_title), stringResource(R.string.you_reports_note), onClick = { root.push(ReportScreen()) })
                    RowDivider()
                    NavRow(R.drawable.ic_small_backup, colors.flame, stringResource(R.string.you_backup), stringResource(R.string.you_backup_note), onClick = { root.push(BackupScreen) })
                    RowDivider()
                    NavRow(R.drawable.ic_small_settings, MaterialTheme.colorScheme.onSurface, stringResource(R.string.settings), stringResource(R.string.you_settings_note), onClick = { root.push(SettingsScreen) })
                    RowDivider()
                    NavRow(R.drawable.ic_info, PROTEIN_BLUE, stringResource(R.string.about_stepsy), stringResource(R.string.you_about_note, BuildConfig.VERSION_NAME), onClick = { dialog = YouDialog.ABOUT })
                }
            }
        }
    }

    when (dialog) {
        YouDialog.CALORIES -> CalorieGoalDialog(
            current = calorieGoal,
            onSave = {
                dialog = null
                AppPreferences.calorieGoal = it
            },
            onDismiss = { dialog = null },
        )
        YouDialog.PROTEIN -> ProteinTargetDialog(onDismiss = { dialog = null })
        YouDialog.ABOUT -> HtmlDialog(
            title = stringResource(R.string.about_stepsy),
            html = stringResource(R.string.about_html, BuildConfig.VERSION_NAME),
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun BadgesTile(badges: List<com.prateek.taro.achievements.BadgeResult>?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    BentoTile(colors.special, modifier = modifier.fillMaxWidth(), label = stringResource(R.string.achievements_title), tinted = true, onClick = onClick, order = 1) {
        if (badges == null) {
            Skeleton {
                BoneText(type.value, 0.3f, Modifier.padding(top = spacing.tight))
                BoneText(type.note, 0.6f)
            }
            return@BentoTile
        }
        val earned = badges.filter { it.earned }
        BentoValue(earned.size.toString(), MaterialTheme.colorScheme.onSurface, unit = stringResource(R.string.you_badges_of, badges.size))
        val recent = earned.sortedByDescending { it.progress.earnedOn }.take(RECENT_BADGES)
        if (recent.isNotEmpty()) {
            val disc = with(LocalDensity.current) { type.fact.lineHeight.toDp() } * BADGE_SHARE
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.gap), modifier = Modifier.padding(top = spacing.gap)) {
                recent.forEach { badge ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(disc)
                                .clip(CircleShape)
                                .background(colors.special.copy(alpha = BADGE_ALPHA)),
                        ) {
                            Icon(painterResource(badge.def.icon), contentDescription = null, tint = colors.special, modifier = Modifier.size(disc / 2))
                        }
                        Text(
                            text = stringResource(badge.def.title),
                            style = type.note,
                            color = colors.accent,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = spacing.tight),
                        )
                    }
                }
            }
        }
        LinkText(stringResource(R.string.you_all_badges), colors.special, onClick = onClick, trailingIcon = R.drawable.ic_chevron_right, modifier = Modifier.padding(top = spacing.tight))
    }
}
