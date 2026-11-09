package com.amiteze.intervaltimer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.amiteze.intervaltimer.model.Block
import com.amiteze.intervaltimer.model.BlockStage
import com.amiteze.intervaltimer.model.Workout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** One DataStore file for the whole app. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "interval_timer")

private val WORKOUTS = stringPreferencesKey("workouts_json")
private val SOUND = booleanPreferencesKey("sound_enabled")
private val VIBRATION = booleanPreferencesKey("vibration_enabled")

/** User settings. */
data class Settings(val sound: Boolean = true, val vibration: Boolean = true)

/** Saves workouts (as one JSON string) and settings on the device. */
class Storage(context: Context) {
    private val store = context.applicationContext.dataStore

    /** The saved workouts. On first launch two examples are shown. */
    val workouts: Flow<List<Workout>> = store.data.map { prefs ->
        prefs[WORKOUTS]?.let { WorkoutJson.listFromString(it) } ?: examples()
    }

    val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(sound = prefs[SOUND] ?: true, vibration = prefs[VIBRATION] ?: true)
    }

    suspend fun saveWorkouts(list: List<Workout>) {
        store.edit { it[WORKOUTS] = WorkoutJson.listToString(list) }
    }

    suspend fun setSound(on: Boolean) = store.edit { it[SOUND] = on }
    suspend fun setVibration(on: Boolean) = store.edit { it[VIBRATION] = on }

    private fun examples() = listOf(
        Workout(
            name = "Boxing rounds + Tabata",
            warmupEnabled = true, warmupSec = 120,
            blocks = listOf(
                Block(name = "Boxing", sets = 5, stages = listOf(
                    BlockStage(name = "Fight", sec = 180),
                    BlockStage(name = "Rest", sec = 60, rest = true),
                )),
                Block(name = "Tabata", sets = 8, stages = listOf(
                    BlockStage(name = "Burpees", sec = 30),
                    BlockStage(name = "Rest", sec = 15, rest = true),
                )),
            ),
            cooldownEnabled = true, cooldownSec = 120,
        ),
        Workout(
            name = "Circuit",
            warmupEnabled = true, warmupSec = 10,
            blocks = listOf(
                Block(name = "Circuit", sets = 3, stages = listOf(
                    BlockStage(name = "Squats", sec = 40),
                    BlockStage(name = "Push-ups", sec = 40),
                    BlockStage(name = "Plank", sec = 40),
                    BlockStage(name = "Rest", sec = 60, rest = true),
                )),
            ),
        ),
    )
}
