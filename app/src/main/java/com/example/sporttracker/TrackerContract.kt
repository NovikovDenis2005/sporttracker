package com.example.sporttracker

enum class AppModule {
    RUNNING, WORKOUTS
}

enum class AppScreen {
    MAIN_MENU, ACTIVE_PROCESS, HISTORY, ACHIEVEMENTS, PROFILE
}

enum class HistoryFilter {
    ALL, RUNNING, WORKOUTS
}

data class DailyStat(
    val dateString: String,
    val exercisesCompleted: Int
)

data class RunningTest(
    val distanceMeters: Int,
    val timeMillis: Long,
    val grade: String,
    val calories: Int,
    val dateString: String
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val isUnlocked: Boolean
)

data class Exercise(
    val name: String,
    val description: String,
    val durationSeconds: Int,
    val difficulty: Int
)

data class WorkoutProgram(
    val title: String,
    val category: String,
    val exercises: List<Exercise>
)

data class TrackerState(
    val currentModule: AppModule = AppModule.WORKOUTS,
    val currentScreen: AppScreen = AppScreen.MAIN_MENU,
    val isDarkTheme: Boolean = false,
    val historyFilter: HistoryFilter = HistoryFilter.ALL,

    // Силовые тренировки
    val programs: List<WorkoutProgram> = emptyList(),
    val selectedProgram: WorkoutProgram? = null,
    val currentExerciseIndex: Int = 0,
    val timerSeconds: Int = 0,
    val initialDurationSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val isShowingExerciseDetails: Boolean = false,
    val workoutAchievements: List<Achievement> = emptyList(),

    // Бег
    val selectedRunningDistance: Int = 100,
    val stopwatchMillis: Long = 0L,
    val isStopwatchRunning: Boolean = false,
    val runningResults: List<RunningTest> = emptyList(),
    val runningAchievements: List<Achievement> = emptyList(),

    // Метрики пользователя и калории
    val records: List<RecordEntity> = emptyList(),
    val dailyStats: List<DailyStat> = emptyList(),
    val userName: String = "Атлет",
    val userWeightKg: Float = 75f,
    val userHeightCm: Float = 178f,
    val todayCalories: Int = 0
)

sealed class TrackerEvent {
    data class SwitchModule(val module: AppModule) : TrackerEvent()
    data class ChangeScreen(val screen: AppScreen) : TrackerEvent()
    object ToggleTheme : TrackerEvent()
    data class SetHistoryFilter(val filter: HistoryFilter) : TrackerEvent()
    object ClearAllHistory : TrackerEvent()

    // Профиль
    data class UpdateProfileParams(val name: String, val weight: Float, val height: Float) : TrackerEvent()

    // Тренировки
    data class SelectProgram(val program: WorkoutProgram) : TrackerEvent()
    object StartTimer : TrackerEvent()
    object PauseTimer : TrackerEvent()
    object ResetTimer : TrackerEvent()
    object NextExercise : TrackerEvent()
    object PrevExercise : TrackerEvent()
    object TickWorkout : TrackerEvent()
    object FinishWorkout : TrackerEvent()
    object ShowExerciseDetails : TrackerEvent()
    object HideExerciseDetails : TrackerEvent()

    // Бег / Секундомер
    data class SelectDistance(val distance: Int) : TrackerEvent()
    object StartStopwatch : TrackerEvent()
    object PauseStopwatch : TrackerEvent()
    object ResetStopwatch : TrackerEvent()
    object SaveStopwatchResult : TrackerEvent()
    object BackToMenu : TrackerEvent()
}