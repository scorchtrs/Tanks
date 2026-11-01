package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.model.FloatingText
import com.example.model.Particle
import com.example.model.PowerUpCrate
import com.example.model.PowerUpType
import com.example.model.Projectile
import com.example.model.Tank
import com.example.model.WeaponType
import com.example.physics.TerrainEngine
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun GameCanvas(
    terrain: TerrainEngine,
    tanks: List<Tank>,
    activeTankIndex: Int,
    powerUpCrates: List<PowerUpCrate>,
    projectiles: List<Projectile>,
    particles: List<Particle>,
    floatingTexts: List<FloatingText>,
    trajectoryPoints: List<Offset>,
    windSpeed: Float,
    isFiring: Boolean,
    onAimChange: (Float, Float) -> Unit, // angle, power
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_battlefield_canvas")
            .pointerInput(isFiring, activeTankIndex) {
                if (!isFiring) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val touchX = change.position.x
                        val touchY = change.position.y
                        val scaleX = size.width / terrain.worldWidth
                        val scaleY = size.height / terrain.worldHeight

                        val activeTank = tanks.getOrNull(activeTankIndex) ?: return@detectDragGestures
                        val tankCanvasX = activeTank.x * scaleX
                        val tankCanvasY = activeTank.y * scaleY

                        val dx = touchX - tankCanvasX
                        val dy = -(touchY - tankCanvasY) // invert Y for screen coords

                        var deg = atan2(dy, dx) * (180f / PI.toFloat())
                        if (deg < 0) deg += 360f

                        if (deg in 0f..180f) {
                            val dist = sqrt(dx * dx + dy * dy)
                            val power = (dist / 2.5f).coerceIn(10f, 100f)
                            onAimChange(deg, power)
                        }
                    }
                }
            }
    ) {
        val scaleX = size.width / terrain.worldWidth
        val scaleY = size.height / terrain.worldHeight

        // 1. Draw Layered Sky
        drawSky(windSpeed)

        // 2. Draw Distant Mountains
        drawDistantMountains(scaleX, scaleY)

        // 3. Draw Deformed Terrain
        drawTerrain(terrain, scaleX, scaleY)

        // 4. Draw Trajectory Guide (if active)
        if (trajectoryPoints.isNotEmpty()) {
            drawTrajectoryGuide(trajectoryPoints, scaleX, scaleY)
        }

        // 5. Draw Power-Up Crates
        for (crate in powerUpCrates) {
            drawCrate(crate, scaleX, scaleY)
        }

        // 6. Draw Tanks
        for (i in tanks.indices) {
            val tank = tanks[i]
            val isActive = i == activeTankIndex
            drawTank(tank, isActive, scaleX, scaleY)
        }

        // 7. Draw Active Projectiles & Trails
        for (proj in projectiles) {
            drawProjectile(proj, scaleX, scaleY)
        }

        // 8. Draw Explosions & Particles
        for (part in particles) {
            drawParticle(part, scaleX, scaleY)
        }

        // 9. Draw Floating Combat Text
        for (ft in floatingTexts) {
            drawFloatingText(ft, scaleX, scaleY)
        }
    }
}

private fun DrawScope.drawSky(windSpeed: Float) {
    // Scorched Earth sunset / twilight atmosphere
    val skyGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Deep Night Slate
            Color(0xFF1E1B4B), // Indigo Horizon
            Color(0xFF4C1D95), // Twilight Purple
            Color(0xFF831843), // Scorched Crimson
            Color(0xFFC2410C), // Dusk Orange
            Color(0xFFF59E0B)  // Golden Horizon glow
        ),
        startY = 0f,
        endY = size.height * 0.85f
    )
    drawRect(brush = skyGradient)

    // Sun / Moon on horizon
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0x00FEF08A)),
            center = Offset(size.width * 0.78f, size.height * 0.28f),
            radius = 55f
        ),
        radius = 55f,
        center = Offset(size.width * 0.78f, size.height * 0.28f)
    )

    // Distant drifting wind particles/clouds
    val windDir = if (windSpeed >= 0) 1f else -1f
    val windIntensity = (kotlin.math.abs(windSpeed) / 100f).coerceIn(0.1f, 1f)
    for (i in 0 until 12) {
        val cx = ((i * 140f + (System.currentTimeMillis() % 100000) * 0.04f * windDir * windIntensity) % (size.width + 200f)) - 100f
        val cy = (i * 35f) % (size.height * 0.35f) + 30f
        val cloudAlpha = 0.12f + (i % 3) * 0.05f
        drawCircle(
            color = Color.White.copy(alpha = cloudAlpha),
            radius = 28f + (i % 4) * 8f,
            center = Offset(cx, cy)
        )
    }
}

private fun DrawScope.drawDistantMountains(scaleX: Float, scaleY: Float) {
    val mountainPath = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, size.height * 0.48f)
        lineTo(size.width * 0.18f, size.height * 0.38f)
        lineTo(size.width * 0.35f, size.height * 0.52f)
        lineTo(size.width * 0.55f, size.height * 0.34f)
        lineTo(size.width * 0.75f, size.height * 0.46f)
        lineTo(size.width * 0.90f, size.height * 0.36f)
        lineTo(size.width, size.height * 0.50f)
        lineTo(size.width, size.height)
        close()
    }
    drawPath(mountainPath, color = Color(0x381E1035))
}

private fun DrawScope.drawTerrain(terrain: TerrainEngine, scaleX: Float, scaleY: Float) {
    val path = Path()
    path.moveTo(0f, size.height)

    val firstY = terrain.heights[0] * scaleY
    path.lineTo(0f, firstY)

    for (i in 1 until terrain.pointCount) {
        val x = i * terrain.stepX * scaleX
        val y = terrain.heights[i] * scaleY
        path.lineTo(x, y)
    }

    path.lineTo(size.width, size.height)
    path.close()

    // Multi-layered dirt strata gradient
    val terrainBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF4D7C0F), // Surface Moss / Grass Green
            Color(0xFF78350F), // Soil Brown
            Color(0xFF451A03), // Dark Earth
            Color(0xFF1C1917)  // Deep Bedrock Slate
        ),
        startY = size.height * 0.25f,
        endY = size.height
    )
    drawPath(path, brush = terrainBrush)

    // Vibrant green grass top line
    val strokePath = Path()
    strokePath.moveTo(0f, firstY)
    for (i in 1 until terrain.pointCount) {
        val x = i * terrain.stepX * scaleX
        val y = terrain.heights[i] * scaleY
        strokePath.lineTo(x, y)
    }
    drawPath(
        strokePath,
        color = Color(0xFF84CC16), // Bright Lime grass highlight
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawTank(tank: Tank, isActive: Boolean, scaleX: Float, scaleY: Float) {
    if (tank.isDestroyed) {
        // Destroyed burning chassis
        val cx = tank.x * scaleX
        val cy = tank.y * scaleY
        drawCircle(color = Color(0xFF27272A), radius = 12f, center = Offset(cx, cy - 4f))
        drawCircle(color = Color(0xFFFF5722), radius = 6f, center = Offset(cx, cy - 8f))
        return
    }

    val cx = tank.x * scaleX
    val cy = tank.y * scaleY

    // 1. Force Shield bubble (scaled 10% bigger)
    if (tank.isShieldActive) {
        val pulse = (sin(System.currentTimeMillis() * 0.008) * 3f).toFloat()
        val shieldR = 37.5f + pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x3300E5FF), Color(0x6600E5FF), Color(0xCC00E5FF)),
                center = Offset(cx, cy - 13.5f),
                radius = shieldR
            ),
            radius = shieldR,
            center = Offset(cx, cy - 13.5f)
        )
        drawCircle(
            color = Color(0xFF00E5FF),
            radius = shieldR,
            center = Offset(cx, cy - 13.5f),
            style = Stroke(width = 2f)
        )
    }

    // 2. Double Damage Fiery Aura (scaled 10% bigger)
    if (tank.hasDoubleDamage) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x55FF1744), Color(0x00FF1744)),
                center = Offset(cx, cy - 15.5f),
                radius = 46f
            ),
            radius = 46f,
            center = Offset(cx, cy - 15.5f)
        )
    }

    // Tank body tilted according to ground slope (10% bigger chassis and treads)
    rotate(degrees = tank.tiltAngle, pivot = Offset(cx, cy)) {
        // Treads (black rounded box, +10% bigger: 41.8 x 11)
        drawRoundRect(
            color = Color(0xFF18181B),
            topLeft = Offset(cx - 20.9f, cy - 7.7f),
            size = Size(41.8f, 11f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.4f, 4.4f)
        )
        // Road wheels (+10% bigger)
        for (w in listOf(-13.2f, -4.4f, 4.4f, 13.2f)) {
            drawCircle(color = Color(0xFF52525B), radius = 3.85f, center = Offset(cx + w, cy - 2.2f))
        }

        // Main Hull Chassis (+10% bigger: 33 x 10)
        drawRoundRect(
            color = tank.color,
            topLeft = Offset(cx - 16.5f, cy - 16.5f),
            size = Size(33f, 10f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.75f, 2.75f)
        )

        // Turret Dome (+10% bigger)
        drawCircle(
            color = tank.color.copy(alpha = 0.9f),
            radius = 7.7f,
            center = Offset(cx, cy - 16.5f)
        )
    }

    // Cannon Barrel rotated to aimAngle (+10% bigger barrel: len 24.2)
    val barrelRad = tank.aimAngle * (PI.toFloat() / 180f)
    val barrelLen = 24.2f
    val turretCenter = Offset(cx, cy - 16.5f)
    val barrelEnd = Offset(
        cx + cos(barrelRad) * barrelLen,
        cy - 16.5f - sin(barrelRad) * barrelLen
    )
    drawLine(
        color = if (tank.hasDoubleDamage) Color(0xFFFF1744) else Color(0xFFE4E4E7),
        start = turretCenter,
        end = barrelEnd,
        strokeWidth = if (tank.hasDoubleDamage) 5.5f else 4.4f,
        cap = StrokeCap.Round
    )

    // Player Overhead HUD (Health bar, Shield bar, Name, Active Turn indicator, +10% larger)
    val hudY = cy - 40f

    // Active arrow beacon
    if (isActive) {
        val bounce = (sin(System.currentTimeMillis() * 0.01) * 4f).toFloat()
        val arrowY = hudY - 15f + bounce
        val arrowPath = Path().apply {
            moveTo(cx, arrowY + 7f)
            lineTo(cx - 6f, arrowY)
            lineTo(cx + 6f, arrowY)
            close()
        }
        val beaconColor = if (!tank.isHuman) Color(0xFFFF5252) else tank.color
        drawPath(arrowPath, color = beaconColor)
    }

    // Health Bar Background (+10% bigger: 48.4 x 5.5)
    val barWidth = 48.4f
    val barHeight = 5.5f
    val barX = cx - barWidth / 2f
    drawRoundRect(
        color = Color(0xFF27272A),
        topLeft = Offset(barX, hudY),
        size = Size(barWidth, barHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.2f, 2.2f)
    )
    // Health Bar Fill
    val hpFrac = (tank.health / 100f).coerceIn(0f, 1f)
    val hpColor = when {
        hpFrac > 0.5f -> Color(0xFF22C55E) // Green
        hpFrac > 0.25f -> Color(0xFFEAB308) // Amber
        else -> Color(0xFFEF4444) // Red
    }
    drawRoundRect(
        color = hpColor,
        topLeft = Offset(barX, hudY),
        size = Size(barWidth * hpFrac, barHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.2f, 2.2f)
    )

    // Shield Bar (thin blue bar over health)
    if (tank.shield > 0) {
        val shieldFrac = (tank.shield / 100f).coerceIn(0f, 1f)
        drawRoundRect(
            color = Color(0xFF00E5FF),
            topLeft = Offset(barX, hudY - 3f),
            size = Size(barWidth * shieldFrac, 2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f, 1f)
        )
    }
}

private fun DrawScope.drawCrate(crate: PowerUpCrate, scaleX: Float, scaleY: Float) {
    val cx = crate.x * scaleX
    val cy = crate.y * scaleY

    // Parachute if falling
    if (crate.isParachuting) {
        val chuteY = cy - 28f
        val chuteW = 28f
        val chuteH = 16f
        val sway = (sin((crate.x + System.currentTimeMillis() * 0.005f)) * 3f).toFloat()

        // Suspension ropes
        drawLine(color = Color(0xFFD4D4D8), start = Offset(cx - chuteW / 2 + sway, chuteY), end = Offset(cx - 6f, cy - 7f), strokeWidth = 1f)
        drawLine(color = Color(0xFFD4D4D8), start = Offset(cx + chuteW / 2 + sway, chuteY), end = Offset(cx + 6f, cy - 7f), strokeWidth = 1f)
        drawLine(color = Color(0xFFD4D4D8), start = Offset(cx + sway, chuteY - chuteH), end = Offset(cx, cy - 7f), strokeWidth = 1f)

        // Parachute canopy
        val canopyPath = Path().apply {
            moveTo(cx - chuteW / 2 + sway, chuteY)
            quadraticTo(cx + sway, chuteY - chuteH * 1.5f, cx + chuteW / 2 + sway, chuteY)
            close()
        }
        drawPath(canopyPath, color = Color(0xFFF43F5E))
    }

    // Glowing PowerUp Crate box
    val crateSize = 18f
    val boxX = cx - crateSize / 2
    val boxY = cy - crateSize / 2

    // Outer glow
    drawCircle(
        color = Color(crate.type.color).copy(alpha = 0.35f),
        radius = 16f,
        center = Offset(cx, cy)
    )

    // Crate Body
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(boxX, boxY),
        size = Size(crateSize, crateSize),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
    )
    drawRoundRect(
        color = Color(crate.type.color),
        topLeft = Offset(boxX, boxY),
        size = Size(crateSize, crateSize),
        style = Stroke(width = 2f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
    )

    // Center icon emblem
    drawCircle(
        color = Color(crate.type.color),
        radius = 4f,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawProjectile(proj: Projectile, scaleX: Float, scaleY: Float) {
    val px = proj.x * scaleX
    val py = proj.y * scaleY

    // Smoke Trail
    if (proj.trail.isNotEmpty()) {
        val trailPath = Path()
        val first = proj.trail.first()
        trailPath.moveTo(first.x * scaleX, first.y * scaleY)
        for (i in 1 until proj.trail.size) {
            val pt = proj.trail[i]
            trailPath.lineTo(pt.x * scaleX, pt.y * scaleY)
        }
        drawPath(
            trailPath,
            color = Color(proj.weaponType.color).copy(alpha = 0.5f),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )
    }

    // Projectile Head
    val radius = when (proj.weaponType) {
        WeaponType.NUKE -> 8f
        WeaponType.BABY_NUKE -> 6.5f
        WeaponType.DEATHS_HEAD -> if (proj.isSubmunition) 5f else 8f
        WeaponType.MIRV -> if (proj.isSubmunition) 3.5f else 5.5f
        WeaponType.BABY_MISSILE -> 3.8f
        else -> 4.5f
    }
    drawCircle(
        color = if (proj.isDoubleDamage) Color(0xFFFF1744) else Color(proj.weaponType.color),
        radius = radius,
        center = Offset(px, py)
    )
    drawCircle(
        color = Color.White,
        radius = radius * 0.5f,
        center = Offset(px, py)
    )
}

private fun DrawScope.drawParticle(part: Particle, scaleX: Float, scaleY: Float) {
    val px = part.x * scaleX
    val py = part.y * scaleY
    val alpha = (part.currentLife / part.maxLife).coerceIn(0f, 1f)
    val r = part.radius * alpha

    drawCircle(
        color = part.color.copy(alpha = alpha),
        radius = r.coerceAtLeast(1f),
        center = Offset(px, py)
    )
}

private fun DrawScope.drawTrajectoryGuide(points: List<Offset>, scaleX: Float, scaleY: Float) {
    val path = Path()
    if (points.isEmpty()) return
    path.moveTo(points.first().x * scaleX, points.first().y * scaleY)
    for (i in 1 until points.size) {
        path.lineTo(points[i].x * scaleX, points[i].y * scaleY)
    }
    drawPath(
        path = path,
        color = Color(0xFFD500F9).copy(alpha = 0.8f),
        style = Stroke(
            width = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
        )
    )
    // Target beacon at end of tracer
    val last = points.last()
    drawCircle(
        color = Color(0xFFD500F9),
        radius = 6f,
        center = Offset(last.x * scaleX, last.y * scaleY),
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.drawFloatingText(ft: FloatingText, scaleX: Float, scaleY: Float) {
    val px = ft.x * scaleX
    val py = ft.y * scaleY
    val alpha = (ft.currentLife / ft.maxLife).coerceIn(0f, 1f)

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (alpha * 255).toInt(),
                (ft.color.red * 255).toInt(),
                (ft.color.green * 255).toInt(),
                (ft.color.blue * 255).toInt()
            )
            textSize = 28f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
            setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)
        }
        drawText(ft.text, px, py, paint)
    }
}
