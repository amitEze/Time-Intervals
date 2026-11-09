package com.amiteze.intervaltimer.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Plays beeps generated in code (no audio files).
 *
 * Before each beep we ask for AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK, so music
 * apps (Spotify, YouTube Music...) only get quieter for a moment and keep
 * playing. Focus is given back right after the beep ends.
 */
class Beeper(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())

    // "Navigation guidance" is the usage meant for short prompts over music
    // (like GPS directions). It plays at media volume.
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener { /* beeps are too short to react */ }
        .build()

    // Tweak sounds here: frequency in Hz, length in ms.
    private val shortTone = Tone(freqHz = 880.0, lengthMs = 150)
    private val longTone = Tone(freqHz = 1320.0, lengthMs = 600)

    private val abandonFocus = Runnable { audioManager.abandonAudioFocusRequest(focusRequest) }

    /** Countdown beep for the last 5 seconds. */
    fun shortBeep() = play(shortTone)

    /** Longer, higher tone when a new stage starts (or the workout ends). */
    fun longBeep() = play(longTone)

    private fun play(tone: Tone) {
        handler.removeCallbacks(abandonFocus)
        audioManager.requestAudioFocus(focusRequest)
        tone.play()
        handler.postDelayed(abandonFocus, tone.lengthMs + 200L)
    }

    fun release() {
        handler.removeCallbacks(abandonFocus)
        audioManager.abandonAudioFocusRequest(focusRequest)
        shortTone.release()
        longTone.release()
    }

    /** A sine wave kept in a small static AudioTrack so it can be replayed instantly. */
    private inner class Tone(freqHz: Double, val lengthMs: Int) {
        private val sampleRate = 44_100
        private val samples = sampleRate * lengthMs / 1000
        private val track: AudioTrack

        init {
            val fade = sampleRate / 100 // 10 ms fade in/out avoids clicks
            val data = ShortArray(samples) { i ->
                val envelope = min(1.0, min(i, samples - 1 - i).toDouble() / fade)
                (sin(2 * PI * freqHz * i / sampleRate) * envelope * 0.8 * Short.MAX_VALUE).toInt().toShort()
            }
            track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples * 2)
                .build()
            track.write(data, 0, data.size)
        }

        fun play() {
            try {
                track.stop()
                track.reloadStaticData() // rewind to the start
                track.play()
            } catch (e: IllegalStateException) {
                // Audio device busy; skip this beep.
            }
        }

        fun release() = track.release()
    }
}
