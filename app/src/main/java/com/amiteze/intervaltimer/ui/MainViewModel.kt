package com.amiteze.intervaltimer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amiteze.intervaltimer.data.Settings
import com.amiteze.intervaltimer.data.Storage
import com.amiteze.intervaltimer.model.Block
import com.amiteze.intervaltimer.model.Workout
import com.amiteze.intervaltimer.service.RunState
import com.amiteze.intervaltimer.service.TimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** Which screen is showing. */
enum class Screen { LIST, EDIT, RUN, SETTINGS }

/** Holds the app's state and handles every button press. */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = Storage(app)

    val workouts: StateFlow<List<Workout>> =
        storage.workouts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val settings: StateFlow<Settings> =
        storage.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    val run: StateFlow<RunState> = TimerService.state

    private val _screen = MutableStateFlow(if (TimerService.state.value.active) Screen.RUN else Screen.LIST)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    /** The workout being edited (a copy; saved only on "Save"). */
    private val _draft = MutableStateFlow<Workout?>(null)
    val draft: StateFlow<Workout?> = _draft.asStateFlow()

    fun go(screen: Screen) { _screen.value = screen }

    // ---- Workout list ----

    fun newWorkout() = edit(Workout())

    fun edit(w: Workout) {
        _draft.value = w
        _screen.value = Screen.EDIT
    }

    fun duplicate(w: Workout) = saveAll { list ->
        val copy = w.copy(
            id = UUID.randomUUID().toString(),
            name = "${w.name} (copy)",
            blocks = w.blocks.map { it.copy(id = UUID.randomUUID().toString()) },
        )
        val i = list.indexOfFirst { it.id == w.id }
        list.toMutableList().apply { add(i + 1, copy) }
    }

    fun delete(w: Workout) = saveAll { list -> list.filterNot { it.id == w.id } }

    // ---- Editor ----

    fun updateDraft(w: Workout) { _draft.value = w }

    fun addBlock() = _draft.value?.let { d ->
        val last = d.blocks.lastOrNull() ?: Block()
        updateDraft(d.copy(blocks = d.blocks + last.copy(id = UUID.randomUUID().toString())))
    }

    fun saveDraft() {
        val d = _draft.value ?: return
        val clean = d.copy(
            name = d.name.ifBlank { "Workout" },
            blocks = d.blocks.ifEmpty { listOf(Block()) }.map { it.copy(rounds = it.rounds.coerceAtLeast(1)) },
        )
        saveAll { list ->
            if (list.any { it.id == clean.id }) list.map { if (it.id == clean.id) clean else it } else list + clean
        }
        _draft.value = null
        _screen.value = Screen.LIST
    }

    fun cancelEdit() {
        _draft.value = null
        _screen.value = Screen.LIST
    }

    private fun saveAll(change: (List<Workout>) -> List<Workout>) = viewModelScope.launch {
        storage.saveWorkouts(change(storage.workouts.first()))
    }

    // ---- Timer ----

    fun start(w: Workout) {
        TimerService.start(getApplication(), w)
        _screen.value = Screen.RUN
    }

    fun timerAction(action: String) = TimerService.send(getApplication(), action)

    fun closeRun() {
        TimerService.clearFinished()
        _screen.value = Screen.LIST
    }

    // ---- Settings ----

    fun setSound(on: Boolean) = viewModelScope.launch { storage.setSound(on) }
    fun setVibration(on: Boolean) = viewModelScope.launch { storage.setVibration(on) }
}
