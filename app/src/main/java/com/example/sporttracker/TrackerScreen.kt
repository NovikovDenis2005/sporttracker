package com.example.sporttracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

private val LightGreenColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    primaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFF4CAF50),
    secondaryContainer = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFFF1F8E9),
    background = Color(0xFFF9FBF7)
)

private val DarkGreenColors = darkColorScheme(
    primary = Color(0xFF81C784),
    primaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF66BB6A),
    secondaryContainer = Color(0xFF2E7D32),
    surfaceVariant = Color(0xFF212121),
    background = Color(0xFF121212)
)

@Composable
fun SportTrackerTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkGreenColors else LightGreenColors
    MaterialTheme(colorScheme = colors, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(state: TrackerState, onEvent: (TrackerEvent) -> Unit) {
    SportTrackerTheme(darkTheme = state.isDarkTheme) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text("SportTracker Pro", fontWeight = FontWeight.Black) },
                        actions = {
                            IconButton(onClick = { onEvent(TrackerEvent.ToggleTheme) }) {
                                Icon(
                                    imageVector = if (state.isDarkTheme) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                    contentDescription = "Смена темы"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onEvent(TrackerEvent.SwitchModule(AppModule.RUNNING)) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.currentModule == AppModule.RUNNING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (state.currentModule == AppModule.RUNNING) Color.White else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.DirectionsRun, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("БЕГ И СКОРОСТЬ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onEvent(TrackerEvent.SwitchModule(AppModule.WORKOUTS)) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.currentModule == AppModule.WORKOUTS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (state.currentModule == AppModule.WORKOUTS) Color.White else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ТРЕНИРОВКИ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = state.currentScreen == AppScreen.MAIN_MENU || state.currentScreen == AppScreen.ACTIVE_PROCESS,
                        onClick = { onEvent(TrackerEvent.ChangeScreen(AppScreen.MAIN_MENU)) },
                        label = { Text(if (state.currentModule == AppModule.RUNNING) "Секундомер" else "Программы") },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = state.currentScreen == AppScreen.HISTORY,
                        onClick = { onEvent(TrackerEvent.ChangeScreen(AppScreen.HISTORY)) },
                        label = { Text("История") },
                        icon = { Icon(Icons.Default.List, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = state.currentScreen == AppScreen.ACHIEVEMENTS,
                        onClick = { onEvent(TrackerEvent.ChangeScreen(AppScreen.ACHIEVEMENTS)) },
                        label = { Text("Ачивки") },
                        icon = { Icon(Icons.Default.Star, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = state.currentScreen == AppScreen.PROFILE,
                        onClick = { onEvent(TrackerEvent.ChangeScreen(AppScreen.PROFILE)) },
                        label = { Text("Профиль") },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                when (state.currentScreen) {
                    AppScreen.MAIN_MENU -> {
                        if (state.currentModule == AppModule.RUNNING) RunningStopwatchScreen(state, onEvent)
                        else WorkoutsMenuScreen(state, onEvent)
                    }
                    AppScreen.ACTIVE_PROCESS -> {
                        if (state.currentModule == AppModule.WORKOUTS) WorkoutPlayerScreen(state, onEvent)
                    }
                    AppScreen.HISTORY -> HistoryScreen(state, onEvent)
                    AppScreen.ACHIEVEMENTS -> IntegratedAchievementsScreen(state)
                    AppScreen.PROFILE -> StandardProfileScreen(state, onEvent)
                }
            }
        }
    }
}

@Composable
fun RunningStopwatchScreen(state: TrackerState, onEvent: (TrackerEvent) -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CalorieStickerWidget(state.todayCalories)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Зачетный тест скорости", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(100, 1000).forEach { distance ->
                Card(
                    modifier = Modifier.weight(1f).clickable { onEvent(TrackerEvent.SelectDistance(distance)) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.selectedRunningDistance == distance) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "$distance м",
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = if (state.selectedRunningDistance == distance) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.size(190.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val min = (state.stopwatchMillis / 60000) % 60
                val sec = (state.stopwatchMillis / 1000) % 60
                val milli = (state.stopwatchMillis / 10) % 100
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d.%02d", min, sec, milli),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("миллисекунды", fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { if (state.isStopwatchRunning) onEvent(TrackerEvent.PauseStopwatch) else onEvent(TrackerEvent.StartStopwatch) },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (state.isStopwatchRunning) "ПАУЗА" else "СТАРТ ТЕСТА")
            }

            Button(
                onClick = { onEvent(TrackerEvent.SaveStopwatchResult) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("ФИНИШ (СОХРАНИТЬ)")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = { onEvent(TrackerEvent.ResetStopwatch) }, modifier = Modifier.fillMaxWidth()) {
            Text("СБРОСИТЬ ТАЙМЕР")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Результаты на ${state.selectedRunningDistance}м за день:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))

                val history = state.runningResults.filter { it.distanceMeters == state.selectedRunningDistance }
                if (history.isEmpty()) {
                    Text("История чиста. Нажмите ФИНИШ, чтобы добавить заезд.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    history.reversed().forEach { run ->
                        val runMin = (run.timeMillis / 60000) % 60
                        val runSec = (run.timeMillis / 1000) % 60
                        val runMilli = (run.timeMillis / 10) % 100
                        val timeStr = String.format(Locale.getDefault(), "%02d:%02d.%02d", runMin, runSec, runMilli)

                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(run.dateString, fontSize = 13.sp, color = Color.Gray)
                                Text("$timeStr с | ${run.grade}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text("🔥 Расход за забег: ${run.calories} ккал", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                            HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun CalorieStickerWidget(calories: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⚡", fontSize = 36.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Общий расход сегодня: $calories ккал", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Text("Суммируются силовые нагрузки и беговые зачеты", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun WorkoutsMenuScreen(state: TrackerState, onEvent: (TrackerEvent) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CalorieStickerWidget(state.todayCalories); Spacer(modifier = Modifier.height(8.dp)) }
        items(state.programs) { program ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onEvent(TrackerEvent.SelectProgram(program)) }) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(program.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Зона: ${program.category} • Упражнений: ${program.exercises.size}", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun WorkoutPlayerScreen(state: TrackerState, onEvent: (TrackerEvent) -> Unit) {
    val program = state.selectedProgram ?: return
    val exercise = program.exercises.getOrNull(state.currentExerciseIndex) ?: return

    if (state.isShowingExerciseDetails) {
        AlertDialog(
            onDismissRequest = { onEvent(TrackerEvent.HideExerciseDetails) },
            title = { Text(exercise.name) },
            text = { Text(exercise.description) },
            confirmButton = { Button(onClick = { onEvent(TrackerEvent.HideExerciseDetails) }) { Text("Понятно") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Упражнение ${state.currentExerciseIndex + 1} из ${program.exercises.size}", color = Color.Gray)
            Text(exercise.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            IconButton(onClick = { onEvent(TrackerEvent.ShowExerciseDetails) }) { Icon(Icons.Default.Info, contentDescription = null) }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val min = state.timerSeconds / 60
                val sec = state.timerSeconds % 60
                Text(String.format(Locale.getDefault(), "%02d:%02d", min, sec), fontSize = 56.sp, fontWeight = FontWeight.Black)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { if (state.isTimerRunning) onEvent(TrackerEvent.PauseTimer) else onEvent(TrackerEvent.StartTimer) }, modifier = Modifier.weight(1f)) {
                Text(if (state.isTimerRunning) "ПАУЗА" else "СТАРТ")
            }
            OutlinedButton(onClick = { onEvent(TrackerEvent.ResetTimer) }) { Text("СБРОС") }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { onEvent(TrackerEvent.PrevExercise) }) { Text("Назад") }
            Button(onClick = { onEvent(TrackerEvent.NextExercise) }) { Text("Вперед") }
        }
    }
}

// ОБНОВЛЕННЫЙ ЭКРАН ИСТОРИИ (Улучшенный дизайн)
@Composable
fun HistoryScreen(state: TrackerState, onEvent: (TrackerEvent) -> Unit) {
    val filteredRecords = state.records.filter {
        when (state.historyFilter) {
            HistoryFilter.ALL -> true
            HistoryFilter.RUNNING -> it.exerciseName.contains("Зачет Бег")
            HistoryFilter.WORKOUTS -> !it.exerciseName.contains("Зачет Бег")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = state.historyFilter.ordinal) {
            HistoryFilter.values().forEach { filter ->
                Tab(
                    selected = state.historyFilter == filter,
                    onClick = { onEvent(TrackerEvent.SetHistoryFilter(filter)) },
                    text = {
                        Text(
                            text = when(filter) {
                                HistoryFilter.ALL -> "Все"
                                HistoryFilter.RUNNING -> "Бег"
                                HistoryFilter.WORKOUTS -> "Силовые"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { onEvent(TrackerEvent.ClearAllHistory) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Очистить журнал", fontWeight = FontWeight.Bold)
            }
        }

        if (filteredRecords.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("База данных пуста. Завершите тренировку!", color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Выводим записи от новых к старым
                items(filteredRecords.reversed()) { record ->
                    val isRunning = record.exerciseName.contains("Зачет Бег")
                    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru")).format(Date(record.timestamp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (isRunning) "🏃" else "🏋️", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(record.exerciseName, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(record.result, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(dateStr, color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ОБНОВЛЕННЫЙ ЭКРАН АЧИВОК (Сетка вместо ленты)
@Composable
fun IntegratedAchievementsScreen(state: TrackerState) {
    val achs = if (state.currentModule == AppModule.RUNNING) state.runningAchievements else state.workoutAchievements

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Спортивные Достижения", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("Выполняйте нормативы, чтобы разблокировать", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(achs) { ach ->
                Card(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (ach.isUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(if (ach.isUnlocked) ach.iconEmoji else "🔒", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = ach.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = if (ach.isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ach.description,
                            fontSize = 10.sp,
                            color = if (ach.isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StandardProfileScreen(state: TrackerState, onEvent: (TrackerEvent) -> Unit) {
    var nameText by remember { mutableStateOf(state.userName) }
    var weightText by remember { mutableStateOf(state.userWeightKg.toString()) }
    var heightText by remember { mutableStateOf(state.userHeightCm.toString()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Card(modifier = Modifier.size(80.dp), shape = CircleShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.userName.take(1), fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text("Параметры тела для расчета калорий", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = nameText, onValueChange = { nameText = it }, label = { Text("Имя") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = weightText, onValueChange = { weightText = it }, label = { Text("Вес (кг)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = heightText, onValueChange = { heightText = it }, label = { Text("Рост (см)") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val w = weightText.toFloatOrNull() ?: 75f
                val h = heightText.toFloatOrNull() ?: 178f
                onEvent(TrackerEvent.UpdateProfileParams(nameText, w, h))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("СОХРАНИТЬ ДАННЫЕ МЕТРИКИ")
        }
    }
}