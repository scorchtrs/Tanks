package com.example.physics

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class TerrainEngine(
    val pointCount: Int = 500,
    val worldWidth: Float = 1000f,
    val worldHeight: Float = 600f
) {
    // Array storing y-coordinate of terrain surface (0 at top, worldHeight at bottom)
    val heights = FloatArray(pointCount)
    val stepX: Float = worldWidth / (pointCount - 1)

    fun generate(seed: Long = System.currentTimeMillis(), style: TerrainStyle = TerrainStyle.HILLS) {
        val rand = Random(seed)
        val baseBaseline = worldHeight * 0.62f
        val amp1 = when (style) {
            TerrainStyle.HILLS -> worldHeight * 0.18f
            TerrainStyle.PEAKS -> worldHeight * 0.28f
            TerrainStyle.CANYON -> worldHeight * 0.24f
            TerrainStyle.PLATEAU -> worldHeight * 0.15f
        }
        val freq1 = (rand.nextFloat() * 1.5f + 1.2f) * 2f * PI.toFloat() / worldWidth
        val freq2 = (rand.nextFloat() * 2.0f + 2.5f) * 2f * PI.toFloat() / worldWidth
        val freq3 = (rand.nextFloat() * 4.0f + 5.0f) * 2f * PI.toFloat() / worldWidth
        val phase1 = rand.nextFloat() * 2f * PI.toFloat()
        val phase2 = rand.nextFloat() * 2f * PI.toFloat()
        val phase3 = rand.nextFloat() * 2f * PI.toFloat()

        for (i in 0 until pointCount) {
            val x = i * stepX
            var h = baseBaseline +
                    sin(x * freq1 + phase1) * amp1 +
                    cos(x * freq2 + phase2) * (amp1 * 0.45f) +
                    sin(x * freq3 + phase3) * (amp1 * 0.18f)

            if (style == TerrainStyle.CANYON) {
                // Dig a deep valley in the middle
                val distFromMid = kotlin.math.abs(x - worldWidth * 0.5f) / (worldWidth * 0.5f)
                if (distFromMid < 0.45f) {
                    val canyonDepth = (1f - distFromMid / 0.45f) * (worldHeight * 0.25f)
                    h += canyonDepth
                }
            } else if (style == TerrainStyle.PLATEAU) {
                // Flatten tops
                if (h < worldHeight * 0.45f) {
                    h = worldHeight * 0.45f + (h - worldHeight * 0.45f) * 0.2f
                }
            }

            // Clamp so terrain doesn't touch the top sky or sink off the bottom
            heights[i] = h.coerceIn(worldHeight * 0.22f, worldHeight * 0.88f)
        }
    }

    fun getHeightAt(x: Float): Float {
        val clampedX = x.coerceIn(0f, worldWidth)
        val indexFloat = clampedX / stepX
        val indexLow = indexFloat.toInt().coerceIn(0, pointCount - 1)
        val indexHigh = min(indexLow + 1, pointCount - 1)
        val frac = indexFloat - indexLow
        return heights[indexLow] * (1f - frac) + heights[indexHigh] * frac
    }

    fun getSlopeAngleAt(x: Float): Float {
        val sampleDist = stepX * 2f
        val xLeft = (x - sampleDist).coerceAtLeast(0f)
        val xRight = (x + sampleDist).coerceAtMost(worldWidth)
        val yLeft = getHeightAt(xLeft)
        val yRight = getHeightAt(xRight)
        return (atan2(yRight - yLeft, xRight - xLeft) * (180f / PI.toFloat())).coerceIn(-45f, 45f)
    }

    /**
     * Carves a circular explosion crater out of the terrain.
     * In screen coordinates, larger Y is deeper down, so carving moves heights[i] DOWN (greater Y).
     */
    fun carveCrater(cx: Float, cy: Float, radius: Float): Boolean {
        var affected = false
        val minX = (cx - radius).coerceAtLeast(0f)
        val maxX = (cx + radius).coerceAtMost(worldWidth)
        val startIdx = (minX / stepX).toInt().coerceIn(0, pointCount - 1)
        val endIdx = (maxX / stepX).toInt().coerceIn(0, pointCount - 1)

        val rSq = radius * radius
        for (i in startIdx..endIdx) {
            val x = i * stepX
            val dx = x - cx
            val dxSq = dx * dx
            if (dxSq < rSq) {
                val halfChord = sqrt(rSq - dxSq)
                val craterBottomY = cy + halfChord
                if (craterBottomY > heights[i]) {
                    // Deepen the ground
                    heights[i] = min(worldHeight * 0.95f, max(heights[i], craterBottomY))
                    affected = true
                }
            }
        }
        if (affected) {
            settleTerrain(startIdx, endIdx)
        }
        return affected
    }

    /**
     * Adds a dirt mound (for Dirt Bombs / terrain building).
     * Moving heights[i] UP (smaller Y).
     */
    fun addDirt(cx: Float, cy: Float, radius: Float, maxAddition: Float = 35f) {
        val minX = (cx - radius).coerceAtLeast(0f)
        val maxX = (cx + radius).coerceAtMost(worldWidth)
        val startIdx = (minX / stepX).toInt().coerceIn(0, pointCount - 1)
        val endIdx = (maxX / stepX).toInt().coerceIn(0, pointCount - 1)

        for (i in startIdx..endIdx) {
            val x = i * stepX
            val dx = kotlin.math.abs(x - cx)
            if (dx < radius) {
                val factor = cos((dx / radius) * (PI.toFloat() / 2f))
                heights[i] = (heights[i] - maxAddition * factor).coerceAtLeast(worldHeight * 0.15f)
            }
        }
        settleTerrain(startIdx, endIdx)
    }

    /**
     * Simulates dirt physics: prevents vertical unsupportable spikes by letting dirt slide down slopes.
     */
    fun settleTerrain(centerStart: Int = 0, centerEnd: Int = pointCount - 1) {
        val maxStepDiff = stepX * 1.8f // Maximum natural angle of repose
        val pad = 15
        val start = (centerStart - pad).coerceAtLeast(1)
        val end = (centerEnd + pad).coerceAtMost(pointCount - 2)

        for (pass in 0 until 4) {
            for (i in start..end) {
                val diffLeft = heights[i] - heights[i - 1]
                if (diffLeft > maxStepDiff) {
                    val transfer = (diffLeft - maxStepDiff) * 0.35f
                    heights[i] -= transfer
                    heights[i - 1] += transfer
                } else if (diffLeft < -maxStepDiff) {
                    val transfer = (-diffLeft - maxStepDiff) * 0.35f
                    heights[i] += transfer
                    heights[i - 1] -= transfer
                }
            }
        }
    }
}

enum class TerrainStyle(val displayName: String) {
    HILLS("Rolling Hills"),
    PEAKS("Jagged Peaks"),
    CANYON("Deep Canyon"),
    PLATEAU("Mesa Plateau")
}
