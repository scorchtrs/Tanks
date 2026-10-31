package com.example.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

enum class WeaponType(
    val displayName: String,
    val description: String,
    val cost: Int,
    val baseDamage: Int,
    val blastRadius: Float,
    val speedMultiplier: Float,
    val burstCount: Int,
    val color: Long,
    val isDefault: Boolean = false
) {
    BABY_MISSILE(
        displayName = "Baby Missile",
        description = "Standard free artillery shell. Dependable basic ordnance with medium blast.",
        cost = 0,
        baseDamage = 30,
        blastRadius = 32f,
        speedMultiplier = 1.0f,
        burstCount = 1,
        color = 0xFFFFB300, // Amber
        isDefault = true
    ),
    MISSILE(
        displayName = "Missile",
        description = "Upgraded tactical missile with enhanced explosive yield.",
        cost = 100,
        baseDamage = 45,
        blastRadius = 42f,
        speedMultiplier = 1.05f,
        burstCount = 1,
        color = 0xFFFF9800 // Orange
    ),
    BABY_NUKE(
        displayName = "Baby Nuke",
        description = "Compact nuclear warhead carving a large crater with heavy fallout.",
        cost = 240,
        baseDamage = 70,
        blastRadius = 65f,
        speedMultiplier = 0.9f,
        burstCount = 1,
        color = 0xFFFF5722 // Red-Orange
    ),
    NUKE(
        displayName = "Nuke",
        description = "Massive nuclear warhead producing a giant crater and devastating blast wave.",
        cost = 450,
        baseDamage = 95,
        blastRadius = 90f,
        speedMultiplier = 0.8f,
        burstCount = 1,
        color = 0xFFFF1744 // Bright Red
    ),
    MIRV(
        displayName = "MIRV",
        description = "Multiple warhead shell that breaks up into 3 projectiles at the top of its flight.",
        cost = 260,
        baseDamage = 35, // x3
        blastRadius = 35f,
        speedMultiplier = 1.0f,
        burstCount = 1,
        color = 0xFF00E5FF // Cyan
    ),
    DEATHS_HEAD(
        displayName = "Death's Head",
        description = "The ultimate Scorched Earth superweapon. Splits into 6 mini-nukes!",
        cost = 650,
        baseDamage = 45, // x6
        blastRadius = 48f,
        speedMultiplier = 0.88f,
        burstCount = 1,
        color = 0xFFD500F9 // Magenta/Skull Purple
    ),
    BOUNCER(
        displayName = "Bouncer",
        description = "High-elasticity shell that ricochets up to 3 times off hillsides.",
        cost = 180,
        baseDamage = 45,
        blastRadius = 40f,
        speedMultiplier = 1.15f,
        burstCount = 1,
        color = 0xFF76FF03 // Lime
    ),
    ROLLER(
        displayName = "Roller",
        description = "Shell drops to earth and rolls down slopes into the lowest valley.",
        cost = 190,
        baseDamage = 50,
        blastRadius = 44f,
        speedMultiplier = 1.0f,
        burstCount = 1,
        color = 0xFFE040FB // Neon Violet
    ),
    DIGGER(
        displayName = "Digger",
        description = "Burrows straight into the mountainside before detonating underground.",
        cost = 210,
        baseDamage = 55,
        blastRadius = 48f,
        speedMultiplier = 1.05f,
        burstCount = 1,
        color = 0xFFFFD600 // Yellow
    ),
    DIRT_BOMB(
        displayName = "Dirt Bomb",
        description = "Detonates into an enormous mound of earth, burying enemy tanks.",
        cost = 130,
        baseDamage = 15,
        blastRadius = 60f,
        speedMultiplier = 0.95f,
        burstCount = 1,
        color = 0xFF8D6E63 // Earth Brown
    ),
    NAPALM(
        displayName = "Napalm",
        description = "Incendiary chemical jelly that flows and ignites along terrain contours.",
        cost = 270,
        baseDamage = 50,
        blastRadius = 50f,
        speedMultiplier = 1.0f,
        burstCount = 1,
        color = 0xFFFF6E40 // Fiery Coral
    )
}

enum class PowerUpType(
    val displayName: String,
    val description: String,
    val color: Long,
    val durationTurns: Int
) {
    SHIELD_GENERATOR(
        displayName = "Force Shield",
        description = "Generates a +75 HP energy dome absorbing incoming damage.",
        color = 0xFF00E5FF, // Cyan
        durationTurns = 3
    ),
    DOUBLE_DAMAGE(
        displayName = "Double Damage",
        description = "Infuses your cannon with plasma for 2x damage on next shots.",
        color = 0xFFFF1744, // Red
        durationTurns = 2
    ),
    SUPER_PROPULSION(
        displayName = "Hyper Velocity",
        description = "Projectiles travel with extreme velocity and ignore half of wind drift.",
        color = 0xFFFFEA00, // Electric Yellow
        durationTurns = 2
    ),
    MOBILITY_OVERDRIVE(
        displayName = "Fuel Boost",
        description = "Instantly replenishes 100% fuel for tactical repositioning.",
        color = 0xFF00E676, // Bright Green
        durationTurns = 1
    ),
    REPAIR_KIT(
        displayName = "Nanite Repair",
        description = "Instantly repairs +45 Hull Armor.",
        color = 0xFF2979FF, // Royal Blue
        durationTurns = 0
    ),
    LASER_GUIDE(
        displayName = "Trajectory Tracer",
        description = "Projects a precision ballistic trajectory dotted line before firing.",
        color = 0xFFD500F9, // Magenta
        durationTurns = 2
    ),
    AIR_STRIKE(
        displayName = "Air Strike Beacon",
        description = "Calls down 3 heavy mortar shells over enemy coordinates.",
        color = 0xFFFF9100, // Neon Orange
        durationTurns = 0
    )
}

data class ActiveBuff(
    val type: PowerUpType,
    val turnsRemaining: Int,
    val value: Float = 1f
)

data class Tank(
    val id: Int,
    val name: String,
    val isHuman: Boolean,
    val color: Color,
    val x: Float,
    val y: Float = 0f,
    val tiltAngle: Float = 0f,
    val aimAngle: Float = 45f, // 0 to 180 degrees
    val aimPower: Float = 55f, // 5 to 100 percent
    val health: Int = 100,
    val maxHealth: Int = 100,
    val shield: Int = 0,
    val maxShield: Int = 100,
    val fuel: Float = 100f,
    val maxFuel: Float = 100f,
    val cash: Int = 200,
    val selectedWeapon: WeaponType = WeaponType.BABY_MISSILE,
    val inventory: Map<WeaponType, Int> = mapOf(
        WeaponType.BABY_MISSILE to 999,
        WeaponType.MISSILE to 3,
        WeaponType.BOUNCER to 2,
        WeaponType.MIRV to 2,
        WeaponType.BABY_NUKE to 1
    ),
    val activeBuffs: List<ActiveBuff> = emptyList(),
    val isFalling: Boolean = false,
    val isDestroyed: Boolean = false
) {
    val isShieldActive: Boolean get() = shield > 0
    val hasDoubleDamage: Boolean get() = activeBuffs.any { it.type == PowerUpType.DOUBLE_DAMAGE && it.turnsRemaining > 0 }
    val hasSuperPropulsion: Boolean get() = activeBuffs.any { it.type == PowerUpType.SUPER_PROPULSION && it.turnsRemaining > 0 }
    val hasLaserGuide: Boolean get() = activeBuffs.any { it.type == PowerUpType.LASER_GUIDE && it.turnsRemaining > 0 }
}

data class PowerUpCrate(
    val id: Long,
    val type: PowerUpType,
    val x: Float,
    val y: Float,
    val vy: Float = 0f,
    val isParachuting: Boolean = true,
    val isGrounded: Boolean = false
)

data class Projectile(
    val id: Long,
    val ownerId: Int,
    val weaponType: WeaponType,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val bouncesLeft: Int = 0,
    val isSubmunition: Boolean = false,
    val trail: List<Offset> = emptyList(),
    val digDepth: Float = 0f,
    val isRolling: Boolean = false,
    val isDoubleDamage: Boolean = false,
    val isHyperVelocity: Boolean = false,
    val customData: Float = 0f
)

enum class ParticleType {
    EXPLOSION_FIRE,
    SMOKE,
    SPARK,
    DIRT_CHUNK,
    SHIELD_BURST,
    NAPALM_FIRE,
    POWERUP_BEACON
}

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val radius: Float,
    val maxLife: Float,
    val currentLife: Float = maxLife,
    val type: ParticleType = ParticleType.EXPLOSION_FIRE
)

data class FloatingText(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val color: Color,
    val maxLife: Float = 1.8f,
    val currentLife: Float = 1.8f
)

enum class AiDifficulty(val displayName: String, val errorDegrees: Float, val errorPower: Float) {
    RECRUIT("Recruit (Easy)", 10f, 15f),
    GUNNER("Gunner (Medium)", 4f, 7f),
    SNIPER("Sniper (Hard)", 1.5f, 3f),
    CYBORG("Cyborg (Extreme)", 0.3f, 1f)
}

enum class MatchMode(val displayName: String) {
    VS_AI("Player vs AI"),
    PASS_AND_PLAY("Pass & Play (2P)"),
    CORRESPONDENCE("Correspondence (Share Turn)")
}

enum class WindMode(val displayName: String, val maxSpeed: Float) {
    CALM("Calm", 15f),
    MILD("Breezy", 45f),
    EXTREME("Gale Force", 90f),
    DYNAMIC("Shifting Storm", 75f)
}

data class GameSettings(
    val roundsCount: Int = 3,
    val matchMode: MatchMode = MatchMode.VS_AI,
    val aiDifficulty: AiDifficulty = AiDifficulty.GUNNER,
    val windMode: WindMode = WindMode.DYNAMIC
)
