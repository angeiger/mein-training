package de.meintraining.app.ui.screens

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.meintraining.app.TrainingApp
import de.meintraining.app.catalog.Kind
import de.meintraining.app.data.MachineSettings
import de.meintraining.app.data.Plan
import de.meintraining.app.data.PlanItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A running countdown. Times are [SystemClock.elapsedRealtime] milliseconds. */
sealed interface WorkoutTimer {
    val totalMs: Long

    data class Rest(val endsAt: Long, override val totalMs: Long) : WorkoutTimer
    data class Hold(val itemId: Long, val set: Int, val endsAt: Long, override val totalMs: Long) : WorkoutTimer
    data class Cardio(
        val itemId: Long,
        val endsAt: Long,
        val remainingMs: Long,
        val running: Boolean,
        override val totalMs: Long,
    ) : WorkoutTimer
}

/**
 * Where she is in the workout. Position, ticked sets and the start time live in the
 * saved state, so they survive the app being closed in the background (for example
 * while the camera is open). Running timers do not; she just taps again.
 */
class WorkoutViewModel(
    private val app: TrainingApp,
    planId: Long,
    private val state: SavedStateHandle,
) : ViewModel() {

    private val repo = app.repository

    val plan: StateFlow<Plan?> = repo.plan(planId).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val settings: StateFlow<Map<String, MachineSettings>> =
        repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val index: StateFlow<Int> = state.getStateFlow(KEY_INDEX, 0)
    val done: StateFlow<ArrayList<String>> = state.getStateFlow(KEY_DONE, arrayListOf())
    val finished: StateFlow<Boolean> = state.getStateFlow(KEY_FINISHED, false)

    private val startedAt: Long = state[KEY_STARTED] ?: System.currentTimeMillis().also { state[KEY_STARTED] = it }

    private val _timer = MutableStateFlow<WorkoutTimer?>(null)
    val timer: StateFlow<WorkoutTimer?> = _timer.asStateFlow()
    private var timerJob: Job? = null

    private val tones: ToneGenerator? by lazy {
        runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90) }.getOrNull()
    }

    fun isDone(itemId: Long, set: Int) = key(itemId, set) in done.value

    fun doneSets(item: PlanItem): Int = (0 until item.sets).count { isDone(item.id, it) }

    fun isComplete(item: PlanItem): Boolean =
        if (item.exercise.kind == Kind.CARDIO) isDone(item.id, 0) else doneSets(item) >= item.sets

    /** Tap on a set circle. */
    fun tapSet(item: PlanItem, set: Int) {
        if (isDone(item.id, set)) {
            setDone(item.id, set, false)
            return
        }
        when (item.exercise.kind) {
            Kind.REPS -> {
                setDone(item.id, set, true)
                afterSet(item)
            }
            Kind.HOLD -> startHold(item, set)
            Kind.CARDIO -> toggleCardioDone(item)
        }
    }

    private fun afterSet(item: PlanItem) {
        if (doneSets(item) < item.sets && item.restSeconds > 0) startRest(item.restSeconds)
        else if (_timer.value is WorkoutTimer.Rest) stopTimer()
    }

    private fun startRest(seconds: Int) {
        val total = seconds * 1000L
        run(WorkoutTimer.Rest(now() + total, total), total) {
            _timer.value = null
            alert()
        }
    }

    private fun startHold(item: PlanItem, set: Int) {
        val total = item.amount * 1000L
        run(WorkoutTimer.Hold(item.id, set, now() + total, total), total) {
            _timer.value = null
            setDone(item.id, set, true)
            alert()
            afterSet(item)
        }
    }

    /** Start, pause or continue the countdown of a cardio exercise. */
    fun cardioStartPause(item: PlanItem) {
        val t = _timer.value
        if (t is WorkoutTimer.Cardio && t.itemId == item.id) {
            if (t.running) {
                timerJob?.cancel()
                _timer.value = t.copy(running = false, remainingMs = (t.endsAt - now()).coerceAtLeast(0))
            } else {
                runCardio(item, t.remainingMs, t.totalMs)
            }
        } else {
            val total = item.amount * 60_000L
            runCardio(item, total, total)
        }
    }

    private fun runCardio(item: PlanItem, remaining: Long, total: Long) {
        run(WorkoutTimer.Cardio(item.id, now() + remaining, remaining, true, total), remaining) {
            _timer.value = null
            setDone(item.id, 0, true)
            alert()
        }
    }

    fun toggleCardioDone(item: PlanItem) {
        val nowDone = !isDone(item.id, 0)
        setDone(item.id, 0, nowDone)
        val t = _timer.value
        if (nowDone && t is WorkoutTimer.Cardio && t.itemId == item.id) stopTimer()
    }

    fun stopTimer() {
        timerJob?.cancel()
        _timer.value = null
    }

    fun goTo(newIndex: Int) {
        // A rest keeps running when she walks to the next machine; holds and cardio stop.
        if (_timer.value !is WorkoutTimer.Rest) stopTimer()
        state[KEY_INDEX] = newIndex
    }

    fun finish() {
        stopTimer()
        state[KEY_FINISHED] = true
    }

    fun minutesSoFar(): Int = ((System.currentTimeMillis() - startedAt) / 60_000).toInt().coerceAtLeast(1)

    private fun run(timer: WorkoutTimer, durationMs: Long, onEnd: () -> Unit) {
        timerJob?.cancel()
        _timer.value = timer
        timerJob = viewModelScope.launch {
            delay(durationMs)
            onEnd()
        }
    }

    private fun setDone(itemId: Long, set: Int, value: Boolean) {
        val list = ArrayList(done.value)
        val k = key(itemId, set)
        if (value) {
            if (k !in list) list += k
        } else {
            list -= k
        }
        state[KEY_DONE] = list
    }

    /** Buzz twice and beep, so she notices in a noisy gym. */
    private fun alert() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                app.getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 350, 200, 350), -1))
        }
        runCatching { tones?.startTone(ToneGenerator.TONE_PROP_BEEP2, 500) }
    }

    override fun onCleared() {
        tones?.release()
    }

    private fun key(itemId: Long, set: Int) = "$itemId:$set"

    companion object {
        private const val KEY_INDEX = "index"
        private const val KEY_DONE = "done"
        private const val KEY_FINISHED = "finished"
        private const val KEY_STARTED = "startedAt"

        fun now() = SystemClock.elapsedRealtime()

        fun factory(planId: Long) = viewModelFactory {
            initializer {
                WorkoutViewModel(this[APPLICATION_KEY] as TrainingApp, planId, createSavedStateHandle())
            }
        }
    }
}
