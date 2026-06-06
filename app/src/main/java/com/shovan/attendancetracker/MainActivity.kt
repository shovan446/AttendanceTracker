package com.shovan.attendancetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Link
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: AttendanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            val themeMode = remember(isDarkMode) { isDarkMode }

            MaterialTheme(
                colorScheme = if (themeMode) darkColorScheme() else lightColorScheme()
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppScaffold(viewModel, themeMode, onThemeChange = { isDarkMode = it })
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Calendar : Screen("calendar", "Calendar", Icons.Default.DateRange)
    object Timetable : Screen("timetable", "Timetable", Icons.AutoMirrored.Filled.List)
    object Stats : Screen("stats", "Overall Stats", Icons.Default.CheckCircle)
    object Subjects : Screen("subjects", "Subjects", Icons.Default.Edit)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: AttendanceViewModel, isDarkMode: Boolean, onThemeChange: (Boolean) -> Unit) {
    val items = listOf(Screen.Calendar, Screen.Timetable, Screen.Stats, Screen.Subjects, Screen.Settings)
    val pagerState = rememberPagerState(pageCount = { items.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when(items[pagerState.currentPage]) {
                            Screen.Calendar -> "Attendance Calendar"
                            Screen.Timetable -> "Weekly Timetable"
                            Screen.Stats -> "Overall Performance"
                            Screen.Subjects -> "Manage Subjects"
                            Screen.Settings -> "Settings"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { 
                            Text(
                                text = if (screen == Screen.Stats) "Stats" else screen.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(paddingValues),
            beyondViewportPageCount = 1
        ) { page ->
            when (items[page]) {
                Screen.Calendar -> CalendarTabScreen(viewModel, isDarkMode)
                Screen.Timetable -> TimetableTabScreen(viewModel)
                Screen.Stats -> StatsTabScreen(viewModel, isDarkMode)
                Screen.Subjects -> SubjectsTabScreen(viewModel)
                Screen.Settings -> SettingsTabScreen(isDarkMode, onThemeChange)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarTabScreen(viewModel: AttendanceViewModel, isDarkMode: Boolean) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val scheduledSubjects by viewModel.scheduledSubjectsForSelectedDate.collectAsState()
    val attendanceRecords by viewModel.attendanceRecordsForSelectedDate.collectAsState()
    
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Pick Date")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Scheduled Classes:", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        if (scheduledSubjects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No classes scheduled for this day.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(scheduledSubjects) { subject ->
                    val record = attendanceRecords[subject.id]
                    AttendanceMarkingCard(subject, record, isDarkMode) { status ->
                        viewModel.logAttendance(subject.id, selectedDate, status)
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceMarkingCard(subject: Subject, record: AttendanceRecord?, isDarkMode: Boolean, onStatusChange: (String) -> Unit) {
    val status = record?.status ?: "PENDING"
    
    val targetColor = when(status) {
        "PRESENT" -> if (isDarkMode) Color(0xFF1B5E20) else Color(0xFFE8F5E9)
        "ABSENT" -> if (isDarkMode) Color(0xFFB71C1C) else Color(0xFFFFEBEE)
        "CANCELLED" -> if (isDarkMode) Color(0xFF455A64) else Color(0xFFECEFF1)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 200),
        label = "CardColorAnimation"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = animatedColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Status: $status", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AttendanceButton("Present", status == "PRESENT", Color(0xFF4CAF50), Modifier.weight(1f)) { onStatusChange("PRESENT") }
                AttendanceButton("Absent", status == "ABSENT", Color(0xFFF44336), Modifier.weight(1f)) { onStatusChange("ABSENT") }
                AttendanceButton("Cancel", status == "CANCELLED", Color(0xFF9E9E9E), Modifier.weight(1f)) { onStatusChange("CANCELLED") }
            }
        }
    }
}

@Composable
fun AttendanceButton(text: String, isSelected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else color.copy(alpha = 0.2f),
            contentColor = if (isSelected) Color.White else color
        ),
        modifier = modifier,
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(text, fontSize = 12.sp)
    }
}

@Composable
fun StatsTabScreen(viewModel: AttendanceViewModel, isDarkMode: Boolean) {
    val statsList by viewModel.subjectsWithStatsState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Current Attendance Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(statsList) { item ->
                val isSafe = item.attendancePercentage >= item.subject.minAttendancePercentage
                val targetColor = if (isSafe) {
                    if (isDarkMode) Color(0xFF1B5E20).copy(alpha = 0.3f) else Color(0xFFE8F5E9)
                } else {
                    if (isDarkMode) Color(0xFFB71C1C).copy(alpha = 0.3f) else Color(0xFFFFEBEE)
                }
                
                val animatedColor by animateColorAsState(
                    targetValue = targetColor,
                    animationSpec = tween(durationMillis = 200),
                    label = "StatsCardColor"
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = animatedColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%%", item.attendancePercentage),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSafe) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                        Text("Attended: ${item.presentClasses}/${item.totalClasses}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = item.statusMessage, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableTabScreen(viewModel: AttendanceViewModel) {
    val allSubjects by viewModel.allSubjects.collectAsState()
    val selectedDay by viewModel.selectedDayOfWeek.collectAsState()
    val scheduledForDay by viewModel.timetableSubjectsForSelectedDay.collectAsState()
    val daysLabels = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    
    var isEditMode by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Weekly Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = { isEditMode = !isEditMode }) {
                Text(if (isEditMode) "Done" else "Edit")
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(7) { idx ->
                val dayNum = idx + 1
                FilterChip(
                    selected = selectedDay == dayNum,
                    onClick = { viewModel.selectDay(dayNum) },
                    label = { Text(daysLabels[idx]) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        AnimatedContent(
            targetState = isEditMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "TimetableEditAnimation"
        ) { targetEditMode ->
            if (targetEditMode) {
                Column {
                    Text("Tap to add/remove from ${daysLabels[selectedDay - 1]}:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (allSubjects.isEmpty()) {
                        Text("Add subjects first!", color = Color.Gray)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(allSubjects) { subject ->
                                val isAllocated = scheduledForDay.any { it.id == subject.id }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isAllocated) {
                                                viewModel.removeSubjectFromDay(subject.id, selectedDay)
                                            } else {
                                                viewModel.assignSubjectToDay(subject.id, selectedDay)
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isAllocated) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isAllocated) "✓ ${subject.name}" else "+ Allocate ${subject.name}",
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isAllocated) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
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
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (scheduledForDay.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No classes scheduled.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(scheduledForDay) { subject ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.padding(16.dp)) {
                                        Text(subject.name, fontWeight = FontWeight.Bold)
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
fun SubjectsTabScreen(viewModel: AttendanceViewModel) {
    val allSubjects by viewModel.allSubjects.collectAsState()
    var subjectInput by remember { mutableStateOf("") }
    var minPercentageInput by remember { mutableStateOf("75") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Add New Subject", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = subjectInput,
            onValueChange = { subjectInput = it },
            label = { Text("Subject Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = minPercentageInput,
            onValueChange = { minPercentageInput = it },
            label = { Text("Min Attendance (%)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                if (subjectInput.isNotBlank()) {
                    val pct = minPercentageInput.toIntOrNull() ?: 75
                    viewModel.addSubject(subjectInput, pct)
                    subjectInput = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Subject")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Manage Existing Subjects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(allSubjects) { subject ->
                SubjectEditCard(subject, viewModel)
            }
        }
    }
}

@Composable
fun SubjectEditCard(subject: Subject, viewModel: AttendanceViewModel) {
    var initialPresent by remember { mutableStateOf(subject.initialPresent.toString()) }
    var initialTotal by remember { mutableStateOf(subject.initialTotal.toString()) }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(subject.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { viewModel.deleteSubject(subject) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = initialPresent,
                    onValueChange = { 
                        initialPresent = it
                        it.toIntOrNull()?.let { p -> viewModel.updateInitialAttendance(subject, p, initialTotal.toIntOrNull() ?: subject.initialTotal) }
                    },
                    label = { Text("Attended") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = initialTotal,
                    onValueChange = { 
                        initialTotal = it
                        it.toIntOrNull()?.let { t -> viewModel.updateInitialAttendance(subject, initialPresent.toIntOrNull() ?: subject.initialPresent, t) }
                    },
                    label = { Text("Total Held") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Text("Adjust these for past data not logged in calendar", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun SettingsTabScreen(isDarkMode: Boolean, onThemeChange: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark Mode", fontWeight = FontWeight.Medium)
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onThemeChange(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("About Developer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        val uriHandler = LocalUriHandler.current
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Connect with me:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { uriHandler.openUri("mailto:saishovan2@gmail.com") }) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_google_gmail),
                                contentDescription = "Email",
                                modifier = Modifier.size(32.dp),
                                tint = Color(0xFFEA4335) // Official Gmail Red
                            )
                        }
                        Text("Email", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { uriHandler.openUri("https://www.linkedin.com/in/sai-shovan-narayan-sarangi") }) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_linkedin),
                                contentDescription = "LinkedIn",
                                modifier = Modifier.size(32.dp),
                                tint = Color(0xFF0A66C2) // Official LinkedIn Blue
                            )
                        }
                        Text("LinkedIn", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { uriHandler.openUri("https://github.com/shovan446") }) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_github),
                                contentDescription = "GitHub",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onBackground // Keeps GitHub dynamic
                            )
                        }
                        Text("GitHub", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "By an ITERian, for the ITERians",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Sai Shovan [CSE-CYSC, ITER|SOA]",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        }
    }
}
