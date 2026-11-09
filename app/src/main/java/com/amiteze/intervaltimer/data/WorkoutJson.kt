package com.amiteze.intervaltimer.data

import com.amiteze.intervaltimer.model.Block
import com.amiteze.intervaltimer.model.BlockStage
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
        .put("blocks", JSONArray().apply { w.blocks.forEach { put(blockToJson(it)) } })

    private fun blockToJson(b: Block) = JSONObject()
        .put("id", b.id)
        .put("name", b.name)
        .put("sets", b.sets)
        .put("stages", JSONArray().apply {
            b.stages.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("sec", it.sec).put("rest", it.rest)) }
        })

    fun fromJson(o: JSONObject): Workout {
        val arr = o.optJSONArray("blocks") ?: JSONArray()
        return Workout(
            id = o.optString("id").ifEmpty { Workout().id },
            name = o.optString("name", "Workout"),
            warmupEnabled = o.optBoolean("warmupEnabled", false),
            warmupSec = o.optInt("warmupSec", 60),
            blocks = (0 until arr.length()).map { blockFromJson(arr.getJSONObject(it)) },
            cooldownEnabled = o.optBoolean("cooldownEnabled", false),
            cooldownSec = o.optInt("cooldownSec", 60),
        )
    }

    private fun blockFromJson(b: JSONObject): Block {
        val id = b.optString("id").ifEmpty { Block().id }
        val arr = b.optJSONArray("stages")
        if (arr == null) {
            // Old format (before custom stages): workSec / restSec / rounds.
            return Block(
                id = id,
                sets = b.optInt("rounds", 1),
                stages = listOf(
                    BlockStage(name = "Work", sec = b.optInt("workSec", 30)),
                    BlockStage(name = "Rest", sec = b.optInt("restSec", 15), rest = true),
                ),
            )
        }
        return Block(
            id = id,
            name = b.optString("name", ""),
            sets = b.optInt("sets", 1),
            stages = (0 until arr.length()).map { i ->
                val s = arr.getJSONObject(i)
                BlockStage(
                    id = s.optString("id").ifEmpty { BlockStage().id },
                    name = s.optString("name", "Work"),
                    sec = s.optInt("sec", 30),
                    rest = s.optBoolean("rest", false),
                )
            },
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
