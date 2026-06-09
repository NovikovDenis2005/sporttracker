package com.example.sporttracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TrackerViewModel(private val repository: RecordRepository) : ViewModel() {

    private val _state = MutableStateFlow(TrackerState(programs = getMassivePrograms()))
    val state: StateFlow<TrackerState> = _state.asStateFlow()

    private var workoutTimerJob: Job? = null
    private var stopwatchJob: Job? = null
    private var stopwatchStartTime = 0L

    init {
        loadDataFromRepository()
    }

    private fun startTimer() {
        stopTimer()
        _state.update { it.copy(isTimerRunning = true) }
        workoutTimerJob = viewModelScope.launch {
            while (_state.value.timerSeconds > 0) {
                delay(1000)
                onEvent(TrackerEvent.TickWorkout)
            }
        }
    }

    private fun stopTimer() {
        workoutTimerJob?.cancel()
        _state.update { it.copy(isTimerRunning = false) }
    }

    private fun stopStopwatch() {
        stopwatchJob?.cancel()
        _state.update { it.copy(isStopwatchRunning = false) }
    }

    private fun loadDataFromRepository() {
        viewModelScope.launch {
            repository.getRecords().collect { recordList ->
                val format = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
                val stats = recordList.groupBy { format.format(Date(it.timestamp)) }
                    .map { (date, recs) -> DailyStat(date, recs.size) }

                val todayStr = SimpleDateFormat("dd.MM.yyyy", Locale("ru")).format(Date())
                var calsAccumulator = 0
                recordList.forEach { rec ->
                    val recDate = SimpleDateFormat("dd.MM.yyyy", Locale("ru")).format(Date(rec.timestamp))
                    if (recDate == todayStr) {
                        Regex("(\\d+)\\s*ккал").find(rec.result)?.let { match ->
                            calsAccumulator += match.groupValues[1].toIntOrNull() ?: 0
                        }
                    }
                }

                val totalWorkouts = recordList.filter { !it.exerciseName.contains("Зачет Бег") }.size
                val totalRuns = recordList.filter { it.exerciseName.contains("Зачет Бег") }.size

                // РАСШИРЕННЫЕ АЧИВКИ
                val wAchievements = listOf(
                    Achievement("w1", "Первый шаг", "Выполнена 1 тренировка", "🥉", totalWorkouts >= 1),
                    Achievement("w2", "Стальной хват", "Выполнено 5 тренировок", "🥈", totalWorkouts >= 5),
                    Achievement("w3", "Разорвавший оковы", "10 тяжелых тренировок", "🥇", totalWorkouts >= 10),
                    Achievement("w4", "Машина", "Выполнено 20 тренировок", "🦾", totalWorkouts >= 20),
                    Achievement("w5", "Киборг", "Выполнено 50 тренировок", "🤖", totalWorkouts >= 50)
                )

                val rAchievements = listOf(
                    Achievement("r1", "Спринтер", "Первый беговой зачет сдан", "⚡", totalRuns >= 1),
                    Achievement("r2", "Покоритель ветра", "Сдано 5 зачетов бега", "🏃‍♂️", totalRuns >= 5),
                    Achievement("r3", "Вспышка", "10 скоростных зачетов", "🔥", totalRuns >= 10),
                    Achievement("r4", "Марафонец", "20 беговых зачетов", "💨", totalRuns >= 20),
                    Achievement("r5", "Флеш", "50 беговых зачетов", "⚡⚡", totalRuns >= 50)
                )

                _state.update { it.copy(
                    records = recordList,
                    dailyStats = stats,
                    workoutAchievements = wAchievements,
                    runningAchievements = rAchievements,
                    todayCalories = calsAccumulator
                ) }
            }
        }
    }

    fun onEvent(event: TrackerEvent) {
        when (event) {
            TrackerEvent.ToggleTheme -> _state.update { it.copy(isDarkTheme = !it.isDarkTheme) }
            is TrackerEvent.SetHistoryFilter -> _state.update { it.copy(historyFilter = event.filter) }

            TrackerEvent.ClearAllHistory -> {
                viewModelScope.launch {
                    repository.clearAllRecords()
                    _state.update { it.copy(runningResults = emptyList(), todayCalories = 0) }
                }
            }

            is TrackerEvent.SwitchModule -> {
                stopTimer()
                stopStopwatch()
                _state.update { it.copy(currentModule = event.module, currentScreen = AppScreen.MAIN_MENU) }
            }

            is TrackerEvent.ChangeScreen -> _state.update { it.copy(currentScreen = event.screen) }

            is TrackerEvent.UpdateProfileParams -> {
                _state.update { it.copy(userName = event.name, userWeightKg = event.weight, userHeightCm = event.height) }
                recalculateTodayCalories()
            }

            is TrackerEvent.SelectProgram -> {
                val firstDuration = event.program.exercises.getOrNull(0)?.durationSeconds ?: 0
                _state.update { it.copy(
                    selectedProgram = event.program, currentExerciseIndex = 0,
                    timerSeconds = firstDuration, initialDurationSeconds = firstDuration,
                    isTimerRunning = false, currentScreen = AppScreen.ACTIVE_PROCESS
                ) }
                stopTimer()
            }

            TrackerEvent.StartTimer -> startTimer()
            TrackerEvent.PauseTimer -> stopTimer()
            TrackerEvent.ResetTimer -> {
                val currentSec = _state.value.selectedProgram?.exercises?.getOrNull(_state.value.currentExerciseIndex)?.durationSeconds ?: 0
                _state.update { it.copy(timerSeconds = currentSec, isTimerRunning = false) }
                stopTimer()
            }

            TrackerEvent.NextExercise -> {
                val nextIndex = _state.value.currentExerciseIndex + 1
                val exercises = _state.value.selectedProgram?.exercises ?: emptyList()
                if (nextIndex < exercises.size) {
                    val nextSec = exercises[nextIndex].durationSeconds
                    _state.update { it.copy(currentExerciseIndex = nextIndex, timerSeconds = nextSec, initialDurationSeconds = nextSec, isTimerRunning = false) }
                    stopTimer()
                } else {
                    onEvent(TrackerEvent.FinishWorkout)
                }
            }

            TrackerEvent.PrevExercise -> {
                val prevIndex = _state.value.currentExerciseIndex - 1
                if (prevIndex >= 0) {
                    val exercises = _state.value.selectedProgram?.exercises ?: emptyList()
                    val prevSec = exercises[prevIndex].durationSeconds
                    _state.update { it.copy(currentExerciseIndex = prevIndex, timerSeconds = prevSec, initialDurationSeconds = prevSec, isTimerRunning = false) }
                    stopTimer()
                }
            }

            TrackerEvent.TickWorkout -> {
                if (_state.value.timerSeconds > 0) {
                    _state.update { it.copy(timerSeconds = _state.value.timerSeconds - 1) }
                } else {
                    stopTimer()
                    onEvent(TrackerEvent.NextExercise)
                }
            }

            TrackerEvent.FinishWorkout -> {
                val title = _state.value.selectedProgram?.title ?: "Силовая"
                val count = _state.value.selectedProgram?.exercises?.size ?: 0
                val growthFactor = _state.value.userHeightCm / 175f
                val calculatedCals = (count * 15 * (_state.value.userWeightKg / 70f) * growthFactor).toInt()

                viewModelScope.launch {
                    repository.addRecord(title, "Выполнено: $count упр. | Расход: $calculatedCals ккал")
                }
                _state.update { it.copy(selectedProgram = null, currentScreen = AppScreen.HISTORY) }
                stopTimer()
            }

            is TrackerEvent.SelectDistance -> _state.update { it.copy(selectedRunningDistance = event.distance, stopwatchMillis = 0L) }

            TrackerEvent.StartStopwatch -> {
                stopwatchJob?.cancel()
                _state.update { it.copy(isStopwatchRunning = true) }
                stopwatchStartTime = System.currentTimeMillis() - _state.value.stopwatchMillis
                stopwatchJob = viewModelScope.launch {
                    while (true) {
                        delay(15)
                        val elapsed = System.currentTimeMillis() - stopwatchStartTime
                        _state.update { it.copy(stopwatchMillis = elapsed) }
                    }
                }
            }

            TrackerEvent.PauseStopwatch -> stopStopwatch()
            TrackerEvent.ResetStopwatch -> {
                stopStopwatch()
                _state.update { it.copy(stopwatchMillis = 0L) }
            }

            TrackerEvent.SaveStopwatchResult -> {
                stopStopwatch()
                val distance = _state.value.selectedRunningDistance
                val rawMillis = _state.value.stopwatchMillis
                val totalSeconds = (rawMillis / 1000f)

                val grade = calculateRunningGrade(distance, totalSeconds)
                val cals = calculateRunningCalories(distance, _state.value.userWeightKg, _state.value.userHeightCm)
                val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale("ru")).format(Date())

                val formattedTime = String.format(Locale.getDefault(), "%02d:%02d.%02d", (rawMillis / 60000) % 60, (rawMillis / 1000) % 60, (rawMillis / 10) % 100)
                val newRun = RunningTest(distance, rawMillis, grade, cals, dateStr)

                _state.update { it.copy(runningResults = _state.value.runningResults + newRun) }

                viewModelScope.launch {
                    repository.addRecord("Зачет Бег ${distance}м", "Время: $formattedTime сек | $grade | Расход: $cals ккал")
                }
            }

            TrackerEvent.BackToMenu -> { stopTimer(); stopStopwatch(); _state.update { it.copy(selectedProgram = null, currentScreen = AppScreen.MAIN_MENU) } }
            TrackerEvent.ShowExerciseDetails -> _state.update { it.copy(isShowingExerciseDetails = true) }
            TrackerEvent.HideExerciseDetails -> _state.update { it.copy(isShowingExerciseDetails = false) }
        }
    }

    private fun recalculateTodayCalories() {
        val todayStr = SimpleDateFormat("dd.MM.yyyy", Locale("ru")).format(Date())
        var calsAccumulator = 0
        _state.value.records.forEach { rec ->
            val recDate = SimpleDateFormat("dd.MM.yyyy", Locale("ru")).format(Date(rec.timestamp))
            if (recDate == todayStr) {
                Regex("(\\d+)\\s*ккал").find(rec.result)?.let { match ->
                    calsAccumulator += match.groupValues[1].toIntOrNull() ?: 0
                }
            }
        }
        _state.update { it.copy(todayCalories = calsAccumulator) }
    }

    private fun calculateRunningCalories(distanceMeters: Int, weightKg: Float, heightCm: Float): Int {
        val distanceKm = distanceMeters / 1000f
        val heightModifier = heightCm / 180f
        val factor = if (distanceMeters == 100) 1.5f else 1.1f
        return (weightKg * distanceKm * factor * heightModifier).toInt().coerceAtLeast(15)
    }

    private fun calculateRunningGrade(distance: Int, totalSeconds: Float): String {
        if (totalSeconds == 0f) return "Нет данных"
        return when (distance) {
            100 -> when {
                totalSeconds <= 13.2f -> "Золото 🥇"
                totalSeconds <= 14.5f -> "Серебро 🥈"
                totalSeconds <= 16.5f -> "Бронза 🥉"
                else -> "Норматив сдан 🏃‍♂️"
            }
            1000 -> when {
                totalSeconds <= 210f -> "Золото 🥇"
                totalSeconds <= 245f -> "Серебро 🥈"
                totalSeconds <= 285f -> "Бронза 🥉"
                else -> "Норматив сдан 🏃‍♂️"
            }
            3000 -> when {
                totalSeconds <= 735f -> "Золото 🥇"
                totalSeconds <= 815f -> "Серебро 🥈"
                totalSeconds <= 915f -> "Бронза 🥉"
                else -> "Норматив сдан 🏃‍♂️"
            }
            else -> "Тест завершен ✅"
        }
    }

    // ДОБАВЛЕНЫ НОВЫЕ ПРОГРАММЫ И УПРАЖНЕНИЯ
    private fun getMassivePrograms() = listOf(
        WorkoutProgram("Абсолютная сила: Ноги", "Ноги", listOf(
            Exercise("Разминка суставов", "Вращения коленями, легкие выпады без веса.", 60, 1),
            Exercise("Приседания со штангой", "Держите спину ровно, приседайте глубоко.", 120, 5),
            Exercise("Жим ногами в тренажере", "Стопы на ширине плеч, не выпрямляйте колени до конца.", 90, 4),
            Exercise("Выпады с гантелями", "Поочередные шаги вперед, колено почти касается пола.", 90, 3),
            Exercise("Подъем на носки стоя", "Тренировка икроножных мышц.", 60, 2)
        )),
        WorkoutProgram("Титановая Грудь", "Грудь", listOf(
            Exercise("Отжимания от пола", "Разминочный подход.", 60, 2),
            Exercise("Жим лежа", "Классическое базовое упражнение. Лопатки сведены.", 120, 4),
            Exercise("Жим гантелей под углом", "Акцент на верхнюю часть грудных мышц.", 90, 4),
            Exercise("Сведение рук в кроссовере", "Пампинг грудных в конце тренировки.", 90, 3)
        )),
        WorkoutProgram("V-образная Спина", "Спина", listOf(
            Exercise("Подтягивания", "Тяга к груди широким хватом.", 90, 4),
            Exercise("Тяга штанги в наклоне", "Прямая спина, тяга к поясу.", 120, 4),
            Exercise("Тяга верхнего блока", "Изоляция широчайших мышц спины.", 90, 3),
            Exercise("Гиперэкстензия", "Укрепление поясничного отдела.", 60, 2)
        )),
        WorkoutProgram("Стальной Пресс", "Коровая зона", listOf(
            Exercise("Скручивания", "Классические скручивания на коврике.", 60, 2),
            Exercise("Планка", "Держим ровно, не прогибаем поясницу.", 60, 3),
            Exercise("Подъем ног в висе", "Акцент на нижние кубики пресса.", 90, 4),
            Exercise("Русские скручивания", "С утяжелителем в руках, повороты корпуса.", 60, 3)
        ))
    )
}