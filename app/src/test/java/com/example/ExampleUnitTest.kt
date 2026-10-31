package com.example

import com.example.model.Projectile
import com.example.model.WeaponType
import com.example.physics.BallisticsEngine
import com.example.physics.TerrainEngine
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTerrainEngineGenerationAndCrater() {
        val terrain = TerrainEngine(pointCount = 200, worldWidth = 600f, worldHeight = 400f)
        terrain.generate(seed = 42L)

        val initialY = terrain.getHeightAt(300f)
        assertTrue("Terrain height should be within bounds", initialY > 50f && initialY < 380f)

        // Carve crater at center
        val affected = terrain.carveCrater(300f, initialY, radius = 30f)
        assertTrue("Terrain should be affected by crater", affected)

        val newY = terrain.getHeightAt(300f)
        assertTrue("Crater should lower terrain (greater Y)", newY >= initialY)
    }

    @Test
    fun testBallisticsTrajectoryWithWind() {
        val ballistics = BallisticsEngine(gravity = 250f)
        val terrain = TerrainEngine(pointCount = 100, worldWidth = 500f, worldHeight = 300f)
        terrain.generate(seed = 1L)

        // Trajectory with 0 wind
        val calmTraj = ballistics.simulateTrajectory(
            startX = 50f,
            startY = 200f,
            angleDeg = 45f,
            power = 50f,
            windSpeed = 0f,
            isHyper = false,
            terrain = terrain
        )

        // Trajectory with strong tailwind (+60)
        val windTraj = ballistics.simulateTrajectory(
            startX = 50f,
            startY = 200f,
            angleDeg = 45f,
            power = 50f,
            windSpeed = 60f,
            isHyper = false,
            terrain = terrain
        )

        assertTrue(calmTraj.isNotEmpty())
        assertTrue(windTraj.isNotEmpty())
        // Tailwind should carry projectile further right
        assertTrue("Tailwind should carry further right", windTraj.last().x > calmTraj.last().x)
    }

    @Test
    fun testScorchedEarthArsenalIntegrity() {
        val weapons = WeaponType.values()
        assertTrue("Should have at least 10 weapons", weapons.size >= 10)

        assertNotNull(WeaponType.valueOf("BABY_MISSILE"))
        assertNotNull(WeaponType.valueOf("MISSILE"))
        assertNotNull(WeaponType.valueOf("BABY_NUKE"))
        assertNotNull(WeaponType.valueOf("NUKE"))
        assertNotNull(WeaponType.valueOf("MIRV"))
        assertNotNull(WeaponType.valueOf("DEATHS_HEAD"))
        assertNotNull(WeaponType.valueOf("BOUNCER"))
        assertNotNull(WeaponType.valueOf("ROLLER"))
        assertNotNull(WeaponType.valueOf("DIGGER"))
        assertNotNull(WeaponType.valueOf("DIRT_BOMB"))
        assertNotNull(WeaponType.valueOf("NAPALM"))
    }

    @Test
    fun testMirvSplitsAtTopOfFlight() {
        val terrain = TerrainEngine(pointCount = 200, worldWidth = 800f, worldHeight = 500f)
        terrain.generate(seed = 42L)
        val ballistics = BallisticsEngine(gravity = 260f)

        // Launch a MIRV upwards: vy is negative (-50f)
        var mirv = Projectile(
            id = 1,
            ownerId = 0,
            weaponType = WeaponType.MIRV,
            x = 200f,
            y = 200f,
            vx = 50f,
            vy = -50f
        )

        var splitTriggered = false
        var splitProj: Projectile? = null

        for (step in 0 until 50) {
            val next = ballistics.stepProjectile(
                proj = mirv,
                dt = 0.05f,
                windSpeed = 0f,
                terrain = terrain,
                onDetonate = { _, _, _ -> },
                onSplitMirv = { p ->
                    splitTriggered = true
                    splitProj = p
                }
            )
            if (next == null) break
            mirv = next
        }

        assertTrue("MIRV should trigger split at top of flight", splitTriggered)
        assertNotNull(splitProj)
    }
}
