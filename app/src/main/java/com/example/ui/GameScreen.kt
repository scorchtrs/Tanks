package com.example.ui

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.findActivity
import com.example.viewmodel.GameScreenState
import com.example.viewmodel.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Force landscape mode when in game
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val prevOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = prevOrientation
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val screenState by viewModel.screenState.collectAsState()
    val tanks by viewModel.tanks.collectAsState()
    val activeIdx by viewModel.activeTankIndex.collectAsState()
    val currentRound by viewModel.currentRound.collectAsState()
    val currentTurnNumber by viewModel.currentTurnNumber.collectAsState()
    val localPlayerIndex by viewModel.localPlayerIndex.collectAsState()
    val currentMatchId by viewModel.currentMatchId.collectAsState()
    val roundScores by viewModel.roundScores.collectAsState()
    val windSpeed by viewModel.windSpeed.collectAsState()
    val powerUpCrates by viewModel.powerUpCrates.collectAsState()
    val projectiles by viewModel.projectiles.collectAsState()
    val particles by viewModel.particles.collectAsState()
    val floatingTexts by viewModel.floatingTexts.collectAsState()
    val trajectoryPoints by viewModel.trajectoryPoints.collectAsState()
    val roundWinner by viewModel.roundWinner.collectAsState()
    val matchWinner by viewModel.matchWinner.collectAsState()
    val lastTurnCode by viewModel.lastTurnCode.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val replayState by viewModel.replayState.collectAsState()

    var isMuted by remember { mutableStateOf(viewModel.audio.isMuted) }
    var isCheckingRemote by remember { mutableStateOf(false) }

    val activeTank = tanks.getOrNull(activeIdx)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF020617)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top HUD Bar: Adaptive for Portrait and Landscape
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xE60F172A))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Back button + Round info + Scores + Active Tank turn tag + Cash
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.returnToMenu() },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("back_to_menu_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Menu",
                                tint = Color(0xFF94A3B8)
                            )
                        }

                        Column {
                            Text(
                                text = "RND $currentRound/${settings.roundsCount}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF38BDF8),
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "P1: ${roundScores[0] ?: 0}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF)
                                )
                                Text(text = " • ", fontSize = 10.sp, color = Color(0xFF64748B))
                                Text(
                                    text = "P2: ${roundScores[1] ?: 0}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5252)
                                )
                            }
                        }

                        if (activeTank != null) {
                            Box(
                                modifier = Modifier
                                    .background(activeTank.color.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .border(1.dp, activeTank.color, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${activeTank.name.uppercase()}'S TURN",
                                    color = activeTank.color,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "$${activeTank.cash}",
                                color = Color(0xFFFFD700),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Center: Dynamic Wind Gauge
                    WindIndicator(windSpeed = windSpeed)

                    // Right: Active Buff Badges + Armory button + Sound Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (activeTank != null) {
                            if (activeTank.isShieldActive) {
                                BuffBadge(icon = Icons.Default.Shield, text = "${activeTank.shield}", color = Color(0xFF00E5FF))
                            }
                            if (activeTank.hasDoubleDamage) {
                                BuffBadge(icon = Icons.Default.Whatshot, text = "2x", color = Color(0xFFFF1744))
                            }
                            if (activeTank.hasSuperPropulsion) {
                                BuffBadge(icon = Icons.Default.ElectricBolt, text = "HYPER", color = Color(0xFFFFEA00))
                            }
                            if (activeTank.hasLaserGuide) {
                                BuffBadge(icon = Icons.Default.Whatshot, text = "TRACER", color = Color(0xFFD500F9))
                            }

                            // Armory Shop Button in Top Bar for Landscape
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF475569), RoundedCornerShape(8.dp))
                                    .clickable(enabled = screenState != GameScreenState.FIRING) { viewModel.openShop() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("open_shop_button")
                            ) {
                                Text(
                                    text = "ARMORY",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                viewModel.audio.isMuted = isMuted
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = if (isMuted) Color(0xFF64748B) else Color(0xFF38BDF8)
                            )
                        }
                    }
                }
            } else {
                // Portrait Top HUD Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xE60F172A))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back to Menu button & Round badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.returnToMenu() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("back_to_menu_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Menu",
                                tint = Color(0xFF94A3B8)
                            )
                        }

                        Column {
                            Text(
                                text = "ROUND $currentRound OF ${settings.roundsCount}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF38BDF8),
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "P1: ${roundScores[0] ?: 0}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF)
                                )
                                Text(
                                    text = " • ",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "P2: ${roundScores[1] ?: 0}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5252)
                                )
                            }
                        }
                    }

                    // Center: Dynamic Wind Gauge
                    WindIndicator(windSpeed = windSpeed)

                    // Sound Mute Toggle
                    IconButton(
                        onClick = {
                            isMuted = !isMuted
                            viewModel.audio.isMuted = isMuted
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = if (isMuted) Color(0xFF64748B) else Color(0xFF38BDF8)
                        )
                    }
                }
            }

            // Dedicated Turn Indicator: Shows whose move it is, distinguishing Player vs AI and handling Correspondence play
            if (activeTank != null) {
                TurnIndicator(
                    activeTank = activeTank,
                    matchMode = settings.matchMode,
                    currentMatchId = currentMatchId,
                    turnNumber = currentTurnNumber,
                    currentRound = currentRound,
                    totalRounds = settings.roundsCount,
                    isFiring = screenState == GameScreenState.FIRING || screenState == GameScreenState.REPLAYING,
                    isReplaying = screenState == GameScreenState.REPLAYING,
                    localPlayerIndex = localPlayerIndex,
                    isCheckingRemote = isCheckingRemote,
                    onCheckRemoteTurn = {
                        isCheckingRemote = true
                        viewModel.syncCorrespondenceRemote(context) { success, msg ->
                            isCheckingRemote = false
                            viewModel.addFloatingText(msg, 500f, 100f, if (success) Color(0xFF10B981) else Color(0xFFEF4444))
                        }
                    },
                    onSwitchRole = {
                        val nextRole = (localPlayerIndex + 1) % 2
                        viewModel.setLocalPlayerIndex(nextRole)
                        viewModel.addFloatingText("Role set to Player ${nextRole + 1}", 500f, 120f, Color(0xFF38BDF8))
                    },
                    onRenamePlayer = { tankId, newName ->
                        viewModel.setTankName(tankId, newName, context)
                        viewModel.addFloatingText("Commander renamed to $newName!", 500f, 120f, Color(0xFF10B981))
                    },
                    isLandscape = isLandscape
                )
            }

            // Battlefield Canvas in center (expanding to take up available vertical space)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                GameCanvas(
                    terrain = viewModel.terrain,
                    tanks = tanks,
                    activeTankIndex = activeIdx,
                    powerUpCrates = powerUpCrates,
                    projectiles = projectiles,
                    particles = particles,
                    floatingTexts = floatingTexts,
                    trajectoryPoints = trajectoryPoints,
                    windSpeed = windSpeed,
                    isFiring = screenState == GameScreenState.FIRING || screenState == GameScreenState.REPLAYING,
                    onAimChange = { angle, power ->
                        if (screenState != GameScreenState.REPLAYING) {
                            viewModel.setAimAngle(angle)
                            viewModel.setAimPower(power)
                        }
                    }
                )

                // Replay Banner Overlay
                if (screenState == GameScreenState.REPLAYING && replayState != null) {
                    val rState = replayState!!
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .testTag("correspondence_replay_banner"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xF20F172A)),
                        border = BorderStroke(1.5.dp, Color(0xFFF59E0B))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Replay",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "ACTION REPLAY: ${rState.shooterName.uppercase()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFF59E0B),
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = " (Turn #${rState.turnNumber})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Text(
                                    text = "Fired ${rState.weaponName} • ${rState.angle.toInt()}° angle • ${rState.power.toInt()}% power",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE2E8F0)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = { viewModel.skipReplay() },
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("skip_replay_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                border = BorderStroke(1.dp, Color(0xFF475569))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "SKIP",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Default.FastForward,
                                        contentDescription = "Skip Replay",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Tactical Control Deck (adaptive layout)
            if (activeTank != null) {
                TacticalControlDeck(
                    activeTank = activeTank,
                    isFiring = screenState == GameScreenState.FIRING || screenState == GameScreenState.REPLAYING,
                    onMove = { direction -> viewModel.driveTank(direction) },
                    onAngleChange = { angle -> viewModel.setAimAngle(angle) },
                    onAngleAdjust = { delta -> viewModel.adjustAimAngle(delta) },
                    onPowerChange = { power -> viewModel.setAimPower(power) },
                    onPowerAdjust = { delta -> viewModel.adjustAimPower(delta) },
                    onSelectWeapon = { wpn -> viewModel.selectWeapon(wpn) },
                    onFire = { viewModel.fireWeapon() },
                    onOpenShop = { viewModel.openShop() },
                    isLandscape = isLandscape
                )
            }
        }

        // Overlay Dialogs based on ScreenState
        when (screenState) {
            GameScreenState.SHOP -> {
                if (activeTank != null) {
                    ShopDialog(
                        activeTank = activeTank,
                        onBuyWeapon = { viewModel.buyWeapon(it) },
                        onBuyShield = { viewModel.buyShieldBattery() },
                        onBuyFuel = { viewModel.buyFuelRefill() },
                        onBuyRepair = { viewModel.buyNaniteRepair() },
                        onDismiss = { viewModel.closeShop() }
                    )
                }
            }
            GameScreenState.ROUND_OVER -> {
                RoundOverDialog(
                    roundNumber = currentRound,
                    winner = roundWinner,
                    scores = roundScores,
                    onNextRound = { viewModel.nextRound() },
                    onOpenShop = { viewModel.openShop() }
                )
            }
            GameScreenState.MATCH_OVER -> {
                MatchOverDialog(
                    winner = matchWinner,
                    scores = roundScores,
                    onPlayAgain = { viewModel.startMatch() },
                    onMainMenu = { viewModel.returnToMenu() }
                )
            }
            GameScreenState.CORRESPONDENCE_SHARE -> {
                val p1 = tanks.getOrNull(0)?.name ?: "Player 1"
                val p2 = tanks.getOrNull(1)?.name ?: "Player 2"
                CorrespondenceShareDialog(
                    turnCode = lastTurnCode,
                    matchId = currentMatchId,
                    isGameOver = matchWinner != null,
                    winnerName = matchWinner?.name ?: "",
                    p1Name = p1,
                    p2Name = p2,
                    onContinue = { viewModel.continueAfterShare() },
                    onMainMenu = { viewModel.returnToMenu() }
                )
            }
            else -> { /* In normal play or firing */ }
        }
    }
}
