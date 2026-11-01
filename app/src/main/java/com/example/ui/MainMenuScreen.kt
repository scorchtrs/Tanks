package com.example.ui

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.correspondence.CorrespondenceSlotsManager
import com.example.model.AiDifficulty
import com.example.model.GameSettings
import com.example.model.MatchMode
import com.example.model.WindMode
import kotlinx.coroutines.launch

@Composable
fun MainMenuScreen(
    currentSettings: GameSettings,
    onStartGame: (GameSettings) -> Unit,
    onLoadCorrespondenceCode: (String) -> Boolean,
    onPlaySlot: (slotNumber: Int, matchId: String, turnCode: String?, p1Name: String?, p2Name: String?) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var selectedMode by remember { mutableStateOf(currentSettings.matchMode) }
    var selectedDifficulty by remember { mutableStateOf(currentSettings.aiDifficulty) }
    var selectedRounds by remember { mutableStateOf(currentSettings.roundsCount) }
    var selectedWind by remember { mutableStateOf(currentSettings.windMode) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showMariaDbSettings by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Automatically get current game state of each game from remote database when correspondence play is active/selected
    LaunchedEffect(selectedMode) {
        if (selectedMode == MatchMode.CORRESPONDENCE) {
            CorrespondenceSlotsManager.refreshAllSlots(context)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF090D16)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLandscape) {
                // Landscape Two-Column Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .widthIn(max = 920.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Branding + Game Mode + Load Turn
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SCORCHED TANKS",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            style = androidx.compose.ui.text.TextStyle(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFFDC2626))
                                )
                            ),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "THE MOTHER OF ALL ARTILLERY BATTLES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "GAME MODE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                ModeOptionRow(
                                    title = "Player vs AI",
                                    subtitle = "Duel against computer artillery",
                                    icon = Icons.Default.SmartToy,
                                    isSelected = selectedMode == MatchMode.VS_AI,
                                    onClick = { selectedMode = MatchMode.VS_AI }
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                ModeOptionRow(
                                    title = "Pass & Play (2 Players)",
                                    subtitle = "Hotseat 2-player battle",
                                    icon = Icons.Default.People,
                                    isSelected = selectedMode == MatchMode.PASS_AND_PLAY,
                                    onClick = { selectedMode = MatchMode.PASS_AND_PLAY }
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                ModeOptionRow(
                                    title = "Correspondence Play",
                                    subtitle = "Async duel via turn share code",
                                    icon = Icons.Default.Email,
                                    isSelected = selectedMode == MatchMode.CORRESPONDENCE,
                                    onClick = { 
                                        selectedMode = MatchMode.CORRESPONDENCE
                                        scope.launch { CorrespondenceSlotsManager.refreshAllSlots(context) }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("load_turn_code_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LOAD CORRESPONDENCE TURN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Right Column: Match Configuration + Start Button OR Correspondence 5 Slots Panel
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (selectedMode == MatchMode.CORRESPONDENCE) {
                            CorrespondenceGamesPanel(
                                onPlaySlot = onPlaySlot,
                                onOpenSettings = { showMariaDbSettings = true }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val newSettings = GameSettings(
                                        roundsCount = selectedRounds,
                                        matchMode = selectedMode,
                                        aiDifficulty = selectedDifficulty,
                                        windMode = selectedWind
                                    )
                                    onStartGame(newSettings)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("start_quick_correspondence_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "START NEW QUICK CORRESPONDENCE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            // AI Difficulty (if VS_AI)
                            if (selectedMode == MatchMode.VS_AI) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "AI DIFFICULTY",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8),
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            AiDifficulty.values().forEach { diff ->
                                                val isSel = selectedDifficulty == diff
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSel) Color(0xFF0284C7) else Color(0xFF1E293B))
                                                        .clickable { selectedDifficulty = diff }
                                                        .padding(vertical = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = diff.name,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSel) Color.White else Color(0xFF94A3B8)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Wind Dynamics Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "WIND DYNAMICS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        WindMode.values().forEach { wm ->
                                            val isSel = selectedWind == wm
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSel) Color(0xFF0284C7) else Color(0xFF1E293B))
                                                    .clickable { selectedWind = wm }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = wm.displayName,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSel) Color.White else Color(0xFF94A3B8)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "MATCH ROUNDS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(1, 3, 5, 10).forEach { rnd ->
                                            val isSel = selectedRounds == rnd
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSel) Color(0xFF0284C7) else Color(0xFF1E293B))
                                                    .clickable { selectedRounds = rnd }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$rnd",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSel) Color.White else Color(0xFF94A3B8)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Start Battle Button
                            Button(
                                onClick = {
                                    val newSettings = GameSettings(
                                        roundsCount = selectedRounds,
                                        matchMode = selectedMode,
                                        aiDifficulty = selectedDifficulty,
                                        windMode = selectedWind
                                    )
                                    onStartGame(newSettings)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("start_battle_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF22C55E)
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "START BATTLE",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Portrait Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .widthIn(max = 560.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Header / Title Banner
                    Text(
                        text = "SCORCHED TANKS",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFFDC2626))
                            )
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "THE MOTHER OF ALL ARTILLERY BATTLES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    // Match Mode Selector Cards
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "GAME MODE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            ModeOptionRow(
                                title = "Player vs AI",
                                subtitle = "Duel against tactical computer artillery",
                                icon = Icons.Default.SmartToy,
                                isSelected = selectedMode == MatchMode.VS_AI,
                                onClick = { selectedMode = MatchMode.VS_AI }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            ModeOptionRow(
                                title = "Pass & Play (2 Players)",
                                subtitle = "Hotseat 2-player battle on this device",
                                icon = Icons.Default.People,
                                isSelected = selectedMode == MatchMode.PASS_AND_PLAY,
                                onClick = { selectedMode = MatchMode.PASS_AND_PLAY }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            ModeOptionRow(
                                title = "Correspondence Play",
                                subtitle = "Async duel via turn share code / messages",
                                icon = Icons.Default.Email,
                                isSelected = selectedMode == MatchMode.CORRESPONDENCE,
                                onClick = { 
                                    selectedMode = MatchMode.CORRESPONDENCE
                                    scope.launch { CorrespondenceSlotsManager.refreshAllSlots(context) }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5 Independent Correspondence Slots Panel (accessible in Correspondence Mode)
                    if (selectedMode == MatchMode.CORRESPONDENCE) {
                        CorrespondenceGamesPanel(
                            onPlaySlot = onPlaySlot,
                            onOpenSettings = { showMariaDbSettings = true }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // AI Difficulty (if VS_AI)
                    if (selectedMode == MatchMode.VS_AI) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "AI DIFFICULTY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AiDifficulty.values().forEach { diff ->
                                        val isSel = selectedDifficulty == diff
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) Color(0xFF0284C7) else Color(0xFF1E293B))
                                                .clickable { selectedDifficulty = diff }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = diff.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) Color.White else Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Wind & Rounds Settings Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "WIND DYNAMICS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                WindMode.values().forEach { wm ->
                                    val isSel = selectedWind == wm
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) Color(0xFF0284C7) else Color(0xFF1E293B))
                                            .clickable { selectedWind = wm }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = wm.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "MATCH ROUNDS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1, 3, 5, 10).forEach { rnd ->
                                    val isSel = selectedRounds == rnd
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) Color(0xFF0284C7) else Color(0xFF1E293B))
                                            .clickable { selectedRounds = rnd }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$rnd ${if (rnd == 1) "Round" else "Rounds"}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Start Battle Button
                    Button(
                        onClick = {
                            val newSettings = GameSettings(
                                roundsCount = selectedRounds,
                                matchMode = selectedMode,
                                aiDifficulty = selectedDifficulty,
                                windMode = selectedWind
                            )
                            onStartGame(newSettings)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_battle_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF22C55E)
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "START BATTLE",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedMode != MatchMode.CORRESPONDENCE) {
                        OutlinedButton(
                            onClick = { 
                                selectedMode = MatchMode.CORRESPONDENCE
                                scope.launch { CorrespondenceSlotsManager.refreshAllSlots(context) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("open_correspondence_slots_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B))
                        ) {
                            Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "5 CORRESPONDENCE GAMES (SLOTS 1-5)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Load Correspondence Turn Code
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("load_turn_code_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LOAD CORRESPONDENCE TURN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showMariaDbSettings = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("mariadb_config_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MARIADB SERVER CONFIG (trsbox.net)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (showImportDialog) {
                CorrespondenceImportDialog(
                    onLoadCode = onLoadCorrespondenceCode,
                    onDismiss = { showImportDialog = false }
                )
            }

            if (showMariaDbSettings) {
                MariaDbSettingsDialog(
                    onDismiss = { showMariaDbSettings = false }
                )
            }
        }
    }
}

@Composable
private fun ModeOptionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A))
            .border(
                1.dp,
                if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }

        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                fontSize = 13.sp
            )
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
    }
}
