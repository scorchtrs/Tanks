package com.example.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.correspondence.CorrespondenceSlotState
import com.example.correspondence.CorrespondenceSlotsManager
import com.example.correspondence.MariaDbManager
import kotlinx.coroutines.launch

@Composable
fun CorrespondenceGamesPanel(
    onPlaySlot: (slotNumber: Int, matchId: String, turnCode: String?, p1Name: String?, p2Name: String?) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val slots by CorrespondenceSlotsManager.slotsState.collectAsState()
    var isRefreshingAll by remember { mutableStateOf(false) }
    var slotLoadingIndex by remember { mutableStateOf<Int?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Dialog to rename slot's matchId
    var editingSlot by remember { mutableStateOf<CorrespondenceSlotState?>(null) }
    var editMatchIdText by remember { mutableStateOf("") }

    // Dialog to rename player commanders
    var editingNamesSlot by remember { mutableStateOf<CorrespondenceSlotState?>(null) }
    var editP1Name by remember { mutableStateOf("") }
    var editP2Name by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        CorrespondenceSlotsManager.refreshAllSlots(context)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("correspondence_games_panel"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10192C)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "5 CORRESPONDENCE GAMES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Play 5 parallel matches on earth@trsbox.net until game over",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            isRefreshingAll = true
                            scope.launch {
                                CorrespondenceSlotsManager.refreshAllSlots(context)
                                isRefreshingAll = false
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (isRefreshingAll) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF38BDF8), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh slots", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        }
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = "MariaDB Settings", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = statusMessage!!,
                    fontSize = 10.sp,
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5 Game Slots List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                slots.forEach { slot ->
                    CorrespondenceSlotItem(
                        slot = slot,
                        isLoadingAction = slotLoadingIndex == slot.slotNumber,
                        onPlayOrResume = {
                            slotLoadingIndex = slot.slotNumber
                            statusMessage = null
                            scope.launch {
                                val config = MariaDbManager.loadConfig(context)
                                if (config.user.isBlank()) {
                                    slotLoadingIndex = null
                                    onOpenSettings()
                                    return@launch
                                }

                                if (slot.existsOnServer) {
                                    // Fetch latest turn
                                    val res = MariaDbManager.fetchLatestTurn(context, slot.matchId)
                                    slotLoadingIndex = null
                                    res.fold(
                                        onSuccess = { turnResult ->
                                            if (turnResult != null) {
                                                onPlaySlot(slot.slotNumber, slot.matchId, turnResult.turnCode, turnResult.p1Name, turnResult.p2Name)
                                            } else {
                                                // Start new match
                                                onPlaySlot(slot.slotNumber, slot.matchId, null, slot.p1Name, slot.p2Name)
                                            }
                                        },
                                        onFailure = { err ->
                                            statusMessage = "Slot ${slot.slotNumber} error: ${err.localizedMessage}"
                                        }
                                    )
                                } else {
                                    // New game
                                    slotLoadingIndex = null
                                    onPlaySlot(slot.slotNumber, slot.matchId, null, slot.p1Name, slot.p2Name)
                                }
                            }
                        },
                        onResetGame = {
                            scope.launch {
                                slotLoadingIndex = slot.slotNumber
                                CorrespondenceSlotsManager.resetSlotGame(context, slot.slotNumber)
                                slotLoadingIndex = null
                            }
                        },
                        onEditMatchId = {
                            editingSlot = slot
                            editMatchIdText = slot.matchId
                        },
                        onEditPlayerNames = {
                            editingNamesSlot = slot
                            editP1Name = slot.p1Name
                            editP2Name = slot.p2Name
                        }
                    )
                }
            }
        }
    }

    // Dialog to edit/rename Match ID for a slot
    if (editingSlot != null) {
        val slot = editingSlot!!
        AlertDialog(
            onDismissRequest = { editingSlot = null },
            title = {
                Text("Configure Slot ${slot.slotNumber} Match ID", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Set the Match ID used on database 'earth' for this slot. Share this same ID with your opponent so both connect to the same battle.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editMatchIdText,
                        onValueChange = { editMatchIdText = it.uppercase() },
                        label = { Text("Match ID") },
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
                        if (editMatchIdText.isNotBlank()) {
                            CorrespondenceSlotsManager.setSlotMatchId(context, slot.slotNumber, editMatchIdText.trim())
                            scope.launch { CorrespondenceSlotsManager.refreshAllSlots(context) }
                        }
                        editingSlot = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingSlot = null }) {
                    Text("CANCEL")
                }
            },
            containerColor = Color(0xFF0F172A),
            textContentColor = Color.White,
            titleContentColor = Color.White
        )
    }

    // Dialog to edit player commander names for a slot
    if (editingNamesSlot != null) {
        val slot = editingNamesSlot!!
        AlertDialog(
            onDismissRequest = { editingNamesSlot = null },
            title = {
                Text("Change Names (Slot ${slot.slotNumber})", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Set commander names for this correspondence match. Names are saved to MariaDB database 'earth' on trsbox.net.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editP1Name,
                        onValueChange = { editP1Name = it },
                        label = { Text("Player 1 Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedTextColor = Color(0xFF00E5FF),
                            unfocusedTextColor = Color(0xFF00E5FF)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editP2Name,
                        onValueChange = { editP2Name = it },
                        label = { Text("Player 2 Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedTextColor = Color(0xFFFF5252),
                            unfocusedTextColor = Color(0xFFFF5252)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p1 = editP1Name.ifBlank { "Player 1" }
                        val p2 = editP2Name.ifBlank { "Player 2" }
                        scope.launch {
                            CorrespondenceSlotsManager.updateSlotPlayerNames(context, slot.slotNumber, p1, p2)
                        }
                        editingNamesSlot = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("SAVE TO DATABASE")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingNamesSlot = null }) {
                    Text("CANCEL")
                }
            },
            containerColor = Color(0xFF0F172A),
            textContentColor = Color.White,
            titleContentColor = Color.White
        )
    }
}

@Composable
private fun CorrespondenceSlotItem(
    slot: CorrespondenceSlotState,
    isLoadingAction: Boolean,
    onPlayOrResume: () -> Unit,
    onResetGame: () -> Unit,
    onEditMatchId: () -> Unit,
    onEditPlayerNames: () -> Unit
) {
    val borderColor = when {
        slot.isGameOver -> Color(0xFFEAB308)
        slot.existsOnServer -> Color(0xFF10B981)
        else -> Color(0xFF334155)
    }

    val cardBg = when {
        slot.isGameOver -> Color(0xFF1C1917)
        slot.existsOnServer -> Color(0xFF0E221D)
        else -> Color(0xFF0B1322)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(10.dp))
            .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(10.dp)
            .testTag("slot_item_${slot.slotNumber}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Slot Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge: Slot Number
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1E293B), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SLOT ${slot.slotNumber}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = slot.matchId,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )

                    IconButton(
                        onClick = onEditMatchId,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Match ID",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Player Names Line with Change Name Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onEditPlayerNames() }
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "${slot.p1Name}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                    Text(
                        text = " vs ",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${slot.p2Name}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5252)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Change Names",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(11.dp)
                    )
                }

                // Status line
                if (slot.isGameOver) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFEAB308),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GAME OVER: ${slot.winnerName.ifBlank { "Battle Concluded" }} (Turn #${slot.turnNumber})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEAB308)
                        )
                    }
                } else if (slot.existsOnServer) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val turnWhose = if (slot.activeTankIndex == 0) "${slot.p1Name}'s Turn" else "${slot.p2Name}'s Turn"
                        Text(
                            text = "IN PROGRESS: Turn #${slot.turnNumber} • $turnWhose",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF34D399)
                        )
                    }
                } else {
                    Text(
                        text = "EMPTY • Tap to start battle",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                if (slot.lastUpdated.isNotBlank()) {
                    Text(
                        text = "Updated: ${slot.lastUpdated.take(16)}",
                        fontSize = 9.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (slot.isGameOver) {
                    // Start fresh game after game over
                    Button(
                        onClick = onResetGame,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        modifier = Modifier.testTag("reset_slot_${slot.slotNumber}")
                    ) {
                        if (isLoadingAction) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("NEW GAME", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Play or Resume
                    Button(
                        onClick = onPlayOrResume,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (slot.existsOnServer) Color(0xFF10B981) else Color(0xFF0284C7)
                        ),
                        modifier = Modifier.testTag("play_slot_${slot.slotNumber}")
                    ) {
                        if (isLoadingAction) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (slot.existsOnServer) "RESUME" else "PLAY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
