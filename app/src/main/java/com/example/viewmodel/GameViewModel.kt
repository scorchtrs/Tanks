package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ArtilleryAi
import com.example.audio.RetroAudioPlayer
import com.example.correspondence.CorrespondenceManager
import com.example.correspondence.CorrespondenceSlotsManager
import com.example.correspondence.CorrespondenceTurnData
import com.example.correspondence.MariaDbManager
import com.example.correspondence.CraterRecord
import com.example.correspondence.CrateRecord
import com.example.model.ActiveBuff
import com.example.model.AiDifficulty
import com.example.model.FloatingText
import com.example.model.GameSettings
import com.example.model.MatchMode
import com.example.model.Particle
import com.example.model.ParticleType
import com.example.model.PowerUpCrate
import com.example.model.PowerUpType
import com.example.model.Projectile
import com.example.model.Tank
import com.example.model.WeaponType
import com.example.model.WindMode
import com.example.physics.BallisticsEngine
import com.example.physics.TerrainEngine
import com.example.physics.TerrainStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class GameScreenState {
    MENU,
    PLAYING,
    FIRING,
    REPLAYING,
    ROUND_OVER,
    SHOP,
    MATCH_OVER,
    CORRESPONDENCE_SHARE
}

data class ReplayBannerState(
    val shooterName: String,
    val targetName: String,
    val weaponName: String,
    val angle: Float,
    val power: Float,
    val turnNumber: Int
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val terrain = TerrainEngine(pointCount = 500, worldWidth = 1000f, worldHeight = 600f)
    val ballistics = BallisticsEngine()
    val audio = RetroAudioPlayer()
    private val ai = ArtilleryAi(ballistics)

    private val _screenState = MutableStateFlow(GameScreenState.MENU)
    val screenState: StateFlow<GameScreenState> = _screenState.asStateFlow()

    private val _settings = MutableStateFlow(GameSettings())
    val settings: StateFlow<GameSettings> = _settings.asStateFlow()

    private val _tanks = MutableStateFlow<List<Tank>>(emptyList())
    val tanks: StateFlow<List<Tank>> = _tanks.asStateFlow()

    private val _activeTankIndex = MutableStateFlow(0)
    val activeTankIndex: StateFlow<Int> = _activeTankIndex.asStateFlow()

    private val _currentRound = MutableStateFlow(1)
    val currentRound: StateFlow<Int> = _currentRound.asStateFlow()

    private val _roundScores = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val roundScores: StateFlow<Map<Int, Int>> = _roundScores.asStateFlow()

    private val _windSpeed = MutableStateFlow(0f)
    val windSpeed: StateFlow<Float> = _windSpeed.asStateFlow()

    private val _powerUpCrates = MutableStateFlow<List<PowerUpCrate>>(emptyList())
    val powerUpCrates: StateFlow<List<PowerUpCrate>> = _powerUpCrates.asStateFlow()

    private val _projectiles = MutableStateFlow<List<Projectile>>(emptyList())
    val projectiles: StateFlow<List<Projectile>> = _projectiles.asStateFlow()

    private val _particles = MutableStateFlow<List<Particle>>(emptyList())
    val particles: StateFlow<List<Particle>> = _particles.asStateFlow()

    private val _floatingTexts = MutableStateFlow<List<FloatingText>>(emptyList())
    val floatingTexts: StateFlow<List<FloatingText>> = _floatingTexts.asStateFlow()

    private val _trajectoryPoints = MutableStateFlow<List<Offset>>(emptyList())
    val trajectoryPoints: StateFlow<List<Offset>> = _trajectoryPoints.asStateFlow()

    private val _roundWinner = MutableStateFlow<Tank?>(null)
    val roundWinner: StateFlow<Tank?> = _roundWinner.asStateFlow()

    private val _matchWinner = MutableStateFlow<Tank?>(null)
    val matchWinner: StateFlow<Tank?> = _matchWinner.asStateFlow()

    private val _lastTurnCode = MutableStateFlow("")
    val lastTurnCode: StateFlow<String> = _lastTurnCode.asStateFlow()

    val currentMatchId = MutableStateFlow("EARTH-GAME-1")

    private val _currentTurnNumber = MutableStateFlow(1)
    val currentTurnNumber: StateFlow<Int> = _currentTurnNumber.asStateFlow()

    private val _localPlayerIndex = MutableStateFlow(0)
    val localPlayerIndex: StateFlow<Int> = _localPlayerIndex.asStateFlow()

    fun setLocalPlayerIndex(idx: Int) {
        _localPlayerIndex.value = idx.coerceIn(0, 1)
    }

    private val recordedCraters = mutableListOf<CraterRecord>()
    private var lastFiredData: CorrespondenceTurnData? = null
    private var simJob: Job? = null
    private var replayJob: Job? = null
    private var loadedTurnData: CorrespondenceTurnData? = null

    private val _replayState = MutableStateFlow<ReplayBannerState?>(null)
    val replayState: StateFlow<ReplayBannerState?> = _replayState.asStateFlow()

    private var nextCrateId = 1L
    private var nextProjId = 1L
    private var nextTextId = 1L
    private var terrainSeed = 12345L

    val activeTank: Tank?
        get() = _tanks.value.getOrNull(_activeTankIndex.value)

    init {
        startMatch()
        _screenState.value = GameScreenState.MENU
    }

    fun updateSettings(newSettings: GameSettings) {
        _settings.value = newSettings
    }

    fun startMatch() {
        _currentRound.value = 1
        _currentTurnNumber.value = 1
        _roundScores.value = mapOf(0 to 0, 1 to 0)
        _roundWinner.value = null
        _matchWinner.value = null
        recordedCraters.clear()
        startRound(roundNum = 1)
        _screenState.value = GameScreenState.PLAYING
    }

    fun startRound(roundNum: Int) {
        terrainSeed = System.currentTimeMillis()
        val styles = TerrainStyle.values()
        val style = styles[(roundNum - 1) % styles.size]
        terrain.generate(seed = terrainSeed, style = style)

        // Initialize tanks
        val isP2Human = _settings.value.matchMode == MatchMode.PASS_AND_PLAY ||
                _settings.value.matchMode == MatchMode.CORRESPONDENCE

        val p1StartX = 120f
        val p2StartX = 880f

        val p1 = Tank(
            id = 0,
            name = "Player 1",
            isHuman = true,
            color = Color(0xFF00E5FF), // Neon Cyan
            x = p1StartX,
            y = terrain.getHeightAt(p1StartX),
            tiltAngle = terrain.getSlopeAngleAt(p1StartX),
            aimAngle = 45f,
            aimPower = 55f,
            health = 100,
            cash = _tanks.value.getOrNull(0)?.cash ?: 200,
            inventory = _tanks.value.getOrNull(0)?.inventory ?: mapOf(
                WeaponType.BABY_MISSILE to 999,
                WeaponType.MISSILE to 3,
                WeaponType.BOUNCER to 2,
                WeaponType.MIRV to 2,
                WeaponType.BABY_NUKE to 1
            )
        )

        val p2Name = if (isP2Human) "Player 2" else "CPU (${_settings.value.aiDifficulty.displayName.substringBefore(" ")})"
        val p2 = Tank(
            id = 1,
            name = p2Name,
            isHuman = isP2Human,
            color = Color(0xFFFF5252), // Fiery Coral
            x = p2StartX,
            y = terrain.getHeightAt(p2StartX),
            tiltAngle = terrain.getSlopeAngleAt(p2StartX),
            aimAngle = 135f,
            aimPower = 55f,
            health = 100,
            cash = _tanks.value.getOrNull(1)?.cash ?: 200,
            inventory = _tanks.value.getOrNull(1)?.inventory ?: mapOf(
                WeaponType.BABY_MISSILE to 999,
                WeaponType.MISSILE to 3,
                WeaponType.BOUNCER to 2,
                WeaponType.MIRV to 2,
                WeaponType.BABY_NUKE to 1
            )
        )

        _tanks.value = listOf(p1, p2)
        _activeTankIndex.value = 0
        _powerUpCrates.value = emptyList()
        _projectiles.value = emptyList()
        _particles.value = emptyList()
        _floatingTexts.value = emptyList()
        _roundWinner.value = null

        rollWind()
        spawnSupplyCrate(forced = true) // Guaranteed crate at round start for immediate excitement!
        updateTrajectoryGuide()
    }

    private fun rollWind() {
        val mode = _settings.value.windMode
        val maxW = mode.maxSpeed
        val rand = Random.Default
        val raw = (rand.nextFloat() * 2f - 1f) * maxW
        _windSpeed.value = (raw * 10f).toInt() / 10f
    }

    fun setAimAngle(angle: Float) {
        val idx = _activeTankIndex.value
        val list = _tanks.value.toMutableList()
        val t = list.getOrNull(idx) ?: return
        list[idx] = t.copy(aimAngle = angle.coerceIn(0f, 180f))
        _tanks.value = list
        updateTrajectoryGuide()
    }

    fun adjustAimAngle(delta: Float) {
        val t = activeTank ?: return
        audio.playClickSound()
        setAimAngle(t.aimAngle + delta)
    }

    fun setAimPower(power: Float) {
        val idx = _activeTankIndex.value
        val list = _tanks.value.toMutableList()
        val t = list.getOrNull(idx) ?: return
        list[idx] = t.copy(aimPower = power.coerceIn(5f, 100f))
        _tanks.value = list
        updateTrajectoryGuide()
    }

    fun adjustAimPower(delta: Float) {
        val t = activeTank ?: return
        audio.playClickSound()
        setAimPower(t.aimPower + delta)
    }

    fun selectWeapon(weapon: WeaponType) {
        val idx = _activeTankIndex.value
        val list = _tanks.value.toMutableList()
        val t = list.getOrNull(idx) ?: return
        val count = t.inventory[weapon] ?: 0
        if (count > 0 || weapon.isDefault) {
            audio.playClickSound()
            list[idx] = t.copy(selectedWeapon = weapon)
            _tanks.value = list
        }
    }

    /**
     * Move tank horizontally along terrain, consuming fuel.
     */
    fun driveTank(direction: Int) {
        val idx = _activeTankIndex.value
        val list = _tanks.value.toMutableList()
        val t = list.getOrNull(idx) ?: return
        if (t.fuel <= 0f || _screenState.value != GameScreenState.PLAYING) return

        val speed = 3.5f
        val newX = (t.x + direction * speed).coerceIn(40f, terrain.worldWidth - 40f)
        val newY = terrain.getHeightAt(newX)
        val newTilt = terrain.getSlopeAngleAt(newX)
        val newFuel = (t.fuel - 1.2f).coerceAtLeast(0f)

        list[idx] = t.copy(x = newX, y = newY, tiltAngle = newTilt, fuel = newFuel)
        _tanks.value = list
        checkCrateCollisionsWithTank(list[idx])
        updateTrajectoryGuide()
    }

    private fun checkCrateCollisionsWithTank(tank: Tank) {
        val crates = _powerUpCrates.value.toMutableList()
        val collected = mutableListOf<PowerUpCrate>()
        for (c in crates) {
            val dist = abs(c.x - tank.x)
            if (dist < 26f && abs(c.y - tank.y) < 30f) {
                applyPowerUp(tank.id, c.type)
                collected.add(c)
            }
        }
        if (collected.isNotEmpty()) {
            crates.removeAll(collected)
            _powerUpCrates.value = crates
        }
    }

    fun applyPowerUp(tankId: Int, type: PowerUpType) {
        audio.playPowerUpSound()
        val list = _tanks.value.toMutableList()
        val idx = list.indexOfFirst { it.id == tankId }
        if (idx == -1) return
        val t = list[idx]

        addFloatingText("+${type.displayName.uppercase()}", t.x, t.y - 30f, Color(type.color))

        when (type) {
            PowerUpType.SHIELD_GENERATOR -> {
                audio.playShieldSound()
                val updatedShield = (t.shield + 75).coerceAtMost(100)
                list[idx] = t.copy(shield = updatedShield)
            }
            PowerUpType.REPAIR_KIT -> {
                val updatedHealth = (t.health + 45).coerceAtMost(t.maxHealth)
                list[idx] = t.copy(health = updatedHealth)
            }
            PowerUpType.MOBILITY_OVERDRIVE -> {
                list[idx] = t.copy(fuel = t.maxFuel)
            }
            PowerUpType.AIR_STRIKE -> {
                val enemy = list.firstOrNull { it.id != tankId }
                if (enemy != null) {
                    callAirStrike(enemy.x)
                }
            }
            else -> {
                // Turn-based buffs (Double Damage, Super Propulsion, Laser Guide)
                val newBuffs = t.activeBuffs.filter { it.type != type } + ActiveBuff(type, type.durationTurns)
                list[idx] = t.copy(activeBuffs = newBuffs)
            }
        }
        _tanks.value = list
        updateTrajectoryGuide()
    }

    private fun callAirStrike(targetX: Float) {
        viewModelScope.launch {
            for (i in -1..1) {
                delay(300)
                val proj = Projectile(
                    id = nextProjId++,
                    ownerId = activeTank?.id ?: 0,
                    weaponType = WeaponType.BABY_MISSILE,
                    x = targetX + i * 40f,
                    y = -20f,
                    vx = (Random.nextFloat() * 20f - 10f),
                    vy = 280f
                )
                _projectiles.value = _projectiles.value + proj
                audio.playFireSound()
            }
        }
    }

    fun fireWeapon() {
        if (_screenState.value != GameScreenState.PLAYING) return
        val shooter = activeTank ?: return

        // Consume ammo
        val weapon = shooter.selectedWeapon
        val currentAmmo = shooter.inventory[weapon] ?: 0
        if (!weapon.isDefault && currentAmmo <= 0) return

        if (!weapon.isDefault) {
            val updatedInv = shooter.inventory.toMutableMap()
            updatedInv[weapon] = (currentAmmo - 1).coerceAtLeast(0)
            val updatedList = _tanks.value.toMutableList()
            updatedList[_activeTankIndex.value] = shooter.copy(
                inventory = updatedInv,
                selectedWeapon = if (updatedInv[weapon] == 0) WeaponType.BABY_MISSILE else weapon
            )
            _tanks.value = updatedList
        }

        _screenState.value = GameScreenState.FIRING

        // Projectile launch vector
        val rad = shooter.aimAngle * (PI.toFloat() / 180f)
        val speedMult = (if (shooter.hasSuperPropulsion) 9.8f else 6.2f) * weapon.speedMultiplier
        val barrelLen = 22f
        val startX = shooter.x + cos(rad) * barrelLen
        val startY = shooter.y - 12f - sin(rad) * barrelLen
        val burst = weapon.burstCount

        // Store last fired action for correspondence play
        val t0 = _tanks.value.getOrNull(0)
        val t1 = _tanks.value.getOrNull(1)
        lastFiredData = CorrespondenceTurnData(
            turnNumber = _currentTurnNumber.value,
            roundNumber = _currentRound.value,
            seed = terrainSeed,
            windSpeed = _windSpeed.value,
            activeTankIndex = _activeTankIndex.value,
            p1Health = t0?.health ?: 100,
            p1Shield = t0?.shield ?: 0,
            p1Cash = t0?.cash ?: 200,
            p1X = t0?.x ?: 120f,
            p2Health = t1?.health ?: 100,
            p2Shield = t1?.shield ?: 0,
            p2Cash = t1?.cash ?: 200,
            p2X = t1?.x ?: 880f,
            lastFiredPlayerId = shooter.id,
            lastFiredAngle = shooter.aimAngle,
            lastFiredPower = shooter.aimPower,
            lastWeaponName = weapon.name,
            lastFiredWindSpeed = _windSpeed.value,
            prevP1Health = t0?.health ?: 100,
            prevP1Shield = t0?.shield ?: 0,
            prevP2Health = t1?.health ?: 100,
            prevP2Shield = t1?.shield ?: 0,
            prevCraterCount = recordedCraters.size,
            craters = recordedCraters.toList(),
            crates = _powerUpCrates.value.map { CrateRecord(it.type, it.x, it.isGrounded) }
        )

        viewModelScope.launch {
            for (b in 0 until burst) {
                if (b > 0) {
                    delay(110)
                }
                audio.playFireSound()
                val spreadAngle = shooter.aimAngle + (b - (burst - 1) / 2f) * 1.5f
                val spreadRad = spreadAngle * (PI.toFloat() / 180f)
                val velocity = shooter.aimPower * speedMult
                val proj = Projectile(
                    id = nextProjId++,
                    ownerId = shooter.id,
                    weaponType = weapon,
                    x = startX,
                    y = startY,
                    vx = cos(spreadRad) * velocity,
                    vy = -sin(spreadRad) * velocity,
                    bouncesLeft = if (weapon == WeaponType.BOUNCER) 3 else 0,
                    isDoubleDamage = shooter.hasDoubleDamage,
                    isHyperVelocity = shooter.hasSuperPropulsion
                )
                _projectiles.value = _projectiles.value + proj
            }
            startSimulationLoop()
        }
    }

    private fun startSimulationLoop() {
        simJob?.cancel()
        simJob = viewModelScope.launch {
            val dt = 0.025f
            while (isActive) {
                var hasActiveObjects = false

                // Step crates
                val updatedCrates = _powerUpCrates.value.map { crate ->
                    ballistics.stepCrate(crate, dt, _windSpeed.value, terrain)
                }
                _powerUpCrates.value = updatedCrates

                // Step projectiles
                val curProjs = _projectiles.value
                if (curProjs.isNotEmpty()) {
                    hasActiveObjects = true
                    val nextProjs = mutableListOf<Projectile>()
                    val spawnedFromSplits = mutableListOf<Projectile>()
                    for (p in curProjs) {
                        val stepped = ballistics.stepProjectile(
                            proj = p,
                            dt = dt,
                            windSpeed = _windSpeed.value,
                            terrain = terrain,
                            onDetonate = { detonatedProj, dx, dy ->
                                handleDetonation(detonatedProj, dx, dy)
                            },
                            onSplitMirv = { mirvProj ->
                                val subs = handleMirvSplit(mirvProj)
                                spawnedFromSplits.addAll(subs)
                            }
                        )
                        if (stepped != null) {
                            nextProjs.add(stepped)
                        }
                    }
                    nextProjs.addAll(spawnedFromSplits)
                    _projectiles.value = nextProjs
                }

                // Step particles
                val curParticles = _particles.value
                if (curParticles.isNotEmpty()) {
                    hasActiveObjects = true
                    val nextParticles = curParticles.mapNotNull { part ->
                        val life = part.currentLife - dt
                        if (life <= 0f) null
                        else part.copy(
                            x = part.x + part.vx * dt,
                            y = part.y + part.vy * dt,
                            currentLife = life
                        )
                    }
                    _particles.value = nextParticles
                }

                // Step floating texts
                val curTexts = _floatingTexts.value
                if (curTexts.isNotEmpty()) {
                    val nextTexts = curTexts.mapNotNull { ft ->
                        val life = ft.currentLife - dt
                        if (life <= 0f) null
                        else ft.copy(y = ft.y - 18f * dt, currentLife = life)
                    }
                    _floatingTexts.value = nextTexts
                }

                // Settle tanks if terrain dropped
                settleTanks()

                if (!hasActiveObjects && _projectiles.value.isEmpty()) {
                    // Turn simulation complete
                    delay(500)
                    endTurn()
                    break
                }

                delay(25)
            }
        }
    }

    private fun handleMirvSplit(parent: Projectile): List<Projectile> {
        val isDeathsHead = parent.weaponType == WeaponType.DEATHS_HEAD
        audio.playExplosionSound(isBig = isDeathsHead)
        val count = if (isDeathsHead) 6 else 3
        val submunitions = mutableListOf<Projectile>()
        for (i in 0 until count) {
            val spreadX = if (count == 3) {
                when (i) {
                    0 -> -32f
                    1 -> 0f
                    else -> 32f
                }
            } else {
                -55f + (110f * i / (count - 1).coerceAtLeast(1))
            }
            val spreadY = if (count == 3) {
                when (i) {
                    1 -> 12f // Center warhead drives downward along trajectory
                    else -> 5f // Flank warheads spread outward
                }
            } else {
                8f + (i % 2) * 6f
            }
            submunitions.add(
                Projectile(
                    id = nextProjId++,
                    ownerId = parent.ownerId,
                    weaponType = if (isDeathsHead) WeaponType.DEATHS_HEAD else WeaponType.MIRV,
                    x = parent.x,
                    y = parent.y,
                    vx = parent.vx + spreadX,
                    vy = parent.vy + spreadY,
                    isSubmunition = true,
                    isDoubleDamage = parent.isDoubleDamage,
                    isHyperVelocity = parent.isHyperVelocity
                )
            )
        }
        spawnExplosionParticles(parent.x, parent.y, if (isDeathsHead) 45f else 28f, if (isDeathsHead) Color(0xFFD500F9) else Color(0xFF00E5FF))
        addFloatingText(
            if (isDeathsHead) "DEATH'S HEAD 6x SPLIT!" else "MIRV 3x SPLIT!",
            parent.x,
            parent.y - 18f,
            if (isDeathsHead) Color(0xFFD500F9) else Color(0xFF00E5FF)
        )
        return submunitions
    }

    private fun handleDetonation(proj: Projectile, cx: Float, cy: Float) {
        val wpn = proj.weaponType
        val mult = if (proj.isDoubleDamage) 2f else 1f
        val damage = (wpn.baseDamage * mult).toInt()
        val blastR = wpn.blastRadius * (if (proj.isDoubleDamage) 1.25f else 1.0f)

        audio.playExplosionSound(isBig = wpn == WeaponType.NUKE || wpn == WeaponType.BABY_NUKE || wpn == WeaponType.DEATHS_HEAD || blastR > 60f)

        // Deform terrain
        if (wpn == WeaponType.DIRT_BOMB) {
            terrain.addDirt(cx, cy, blastR, maxAddition = 40f)
            recordedCraters.add(CraterRecord(cx, cy, blastR, isDirt = true))
        } else {
            terrain.carveCrater(cx, cy, blastR)
            recordedCraters.add(CraterRecord(cx, cy, blastR, isDirt = false))
        }

        // Check damage to tanks
        val tanksList = _tanks.value.toMutableList()
        var shooterCashBonus = 0
        for (i in tanksList.indices) {
            val t = tanksList[i]
            val dist = sqrt((t.x - cx) * (t.x - cx) + (t.y - cy) * (t.y - cy))
            if (dist < blastR + 18f) {
                val falloff = (1f - dist / (blastR + 18f)).coerceIn(0.2f, 1f)
                val rawDmg = (damage * falloff).toInt()

                var actualDmg = rawDmg
                var newShield = t.shield
                var newHealth = t.health

                if (newShield > 0) {
                    audio.playShieldSound()
                    val absorbed = minOf(newShield, actualDmg)
                    newShield -= absorbed
                    actualDmg -= absorbed
                    addFloatingText("SHIELD -$absorbed", t.x, t.y - 28f, Color(0xFF00E5FF))
                }

                if (actualDmg > 0) {
                    newHealth = (newHealth - actualDmg).coerceAtLeast(0)
                    addFloatingText("-$actualDmg HP", t.x, t.y - 45f, Color(0xFFFF1744))
                }

                // Cash reward
                if (t.id != proj.ownerId) {
                    shooterCashBonus += rawDmg * 3
                }

                val isDead = newHealth <= 0
                tanksList[i] = t.copy(
                    health = newHealth,
                    shield = newShield,
                    isDestroyed = isDead
                )
            }
        }

        // Credit shooter cash
        val shooterIdx = tanksList.indexOfFirst { it.id == proj.ownerId }
        if (shooterIdx != -1) {
            val s = tanksList[shooterIdx]
            tanksList[shooterIdx] = s.copy(cash = s.cash + shooterCashBonus)
        }
        _tanks.value = tanksList

        // Check crate detonation / blast pickup
        val crates = _powerUpCrates.value.toMutableList()
        val hitCrates = mutableListOf<PowerUpCrate>()
        for (c in crates) {
            val cDist = sqrt((c.x - cx) * (c.x - cx) + (c.y - cy) * (c.y - cy))
            if (cDist < blastR + 15f) {
                applyPowerUp(proj.ownerId, c.type)
                hitCrates.add(c)
            }
        }
        if (hitCrates.isNotEmpty()) {
            crates.removeAll(hitCrates)
            _powerUpCrates.value = crates
        }

        // Spawn particles
        val pColor = when (wpn) {
            WeaponType.NUKE -> Color(0xFFFF1744)
            WeaponType.BABY_NUKE -> Color(0xFFFF5722)
            WeaponType.DEATHS_HEAD -> Color(0xFFD500F9)
            WeaponType.MIRV -> Color(0xFF00E5FF)
            WeaponType.BOUNCER -> Color(0xFF76FF03)
            WeaponType.ROLLER -> Color(0xFFE040FB)
            WeaponType.DIGGER -> Color(0xFFFFD600)
            WeaponType.DIRT_BOMB -> Color(0xFF8D6E63)
            WeaponType.NAPALM -> Color(0xFFFF6E40)
            else -> Color(0xFFFF9800)
        }
        spawnExplosionParticles(cx, cy, blastR, pColor)
    }

    private fun spawnExplosionParticles(cx: Float, cy: Float, radius: Float, color: Color) {
        val list = mutableListOf<Particle>()
        val count = (radius * 0.75f).toInt().coerceIn(18, 65)
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = Random.nextFloat() * (radius * 3.2f) + 20f
            list.add(
                Particle(
                    x = cx,
                    y = cy,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 35f,
                    color = if (i % 3 == 0) Color.White else color,
                    radius = Random.nextFloat() * 6f + 3f,
                    maxLife = Random.nextFloat() * 0.6f + 0.4f,
                    type = ParticleType.EXPLOSION_FIRE
                )
            )
        }
        _particles.value = _particles.value + list
    }

    private fun settleTanks() {
        val list = _tanks.value.toMutableList()
        var changed = false
        for (i in list.indices) {
            val t = list[i]
            val groundY = terrain.getHeightAt(t.x)
            if (t.y < groundY - 1f) {
                // Falling into crater
                val newY = (t.y + 12f).coerceAtMost(groundY)
                list[i] = t.copy(y = newY, tiltAngle = terrain.getSlopeAngleAt(t.x), isFalling = true)
                changed = true
            } else if (t.isFalling) {
                list[i] = t.copy(y = groundY, tiltAngle = terrain.getSlopeAngleAt(t.x), isFalling = false)
                changed = true
            }
        }
        if (changed) {
            _tanks.value = list
        }
    }

    private fun endTurn() {
        // Check if any tank is destroyed
        val survivors = _tanks.value.filter { it.health > 0 }
        if (survivors.size <= 1) {
            val winner = survivors.firstOrNull()
            _roundWinner.value = winner
            if (winner != null) {
                audio.playVictorySound()
                val scores = _roundScores.value.toMutableMap()
                scores[winner.id] = (scores[winner.id] ?: 0) + 1
                _roundScores.value = scores
            }

            // Check match win
            val targetWins = (_settings.value.roundsCount / 2) + 1
            val maxScore = _roundScores.value.values.maxOrNull() ?: 0
            val isGameOver = maxScore >= targetWins || _currentRound.value >= _settings.value.roundsCount || _settings.value.matchMode == MatchMode.CORRESPONDENCE
            if (isGameOver) {
                _matchWinner.value = winner
                if (_settings.value.matchMode == MatchMode.CORRESPONDENCE) {
                    val p1 = _tanks.value.getOrNull(0)
                    val p2 = _tanks.value.getOrNull(1)
                    val winnerName = winner?.name ?: if ((p1?.health ?: 0) > 0) p1?.name ?: "" else p2?.name ?: ""
                    val finalTurnData = CorrespondenceTurnData(
                        turnNumber = _currentTurnNumber.value,
                        roundNumber = _currentRound.value,
                        seed = terrainSeed,
                        windSpeed = _windSpeed.value,
                        activeTankIndex = _activeTankIndex.value,
                        p1Health = p1?.health ?: 0,
                        p1Shield = p1?.shield ?: 0,
                        p1Cash = p1?.cash ?: 0,
                        p1X = p1?.x ?: 120f,
                        p2Health = p2?.health ?: 0,
                        p2Shield = p2?.shield ?: 0,
                        p2Cash = p2?.cash ?: 0,
                        p2X = p2?.x ?: 880f,
                        lastFiredPlayerId = lastFiredData?.lastFiredPlayerId ?: -1,
                        lastFiredAngle = lastFiredData?.lastFiredAngle ?: 0f,
                        lastFiredPower = lastFiredData?.lastFiredPower ?: 0f,
                        lastWeaponName = lastFiredData?.lastWeaponName ?: "",
                        lastFiredWindSpeed = lastFiredData?.lastFiredWindSpeed ?: 0f,
                        prevP1Health = lastFiredData?.prevP1Health ?: (p1?.health ?: 0),
                        prevP1Shield = lastFiredData?.prevP1Shield ?: (p1?.shield ?: 0),
                        prevP2Health = lastFiredData?.prevP2Health ?: (p2?.health ?: 0),
                        prevP2Shield = lastFiredData?.prevP2Shield ?: (p2?.shield ?: 0),
                        prevCraterCount = lastFiredData?.prevCraterCount ?: maxOf(0, recordedCraters.size - 1),
                        craters = recordedCraters.toList(),
                        crates = _powerUpCrates.value.map { CrateRecord(it.type, it.x, it.isGrounded) },
                        isGameOver = true,
                        winnerName = winnerName
                    )
                    val code = CorrespondenceManager.encodeTurn(finalTurnData)
                    _lastTurnCode.value = code
                    _screenState.value = GameScreenState.CORRESPONDENCE_SHARE

                    // Automatically update remote database after game over in correspondence play
                    autoSaveCorrespondenceTurn(finalTurnData, code, isGameOver = true, winnerName = winnerName)
                } else {
                    _screenState.value = GameScreenState.MATCH_OVER
                }
            } else {
                _screenState.value = GameScreenState.ROUND_OVER
            }
            return
        }

        // Advance buffs on current player's tank
        val currentIdx = _activeTankIndex.value
        val list = _tanks.value.toMutableList()
        val currTank = list[currentIdx]
        val updatedBuffs = currTank.activeBuffs.mapNotNull {
            if (it.turnsRemaining <= 1) null
            else it.copy(turnsRemaining = it.turnsRemaining - 1)
        }
        // Refill partial fuel each turn
        val refilledFuel = (currTank.fuel + 40f).coerceAtMost(currTank.maxFuel)
        list[currentIdx] = currTank.copy(activeBuffs = updatedBuffs, fuel = refilledFuel)
        _tanks.value = list

        // Switch active tank
        val nextIdx = (currentIdx + 1) % _tanks.value.size
        _activeTankIndex.value = nextIdx
        _currentTurnNumber.value = _currentTurnNumber.value + 1

        // Dynamic wind shift
        rollWind()

        // Supply crate airdrop check (40% chance if fewer than 3 crates)
        if (Random.nextFloat() < 0.45f && _powerUpCrates.value.size < 3) {
            spawnSupplyCrate(forced = false)
        }

        // If correspondence play, automatically update remote database after each turn and pause for share/sync
        if (_settings.value.matchMode == MatchMode.CORRESPONDENCE) {
            val p1 = _tanks.value.getOrNull(0)
            val p2 = _tanks.value.getOrNull(1)
            val turnData = CorrespondenceTurnData(
                turnNumber = _currentTurnNumber.value,
                roundNumber = _currentRound.value,
                seed = terrainSeed,
                windSpeed = _windSpeed.value,
                activeTankIndex = nextIdx,
                p1Health = p1?.health ?: 100,
                p1Shield = p1?.shield ?: 0,
                p1Cash = p1?.cash ?: 200,
                p1X = p1?.x ?: 120f,
                p2Health = p2?.health ?: 100,
                p2Shield = p2?.shield ?: 0,
                p2Cash = p2?.cash ?: 200,
                p2X = p2?.x ?: 880f,
                lastFiredPlayerId = lastFiredData?.lastFiredPlayerId ?: -1,
                lastFiredAngle = lastFiredData?.lastFiredAngle ?: 0f,
                lastFiredPower = lastFiredData?.lastFiredPower ?: 0f,
                lastWeaponName = lastFiredData?.lastWeaponName ?: "",
                lastFiredWindSpeed = lastFiredData?.lastFiredWindSpeed ?: 0f,
                prevP1Health = lastFiredData?.prevP1Health ?: (p1?.health ?: 100),
                prevP1Shield = lastFiredData?.prevP1Shield ?: (p1?.shield ?: 0),
                prevP2Health = lastFiredData?.prevP2Health ?: (p2?.health ?: 100),
                prevP2Shield = lastFiredData?.prevP2Shield ?: (p2?.shield ?: 0),
                prevCraterCount = lastFiredData?.prevCraterCount ?: maxOf(0, recordedCraters.size - 1),
                craters = recordedCraters.toList(),
                crates = _powerUpCrates.value.map { CrateRecord(it.type, it.x, it.isGrounded) },
                isGameOver = false,
                winnerName = ""
            )
            val code = CorrespondenceManager.encodeTurn(turnData)
            _lastTurnCode.value = code
            _screenState.value = GameScreenState.CORRESPONDENCE_SHARE

            // Automatically update remote database after each turn
            autoSaveCorrespondenceTurn(turnData, code, isGameOver = false, winnerName = "")
            return
        }

        _screenState.value = GameScreenState.PLAYING
        updateTrajectoryGuide()

        // If next player is AI, trigger AI turn
        val nextTank = _tanks.value[nextIdx]
        if (!nextTank.isHuman) {
            triggerAiTurn(nextTank)
        }
    }

    private fun triggerAiTurn(aiTank: Tank) {
        viewModelScope.launch {
            delay(800)
            val enemy = _tanks.value.firstOrNull { it.id != aiTank.id } ?: return@launch
            val decision = ai.calculateShot(
                aiTank = aiTank,
                targetTank = enemy,
                windSpeed = _windSpeed.value,
                terrain = terrain,
                difficulty = _settings.value.aiDifficulty
            )

            // AI movement
            if (decision.shouldMove != 0) {
                for (s in 0 until 6) {
                    driveTank(decision.shouldMove)
                    delay(80)
                }
            }

            // Smoothly dial aim angle & power
            selectWeapon(decision.weaponType)
            setAimAngle(decision.angle)
            setAimPower(decision.power)
            delay(900)

            fireWeapon()
        }
    }

    private fun autoSaveCorrespondenceTurn(
        turnData: CorrespondenceTurnData,
        turnCode: String,
        isGameOver: Boolean,
        winnerName: String
    ) {
        val appContext = getApplication<Application>()
        viewModelScope.launch {
            val p1 = _tanks.value.getOrNull(0)?.name ?: "Player 1"
            val p2 = _tanks.value.getOrNull(1)?.name ?: "Player 2"
            val matchId = currentMatchId.value.trim().ifBlank { "EARTH-GAME-1" }
            Log.d("GameViewModel", "Auto-saving correspondence turn #${turnData.turnNumber} to $matchId (gameOver=$isGameOver)")
            val result = MariaDbManager.saveTurn(
                context = appContext,
                matchId = matchId,
                turnData = turnData,
                turnCode = turnCode,
                p1Name = p1,
                p2Name = p2,
                isGameOver = isGameOver,
                winnerName = winnerName
            )
            result.fold(
                onSuccess = {
                    Log.i("GameViewModel", "Turn #${turnData.turnNumber} auto-saved successfully to MariaDB earth")
                    addFloatingText(
                        if (isGameOver) "GAME OVER AUTO-SAVED TO DB" else "TURN #${turnData.turnNumber} AUTO-SAVED TO DB",
                        500f, 70f, Color(0xFF10B981)
                    )
                    CorrespondenceSlotsManager.refreshAllSlots(appContext)
                },
                onFailure = { err ->
                    Log.e("GameViewModel", "Failed to auto-save turn to MariaDB", err)
                }
            )
        }
    }

    fun spawnSupplyCrate(forced: Boolean = false) {
        val rand = Random.Default
        val x = (rand.nextFloat() * (terrain.worldWidth - 240f) + 120f)
        val types = PowerUpType.values()
        val type = types[rand.nextInt(types.size)]

        val crate = PowerUpCrate(
            id = nextCrateId++,
            type = type,
            x = x,
            y = -10f,
            isParachuting = true,
            isGrounded = false
        )
        _powerUpCrates.value = _powerUpCrates.value + crate
        addFloatingText("SUPPLY DROP INCOMING!", x, 40f, Color(0xFFFFD600))
    }

    fun addFloatingText(text: String, x: Float, y: Float, color: Color) {
        _floatingTexts.value = _floatingTexts.value + FloatingText(
            id = nextTextId++,
            text = text,
            x = x,
            y = y,
            color = color
        )
    }

    private fun updateTrajectoryGuide() {
        val t = activeTank
        if (t != null && t.hasLaserGuide) {
            val rad = t.aimAngle * (PI.toFloat() / 180f)
            val startX = t.x + cos(rad) * 22f
            val startY = t.y - 12f - sin(rad) * 22f
            val points = ballistics.simulateTrajectory(
                startX = startX,
                startY = startY,
                angleDeg = t.aimAngle,
                power = t.aimPower,
                windSpeed = _windSpeed.value,
                isHyper = t.hasSuperPropulsion,
                terrain = terrain,
                maxSteps = 75
            )
            _trajectoryPoints.value = points
        } else {
            _trajectoryPoints.value = emptyList()
        }
    }

    // Shop purchase
    fun buyWeapon(weapon: WeaponType) {
        val t = activeTank ?: return
        if (t.cash >= weapon.cost) {
            audio.playPowerUpSound()
            val list = _tanks.value.toMutableList()
            val inv = t.inventory.toMutableMap()
            inv[weapon] = (inv[weapon] ?: 0) + 1
            list[_activeTankIndex.value] = t.copy(
                cash = t.cash - weapon.cost,
                inventory = inv
            )
            _tanks.value = list
        }
    }

    fun buyShieldBattery() {
        val cost = 120
        val t = activeTank ?: return
        if (t.cash >= cost && t.shield < 100) {
            audio.playShieldSound()
            val list = _tanks.value.toMutableList()
            list[_activeTankIndex.value] = t.copy(
                cash = t.cash - cost,
                shield = (t.shield + 50).coerceAtMost(100)
            )
            _tanks.value = list
        }
    }

    fun buyFuelRefill() {
        val cost = 60
        val t = activeTank ?: return
        if (t.cash >= cost && t.fuel < 100f) {
            audio.playPowerUpSound()
            val list = _tanks.value.toMutableList()
            list[_activeTankIndex.value] = t.copy(
                cash = t.cash - cost,
                fuel = 100f
            )
            _tanks.value = list
        }
    }

    fun buyNaniteRepair() {
        val cost = 150
        val t = activeTank ?: return
        if (t.cash >= cost && t.health < t.maxHealth) {
            audio.playPowerUpSound()
            val list = _tanks.value.toMutableList()
            list[_activeTankIndex.value] = t.copy(
                cash = t.cash - cost,
                health = (t.health + 40).coerceAtMost(t.maxHealth)
            )
            _tanks.value = list
        }
    }

    fun openShop() {
        _screenState.value = GameScreenState.SHOP
    }

    fun closeShop() {
        _screenState.value = GameScreenState.PLAYING
    }

    fun nextRound() {
        val nextR = _currentRound.value + 1
        _currentRound.value = nextR
        startRound(roundNum = nextR)
        _screenState.value = GameScreenState.PLAYING
    }

    fun returnToMenu() {
        replayJob?.cancel()
        simJob?.cancel()
        _projectiles.value = emptyList()
        _particles.value = emptyList()
        _replayState.value = null
        _screenState.value = GameScreenState.MENU
    }

    fun continueAfterShare() {
        _screenState.value = GameScreenState.PLAYING
        updateTrajectoryGuide()
    }

    fun setTankName(tankIndex: Int, newName: String, context: Context? = null) {
        if (newName.isBlank()) return
        val list = _tanks.value.toMutableList()
        val tank = list.getOrNull(tankIndex) ?: return
        list[tankIndex] = tank.copy(name = newName.trim())
        _tanks.value = list

        if (context != null && _settings.value.matchMode == MatchMode.CORRESPONDENCE) {
            val p1 = list.getOrNull(0)?.name ?: "Player 1"
            val p2 = list.getOrNull(1)?.name ?: "Player 2"
            viewModelScope.launch {
                MariaDbManager.updateMatchPlayerNames(context, currentMatchId.value, p1, p2)
            }
        }
    }

    fun startCorrespondenceSlot(slotNumber: Int, matchId: String, p1Name: String? = null, p2Name: String? = null) {
        currentMatchId.value = matchId
        _settings.value = _settings.value.copy(matchMode = MatchMode.CORRESPONDENCE)
        startMatch()
        if (!p1Name.isNullOrBlank()) setTankName(0, p1Name)
        if (!p2Name.isNullOrBlank()) setTankName(1, p2Name)
    }

    fun loadCorrespondenceSlot(slotNumber: Int, matchId: String, turnCode: String, p1Name: String? = null, p2Name: String? = null): Boolean {
        currentMatchId.value = matchId
        return loadCorrespondenceTurn(turnCode, p1Name, p2Name)
    }

    /**
     * Load correspondence turn code from clipboard or database.
     * Replays the last player's actions prior to allowing the current player to take actions.
     */
    fun loadCorrespondenceTurn(code: String, p1Name: String? = null, p2Name: String? = null): Boolean {
        val data = CorrespondenceManager.decodeTurn(code) ?: return false
        loadedTurnData = data
        _settings.value = _settings.value.copy(matchMode = MatchMode.CORRESPONDENCE)
        _currentRound.value = data.roundNumber
        _currentTurnNumber.value = data.turnNumber
        terrainSeed = data.seed
        terrain.generate(seed = data.seed, style = TerrainStyle.HILLS)

        val resolvedP1Name = p1Name?.ifBlank { null } ?: _tanks.value.getOrNull(0)?.name ?: "Player 1"
        val resolvedP2Name = p2Name?.ifBlank { null } ?: _tanks.value.getOrNull(1)?.name ?: "Player 2"

        // Check if there are last player's actions to replay prior to current player taking actions
        val canReplay = data.lastFiredPlayerId in 0..1 && data.lastWeaponName.isNotBlank() && data.turnNumber > 1

        if (canReplay) {
            startReplay(data, resolvedP1Name, resolvedP2Name)
        } else {
            finishReplayImmediate(data, resolvedP1Name, resolvedP2Name)
        }
        return true
    }

    private fun startReplay(data: CorrespondenceTurnData, resolvedP1Name: String, resolvedP2Name: String) {
        replayJob?.cancel()
        simJob?.cancel()
        _projectiles.value = emptyList()
        _particles.value = emptyList()

        // Apply craters prior to the last shot
        recordedCraters.clear()
        val prevCraters = data.craters.take(data.prevCraterCount.coerceIn(0, data.craters.size))
        for (c in prevCraters) {
            recordedCraters.add(c)
            if (c.isDirt) {
                terrain.addDirt(c.cx, c.cy, c.radius)
            } else {
                terrain.carveCrater(c.cx, c.cy, c.radius)
            }
        }

        val shooterId = data.lastFiredPlayerId
        val weapon = WeaponType.values().find { it.name == data.lastWeaponName } ?: WeaponType.BABY_MISSILE

        val p1 = Tank(
            id = 0,
            name = resolvedP1Name,
            isHuman = true,
            color = Color(0xFF00E5FF),
            x = data.p1X,
            y = terrain.getHeightAt(data.p1X),
            aimAngle = if (shooterId == 0) data.lastFiredAngle else 45f,
            aimPower = if (shooterId == 0) data.lastFiredPower else 55f,
            selectedWeapon = if (shooterId == 0) weapon else WeaponType.BABY_MISSILE,
            health = data.prevP1Health,
            shield = data.prevP1Shield,
            cash = data.p1Cash
        )
        val p2 = Tank(
            id = 1,
            name = resolvedP2Name,
            isHuman = true,
            color = Color(0xFFFF5252),
            x = data.p2X,
            y = terrain.getHeightAt(data.p2X),
            aimAngle = if (shooterId == 1) data.lastFiredAngle else 135f,
            aimPower = if (shooterId == 1) data.lastFiredPower else 55f,
            selectedWeapon = if (shooterId == 1) weapon else WeaponType.BABY_MISSILE,
            health = data.prevP2Health,
            shield = data.prevP2Shield,
            cash = data.p2Cash
        )
        _tanks.value = listOf(p1, p2)
        _activeTankIndex.value = shooterId
        _windSpeed.value = data.lastFiredWindSpeed

        // Reconstruct crates
        _powerUpCrates.value = data.crates.mapIndexed { idx, cr ->
            PowerUpCrate(
                id = idx.toLong() + 100,
                type = cr.type,
                x = cr.x,
                y = terrain.getHeightAt(cr.x) - 14f,
                isParachuting = !cr.isGrounded,
                isGrounded = cr.isGrounded
            )
        }

        val shooterName = if (shooterId == 0) resolvedP1Name else resolvedP2Name
        val targetName = if (shooterId == 0) resolvedP2Name else resolvedP1Name

        _replayState.value = ReplayBannerState(
            shooterName = shooterName,
            targetName = targetName,
            weaponName = weapon.displayName,
            angle = data.lastFiredAngle,
            power = data.lastFiredPower,
            turnNumber = data.turnNumber - 1
        )
        _screenState.value = GameScreenState.REPLAYING

        replayJob = viewModelScope.launch {
            delay(900) // Brief moment for player to see the replay banner & shooter aim
            if (_screenState.value != GameScreenState.REPLAYING) return@launch

            val shooter = _tanks.value.getOrNull(shooterId) ?: return@launch
            val rad = shooter.aimAngle * (PI.toFloat() / 180f)
            val speedMult = 6.2f * weapon.speedMultiplier
            val barrelLen = 22f
            val startX = shooter.x + cos(rad) * barrelLen
            val startY = shooter.y - 12f - sin(rad) * barrelLen
            val burst = weapon.burstCount

            addFloatingText(
                "REPLAY: $shooterName FIRED ${weapon.displayName.uppercase()}!",
                shooter.x, shooter.y - 35f, shooter.color
            )

            for (b in 0 until burst) {
                if (b > 0) delay(110)
                audio.playFireSound()
                val spreadAngle = shooter.aimAngle + (b - (burst - 1) / 2f) * 1.5f
                val spreadRad = spreadAngle * (PI.toFloat() / 180f)
                val velocity = shooter.aimPower * speedMult
                val proj = Projectile(
                    id = nextProjId++,
                    ownerId = shooter.id,
                    weaponType = weapon,
                    x = startX,
                    y = startY,
                    vx = cos(spreadRad) * velocity,
                    vy = -sin(spreadRad) * velocity,
                    bouncesLeft = if (weapon == WeaponType.BOUNCER) 3 else 0
                )
                _projectiles.value = _projectiles.value + proj
            }

            startReplaySimulationLoop(data)
        }
    }

    private fun startReplaySimulationLoop(data: CorrespondenceTurnData) {
        simJob?.cancel()
        simJob = viewModelScope.launch {
            val dt = 0.025f
            while (isActive) {
                var hasActiveObjects = false

                val updatedCrates = _powerUpCrates.value.map { crate ->
                    ballistics.stepCrate(crate, dt, _windSpeed.value, terrain)
                }
                _powerUpCrates.value = updatedCrates

                val curProjs = _projectiles.value
                if (curProjs.isNotEmpty()) {
                    hasActiveObjects = true
                    val nextProjs = mutableListOf<Projectile>()
                    val spawnedFromSplits = mutableListOf<Projectile>()
                    for (p in curProjs) {
                        val stepped = ballistics.stepProjectile(
                            proj = p,
                            dt = dt,
                            windSpeed = _windSpeed.value,
                            terrain = terrain,
                            onDetonate = { detonatedProj, dx, dy ->
                                handleDetonation(detonatedProj, dx, dy)
                            },
                            onSplitMirv = { mirvProj ->
                                val subs = handleMirvSplit(mirvProj)
                                spawnedFromSplits.addAll(subs)
                            }
                        )
                        if (stepped != null) {
                            nextProjs.add(stepped)
                        }
                    }
                    nextProjs.addAll(spawnedFromSplits)
                    _projectiles.value = nextProjs
                }

                val curParticles = _particles.value
                if (curParticles.isNotEmpty()) {
                    hasActiveObjects = true
                    val nextParticles = curParticles.mapNotNull { part ->
                        val life = part.currentLife - dt
                        if (life <= 0f) null
                        else part.copy(
                            x = part.x + part.vx * dt,
                            y = part.y + part.vy * dt,
                            currentLife = life
                        )
                    }
                    _particles.value = nextParticles
                }

                val curTexts = _floatingTexts.value
                if (curTexts.isNotEmpty()) {
                    val nextTexts = curTexts.mapNotNull { ft ->
                        val life = ft.currentLife - dt
                        if (life <= 0f) null
                        else ft.copy(y = ft.y - 18f * dt, currentLife = life)
                    }
                    _floatingTexts.value = nextTexts
                }

                settleTanks()

                if (!hasActiveObjects && _projectiles.value.isEmpty()) {
                    // Replay simulation complete
                    delay(900)
                    finishReplay(data)
                    break
                }

                delay(25)
            }
        }
    }

    fun finishReplay(data: CorrespondenceTurnData? = loadedTurnData) {
        replayJob?.cancel()
        simJob?.cancel()
        _projectiles.value = emptyList()
        _particles.value = emptyList()
        _replayState.value = null

        val targetData = data ?: loadedTurnData ?: return

        // Restore complete craters state
        recordedCraters.clear()
        for (c in targetData.craters) {
            recordedCraters.add(c)
            if (c.isDirt) terrain.addDirt(c.cx, c.cy, c.radius)
            else terrain.carveCrater(c.cx, c.cy, c.radius)
        }

        // Apply final tank states
        val list = _tanks.value.toMutableList()
        val p1 = list.getOrNull(0)
        val p2 = list.getOrNull(1)
        if (p1 != null) {
            list[0] = p1.copy(
                x = targetData.p1X,
                y = terrain.getHeightAt(targetData.p1X),
                health = targetData.p1Health,
                shield = targetData.p1Shield,
                cash = targetData.p1Cash,
                isDestroyed = targetData.p1Health <= 0
            )
        }
        if (p2 != null) {
            list[1] = p2.copy(
                x = targetData.p2X,
                y = terrain.getHeightAt(targetData.p2X),
                health = targetData.p2Health,
                shield = targetData.p2Shield,
                cash = targetData.p2Cash,
                isDestroyed = targetData.p2Health <= 0
            )
        }
        _tanks.value = list
        settleTanks()

        _windSpeed.value = targetData.windSpeed
        _currentTurnNumber.value = targetData.turnNumber
        _currentRound.value = targetData.roundNumber
        _activeTankIndex.value = targetData.activeTankIndex

        val isOver = targetData.isGameOver || (p1?.health ?: 100) <= 0 || (p2?.health ?: 100) <= 0
        if (isOver) {
            val winner = if (targetData.winnerName.isNotBlank()) {
                list.firstOrNull { it.name.equals(targetData.winnerName, ignoreCase = true) }
            } else {
                list.firstOrNull { it.health > 0 }
            }
            _roundWinner.value = winner
            _matchWinner.value = winner
            _screenState.value = GameScreenState.MATCH_OVER
        } else {
            _screenState.value = GameScreenState.PLAYING
            updateTrajectoryGuide()
            val nextPlayerName = list.getOrNull(targetData.activeTankIndex)?.name ?: "Player ${targetData.activeTankIndex + 1}"
            addFloatingText("YOUR TURN, $nextPlayerName!", 500f, 90f, Color(0xFF10B981))
            audio.playPowerUpSound()
        }
    }

    fun skipReplay() {
        finishReplay(loadedTurnData)
    }

    private fun finishReplayImmediate(data: CorrespondenceTurnData, resolvedP1Name: String, resolvedP2Name: String) {
        recordedCraters.clear()
        for (c in data.craters) {
            recordedCraters.add(c)
            if (c.isDirt) terrain.addDirt(c.cx, c.cy, c.radius)
            else terrain.carveCrater(c.cx, c.cy, c.radius)
        }

        val p1 = Tank(
            id = 0,
            name = resolvedP1Name,
            isHuman = true,
            color = Color(0xFF00E5FF),
            x = data.p1X,
            y = terrain.getHeightAt(data.p1X),
            health = data.p1Health,
            shield = data.p1Shield,
            cash = data.p1Cash
        )
        val p2 = Tank(
            id = 1,
            name = resolvedP2Name,
            isHuman = true,
            color = Color(0xFFFF5252),
            x = data.p2X,
            y = terrain.getHeightAt(data.p2X),
            health = data.p2Health,
            shield = data.p2Shield,
            cash = data.p2Cash
        )
        _tanks.value = listOf(p1, p2)
        _windSpeed.value = data.windSpeed
        _activeTankIndex.value = data.activeTankIndex

        _powerUpCrates.value = data.crates.mapIndexed { idx, cr ->
            PowerUpCrate(
                id = idx.toLong() + 100,
                type = cr.type,
                x = cr.x,
                y = terrain.getHeightAt(cr.x) - 14f,
                isParachuting = !cr.isGrounded,
                isGrounded = cr.isGrounded
            )
        }

        val isOver = data.isGameOver || (p1.health <= 0 || p2.health <= 0)
        if (isOver) {
            val winner = if (data.winnerName.isNotBlank()) {
                listOf(p1, p2).firstOrNull { it.name.equals(data.winnerName, ignoreCase = true) }
            } else {
                listOf(p1, p2).firstOrNull { it.health > 0 }
            }
            _roundWinner.value = winner
            _matchWinner.value = winner
            _screenState.value = GameScreenState.MATCH_OVER
        } else {
            _screenState.value = GameScreenState.PLAYING
            updateTrajectoryGuide()
            addFloatingText("TURN #${data.turnNumber} LOADED!", 500f, 100f, Color(0xFF00E5FF))
        }
    }

    /**
     * Checks the remote MariaDB server for the latest turn in correspondence play.
     */
    fun syncCorrespondenceRemote(context: Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val matchId = currentMatchId.value
            val res = MariaDbManager.fetchLatestTurn(context, matchId)
            res.fold(
                onSuccess = { turnResult ->
                    if (turnResult != null) {
                        val turnData = turnResult.turnData
                        if (turnData.turnNumber > _currentTurnNumber.value || 
                            (turnData.turnNumber == _currentTurnNumber.value && turnData.activeTankIndex != _activeTankIndex.value)) {
                            loadCorrespondenceTurn(turnResult.turnCode, turnResult.p1Name, turnResult.p2Name)
                            onResult(true, "Turn #${turnData.turnNumber} synced from server (${turnResult.p1Name} vs ${turnResult.p2Name})!")
                        } else {
                            if (turnResult.p1Name.isNotBlank()) setTankName(0, turnResult.p1Name)
                            if (turnResult.p2Name.isNotBlank()) setTankName(1, turnResult.p2Name)
                            onResult(false, "You are already on the latest turn (#${_currentTurnNumber.value}).")
                        }
                    } else {
                        onResult(false, "No saved turns found for $matchId yet.")
                    }
                },
                onFailure = { err ->
                    onResult(false, "Sync failed: ${err.message ?: "Network error"}")
                }
            )
        }
    }
}
