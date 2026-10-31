package com.example.viewmodel

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ArtilleryAi
import com.example.audio.RetroAudioPlayer
import com.example.correspondence.CorrespondenceManager
import com.example.correspondence.CorrespondenceTurnData
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

class GameViewModel : ViewModel() {

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

    private val recordedCraters = mutableListOf<CraterRecord>()
    private var lastFiredData: CorrespondenceTurnData? = null
    private var simJob: Job? = null
    private var nextCrateId = 1L
    private var nextProjId = 1L
    private var nextTextId = 1L
    private var terrainSeed = 12345L

    val activeTank: Tank?
        get() = _tanks.value.getOrNull(_activeTankIndex.value)

    init {
        startMatch()
    }

    fun updateSettings(newSettings: GameSettings) {
        _settings.value = newSettings
    }

    fun startMatch() {
        _currentRound.value = 1
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
        lastFiredData = CorrespondenceTurnData(
            turnNumber = (_roundScores.value.values.sum()) + 1,
            roundNumber = _currentRound.value,
            seed = terrainSeed,
            windSpeed = _windSpeed.value,
            activeTankIndex = _activeTankIndex.value,
            p1Health = _tanks.value[0].health,
            p1Shield = _tanks.value[0].shield,
            p1Cash = _tanks.value[0].cash,
            p1X = _tanks.value[0].x,
            p2Health = _tanks.value[1].health,
            p2Shield = _tanks.value[1].shield,
            p2Cash = _tanks.value[1].cash,
            p2X = _tanks.value[1].x,
            lastFiredPlayerId = shooter.id,
            lastFiredAngle = shooter.aimAngle,
            lastFiredPower = shooter.aimPower,
            lastWeaponName = weapon.name,
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
                                handleMirvSplit(mirvProj)
                            }
                        )
                        if (stepped != null) {
                            nextProjs.add(stepped)
                        }
                    }
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

    private fun handleMirvSplit(parent: Projectile) {
        val isDeathsHead = parent.weaponType == WeaponType.DEATHS_HEAD
        audio.playExplosionSound(isBig = isDeathsHead)
        val count = if (isDeathsHead) 6 else 3
        val submunitions = mutableListOf<Projectile>()
        for (i in 0 until count) {
            val spread = if (count == 3) {
                when (i) {
                    0 -> -36f
                    1 -> 0f
                    else -> 36f
                }
            } else {
                -50f + (100f * i / (count - 1).coerceAtLeast(1))
            }
            submunitions.add(
                Projectile(
                    id = nextProjId++,
                    ownerId = parent.ownerId,
                    weaponType = if (isDeathsHead) WeaponType.BABY_NUKE else WeaponType.BABY_MISSILE,
                    x = parent.x,
                    y = parent.y,
                    vx = parent.vx + spread,
                    vy = parent.vy + (if (isDeathsHead) 12f else 18f),
                    isSubmunition = true,
                    isDoubleDamage = parent.isDoubleDamage
                )
            )
        }
        _projectiles.value = _projectiles.value.filter { it.id != parent.id } + submunitions
        spawnExplosionParticles(parent.x, parent.y, if (isDeathsHead) 45f else 25f, if (isDeathsHead) Color(0xFFD500F9) else Color(0xFF00E5FF))
        addFloatingText(
            if (isDeathsHead) "DEATH'S HEAD 6x SPLIT!" else "MIRV 3x SPLIT!",
            parent.x,
            parent.y - 18f,
            if (isDeathsHead) Color(0xFFD500F9) else Color(0xFF00E5FF)
        )
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
            if (maxScore >= targetWins || _currentRound.value >= _settings.value.roundsCount) {
                _matchWinner.value = winner
                _screenState.value = GameScreenState.MATCH_OVER
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

        // Dynamic wind shift
        rollWind()

        // Supply crate airdrop check (40% chance if fewer than 3 crates)
        if (Random.nextFloat() < 0.45f && _powerUpCrates.value.size < 3) {
            spawnSupplyCrate(forced = false)
        }

        // If correspondence play, pause for share
        if (_settings.value.matchMode == MatchMode.CORRESPONDENCE && lastFiredData != null) {
            val code = CorrespondenceManager.encodeTurn(lastFiredData!!)
            _lastTurnCode.value = code
            _screenState.value = GameScreenState.CORRESPONDENCE_SHARE
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
        _screenState.value = GameScreenState.MENU
    }

    fun continueAfterShare() {
        _screenState.value = GameScreenState.PLAYING
        updateTrajectoryGuide()
    }

    /**
     * Load correspondence turn code from clipboard or input.
     */
    fun loadCorrespondenceTurn(code: String): Boolean {
        val data = CorrespondenceManager.decodeTurn(code) ?: return false
        _settings.value = _settings.value.copy(matchMode = MatchMode.CORRESPONDENCE)
        _currentRound.value = data.roundNumber
        terrainSeed = data.seed
        terrain.generate(seed = data.seed, style = TerrainStyle.HILLS)

        // Apply saved craters
        recordedCraters.clear()
        for (c in data.craters) {
            recordedCraters.add(c)
            if (c.isDirt) {
                terrain.addDirt(c.cx, c.cy, c.radius)
            } else {
                terrain.carveCrater(c.cx, c.cy, c.radius)
            }
        }

        // Reconstruct tanks
        val p1 = Tank(
            id = 0,
            name = "Player 1",
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
            name = "Player 2",
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

        _screenState.value = GameScreenState.PLAYING
        updateTrajectoryGuide()
        addFloatingText("TURN #${data.turnNumber} LOADED!", 500f, 100f, Color(0xFF00E5FF))
        return true
    }
}
