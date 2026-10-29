package com.example.ui.screens.progress

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserDailyActivityEntity
import com.example.data.repository.GameRepository
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenDark
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.PastelPeach
import com.example.ui.theme.PastelYellow
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryCoralVariant
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Model representing daily summarized progress for a specific day in the past week.
 */
data class DayActivitySummary(
    val dateString: String,      // e.g. "2026-10-29"
    val dayName: String,         // e.g. "Pzt", "Sal", "Çar"
    val formattedDate: String,   // e.g. "29 Eki"
    val isToday: Boolean,
    val playTimeSeconds: Long,
    val gamesCount: Int,
    val scoreEarned: Int,
    val starsEarned: Int
)

enum class ChartMetricType(val title: String, val unit: String, val icon: String) {
    PLAY_TIME("Oynama Süresi", "dk", "⏱️"),
    SCORE("Zihin Puanı", "Puan", "🧠"),
    GAMES_PLAYED("Oyun Sayısı", "Adet", "🎮")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyTrendScreen(
    repository: GameRepository,
    onBackClick: () -> Unit
) {
    val allActivities by repository.allDailyActivities.collectAsState(initial = emptyList())
    var selectedMetric by remember { mutableStateOf(ChartMetricType.PLAY_TIME) }
    var selectedDayIndex by remember { mutableIntStateOf(6) } // Default to today (last index)

    // Build the 7 continuous days ending with today
    val weeklyDays = remember(allActivities) {
        buildWeeklyData(allActivities)
    }

    // Weekly aggregate calculations
    val totalWeeklySeconds = remember(weeklyDays) { weeklyDays.sumOf { it.playTimeSeconds } }
    val avgDailyMinutes = remember(totalWeeklySeconds) { (totalWeeklySeconds / 60) / 7.0 }
    val totalWeeklyScore = remember(weeklyDays) { weeklyDays.sumOf { it.scoreEarned } }
    val totalWeeklyGames = remember(weeklyDays) { weeklyDays.sumOf { it.gamesCount } }
    val peakDay = remember(weeklyDays) {
        weeklyDays.maxByOrNull { it.playTimeSeconds } ?: weeklyDays.last()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("weekly_trend_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Haftalık Aktivite Trendleri",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Recharts İlerleme & Performans Grafikleri",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri Dön"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Metric Selector Tabs (Recharts style legend and toggle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChartMetricType.values().forEach { metric ->
                        val isSelected = selectedMetric == metric
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMetric = metric },
                            label = {
                                Text(
                                    text = "${metric.icon} ${metric.title}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryCoral,
                                selectedLabelColor = com.example.ui.theme.OnNeon
                            ),
                            modifier = Modifier.testTag("filter_chip_${metric.name.lowercase()}")
                        )
                    }
                }
            }

            // Main Recharts-style Chart Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recharts_main_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = when (selectedMetric) {
                                        ChartMetricType.PLAY_TIME -> "Günlük Oyun Süresi Grafiği"
                                        ChartMetricType.SCORE -> "Zihin Skoru Trendi (Alan Eğrisi)"
                                        ChartMetricType.GAMES_PLAYED -> "Tamamlanan Oyun Dağılımı"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Son 7 günlük Room DB verisi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Active indicator pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MintGreen)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "CANLI VERİ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MintGreenDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Selected day detail Recharts tooltip pill
                        val currentDay = weeklyDays.getOrNull(selectedDayIndex) ?: weeklyDays.last()
                        SelectedDayTooltip(
                            day = currentDay,
                            metricType = selectedMetric
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Recharts Canvas Chart
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            when (selectedMetric) {
                                ChartMetricType.PLAY_TIME -> {
                                    RechartsBarChart(
                                        data = weeklyDays,
                                        selectedIndex = selectedDayIndex,
                                        onSelectDay = { selectedDayIndex = it }
                                    )
                                }
                                ChartMetricType.SCORE -> {
                                    RechartsAreaCurveChart(
                                        data = weeklyDays,
                                        selectedIndex = selectedDayIndex,
                                        onSelectDay = { selectedDayIndex = it }
                                    )
                                }
                                ChartMetricType.GAMES_PLAYED -> {
                                    RechartsGamesBarChart(
                                        data = weeklyDays,
                                        selectedIndex = selectedDayIndex,
                                        onSelectDay = { selectedDayIndex = it }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 Çubuklara veya noktalara dokunarak o günün detaylarını inceleyin.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            // Summary Metric KPI Cards
            item {
                Text(
                    text = "Haftalık Özet Metrikler",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WeeklyKpiCard(
                        title = "Toplam Süre",
                        value = formatDuration(totalWeeklySeconds),
                        icon = "⏳",
                        color = PrimaryCoral,
                        modifier = Modifier.weight(1f)
                    )
                    WeeklyKpiCard(
                        title = "Günlük Ortalama",
                        value = "%.1f dk".format(Locale.getDefault(), avgDailyMinutes),
                        icon = "📊",
                        color = MintGreenDark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WeeklyKpiCard(
                        title = "Toplam Skor",
                        value = "+$totalWeeklyScore P",
                        icon = "🏆",
                        color = PrimaryCoralVariant,
                        modifier = Modifier.weight(1f)
                    )
                    WeeklyKpiCard(
                        title = "En Aktif Gün",
                        value = "${peakDay.dayName} (${formatDuration(peakDay.playTimeSeconds)})",
                        icon = "🔥",
                        color = com.example.ui.theme.NeonOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Day-by-Day Detailed Breakdown (Recharts Data Table)
            item {
                Text(
                    text = "7 Günlük Detaylı Döküm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(weeklyDays.reversed()) { day ->
                DayBreakdownItemCard(
                    day = day,
                    isSelected = weeklyDays.indexOf(day) == selectedDayIndex,
                    onClick = { selectedDayIndex = weeklyDays.indexOf(day) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/**
 * Recharts-inspired Interactive Bar Chart for Daily Play Time.
 */
@Composable
fun RechartsBarChart(
    data: List<DayActivitySummary>,
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit
) {
    val maxMinutes = remember(data) {
        val maxVal = data.maxOfOrNull { it.playTimeSeconds / 60.0 } ?: 10.0
        maxOf(maxVal, 10.0) // At least 10 minutes for nice scaling
    }

    val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val textPrimaryColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val activeBarColor = PrimaryCoral
    val regularBarColor = PrimaryCoral.copy(alpha = 0.65f)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(data) {
                detectTapGestures { offset ->
                    val widthPerBar = size.width / data.size
                    val index = (offset.x / widthPerBar).toInt().coerceIn(0, data.size - 1)
                    onSelectDay(index)
                }
            }
            .testTag("play_time_recharts_canvas")
    ) {
        val width = size.width
        val height = size.height
        val bottomMargin = 40.dp.toPx()
        val topMargin = 20.dp.toPx()
        val chartHeight = height - bottomMargin - topMargin
        val barCount = data.size
        val barSlotWidth = width / barCount
        val barWidth = barSlotWidth * 0.45f

        // Draw 3 horizontal dashed grid lines (Recharts CartesianGrid)
        val gridSteps = 3
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        for (i in 0..gridSteps) {
            val y = topMargin + chartHeight * (i.toFloat() / gridSteps)
            drawLine(
                color = gridLineColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = pathEffect
            )
            val minutesAtGrid = (maxMinutes * (1.0 - (i.toFloat() / gridSteps))).toInt()
            drawContext.canvas.nativeCanvas.drawText(
                "${minutesAtGrid}dk",
                10f,
                y - 6f,
                android.graphics.Paint().apply {
                    color = textPrimaryColor
                    textSize = 24f
                    isAntiAlias = true
                }
            )
        }

        // Draw Bars
        data.forEachIndexed { index, day ->
            val minutes = day.playTimeSeconds / 60.0
            val normalizedHeight = ((minutes / maxMinutes).toFloat() * chartHeight).coerceAtLeast(8.dp.toPx())
            val left = index * barSlotWidth + (barSlotWidth - barWidth) / 2f
            val top = topMargin + (chartHeight - normalizedHeight)
            val isSelected = index == selectedIndex

            // Gradient for bar
            val barBrush = if (isSelected) {
                Brush.verticalGradient(
                    colors = listOf(PrimaryCoralVariant, PrimaryCoral),
                    startY = top,
                    endY = top + normalizedHeight
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(regularBarColor, regularBarColor.copy(alpha = 0.4f)),
                    startY = top,
                    endY = top + normalizedHeight
                )
            }

            // Draw rounded top bar
            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(left, top),
                size = Size(barWidth, normalizedHeight),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )

            // If selected, draw an accent top cap highlight
            if (isSelected) {
                drawRoundRect(
                    color = com.example.ui.theme.OnNeon.copy(alpha = 0.7f),
                    topLeft = Offset(left + 2f, top + 2f),
                    size = Size(barWidth - 4f, 4.dp.toPx()),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }

            // Day label below bar
            val labelText = if (day.isToday) "Bugün" else day.dayName
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                left + (barWidth / 2f),
                height - 12.dp.toPx(),
                android.graphics.Paint().apply {
                    color = if (isSelected) PrimaryCoral.toArgb() else textPrimaryColor
                    textSize = if (isSelected) 28f else 26f
                    isFakeBoldText = isSelected
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }
    }
}

/**
 * Recharts-inspired Area & Smooth Curve Chart for Daily Score Trends.
 */
@Composable
fun RechartsAreaCurveChart(
    data: List<DayActivitySummary>,
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit
) {
    val maxScore = remember(data) {
        val maxVal = data.maxOfOrNull { it.scoreEarned } ?: 100
        maxOf(maxVal, 100)
    }

    val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val textPrimaryColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val curveColor = MintGreenDark

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(data) {
                detectTapGestures { offset ->
                    val widthPerPoint = size.width / (data.size - 1).coerceAtLeast(1)
                    val index = ((offset.x + widthPerPoint / 2) / widthPerPoint).toInt().coerceIn(0, data.size - 1)
                    onSelectDay(index)
                }
            }
            .testTag("score_curve_recharts_canvas")
    ) {
        val width = size.width
        val height = size.height
        val bottomMargin = 40.dp.toPx()
        val topMargin = 20.dp.toPx()
        val horizontalMargin = 24.dp.toPx()
        val chartHeight = height - bottomMargin - topMargin
        val chartWidth = width - (horizontalMargin * 2)

        // Grid lines
        val gridSteps = 3
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        for (i in 0..gridSteps) {
            val y = topMargin + chartHeight * (i.toFloat() / gridSteps)
            drawLine(
                color = gridLineColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = pathEffect
            )
        }

        // Calculate points
        val points = data.mapIndexed { index, day ->
            val x = horizontalMargin + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * chartWidth
            val normalizedY = (day.scoreEarned.toFloat() / maxScore).coerceIn(0f, 1f)
            val y = topMargin + (chartHeight * (1f - normalizedY))
            Offset(x, y)
        }

        if (points.isNotEmpty()) {
            val curvePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val controlX1 = (p0.x + p1.x) / 2f
                    val controlY1 = p0.y
                    val controlX2 = (p0.x + p1.x) / 2f
                    val controlY2 = p1.y
                    cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                }
            }

            // Fill area underneath (Recharts Area gradient)
            val areaPath = Path().apply {
                addPath(curvePath)
                lineTo(points.last().x, topMargin + chartHeight)
                lineTo(points.first().x, topMargin + chartHeight)
                close()
            }

            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(curveColor.copy(alpha = 0.35f), curveColor.copy(alpha = 0.02f)),
                    startY = topMargin,
                    endY = topMargin + chartHeight
                )
            )

            // Draw Smooth Curve Stroke
            drawPath(
                path = curvePath,
                color = curveColor,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw Data Points (Recharts Dots)
            points.forEachIndexed { index, point ->
                val isSelected = index == selectedIndex
                if (isSelected) {
                    // Outer glow ring
                    drawCircle(
                        color = curveColor.copy(alpha = 0.25f),
                        radius = 12.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = curveColor,
                        radius = 6.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = com.example.ui.theme.OnNeon,
                        radius = 3.dp.toPx(),
                        center = point
                    )
                } else {
                    drawCircle(
                        color = com.example.ui.theme.OnNeon,
                        radius = 4.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = curveColor,
                        radius = 4.dp.toPx(),
                        center = point,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // X-Axis labels
                val day = data[index]
                val labelText = if (day.isToday) "Bugün" else day.dayName
                drawContext.canvas.nativeCanvas.drawText(
                    labelText,
                    point.x,
                    height - 12.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = if (isSelected) curveColor.toArgb() else textPrimaryColor
                        textSize = if (isSelected) 28f else 26f
                        isFakeBoldText = isSelected
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                )
            }
        }
    }
}

/**
 * Recharts-inspired Games Completed Distribution Bar Chart.
 */
@Composable
fun RechartsGamesBarChart(
    data: List<DayActivitySummary>,
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit
) {
    val maxGames = remember(data) {
        val maxVal = data.maxOfOrNull { it.gamesCount } ?: 5
        maxOf(maxVal, 5)
    }

    val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val textPrimaryColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val barColor = com.example.ui.theme.NeonCyan // Recharts blue

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(data) {
                detectTapGestures { offset ->
                    val widthPerBar = size.width / data.size
                    val index = (offset.x / widthPerBar).toInt().coerceIn(0, data.size - 1)
                    onSelectDay(index)
                }
            }
            .testTag("games_bar_recharts_canvas")
    ) {
        val width = size.width
        val height = size.height
        val bottomMargin = 40.dp.toPx()
        val topMargin = 20.dp.toPx()
        val chartHeight = height - bottomMargin - topMargin
        val barSlotWidth = width / data.size
        val barWidth = barSlotWidth * 0.42f

        // Grid lines
        for (i in 0..maxGames) {
            val y = topMargin + chartHeight * (1f - (i.toFloat() / maxGames))
            drawLine(
                color = gridLineColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        data.forEachIndexed { index, day ->
            val games = day.gamesCount
            val normalizedHeight = ((games.toFloat() / maxGames) * chartHeight).coerceAtLeast(6.dp.toPx())
            val left = index * barSlotWidth + (barSlotWidth - barWidth) / 2f
            val top = topMargin + (chartHeight - normalizedHeight)
            val isSelected = index == selectedIndex

            val brush = if (isSelected) {
                Brush.verticalGradient(listOf(com.example.ui.theme.NeonPurple, com.example.ui.theme.NeonCyan))
            } else {
                Brush.verticalGradient(listOf(barColor.copy(alpha = 0.7f), barColor.copy(alpha = 0.3f)))
            }

            drawRoundRect(
                brush = brush,
                topLeft = Offset(left, top),
                size = Size(barWidth, normalizedHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // Label
            val labelText = if (day.isToday) "Bugün" else day.dayName
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                left + (barWidth / 2f),
                height - 12.dp.toPx(),
                android.graphics.Paint().apply {
                    color = if (isSelected) com.example.ui.theme.NeonPurple.toArgb() else textPrimaryColor
                    textSize = if (isSelected) 28f else 26f
                    isFakeBoldText = isSelected
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }
    }
}

/**
 * Recharts Tooltip floating header widget with rounded box and subtle shadow.
 */
@Composable
fun SelectedDayTooltip(
    day: DayActivitySummary,
    metricType: ChartMetricType
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recharts_tooltip_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (day.isToday) "Bugün (${day.formattedDate})" else "${day.dayName}, ${day.formattedDate}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (day.isToday) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimaryCoral.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Bugün", fontSize = 10.sp, color = PrimaryCoral, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatDuration(day.playTimeSeconds)} • ${day.gamesCount} oyun • ⭐ ${day.starsEarned} yıldız",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryCoral)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = when (metricType) {
                        ChartMetricType.PLAY_TIME -> formatDuration(day.playTimeSeconds)
                        ChartMetricType.SCORE -> "+${day.scoreEarned} P"
                        ChartMetricType.GAMES_PLAYED -> "${day.gamesCount} Oyun"
                    },
                    color = com.example.ui.theme.OnNeon,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun WeeklyKpiCard(
    title: String,
    value: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.testTag("kpi_${title.lowercase().replace(" ", "_")}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(icon, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun DayBreakdownItemCard(
    day: DayActivitySummary,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("day_breakdown_${day.dateString}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (day.playTimeSeconds > 0) MintGreen else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.dayName.take(2),
                        fontWeight = FontWeight.Bold,
                        color = if (day.playTimeSeconds > 0) MintGreenDark else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (day.isToday) "${day.formattedDate} (Bugün)" else "${day.formattedDate} (${day.dayName})",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Text(
                        text = if (day.gamesCount > 0) "${day.gamesCount} oyun oynandı • ⭐ ${day.starsEarned}" else "Dinlenme günü (Aktivite yok)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDuration(day.playTimeSeconds),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (day.playTimeSeconds > 0) PrimaryCoral else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "+${day.scoreEarned} P",
                    style = MaterialTheme.typography.labelSmall,
                    color = MintGreenDark,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Builds contiguous 7-day list ending with today, matching Room DB activities.
 */
private fun buildWeeklyData(activities: List<UserDailyActivityEntity>): List<DayActivitySummary> {
    val result = mutableListOf<DayActivitySummary>()
    val cal = Calendar.getInstance()
    val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfDayName = SimpleDateFormat("EEE", Locale("tr", "TR"))
    val sdfFormattedDate = SimpleDateFormat("d MMM", Locale("tr", "TR"))
    val todayStr = sdfKey.format(Date())

    // Map existing Room DB entries by date key for O(1) lookup
    val activityMap = activities.associateBy { it.date }

    for (i in 6 downTo 0) {
        val loopCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -i)
        }
        val dateKey = sdfKey.format(loopCal.time)
        val dayName = sdfDayName.format(loopCal.time).replace(".", "").replaceFirstChar { it.uppercase() }
        val formattedDate = sdfFormattedDate.format(loopCal.time)
        val isToday = dateKey == todayStr

        val dbEntry = activityMap[dateKey]
        result.add(
            DayActivitySummary(
                dateString = dateKey,
                dayName = dayName,
                formattedDate = formattedDate,
                isToday = isToday,
                playTimeSeconds = dbEntry?.totalPlayTimeSeconds ?: 0L,
                gamesCount = dbEntry?.gamesPlayedCount ?: 0,
                scoreEarned = dbEntry?.totalScoreEarned ?: 0,
                starsEarned = dbEntry?.starsEarned ?: 0
            )
        )
    }
    return result
}

private fun formatDuration(seconds: Long): String {
    val totalSec = max(seconds, 0L)
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val remSec = totalSec % 60
    return when {
        hours > 0 -> "${hours} sa ${minutes} dk"
        minutes > 0 -> "${minutes} dk ${remSec} sn"
        else -> "${remSec} sn"
    }
}
