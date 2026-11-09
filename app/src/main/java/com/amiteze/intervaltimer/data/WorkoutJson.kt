package com.amiteze.intervaltimer.data

import com.amiteze.intervaltimer.model.Block
import com.amiteze.intervaltimer.model.Workout
import org.json.JSONArray
import org.json.JSONObject

/** Converts workouts to/from JSON using Android's built-in org.json (no extra library). */
object WorkoutJson {

    fun toJson(w: Workout): JSONObject = JSONObject()
        .put("id", w.id)
        .put("name", w.name)
        .put("warmupEnabled", w.warmupEnabled)
        .put("warmupSec", w.warmupSec)
        .put("cooldownEnabled", w.cooldownEnabled)
        .put("cooldownSec", w.cooldownSec)
        .put("blocks", JSONArray().apply {
            w.blocks.forEach { b ->
                put(JSONObject().put("id", b.id).put("workSec", b.workSec).put("restSec", b.restSec).put("rounds", b.rounds))
            }
        })

    fun fromJson(o: JSONObject): Workout {
        val blocksJson = o.optJSONArray("blocks") ?: JSONArray()
        val blocks = (0 until blocksJson.length()).map { i ->
            val b = blocksJson.getJSONObject(i)
            Block(
                id = b.optString("id").ifEmpty { Block().id },
                workSec = b.optInt("workSec", 30),
                restSec = b.optInt("restSec", 15),
                rounds = b.optInt("rounds", 1),
            )
        }
        return Workout(
            id = o.optString("id").ifEmpty { Workout().id },
            name = o.optString("name", "Workout"),
            warmupEnabled = o.optBoolean("warmupEnabled", false),
            warmupSec = o.optInt("warmupSec", 60),
            blocks = blocks,
            cooldownEnabled = o.optBoolean("cooldownEnabled", false),
            cooldownSec = o.optInt("cooldownSec", 60),
        )
    }

    fun listToString(list: List<Workout>): String =
        JSONArray().apply { list.forEach { put(toJson(it)) } }.toString()

    fun listFromString(s: String): List<Workout> = try {
        val arr = JSONArray(s)
        (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }

    fun workoutToString(w: Workout): String = toJson(w).toString()
    fun workoutFromString(s: String): Workout = fromJson(JSONObject(s))
}
