package com.amiteze.intervaltimer.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.amiteze.intervaltimer.MainActivity
import com.amiteze.intervaltimer.R
import com.amiteze.intervaltimer.data.Settings
import com.amiteze.intervaltimer.data.Storage
import com.amiteze.intervaltimer.data.WorkoutJson
import com.amiteze.intervaltimer.model.Workout
import com.amiteze.intervaltimer.timer.StageBuilder
import com.amiteze.intervaltimer.timer.TimerEngine
import com.amiteze.intervaltimer.timer.formatMmSs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Runs the workout in a foreground service so it keeps going with the screen
 * off or the app in the background. Holds a partial wake lock, plays beeps,
 * vibrates and shows an ongoing notification with Pause/Stop buttons.
 *
 * The UI talks to it with intents (see the ACTION_ constants) and reads
 * [state] to draw the run screen.
 */
class TimerService : Service() {

    companion object {
        const val ACTION_START = "com.amiteze.intervaltimer.START"
        const val ACTION_PAUSE_RESUME = "com.amiteze.intervaltimer.PAUSE_RESUME"
        const val ACTION_SKIP = "com.amiteze.intervaltimer.SKIP"
        const val ACTION_PREVIOUS = "com.amiteze.intervaltimer.PREVIOUS"
        const val ACTION_STOP = "com.amiteze.intervaltimer.STOP"
        private const val EXTRA_WORKOUT = "workout_json"
        private const val CHANNEL_ID = "timer"
        private const val NOTIFICATION_ID = 1
        private const val TICK_MS = 50L

        private val _state = MutableStateFlow(RunState())
        /** Live timer state for the UI. */
        val state: StateFlow<RunState> = _state.asStateFlow()

        fun start(context: Context, workout: Workout) {
            val intent = Intent(context, TimerService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_WORKOUT, WorkoutJson.workoutToString(workout))
            ContextCompat.startForegroundService(context, intent)
        }

        fun send(context: Context, action: String) {
            context.startService(Intent(context, TimerService::class.java).setAction(action))
        }

        /** Called by the UI after the "Done" screen is closed. */
        fun clearFinished() {
            if (!_state.value.active || _state.value.finished) _state.value = RunState()
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var beeper: Beeper
    private lateinit var vibe: Vibe
    private var wakeLock: PowerManager.WakeLock? = null
    private var settings = Settings()

    private var engine: TimerEngine? = null
    private var workoutName = ""
    private var lastIndex = -1        // stage index at the previous tick
    private var lastSecond = -1       // whole seconds left at the previous tick
    private var lastNotified = ""     // avoids re-posting an identical notification

    private val tick = object : Runnable {
        override fun run() {
            onTick()
            if (engine != null) handler.postDelayed(this, TICK_MS)
        }
    }
    private val stopLater = Runnable { stopSelf() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        beeper = Beeper(this)
        vibe = Vibe(this)
        createChannel()
        scope.launch { Storage(this@TimerService).settings.collect { settings = it } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val now = SystemClock.elapsedRealtime()
        when (intent?.action) {
            ACTION_START -> startWorkout(intent.getStringExtra(EXTRA_WORKOUT), now)
            ACTION_PAUSE_RESUME -> engine?.let { if (it.isPaused) it.resume(now) else it.pause(now); publish(now) }
            ACTION_SKIP -> engine?.let { it.skip(now); afterUserJump(now) }
            ACTION_PREVIOUS -> engine?.let { it.previous(now); afterUserJump(now) }
            ACTION_STOP -> stopWorkout()
        }
        if (engine == null && intent?.action != ACTION_START) stopSelf()
        return START_NOT_STICKY
    }

    private fun startWorkout(json: String?, now: Long) {
        handler.removeCallbacks(stopLater)
        val workout = json?.let { WorkoutJson.workoutFromString(it) } ?: return
        val stages = StageBuilder.build(workout)
        // startForeground must always be called after startForegroundService.
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(RunState(active = true, workoutName = workout.name)),
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )
        if (stages.isEmpty()) {
            stopWorkout()
            return
        }
        workoutName = workout.name
        engine = TimerEngine(stages).also { it.start(now) }
        acquireWakeLock(stages.sumOf { it.durationMs })
        lastIndex = 0
        lastSecond = secondsLeft(now)
        stageChangedFeedback()
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    private fun onTick() {
        val e = engine ?: return
        val now = SystemClock.elapsedRealtime()
        e.update(now)
        if (e.isFinished) {
            finishWorkout()
            return
        }
        val sec = secondsLeft(now)
        if (e.index != lastIndex) {
            // A stage ended on its own: long tone + vibration.
            lastIndex = e.index
            stageChangedFeedback()
        } else if (sec != lastSecond && sec in 1..5 && sec < e.currentStage!!.durationSec) {
            // Countdown beeps at 5, 4, 3, 2, 1 (not on the first second of a stage).
            if (settings.sound) beeper.shortBeep()
        }
        lastSecond = sec
        publish(now)
    }

    /** After skip/previous: no beep, just reset the countdown tracking. */
    private fun afterUserJump(now: Long) {
        val e = engine ?: return
        if (e.isFinished) {
            finishWorkout()
            return
        }
        lastIndex = e.index
        lastSecond = secondsLeft(now)
        publish(now)
    }

    private fun stageChangedFeedback() {
        if (settings.sound) beeper.longBeep()
        if (settings.vibration) vibe.buzz()
    }

    private fun secondsLeft(now: Long): Int = ((engine?.stageRemainingMs(now) ?: 0L) + 999).div(1000).toInt()

    private fun finishWorkout() {
        stageChangedFeedback()
        engine = null
        _state.value = _state.value.copy(
            stage = null, nextStage = null, stageRemainingMs = 0, totalRemainingMs = 0, paused = false, finished = true,
        )
        endForeground()
        handler.postDelayed(stopLater, 1500) // let the final tone finish first
    }

    private fun stopWorkout() {
        engine = null
        _state.value = RunState()
        endForeground()
        stopSelf()
    }

    private fun endForeground() {
        handler.removeCallbacks(tick)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        releaseWakeLock()
        lastNotified = ""
    }

    private fun publish(now: Long) {
        val e = engine ?: return
        val s = RunState(
            active = true,
            workoutName = workoutName,
            stage = e.currentStage,
            nextStage = e.stages.getOrNull(e.index + 1),
            stageIndex = e.index,
            stageCount = e.stages.size,
            stageRemainingMs = e.stageRemainingMs(now),
            totalRemainingMs = e.totalRemainingMs(now),
            paused = e.isPaused,
        )
        _state.value = s
        updateNotification(s)
    }

    // ---- Notification ----

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Workout timer", NotificationManager.IMPORTANCE_LOW)
        channel.setShowBadge(false)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this, requestCode, Intent(this, TimerService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun buildNotification(s: RunState): Notification {
        val stage = s.stage
        val title = if (stage == null) s.workoutName else
            "${stage.type.label}  ${formatMmSs(s.stageRemainingMs)}" + if (s.paused) "  (paused)" else ""
        val text = buildString {
            if (stage != null && stage.round > 0) append("Round ${stage.round}/${stage.roundCount} · Block ${stage.block}/${stage.blockCount} · ")
            append("${formatMmSs(s.totalRemainingMs)} left")
        }
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, if (s.paused) "Resume" else "Pause", servicePendingIntent(ACTION_PAUSE_RESUME, 1))
            .addAction(0, "Stop", servicePendingIntent(ACTION_STOP, 2))
            .build()
    }

    /** Re-posts the notification only when its text changes (about once a second). */
    private fun updateNotification(s: RunState) {
        val key = "${s.stageIndex}|${formatMmSs(s.stageRemainingMs)}|${s.paused}"
        if (key == lastNotified) return
        lastNotified = key
        val allowed = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (allowed) getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(s))
    }

    // ---- Wake lock ----

    private fun acquireWakeLock(workoutMs: Long) {
        releaseWakeLock()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "IntervalTimer:run").apply {
            setReferenceCounted(false)
            // Safety timeout: workout length plus 2 hours for pauses.
            acquire(workoutMs + 2 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        releaseWakeLock()
        beeper.release()
        scope.cancel()
        if (engine != null) _state.value = RunState()
        super.onDestroy()
    }
}
