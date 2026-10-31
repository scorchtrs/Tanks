package com.example.ai

import com.example.model.AiDifficulty
import com.example.model.Tank
import com.example.model.WeaponType
import com.example.physics.BallisticsEngine
import com.example.physics.TerrainEngine
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.random.Random

class ArtilleryAi(
    private val ballistics: BallisticsEngine
) {
    data class AiDecision(
        val angle: Float,
        val power: Float,
        val weaponType: WeaponType,
        val shouldMove: Int = 0 // -1 left, 0 stay, +1 right
    )

    fun calculateShot(
        aiTank: Tank,
        targetTank: Tank,
        windSpeed: Float,
        terrain: TerrainEngine,
        difficulty: AiDifficulty
    ): AiDecision {
        val dx = targetTank.x - aiTank.x
        val isTargetToRight = dx > 0

        // Base angle range: rightward 30° to 75°, leftward 105° to 150°
        var bestAngle = if (isTargetToRight) 45f else 135f
        var bestPower = 55f

        // Ballistic range formula approximation:
        // R ~ (v^2 * sin(2*theta)) / g  plus wind drift
        val dist = abs(dx)
        val dy = targetTank.y - aiTank.y // positive means target is lower down

        val baseSpeedNeeded = sqrt(dist * ballistics.gravity * 0.95f)
        val calculatedPower = (baseSpeedNeeded / 6.2f).coerceIn(20f, 95f)
        bestPower = calculatedPower

        // Wind compensation:
        // Positive wind blows rightward (+X)
        // If firing right and wind is positive (tailwind), reduce power or lower angle
        // If firing right and wind is negative (headwind), increase power or steepen angle
        val windOffset = if (isTargetToRight) -windSpeed * 0.18f else windSpeed * 0.18f
        bestPower = (bestPower + windOffset).coerceIn(15f, 100f)

        // Elevation compensation: if target is higher (dy < 0), add angle/power
        if (dy < -30f) {
            bestPower = (bestPower + 8f).coerceAtMost(100f)
        }

        // Apply difficulty error perturbation
        val rand = Random.Default
        val angleError = (rand.nextFloat() * 2f - 1f) * difficulty.errorDegrees
        val powerError = (rand.nextFloat() * 2f - 1f) * difficulty.errorPower

        var finalAngle = (bestAngle + angleError).coerceIn(10f, 170f)
        var finalPower = (bestPower + powerError).coerceIn(10f, 100f)

        // For CYBORG difficulty, do a 5-step numerical trajectory search to fine-tune!
        if (difficulty == AiDifficulty.CYBORG) {
            var minMissDist = Float.MAX_VALUE
            var tunedAngle = finalAngle
            var tunedPower = finalPower

            for (aTest in listOf(finalAngle - 2f, finalAngle, finalAngle + 2f)) {
                for (pTest in listOf(finalPower - 4f, finalPower, finalPower + 4f)) {
                    val traj = ballistics.simulateTrajectory(
                        startX = aiTank.x,
                        startY = aiTank.y - 10f,
                        angleDeg = aTest,
                        power = pTest,
                        windSpeed = windSpeed,
                        isHyper = aiTank.hasSuperPropulsion,
                        terrain = terrain,
                        maxSteps = 60
                    )
                    if (traj.isNotEmpty()) {
                        val last = traj.last()
                        val miss = abs(last.x - targetTank.x)
                        if (miss < minMissDist) {
                            minMissDist = miss
                            tunedAngle = aTest
                            tunedPower = pTest
                        }
                    }
                }
            }
            finalAngle = tunedAngle
            finalPower = tunedPower
        }

        // Pick weapon
        val weapon = selectWeapon(aiTank, targetTank, difficulty)

        // Decide if AI should reposition slightly if in a deep hole or has fuel
        val shouldMove = if (aiTank.fuel > 30f && rand.nextFloat() < 0.35f) {
            if (isTargetToRight && aiTank.x > 80f) -1 else 1
        } else {
            0
        }

        return AiDecision(
            angle = finalAngle,
            power = finalPower,
            weaponType = weapon,
            shouldMove = shouldMove
        )
    }

    private fun selectWeapon(aiTank: Tank, targetTank: Tank, difficulty: AiDifficulty): WeaponType {
        val rand = Random.Default
        val hasDeathsHead = (aiTank.inventory[WeaponType.DEATHS_HEAD] ?: 0) > 0
        val hasNuke = (aiTank.inventory[WeaponType.NUKE] ?: 0) > 0 || (aiTank.inventory[WeaponType.BABY_NUKE] ?: 0) > 0
        val hasMirv = (aiTank.inventory[WeaponType.MIRV] ?: 0) > 0
        val hasBouncer = (aiTank.inventory[WeaponType.BOUNCER] ?: 0) > 0
        val hasDigger = (aiTank.inventory[WeaponType.DIGGER] ?: 0) > 0

        if (difficulty == AiDifficulty.RECRUIT) {
            return WeaponType.BABY_MISSILE
        }

        // Death's Head or Nuke for high health/shields
        if (hasDeathsHead && targetTank.health > 70 && rand.nextFloat() < 0.7f) {
            return WeaponType.DEATHS_HEAD
        }
        if (hasNuke && (targetTank.health > 60 || targetTank.isShieldActive) && rand.nextFloat() < 0.6f) {
            return if ((aiTank.inventory[WeaponType.NUKE] ?: 0) > 0) WeaponType.NUKE else WeaponType.BABY_NUKE
        }

        // Long distance: MIRV
        if (hasMirv && abs(targetTank.x - aiTank.x) > 350f && rand.nextFloat() < 0.5f) {
            return WeaponType.MIRV
        }

        // Target in a hole: Digger
        if (hasDigger && targetTank.y > aiTank.y + 40f && rand.nextFloat() < 0.5f) {
            return WeaponType.DIGGER
        }

        if (hasBouncer && rand.nextFloat() < 0.35f) {
            return WeaponType.BOUNCER
        }

        return WeaponType.BABY_MISSILE
    }
}
