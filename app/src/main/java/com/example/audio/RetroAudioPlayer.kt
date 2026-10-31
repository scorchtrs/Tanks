package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class RetroAudioPlayer {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val sampleRate = 22050
    var isMuted = false

    private fun playPcm(samples: ShortArray) {
        if (isMuted) return
        scope.launch {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.play()
                // Wait for playback and release
                val durationMs = (samples.size * 1000L) / sampleRate
                kotlinx.coroutines.delay(durationMs + 50)
                track.stop()
                track.release()
            } catch (e: Exception) {
                // Silently ignore audio track errors on low-spec hardware
            }
        }
    }

    /**
     * Cannon firing "THUMP" + rising pitch whoosh
     */
    fun playFireSound() {
        val durationMs = 280
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            // Low boom dropping from 220Hz to 60Hz + white noise puff
            val freq = 220.0 - 160.0 * progress
            val wave = sin(2.0 * PI * freq * t)
            val noise = (Random.nextDouble() * 2.0 - 1.0) * (1.0 - progress)
            val envelope = exp(-progress * 5.0)
            val sample = ((wave * 0.6 + noise * 0.4) * envelope * Short.MAX_VALUE * 0.75).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playPcm(samples)
    }

    /**
     * Explosion boom (distorted noise with bass rumble)
     */
    fun playExplosionSound(isBig: Boolean = false) {
        val durationMs = if (isBig) 650 else 420
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)
        var lastNoise = 0.0
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val rawNoise = Random.nextDouble() * 2.0 - 1.0
            // Low pass filter for rumble
            lastNoise = lastNoise * 0.85 + rawNoise * 0.15
            val subBass = sin(2.0 * PI * (55.0 - 25.0 * progress) * (i.toDouble() / sampleRate))
            val envelope = exp(-progress * (if (isBig) 3.5 else 5.0))
            val combined = (lastNoise * 0.7 + subBass * 0.5) * envelope
            val sample = (combined * Short.MAX_VALUE * 0.85).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playPcm(samples)
    }

    /**
     * Power-up pickup chime (happy arpeggio chime!)
     */
    fun playPowerUpSound() {
        val durationMs = 380
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)
        val notes = doubleArrayOf(440.0, 554.37, 659.25, 880.0) // A C# E A (major arpeggio)
        val noteLength = numSamples / notes.size
        for (i in 0 until numSamples) {
            val noteIdx = (i / noteLength).coerceIn(0, notes.size - 1)
            val t = i.toDouble() / sampleRate
            val localProg = (i % noteLength).toDouble() / noteLength
            val wave = sin(2.0 * PI * notes[noteIdx] * t)
            val envelope = (1.0 - localProg * 0.5)
            val sample = (wave * envelope * Short.MAX_VALUE * 0.6).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playPcm(samples)
    }

    /**
     * Shield hit / deflection hum
     */
    fun playShieldSound() {
        val durationMs = 260
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = 520.0 + sin(t * 80.0) * 120.0
            val wave = sin(2.0 * PI * freq * t)
            val envelope = exp(-progress * 4.0)
            val sample = (wave * envelope * Short.MAX_VALUE * 0.65).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playPcm(samples)
    }

    /**
     * Button click / dial tick
     */
    fun playClickSound() {
        val durationMs = 45
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val wave = sin(2.0 * PI * 1200.0 * t)
            val envelope = 1.0 - progress
            val sample = (wave * envelope * Short.MAX_VALUE * 0.35).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playPcm(samples)
    }

    /**
     * Victory fanfare
     */
    fun playVictorySound() {
        val durationMs = 600
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
        val noteLength = numSamples / notes.size
        for (i in 0 until numSamples) {
            val noteIdx = (i / noteLength).coerceIn(0, notes.size - 1)
            val t = i.toDouble() / sampleRate
            val localProg = (i % noteLength).toDouble() / noteLength
            val wave = sin(2.0 * PI * notes[noteIdx] * t)
            val sample = (wave * (1.0 - localProg * 0.3) * Short.MAX_VALUE * 0.65).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playPcm(samples)
    }
}
