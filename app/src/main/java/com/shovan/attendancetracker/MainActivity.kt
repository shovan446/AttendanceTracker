package com.shovan.attendancetracker

import android.content.Context
import android.os.Bundle
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Check
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class AppAccentTheme(
    val key: String,
    val label: String,
    val swatchColor: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightContainer: Color
) {
    FROST(
        key = "frost",
        label = "Frost",
        swatchColor = Color(0xFFE2E8F0),
        darkPrimary = Color(0xFFF8FAFC),      // Crisp white accent in Dark Mode
        darkOnPrimary = Color(0xFF0F172A),
        darkContainer = Color(0xFF2B313E),
        lightPrimary = Color(0xFF1E293B),     // Deep obsidian ink in Light Mode
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFE2E8F0)
    ),
    SAGE(
        key = "sage",
        label = "Sage",
        swatchColor = Color(0xFF68B994),
        darkPrimary = Color(0xFF68B994),      // Calm eucalyptus mint
        darkOnPrimary = Color(0xFF0D261C),
        darkContainer = Color(0xFF1B332A),
        lightPrimary = Color(0xFF2E6F52),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFDDF0E7)
    ),
    NORDIC(
        key = "nordic",
        label = "Nordic",
        swatchColor = Color(0xFF8AB4F8),
        darkPrimary = Color(0xFF8AB4F8),      // Classic Apple-glass sky blue
        darkOnPrimary = Color(0xFF0F2544),
        darkContainer = Color(0xFF1E3354),
        lightPrimary = Color(0xFF2563EB),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFDBEAFE)
    ),
    AMBER(
        key = "amber",
        label = "Amber",
        swatchColor = Color(0xFFE2B46D),
        darkPrimary = Color(0xFFE2B46D),      // Warm low-blue-light champagne
        darkOnPrimary = Color(0xFF2B1D08),
        darkContainer = Color(0xFF382914),
        lightPrimary = Color(0xFF9A5B25),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFF6E7D4)
    ),
    LAVENDER(
        key = "lavender",
        label = "Lavender",
        swatchColor = Color(0xFFB4A7F5),
        darkPrimary = Color(0xFFB4A7F5),      // Muted periwinkle dusk
        darkOnPrimary = Color(0xFF1D163B),
        darkContainer = Color(0xFF2B2447),
        lightPrimary = Color(0xFF5B4BC4),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFE8E4FA)
    );

    companion object {
        fun fromKey(key: String?): AppAccentTheme =
            entries.find { it.key == key } ?: FROST
    }
}
class MainActivity : ComponentActivity() {
    private val viewModel: AttendanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("attendance_tracker_prefs", Context.MODE_PRIVATE)

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkMode by remember {
                mutableStateOf(
                    if (prefs.contains("dark_mode")) prefs.getBoolean("dark_mode", false)
                    else systemDark
                )
            }
            var accentTheme by remember {
                mutableStateOf(AppAccentTheme.fromKey(prefs.getString("accent_theme", AppAccentTheme.FROST.key)))
            }

            val bgColorInt = if (isDarkMode) 0xFF111318.toInt() else 0xFFF6F5F2.toInt()
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkMode) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (isDarkMode) {
                        SystemBarStyle.dark(bgColorInt)
                    } else {
                        SystemBarStyle.light(bgColorInt, bgColorInt)
                    }
                )
            }

            val colorScheme = if (isDarkMode) {
                darkColorScheme(
                    background = Color(0xFF111318),
                    surface = Color(0xFF111318),
                    surfaceVariant = Color(0xFF1C2029),
                    surfaceContainer = Color(0xFF181B23),
                    surfaceContainerLow = Color(0xFF14171E),
                    surfaceContainerHigh = Color(0xFF222733),
                    primary = accentTheme.darkPrimary,
                    onPrimary = accentTheme.darkOnPrimary,
                    primaryContainer = accentTheme.darkContainer,
                    onPrimaryContainer = accentTheme.darkPrimary
                )
            } else {
                lightColorScheme(
                    background = Color(0xFFF6F5F2),
                    surface = Color(0xFFF6F5F2),
                    surfaceVariant = Color(0xFFECEAE4),
                    surfaceContainer = Color(0xFFFFFFFF),
                    surfaceContainerLow = Color(0xFFFAF9F6),
                    surfaceContainerHigh = Color(0xFFE5E2DC),
                    primary = accentTheme.lightPrimary,
                    onPrimary = accentTheme.lightOnPrimary,
                    primaryContainer = accentTheme.lightContainer,
                    onPrimaryContainer = accentTheme.lightPrimary
                )
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScaffold(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode,
                        accentTheme = accentTheme,
                        onThemeChange = { enabled ->
                            isDarkMode = enabled
                            prefs.edit().putBoolean("dark_mode", enabled).apply()
                        },
                        onAccentChange = { selected ->
                            accentTheme = selected
                            prefs.edit().putString("accent_theme", selected.key).apply()
                        }
                    )
                }
            }
        }
    }
}
sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Calendar : Screen("calendar", "Calendar", Icons.Default.DateRange)
    object Timetable : Screen("timetable", "Timetable", Icons.AutoMirrored.Filled.List)
    object Stats : Screen("stats", "Stats", Icons.Default.CheckCircle)
    object Subjects : Screen("subjects", "Subjects", Icons.Default.Edit)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Suppress("MissingPermission")
fun triggerGlassHaptic(context: Context, durationMs: Long = 16L, amplitude: Int = 95) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255))
            )
        }
    } catch (_: Exception) {
        // Ignore if vibration is blocked by system settings
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: AttendanceViewModel,
    isDarkMode: Boolean,
    accentTheme: AppAccentTheme,
    onThemeChange: (Boolean) -> Unit,
    onAccentChange: (AppAccentTheme) -> Unit
) {
    val items = listOf(Screen.Calendar, Screen.Timetable, Screen.Stats, Screen.Subjects, Screen.Settings)
    val pagerState = rememberPagerState(pageCount = { items.size })
    val coroutineScope = rememberCoroutineScope()

    val topGlow = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkMode) 0.22f else 0.16f)
    val ambientBrush = if (isDarkMode) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1C2333),
                Color(0xFF111318),
                Color(0xFF111318)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE8E4DC),
                Color(0xFFF6F5F2),
                Color(0xFFF6F5F2)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ambientBrush)
            .background(
                Brush.verticalGradient(
                    colors = listOf(topGlow, Color.Transparent),
                    endY = 1350f
                )
            )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        Text(
                            text = when (items[pagerState.currentPage]) {
                                Screen.Calendar -> "Attendance Calendar"
                                Screen.Timetable -> "Weekly Timetable"
                                Screen.Stats -> "Overall Performance"
                                Screen.Subjects -> "Manage Subjects"
                                Screen.Settings -> "Settings"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding()),
                beyondViewportPageCount = 1
            ) { page ->
                when (items[page]) {
                    Screen.Calendar -> CalendarTabScreen(viewModel, isDarkMode)
                    Screen.Timetable -> TimetableTabScreen(viewModel, isDarkMode)
                    Screen.Stats -> StatsTabScreen(viewModel, isDarkMode)
                    Screen.Subjects -> SubjectsTabScreen(viewModel, isDarkMode)
                    Screen.Settings -> SettingsTabScreen(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode,
                        accentTheme = accentTheme,
                        onThemeChange = onThemeChange,
                        onAccentChange = onAccentChange
                    )
                }
            }
        }

        FloatingGlassMagnifierDock(
            items = items,
            selectedIndex = pagerState.currentPage,
            pageOffsetFraction = pagerState.currentPageOffsetFraction,
            isDarkMode = isDarkMode,
            onSelectTab = { index ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(
                        page = index,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}
@Composable
fun FloatingGlassMagnifierDock(
    items: List<Screen>,
    selectedIndex: Int,
    pageOffsetFraction: Float,
    isDarkMode: Boolean,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    var dockWidthPx by remember { mutableStateOf(0) }
    val tabCount = items.size
    val innerPadding = 5.dp
    val innerPaddingPx = with(density) { innerPadding.toPx() }
    val usableWidthPx = (dockWidthPx - innerPaddingPx * 2f).coerceAtLeast(0f)
    val tabWidthPx = if (usableWidthPx > 0f && tabCount > 0) usableWidthPx / tabCount else 0f

    val lensPosition = (selectedIndex + pageOffsetFraction)
        .coerceIn(0f, (tabCount - 1).coerceAtLeast(0).toFloat())

    val dockFill = if (isDarkMode) {
        Color(0xFF191D26).copy(alpha = 0.86f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.84f)
    }

    val dockRim = Brush.verticalGradient(
        colors = if (isDarkMode) {
            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f))
        } else {
            listOf(Color.White.copy(alpha = 0.98f), Color(0xFFD6D3CD).copy(alpha = 0.45f))
        }
    )

    val activeTint = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .shadow(
                elevation = 18.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = if (isDarkMode) 0.55f else 0.10f),
                spotColor = Color.Black.copy(alpha = if (isDarkMode) 0.55f else 0.14f)
            )
            .onSizeChanged { dockWidthPx = it.width },
        shape = CircleShape,
        color = dockFill,
        border = BorderStroke(1.dp, dockRim)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (tabWidthPx > 0f) {
                val offsetPx = lensPosition * tabWidthPx

                Surface(
                    modifier = Modifier
                        .offset { IntOffset(x = offsetPx.roundToInt(), y = 0) }
                        .width(with(density) { tabWidthPx.toDp() })
                        .fillMaxHeight(),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = if (isDarkMode) {
                                listOf(
                                    Color.White.copy(alpha = 0.36f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            } else {
                                listOf(
                                    Color.White,
                                    activeTint.copy(alpha = 0.22f)
                                )
                            }
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = if (isDarkMode) {
                                        listOf(
                                            Color.White.copy(alpha = 0.20f),
                                            activeTint.copy(alpha = 0.24f),
                                            Color.White.copy(alpha = 0.06f)
                                        )
                                    } else {
                                        listOf(
                                            Color.White.copy(alpha = 0.95f),
                                            activeTint.copy(alpha = 0.14f),
                                            Color.White.copy(alpha = 0.65f)
                                        )
                                    }
                                )
                            )
                    )
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                items.forEachIndexed { index, screen ->
                    val distance = abs(lensPosition - index)
                    val proximity = (1f - distance).coerceIn(0f, 1f)
                    val smoothMagnify = proximity * proximity * (3f - 2f * proximity)
                    val iconScale = 1.0f + (0.26f * smoothMagnify)
                    val contentAlpha = 0.48f + (0.52f * smoothMagnify)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (index != selectedIndex) {
                                    triggerGlassHaptic(context)
                                }
                                onSelectTab(index)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                },
                            tint = if (smoothMagnify > 0.5f) {
                                activeTint
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                            }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = screen.title,
                            fontSize = 10.sp,
                            fontWeight = if (smoothMagnify > 0.5f) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            color = if (smoothMagnify > 0.5f) {
                                activeTint
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                            }
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun GlassCard(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    tintColor: Color? = null,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val baseFill = tintColor ?: if (isDarkMode) {
        Color(0xFF1C202B).copy(alpha = 0.72f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.86f)
    }

    val rimBrush = Brush.verticalGradient(
        colors = if (isDarkMode) {
            listOf(
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.04f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFFD6D3CD).copy(alpha = 0.45f)
            )
        }
    )

    Surface(
        modifier = modifier,
        shape = shape,
        color = baseFill,
        border = BorderStroke(1.dp, rimBrush),
        shadowElevation = 0.dp
    ) {
        Column(content = content)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarTabScreen(viewModel: AttendanceViewModel, isDarkMode: Boolean) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val scheduledSlots by viewModel.scheduledSlotsForSelectedDate.collectAsState()
    val attendanceRecords by viewModel.attendanceRecordsForSelectedDate.collectAsState()
    val subjectsWithStats by viewModel.subjectsWithStatsState.collectAsState()
    val statsBySubjectId = remember(subjectsWithStats) {
        subjectsWithStats.associateBy { it.subject.id }
    }
    val context = LocalContext.current
    val today = LocalDate.now()

    val weekDates = remember(selectedDate) {
        val monday = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
        (0..6).map { monday.plusDays(it.toLong()) }
    }

    val allMarkedPresent = remember(scheduledSlots, attendanceRecords) {
        scheduledSlots.isNotEmpty() && scheduledSlots.all { attendanceRecords[it.slot.slotId]?.status == "PRESENT" }
    }
    val allMarkedAbsent = remember(scheduledSlots, attendanceRecords) {
        scheduledSlots.isNotEmpty() && scheduledSlots.all { attendanceRecords[it.slot.slotId]?.status == "ABSENT" }
    }
    val allMarkedCancelled = remember(scheduledSlots, attendanceRecords) {
        scheduledSlots.isNotEmpty() && scheduledSlots.all { attendanceRecords[it.slot.slotId]?.status == "CANCELLED" }
    }
    val hasAnyMarked = remember(scheduledSlots, attendanceRecords) {
        scheduledSlots.any { attendanceRecords[it.slot.slotId] != null }
    }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.selectDate(Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Month/Date Header + Full Calendar Picker Button
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedDate != today) {
                    TextButton(
                        onClick = {
                            triggerGlassHaptic(context)
                            viewModel.selectDate(today)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Today", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Pick Date")
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 7-Day Horizontal Glass Date Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            weekDates.forEach { date ->
                val isSelected = date == selectedDate
                val isToday = date == today

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isSelected) {
                                triggerGlassHaptic(context)
                                viewModel.selectDate(date)
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkMode) 0.24f else 0.14f)
                    } else {
                        if (isDarkMode) Color(0xFF1C202B).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.86f)
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
                        } else {
                            Color.White.copy(alpha = if (isDarkMode) 0.12f else 0.85f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = date.dayOfMonth.toString(),
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("Scheduled Classes:", style = MaterialTheme.typography.titleSmall)

        if (scheduledSlots.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            val presentTint = if (isDarkMode) Color(0xFF68B994) else Color(0xFF2E6F52)
            val absentTint = if (isDarkMode) Color(0xFFE2857E) else Color(0xFFB34740)
            val cancelTint = if (isDarkMode) Color(0xFF8C96A6) else Color(0xFF64748B)
            val clearTint = MaterialTheme.colorScheme.onSurfaceVariant

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BatchActionPill(
                    label = if (allMarkedPresent) "✓ Present" else "All Present",
                    tint = presentTint,
                    isActive = allMarkedPresent,
                    enabled = !allMarkedPresent,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        triggerGlassHaptic(context)
                        viewModel.markAllForSelectedDate("PRESENT")
                    }
                )
                BatchActionPill(
                    label = if (allMarkedAbsent) "✓ Absent" else "All Absent",
                    tint = absentTint,
                    isActive = allMarkedAbsent,
                    enabled = !allMarkedAbsent,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        triggerGlassHaptic(context)
                        viewModel.markAllForSelectedDate("ABSENT")
                    }
                )
                BatchActionPill(
                    label = if (allMarkedCancelled) "✓ Cancel" else "All Cancel",
                    tint = cancelTint,
                    isActive = allMarkedCancelled,
                    enabled = !allMarkedCancelled,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        triggerGlassHaptic(context)
                        viewModel.markAllForSelectedDate("CANCELLED")
                    }
                )
                BatchActionPill(
                    label = "Clear All",
                    tint = clearTint,
                    isActive = false,
                    enabled = hasAnyMarked,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        triggerGlassHaptic(context)
                        viewModel.clearAllForSelectedDate()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (scheduledSlots.isEmpty()) {
            GlassCard(isDarkMode = isDarkMode, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No classes scheduled for this day.",
                    modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(scheduledSlots, key = { it.slot.slotId }) { item ->
                    val subject = item.subject
                    val slot = item.slot
                    val record = attendanceRecords[slot.slotId]
                    val stats = statsBySubjectId[subject.id]
                    val sameSubjectSlots = scheduledSlots.filter { it.subject.id == subject.id }
                    val slotLabel = when {
                        slot.timeString.isNotBlank() && slot.timeString != "Scheduled" -> slot.timeString
                        sameSubjectSlots.size > 1 -> "Class ${sameSubjectSlots.indexOf(item) + 1}"
                        else -> ""
                    }

                    AttendanceMarkingCard(
                        subject = subject,
                        slotLabel = slotLabel,
                        record = record,
                        stats = stats,
                        isDarkMode = isDarkMode,
                        onStatusChange = { status ->
                            if (record?.status == status) {
                                viewModel.removeAttendance(subject.id, slot.slotId, selectedDate)
                            } else {
                                viewModel.logAttendance(subject.id, slot.slotId, selectedDate, status)
                            }
                        },
                        onClearStatus = {
                            viewModel.removeAttendance(subject.id, slot.slotId, selectedDate)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BatchActionPill(
    label: String,
    tint: Color,
    isActive: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val effectiveAlpha = when {
        isActive -> 0.24f
        enabled -> 0.14f
        else -> 0.06f
    }
    val textAlpha = if (enabled || isActive) 1f else 0.42f

    Surface(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = CircleShape,
        color = tint.copy(alpha = effectiveAlpha),
        border = BorderStroke(
            width = 1.dp,
            color = tint.copy(alpha = if (enabled || isActive) 0.45f else 0.18f)
        )
    ) {
        Box(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                color = tint.copy(alpha = textAlpha)
            )
        }
    }
}
@Composable
fun AttendanceMarkingCard(
    subject: Subject,
    slotLabel: String = "",
    record: AttendanceRecord?,
    stats: SubjectWithStats?,
    isDarkMode: Boolean,
    onStatusChange: (String) -> Unit,
    onClearStatus: () -> Unit
) {
    val status = record?.status ?: "PENDING"
    val targetColor = when (status) {
        "PRESENT" -> if (isDarkMode) Color(0xFF1B332A).copy(alpha = 0.78f) else Color(0xFFE6F2EC).copy(alpha = 0.88f)
        "ABSENT" -> if (isDarkMode) Color(0xFF362024).copy(alpha = 0.78f) else Color(0xFFF9EAE8).copy(alpha = 0.88f)
        "CANCELLED" -> if (isDarkMode) Color(0xFF232833).copy(alpha = 0.78f) else Color(0xFFECEAE5).copy(alpha = 0.88f)
        else -> if (isDarkMode) Color(0xFF1C202B).copy(alpha = 0.72f) else Color(0xFFFFFFFF).copy(alpha = 0.86f)
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 200),
        label = "CardColorAnimation"
    )

    val presentAccent = if (isDarkMode) Color(0xFF4CA37C) else Color(0xFF3D8B68)
    val absentAccent = if (isDarkMode) Color(0xFFD66A63) else Color(0xFFC85A53)
    val cancelAccent = if (isDarkMode) Color(0xFF8C96A6) else Color(0xFF788292)

    GlassCard(
        isDarkMode = isDarkMode,
        tintColor = animatedColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (slotLabel.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkMode) 0.22f else 0.12f)
                        ) {
                            Text(
                                text = slotLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (stats != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    val isSafe = stats.attendancePercentage >= subject.minAttendancePercentage
                    Text(
                        text = String.format(
                            Locale.getDefault(),
                            "%.1f%% (%d/%d)",
                            stats.attendancePercentage,
                            stats.presentClasses,
                            stats.totalClasses
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSafe) {
                            if (isDarkMode) Color(0xFF68B994) else Color(0xFF2E6F52)
                        } else {
                            if (isDarkMode) Color(0xFFE2857E) else Color(0xFFB34740)
                        }
                    )
                }
            }

            if (stats != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stats.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status: $status",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
                if (status != "PENDING") {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onClearStatus() }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Attendance",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AttendanceButton("Present", status == "PRESENT", presentAccent, Modifier.weight(1f)) { onStatusChange("PRESENT") }
                AttendanceButton("Absent", status == "ABSENT", absentAccent, Modifier.weight(1f)) { onStatusChange("ABSENT") }
                AttendanceButton("Cancel", status == "CANCELLED", cancelAccent, Modifier.weight(1f)) { onStatusChange("CANCELLED") }
            }
        }
    }
}

@Composable
fun AttendanceButton(
    text: String,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Button(
        onClick = {
            triggerGlassHaptic(context)
            onClick()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else color.copy(alpha = 0.14f),
            contentColor = if (isSelected) Color.White else color
        ),
        modifier = modifier,
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
@Composable
fun StatsTabScreen(viewModel: AttendanceViewModel, isDarkMode: Boolean) {
    val statsList by viewModel.subjectsWithStatsState.collectAsState()

    val totalPresent = remember(statsList) { statsList.sumOf { it.presentClasses } }
    val totalHeld = remember(statsList) { statsList.sumOf { it.totalClasses } }
    val overallPercentage = if (totalHeld > 0) (totalPresent * 100f) / totalHeld else 0f
    val safeSubjectsCount = remember(statsList) {
        statsList.count { it.attendancePercentage >= it.subject.minAttendancePercentage }
    }
    val atRiskCount = statsList.size - safeSubjectsCount
    val isOverallSafe = overallPercentage >= 75f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Current Attendance Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (statsList.isEmpty()) {
            GlassCard(isDarkMode = isDarkMode, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No subjects added yet.",
                    modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hero Aggregate Summary Card
                item(key = "overall_aggregate_card") {
                    val accentColor = if (isOverallSafe) {
                        if (isDarkMode) Color(0xFF68B994) else Color(0xFF2E6F52)
                    } else {
                        if (isDarkMode) Color(0xFFE2857E) else Color(0xFFB34740)
                    }

                    GlassCard(
                        isDarkMode = isDarkMode,
                        tintColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkMode) 0.16f else 0.10f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Combined Attendance",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.1f%%", overallPercentage),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accentColor
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$totalPresent / $totalHeld classes",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (atRiskCount == 0) {
                                            "All ${statsList.size} subjects safe"
                                        } else {
                                            "$safeSubjectsCount safe • $atRiskCount below target"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (atRiskCount == 0) {
                                            if (isDarkMode) Color(0xFF68B994) else Color(0xFF2E6F52)
                                        } else {
                                            if (isDarkMode) Color(0xFFE2857E) else Color(0xFFB34740)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Individual Subject Cards
                items(statsList, key = { it.subject.id }) { item ->
                    val isSafe = item.attendancePercentage >= item.subject.minAttendancePercentage
                    val targetColor = if (isSafe) {
                        if (isDarkMode) Color(0xFF1B332A).copy(alpha = 0.78f) else Color(0xFFE6F2EC).copy(alpha = 0.88f)
                    } else {
                        if (isDarkMode) Color(0xFF362024).copy(alpha = 0.78f) else Color(0xFFF9EAE8).copy(alpha = 0.88f)
                    }

                    val animatedColor by animateColorAsState(
                        targetValue = targetColor,
                        animationSpec = tween(durationMillis = 200),
                        label = "StatsCardColor"
                    )

                    GlassCard(
                        isDarkMode = isDarkMode,
                        tintColor = animatedColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%%", item.attendancePercentage),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSafe) {
                                        if (isDarkMode) Color(0xFF68B994) else Color(0xFF2E6F52)
                                    } else {
                                        if (isDarkMode) Color(0xFFE2857E) else Color(0xFFB34740)
                                    }
                                )
                            }
                            Text(
                                "Attended: ${item.presentClasses}/${item.totalClasses}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = item.statusMessage, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableTabScreen(viewModel: AttendanceViewModel, isDarkMode: Boolean) {
    val allSubjects by viewModel.allSubjects.collectAsState()
    val context = LocalContext.current
    val selectedDay by viewModel.selectedDayOfWeek.collectAsState()
    val scheduledForDay by viewModel.timetableSlotsForSelectedDay.collectAsState()
    val daysLabels = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    var isEditMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Weekly Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = { isEditMode = !isEditMode }) {
                Text(if (isEditMode) "Done" else "Edit", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(7) { idx ->
                val dayNum = idx + 1
                val isSelected = selectedDay == dayNum
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            if (selectedDay != dayNum) {
                                triggerGlassHaptic(context)
                            }
                            viewModel.selectDay(dayNum)
                        },
                    shape = CircleShape,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkMode) 0.24f else 0.14f)
                    } else {
                        if (isDarkMode) Color(0xFF1C202B).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.86f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                        else Color.White.copy(alpha = if (isDarkMode) 0.12f else 0.85f)
                    )
                ) {
                    Text(
                        text = daysLabels[idx],
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        AnimatedContent(
            targetState = isEditMode,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
            label = "TimetableEditAnimation"
        ) { targetEditMode ->
            if (targetEditMode) {
                Column {
                    Text(
                        "Assign classes on ${daysLabels[selectedDay - 1]} (tap + to add multiple slots):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (allSubjects.isEmpty()) {
                        GlassCard(isDarkMode = isDarkMode, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Add subjects in the Subjects tab first.",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 112.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(allSubjects, key = { it.id }) { subject ->
                                val subjectSlots = scheduledForDay.filter { it.subject.id == subject.id }
                                val assignedCount = subjectSlots.size
                                GlassCard(
                                    isDarkMode = isDarkMode,
                                    tintColor = if (assignedCount > 0) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkMode) 0.22f else 0.12f)
                                    } else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (assignedCount > 0) "✓ ${subject.name}" else subject.name,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (assignedCount > 0) {
                                                Text(
                                                    text = if (assignedCount == 1) "1 class scheduled" else "$assignedCount classes scheduled",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        if (assignedCount > 0) {
                                            IconButton(
                                                onClick = {
                                                    triggerGlassHaptic(context)
                                                    subjectSlots.lastOrNull()?.let { lastSlot ->
                                                        viewModel.removeTimetableSlot(lastSlot.slot.slotId)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Remove one slot",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        FilledTonalButton(
                                            onClick = {
                                                triggerGlassHaptic(context)
                                                val label = if (assignedCount == 0) "Scheduled" else "Class ${assignedCount + 1}"
                                                viewModel.assignSubjectToDay(subject.id, selectedDay, label)
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Add class slot",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (assignedCount == 0) "Add" else "+1",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Column {
                    Text("Classes on ${daysLabels[selectedDay - 1]}:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    if (scheduledForDay.isEmpty()) {
                        GlassCard(isDarkMode = isDarkMode, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No classes scheduled for ${daysLabels[selectedDay - 1]}.",
                                modifier = Modifier.padding(20.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 112.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(scheduledForDay, key = { it.slot.slotId }) { item ->
                                val subject = item.subject
                                val slot = item.slot
                                val sameSubjectSlots = scheduledForDay.filter { it.subject.id == subject.id }
                                val slotLabel = when {
                                    slot.timeString.isNotBlank() && slot.timeString != "Scheduled" -> slot.timeString
                                    sameSubjectSlots.size > 1 -> "Class ${sameSubjectSlots.indexOf(item) + 1}"
                                    else -> ""
                                }

                                GlassCard(isDarkMode = isDarkMode, modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(18.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = subject.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (slotLabel.isNotEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkMode) 0.22f else 0.12f)
                                            ) {
                                                Text(
                                                    text = slotLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectsTabScreen(viewModel: AttendanceViewModel, isDarkMode: Boolean) {
    val allSubjects by viewModel.allSubjects.collectAsState()
    var subjectInput by remember { mutableStateOf("") }
    var minPercentageInput by remember { mutableStateOf("75") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "add_subject_section") {
            GlassCard(isDarkMode = isDarkMode, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add New Subject", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = subjectInput,
                        onValueChange = { subjectInput = it },
                        label = { Text("Subject Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = minPercentageInput,
                        onValueChange = { minPercentageInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Min Attendance (%)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val trimmed = subjectInput.trim()
                            if (trimmed.isNotEmpty()) {
                                val pct = (minPercentageInput.toIntOrNull() ?: 75).coerceIn(1, 100)
                                viewModel.addSubject(trimmed, pct)
                                subjectInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Subject")
                    }
                }
            }
        }

        if (allSubjects.isNotEmpty()) {
            item(key = "existing_subjects_header") {
                Text(
                    "Manage Existing Subjects",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        items(allSubjects, key = { it.id }) { subject ->
            SubjectEditCard(subject = subject, viewModel = viewModel, isDarkMode = isDarkMode)
        }
    }
}

@Composable
fun SubjectEditCard(subject: Subject, viewModel: AttendanceViewModel, isDarkMode: Boolean) {
    var initialPresent by remember(subject.id, subject.initialPresent) {
        mutableStateOf(subject.initialPresent.toString())
    }
    var initialTotal by remember(subject.id, subject.initialTotal) {
        mutableStateOf(subject.initialTotal.toString())
    }

    val parsedPresent = initialPresent.toIntOrNull()
    val parsedTotal = initialTotal.toIntOrNull()
    val isValid = parsedPresent != null && parsedTotal != null && parsedPresent >= 0 && parsedTotal >= parsedPresent
    val hasChanges = isValid && (parsedPresent != subject.initialPresent || parsedTotal != subject.initialTotal)

    GlassCard(isDarkMode = isDarkMode, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(subject.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Target: ${subject.minAttendancePercentage}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.deleteSubject(subject) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = initialPresent,
                    onValueChange = { initialPresent = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Attended") },
                    singleLine = true,
                    isError = parsedPresent != null && parsedTotal != null && parsedPresent > parsedTotal,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = initialTotal,
                    onValueChange = { initialTotal = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Total Held") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            if (parsedPresent != null && parsedTotal != null && parsedPresent > parsedTotal) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Attended classes cannot exceed Total Held",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Past data not logged in calendar",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (hasChanges) {
                    TextButton(
                        onClick = {
                            if (parsedPresent != null && parsedTotal != null) {
                                viewModel.updateInitialAttendance(subject, parsedPresent, parsedTotal)
                            }
                        }
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
@Composable
fun SettingsTabScreen(
    viewModel: AttendanceViewModel,
    isDarkMode: Boolean,
    accentTheme: AppAccentTheme,
    onThemeChange: (Boolean) -> Unit,
    onAccentChange: (AppAccentTheme) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("attendance_tracker_prefs", Context.MODE_PRIVATE) }
    var reminderEnabled by remember { mutableStateOf(prefs.getBoolean("reminder_enabled", false)) }
    var reminderHour by remember { mutableStateOf(prefs.getInt("reminder_hour", 17)) }
    var reminderMinute by remember { mutableStateOf(prefs.getInt("reminder_minute", 30)) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            reminderEnabled = true
            prefs.edit().putBoolean("reminder_enabled", true).apply()
            AttendanceReminderWorker.schedule(context, reminderHour, reminderMinute)
            Toast.makeText(context, "Daily reminder enabled", Toast.LENGTH_SHORT).show()
        } else {
            reminderEnabled = false
            prefs.edit().putBoolean("reminder_enabled", false).apply()
            Toast.makeText(context, "Notification permission required for reminders", Toast.LENGTH_LONG).show()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = withContext(Dispatchers.IO) { viewModel.exportBackupJson() }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }
                    Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    } ?: throw IllegalStateException("Could not read file")

                    val result = withContext(Dispatchers.IO) { viewModel.importBackupJson(json) }
                    result.onSuccess { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }.onFailure { err ->
                        Toast.makeText(context, "Invalid backup file: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))

        GlassCard(isDarkMode = isDarkMode, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark Mode", fontWeight = FontWeight.Medium)
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onThemeChange(it) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Glass Tint", fontWeight = FontWeight.Medium)
                    Text(
                        text = accentTheme.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AppAccentTheme.entries.forEach { preset ->
                        val isSelected = preset == accentTheme
                        val displayColor = if (isDarkMode) preset.darkPrimary else preset.lightPrimary

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isSelected) {
                                    triggerGlassHaptic(context)
                                }
                                onAccentChange(preset)
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(displayColor.copy(alpha = if (isSelected) 1f else 0.78f))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) {
                                            if (isDarkMode) Color.White else Color(0xFF0F172A)
                                        } else {
                                            Color.White.copy(alpha = 0.35f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = preset.label,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isDarkMode) preset.darkOnPrimary else preset.lightOnPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = preset.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Data & Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(isDarkMode = isDarkMode, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Save or restore your subjects, weekly timetable, and attendance history as a JSON file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            triggerGlassHaptic(context)
                            val fileName = "attendance_backup_${LocalDate.now()}.json"
                            exportLauncher.launch(fileName)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export JSON", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = {
                            triggerGlassHaptic(context)
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Restore JSON", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Daily Reminder", fontWeight = FontWeight.Medium)
                Text(
                    text = if (reminderEnabled) {
                        val formattedTime = LocalTime.of(reminderHour, reminderMinute)
                            .format(DateTimeFormatter.ofPattern("hh:mm a"))
                        "Notify at $formattedTime if classes are pending"
                    } else {
                        "Remind to log attendance after classes"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = reminderEnabled,
                onCheckedChange = { enabled ->
                    triggerGlassHaptic(context)
                    if (enabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            reminderEnabled = true
                            prefs.edit().putBoolean("reminder_enabled", true).apply()
                            AttendanceReminderWorker.schedule(context, reminderHour, reminderMinute)
                        }
                    } else {
                        reminderEnabled = false
                        prefs.edit().putBoolean("reminder_enabled", false).apply()
                        AttendanceReminderWorker.cancel(context)
                    }
                }
            )
        }

        if (reminderEnabled) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkMode) 0.16f else 0.10f))
                    .clickable {
                        triggerGlassHaptic(context)
                        TimePickerDialog(
                            context,
                            { _, selectedHour, selectedMinute ->
                                reminderHour = selectedHour
                                reminderMinute = selectedMinute
                                prefs.edit()
                                    .putInt("reminder_hour", selectedHour)
                                    .putInt("reminder_minute", selectedMinute)
                                    .apply()
                                AttendanceReminderWorker.schedule(context, selectedHour, selectedMinute)
                            },
                            reminderHour,
                            reminderMinute,
                            false
                        ).show()
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reminder Time",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = LocalTime.of(reminderHour, reminderMinute)
                        .format(DateTimeFormatter.ofPattern("hh:mm a")),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("About Developer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(isDarkMode = isDarkMode, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Connect with me:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { uriHandler.openUri("mailto:saishovan2@gmail.com") }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_google_gmail),
                                contentDescription = "Email",
                                modifier = Modifier.size(32.dp),
                                tint = Color(0xFFEA4335)
                            )
                        }
                        Text("Email", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { uriHandler.openUri("https://www.linkedin.com/in/sai-shovan-narayan-sarangi") }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_linkedin),
                                contentDescription = "LinkedIn",
                                modifier = Modifier.size(32.dp),
                                tint = Color(0xFF0A66C2)
                            )
                        }
                        Text("LinkedIn", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { uriHandler.openUri("https://github.com/shovan446") }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_github),
                                contentDescription = "GitHub",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text("GitHub", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Version 2.1.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.42f)
            )

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Sai Shovan [CSE-CYSC, ITER|SOA]",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.48f)
            )
        }

        Spacer(modifier = Modifier.navigationBarsPadding().height(116.dp))
    }
}