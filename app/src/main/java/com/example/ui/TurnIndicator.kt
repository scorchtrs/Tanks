package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MatchMode
import com.example.model.Tank

@Composable
fun TurnIndicator(
    activeTank: Tank,
    matchMode: MatchMode,
    currentMatchId: String,
    turnNumber: Int,
    currentRound: Int,
    totalRounds: Int,
    isFiring: Boolean,
    localPlayerIndex: Int,
    isCheckingRemote: Boolean,
    onCheckRemoteTurn: () -> Unit,
    onSwitchRole: () -> Unit,
    onRenamePlayer: (tankId: Int, newName: String) -> Unit = { _, _ -> },
    isLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "turn_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    when (matchMode) {
        MatchMode.CORRESPONDENCE -> {
            CorrespondenceTurnIndicator(
                activeTank = activeTank,
                currentMatchId = currentMatchId,
                turnNumber = turnNumber,
                currentRound = currentRound,
                totalRounds = totalRounds,
                isFiring = isFiring,
                localPlayerIndex = localPlayerIndex,
                isCheckingRemote = isCheckingRemote,
                onCheckRemoteTurn = onCheckRemoteTurn,
                onSwitchRole = onSwitchRole,
                onRenamePlayer = onRenamePlayer,
                pulseAlpha = pulseAlpha,
                isLandscape = isLandscape,
                modifier = modifier
            )
        }
        MatchMode.PASS_AND_PLAY -> {
            PassAndPlayTurnIndicator(
                activeTank = activeTank,
                turnNumber = turnNumber,
                isFiring = isFiring,
                pulseAlpha = pulseAlpha,
                isLandscape = isLandscape,
                modifier = modifier
            )
        }
        MatchMode.VS_AI -> {
            VsAiTurnIndicator(
                activeTank = activeTank,
                turnNumber = turnNumber,
                isFiring = isFiring,
                pulseAlpha = pulseAlpha,
                isLandscape = isLandscape,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun VsAiTurnIndicator(
    activeTank: Tank,
    turnNumber: Int,
    isFiring: Boolean,
    pulseAlpha: Float,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val isAi = !activeTank.isHuman

    val containerBg = if (isAi) {
        Color(0xEE1E1124) // Dark crimson-violet for AI
    } else {
        Color(0xEE0B1C2E) // High-tech navy-cyan for Player
    }

    val accentColor = if (isAi) {
        Color(0xFFFF5252) // Warning Red/Coral for AI
    } else {
        Color(0xFF00E5FF) // Neon Cyan for Player
    }

    val borderColor = if (isAi) {
        Color(0xFFDC2626).copy(alpha = pulseAlpha)
    } else {
        Color(0xFF0284C7).copy(alpha = pulseAlpha)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("turn_indicator")
            .background(containerBg)
            .border(1.dp, borderColor)
            .padding(horizontal = if (isLandscape) 12.dp else 14.dp, vertical = if (isLandscape) 4.dp else 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Icon + Role Badge + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Animated Icon Badge
                Box(
                    modifier = Modifier
                        .size(if (isLandscape) 28.dp else 34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = if (isAi) 0.25f else 0.2f))
                        .border(1.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAi) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "AI Opponent",
                            tint = accentColor,
                            modifier = Modifier
                                .size(if (isLandscape) 16.dp else 20.dp)
                                .alpha(pulseAlpha)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MilitaryTech,
                            contentDescription = "Human Commander",
                            tint = accentColor,
                            modifier = Modifier.size(if (isLandscape) 16.dp else 20.dp)
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Status Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isAi) Color(0xFFEF4444) else Color(0xFF0284C7))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (isAi) "AI ACTIVE" else "YOUR TURN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = if (isAi) "CPU (${activeTank.name.uppercase()})" else "${activeTank.name.uppercase()} READY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.testTag("turn_status_title")
                        )

                        Text(
                            text = "• TURN #$turnNumber",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    if (!isLandscape) {
                        Text(
                            text = if (isFiring) {
                                "PROJECTILE IN FLIGHT — CALCULATING IMPACT!"
                            } else if (isAi) {
                                "AI computing ballistic arc & wind drift... Stand by"
                            } else {
                                "Adjust angle & velocity to fire weapon: ${activeTank.selectedWeapon.displayName.uppercase()}"
                            },
                            fontSize = 10.sp,
                            color = if (isAi) Color(0xFFFCA5A5) else Color(0xFF93C5FD),
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            // Right: AI Thinking Indicator or Human Weapon/Fire Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isAi) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF3B0712))
                            .border(1.dp, Color(0xFFDC2626), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Controls locked",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isFiring) "FIRING..." else "AI AIMING...",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF032646))
                            .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            Icons.Default.Adjust,
                            contentDescription = "Aim active",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isFiring) "FIRING..." else "IN CONTROL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF7DD3FC)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PassAndPlayTurnIndicator(
    activeTank: Tank,
    turnNumber: Int,
    isFiring: Boolean,
    pulseAlpha: Float,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val tankColor = activeTank.color

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("turn_indicator")
            .background(Color(0xEE0B132B))
            .border(1.dp, tankColor.copy(alpha = pulseAlpha))
            .padding(horizontal = if (isLandscape) 12.dp else 14.dp, vertical = if (isLandscape) 4.dp else 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isLandscape) 28.dp else 32.dp)
                        .clip(CircleShape)
                        .background(tankColor.copy(alpha = 0.25f))
                        .border(1.dp, tankColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Player",
                        tint = tankColor,
                        modifier = Modifier.size(if (isLandscape) 16.dp else 18.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(tankColor)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${activeTank.name.uppercase()}'S TURN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = "TURN #$turnNumber",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    if (!isLandscape) {
                        Text(
                            text = "Pass device to ${activeTank.name} • Aim & Fire weapon",
                            fontSize = 10.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(tankColor.copy(alpha = 0.2f))
                    .border(1.dp, tankColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${activeTank.selectedWeapon.displayName.uppercase()}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = tankColor
                )
            }
        }
    }
}

@Composable
private fun CorrespondenceTurnIndicator(
    activeTank: Tank,
    currentMatchId: String,
    turnNumber: Int,
    currentRound: Int,
    totalRounds: Int,
    isFiring: Boolean,
    localPlayerIndex: Int,
    isCheckingRemote: Boolean,
    onCheckRemoteTurn: () -> Unit,
    onSwitchRole: () -> Unit,
    onRenamePlayer: (tankId: Int, newName: String) -> Unit,
    pulseAlpha: Float,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val isMyTurn = activeTank.id == localPlayerIndex
    val roleColor = if (isMyTurn) Color(0xFF10B981) else Color(0xFFF59E0B) // Green for Your Turn, Amber for Waiting
    val borderColor = roleColor.copy(alpha = pulseAlpha)

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember(activeTank.name) { mutableStateOf(activeTank.name) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("turn_indicator")
            .background(Color(0xF00A1120))
            .border(1.dp, borderColor)
            .padding(horizontal = if (isLandscape) 12.dp else 14.dp, vertical = if (isLandscape) 4.dp else 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Match ID + Turn Status + Commander Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isLandscape) 28.dp else 34.dp)
                        .clip(CircleShape)
                        .background(roleColor.copy(alpha = 0.2f))
                        .border(1.dp, roleColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isMyTurn) {
                        Icon(
                            imageVector = Icons.Default.Adjust,
                            contentDescription = "Your Turn",
                            tint = roleColor,
                            modifier = Modifier.size(if (isLandscape) 16.dp else 20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = "Waiting for Opponent",
                            tint = roleColor,
                            modifier = Modifier
                                .size(if (isLandscape) 16.dp else 20.dp)
                                .alpha(pulseAlpha)
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Status Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(roleColor)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (isMyTurn) "YOUR MOVE" else "WAITING FOR OPPONENT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        // Commander Name (Clickable to Rename)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    renameInput = activeTank.name
                                    showRenameDialog = true
                                }
                                .padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = activeTank.name.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = activeTank.color
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Rename Player",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(11.dp)
                            )
                        }

                        Text(
                            text = "• ${currentMatchId}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = "• T#$turnNumber",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    if (!isLandscape) {
                        Text(
                            text = if (isMyTurn) {
                                "${activeTank.name}, take aim & fire to save shot to MariaDB on trsbox.net."
                            } else {
                                "Waiting for ${activeTank.name} to fire. Tap SYNC DB to check for their move."
                            },
                            fontSize = 10.sp,
                            color = if (isMyTurn) Color(0xFF86EFAC) else Color(0xFFFDE68A)
                        )
                    }
                }
            }

            // Right: Sync Button + Role Switcher Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Role Toggle Chip: e.g. "Role: P1" / "Role: P2"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF475569), RoundedCornerShape(6.dp))
                        .clickable { onSwitchRole() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .testTag("switch_player_role_chip")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = "Switch player role",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "ROLE: P${localPlayerIndex + 1}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }

                // Remote MariaDB Sync Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F2B48))
                        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                        .clickable(enabled = !isCheckingRemote) { onCheckRemoteTurn() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("turn_indicator_sync_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCheckingRemote) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = Color(0xFF38BDF8),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = "Sync from MariaDB",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCheckingRemote) "SYNCING..." else "SYNC DB",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Text("Rename Tank ${activeTank.id + 1}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Set your commander name for this correspondence battle. This will be saved to database 'earth' on remote server trsbox.net.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text("Commander Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onRenamePlayer(activeTank.id, renameInput.trim())
                        }
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("SAVE TO DATABASE")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRenameDialog = false }) {
                    Text("CANCEL")
                }
            },
            containerColor = Color(0xFF0F172A),
            textContentColor = Color.White,
            titleContentColor = Color.White
        )
    }
}
