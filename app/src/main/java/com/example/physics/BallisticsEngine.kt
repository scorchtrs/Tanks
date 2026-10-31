package com.example.physics

import androidx.compose.ui.geometry.Offset
import com.example.model.PowerUpCrate
import com.example.model.Projectile
import com.example.model.Tank
import com.example.model.WeaponType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class BallisticsEngine(
    val gravity: Float = 260f
) {
    /**
     * Advances a projectile by dt seconds.
     * Returns true if projectile is still active, false if it has detonated or left the world.
     */
    fun stepProjectile(
        proj: Projectile,
        dt: Float,
        windSpeed: Float,
        terrain: TerrainEngine,
        onDetonate: (Projectile, Float, Float) -> Unit,
        onSplitMirv: (Projectile) -> Unit
    ): Projectile? {
        val windFactor = if (proj.isHyperVelocity) 0.5f else 1.0f
        val currentVx = proj.vx + (windSpeed * 1.8f * windFactor) * dt
        val currentVy = proj.vy + gravity * dt

        val nextX = proj.x + currentVx * dt
        val nextY = proj.y + currentVy * dt

        // Out of horizontal bounds or dropped below bottom
        if (nextX < -50f || nextX > terrain.worldWidth + 50f || nextY > terrain.worldHeight + 80f) {
            return null
        }

        // Check MIRV / Death's Head split condition: at the top of its flight (apogee where vy crosses 0)
        if ((proj.weaponType == WeaponType.MIRV || proj.weaponType == WeaponType.DEATHS_HEAD) && !proj.isSubmunition) {
            val reachedApogee = (proj.vy <= 0f && currentVy >= 0f) || (proj.vy > 0f && proj.customData > 0.8f)
            val isAboveGround = nextY < terrain.getHeightAt(nextX.coerceIn(0f, terrain.worldWidth)) - 30f
            if (reachedApogee && isAboveGround) {
                onSplitMirv(proj.copy(x = nextX, y = nextY, vx = currentVx, vy = currentVy))
                return null
            }
        }

        val clampedX = nextX.coerceIn(0f, terrain.worldWidth)
        val groundY = terrain.getHeightAt(clampedX)

        // DIGGER mechanic: can burrow underground up to 45px before detonating
        if (proj.weaponType == WeaponType.DIGGER) {
            if (nextY >= groundY) {
                val newDepth = proj.digDepth + sqrt(currentVx * currentVx + currentVy * currentVy) * dt
                if (newDepth >= 45f || nextY >= terrain.worldHeight * 0.96f) {
                    onDetonate(proj, nextX, nextY)
                    return null
                }
                return proj.copy(
                    x = nextX,
                    y = nextY,
                    vx = currentVx * 0.6f,
                    vy = currentVy * 0.6f,
                    digDepth = newDepth,
                    trail = (proj.trail + Offset(nextX, nextY)).takeLast(16)
                )
            }
        }

        // Collision with terrain
        if (nextY >= groundY) {
            if (proj.weaponType == WeaponType.BOUNCER && proj.bouncesLeft > 0) {
                // Bounce off ground
                val slope = terrain.getSlopeAngleAt(clampedX) * (PI.toFloat() / 180f)
                val normalX = -sin(slope)
                val normalY = cos(slope)
                // Reflect velocity
                val dot = currentVx * normalX + currentVy * normalY
                val rx = (currentVx - 2f * dot * normalX) * 0.65f
                val ry = (currentVy - 2f * dot * normalY) * 0.65f
                return proj.copy(
                    x = nextX,
                    y = groundY - 2f,
                    vx = rx,
                    vy = ry,
                    bouncesLeft = proj.bouncesLeft - 1,
                    trail = (proj.trail + Offset(nextX, groundY)).takeLast(16)
                )
            } else if (proj.weaponType == WeaponType.ROLLER) {
                // Rolls along slope
                val slopeAngle = terrain.getSlopeAngleAt(clampedX)
                val slopeRad = slopeAngle * (PI.toFloat() / 180f)
                val rollSpeed = sin(slopeRad) * 180f
                if (kotlin.math.abs(slopeAngle) < 3f || proj.customData > 2.5f) {
                    // Reached valley bottom or rolled for enough time -> detonate
                    onDetonate(proj, nextX, groundY)
                    return null
                }
                val rollVx = cos(slopeRad) * rollSpeed
                val rollNextX = (proj.x + rollVx * dt).coerceIn(0f, terrain.worldWidth)
                val rollNextY = terrain.getHeightAt(rollNextX)
                return proj.copy(
                    x = rollNextX,
                    y = rollNextY - 3f,
                    vx = rollVx,
                    vy = 0f,
                    isRolling = true,
                    customData = proj.customData + dt,
                    trail = (proj.trail + Offset(rollNextX, rollNextY)).takeLast(16)
                )
            } else {
                // Direct detonation on impact
                onDetonate(proj, nextX, groundY)
                return null
            }
        }

        val updatedTrail = if (proj.trail.isEmpty() || (Offset(nextX, nextY) - proj.trail.last()).getDistanceSquared() > 40f) {
            (proj.trail + Offset(nextX, nextY)).takeLast(20)
        } else {
            proj.trail
        }

        return proj.copy(
            x = nextX,
            y = nextY,
            vx = currentVx,
            vy = currentVy,
            trail = updatedTrail
        )
    }

    /**
     * Simulates ballistic trajectory points for LASER_GUIDE powerup preview
     */
    fun simulateTrajectory(
        startX: Float,
        startY: Float,
        angleDeg: Float,
        power: Float,
        windSpeed: Float,
        isHyper: Boolean,
        terrain: TerrainEngine,
        maxSteps: Int = 80,
        stepDt: Float = 0.045f
    ): List<Offset> {
        val points = mutableListOf<Offset>()
        val rad = angleDeg * (PI.toFloat() / 180f)
        val speedMult = if (isHyper) 9.8f else 6.2f
        val velocity = power * speedMult
        var px = startX
        var py = startY
        var pvx = cos(rad) * velocity
        var pvy = -sin(rad) * velocity

        val windFactor = if (isHyper) 0.5f else 1.0f

        points.add(Offset(px, py))

        for (step in 0 until maxSteps) {
            pvx += (windSpeed * 1.8f * windFactor) * stepDt
            pvy += gravity * stepDt
            px += pvx * stepDt
            py += pvy * stepDt

            if (px < 0f || px > terrain.worldWidth || py > terrain.worldHeight) {
                break
            }

            val ground = terrain.getHeightAt(px)
            if (py >= ground) {
                points.add(Offset(px, ground))
                break
            }
            points.add(Offset(px, py))
        }
        return points
    }

    /**
     * Steps a parachuting supply crate down to the ground with slight wind drift.
     */
    fun stepCrate(crate: PowerUpCrate, dt: Float, windSpeed: Float, terrain: TerrainEngine): PowerUpCrate {
        if (crate.isGrounded) {
            // Anchor to terrain surface in case terrain settles
            val groundY = terrain.getHeightAt(crate.x)
            return crate.copy(y = groundY - 14f)
        }

        val descentSpeed = 65f
        val nextX = (crate.x + windSpeed * 0.25f * dt).coerceIn(20f, terrain.worldWidth - 20f)
        val nextY = crate.y + descentSpeed * dt
        val groundY = terrain.getHeightAt(nextX)

        return if (nextY >= groundY - 14f) {
            crate.copy(x = nextX, y = groundY - 14f, isParachuting = false, isGrounded = true)
        } else {
            crate.copy(x = nextX, y = nextY, isParachuting = true, isGrounded = false)
        }
    }
}
