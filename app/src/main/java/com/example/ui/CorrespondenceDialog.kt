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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.correspondence.CorrespondenceManager
import com.example.correspondence.MariaDbManager
import com.example.correspondence.MariaDbMatchSummary
import kotlinx.coroutines.launch

@Composable
fun CorrespondenceShareDialog(
    turnCode: String,
    matchId: String = "EARTH-GAME-1",
    isGameOver: Boolean = false,
    winnerName: String = "",
    p1Name: String = "Player 1",
    p2Name: String = "Player 2",
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    // MariaDB State
    val prefs = remember { context.getSharedPreferences("scorched_mariadb_prefs", android.content.Context.MODE_PRIVATE) }
    var currentMatchId by remember(matchId) {
        mutableStateOf(matchId.ifBlank { prefs.getString("last_active_match_id", "EARTH-GAME-1") ?: "EARTH-GAME-1" })
    }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val decodedTurn = remember(turnCode) { CorrespondenceManager.decodeTurn(turnCode) }

    // Try auto-upload if configured
    LaunchedEffect(turnCode) {
        val config = MariaDbManager.loadConfig(context)
        if (config.user.isNotBlank() && decodedTurn != null) {
            isUploading = true
            val res = MariaDbManager.saveTurn(
                context = context,
                matchId = currentMatchId.trim(),
                turnData = decodedTurn,
                turnCode = turnCode,
                p1Name = p1Name,
                p2Name = p2Name,
                isGameOver = isGameOver,
                winnerName = winnerName
            )
            isUploading = false
            res.fold(
                onSuccess = {
                    val statusText = if (isGameOver) {
                        "Game Over synced to MariaDB earth! Winner: $winnerName"
                    } else {
                        "Turn #${decodedTurn.turnNumber} synced to MariaDB earth@trsbox.net!"
                    }
                    uploadStatus = Pair(true, statusText)
                    prefs.edit().putString("last_active_match_id", currentMatchId.trim()).apply()
                },
                onFailure = { err ->
                    uploadStatus = Pair(false, "Server sync failed: ${err.localizedMessage}")
                }
            )
        }
    }

    Dialog(
        onDismissRequest = onContinue,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .padding(12.dp)
                .testTag("correspondence_share_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = "Correspondence Play",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(32.dp)
                    )

                    IconButton(
                        onClick = { showSettingsDialog = true }
                    ) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = "MariaDB Settings",
                            tint = Color(0xFF10B981)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "YOUR SHOT IS COMPLETE!",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    text = "Turn state can be sent to your opponent via remote MariaDB database 'earth' or shared manually.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // MariaDB Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MARIADB DATABASE (earth@trsbox.net)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399)
                            )
                            val config = MariaDbManager.loadConfig(context)
                            Text(
                                text = if (config.user.isBlank()) "No credentials" else "User: ${config.user}",
                                fontSize = 10.sp,
                                color = if (config.user.isBlank()) Color(0xFFF87171) else Color(0xFF94A3B8)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = currentMatchId,
                                onValueChange = { currentMatchId = it },
                                label = { Text("Match ID", fontSize = 11.sp) },
                                modifier = Modifier.weight(1.8f),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Button(
                                onClick = {
                                    if (decodedTurn != null) {
                                        val config = MariaDbManager.loadConfig(context)
                                        if (config.user.isBlank()) {
                                            showSettingsDialog = true
                                            return@Button
                                        }
                                        isUploading = true
                                        uploadStatus = null
                                        scope.launch {
                                            val res = MariaDbManager.saveTurn(
                                                context = context,
                                                matchId = currentMatchId.trim(),
                                                turnData = decodedTurn,
                                                turnCode = turnCode,
                                                p1Name = p1Name,
                                                p2Name = p2Name,
                                                isGameOver = isGameOver,
                                                winnerName = winnerName
                                            )
                                            isUploading = false
                                            res.fold(
                                                onSuccess = {
                                                    val statusText = if (isGameOver) {
                                                        "Game Over uploaded! Winner: $winnerName"
                                                    } else {
                                                        "Turn #${decodedTurn.turnNumber} uploaded to MariaDB earth!"
                                                    }
                                                    uploadStatus = Pair(true, statusText)
                                                    prefs.edit().putString("last_active_match_id", currentMatchId.trim()).apply()
                                                },
                                                onFailure = { err ->
                                                    uploadStatus = Pair(false, "Upload error: ${err.localizedMessage}")
                                                }
                                            )
                                        }
                                    }
                                },
                                enabled = !isUploading && currentMatchId.isNotBlank(),
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                if (isUploading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SYNC", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Upload result message
                        if (uploadStatus != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val (ok, msg) = uploadStatus!!
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (ok) Color(0xFF34D399) else Color(0xFFF87171),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = msg,
                                    fontSize = 11.sp,
                                    color = if (ok) Color(0xFF34D399) else Color(0xFFF87171)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Manual Turn code snippet
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "MANUAL TURN CODE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = turnCode.take(65) + if (turnCode.length > 65) "..." else "",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Copy & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            CorrespondenceManager.copyToClipboard(context, turnCode)
                            copied = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                        Text(if (copied) "COPIED!" else "COPY CODE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            CorrespondenceManager.shareTurnIntent(context, turnCode, decodedTurn?.turnNumber ?: 1)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                        Text("SHARE...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                ) {
                    Text("CONTINUE PLAYING", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    if (showSettingsDialog) {
        MariaDbSettingsDialog(
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun CorrespondenceImportDialog(
    onLoadCode: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("scorched_mariadb_prefs", android.content.Context.MODE_PRIVATE) }

    var selectedTab by remember { mutableStateOf(0) } // 0: MariaDB, 1: Paste Code
    var matchIdInput by remember {
        mutableStateOf(prefs.getString("last_active_match_id", "EARTH-ALPHA-1") ?: "EARTH-ALPHA-1")
    }
    var inputCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    var isFetching by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Active Matches on MariaDB
    var matchesList by remember { mutableStateOf<List<MariaDbMatchSummary>>(emptyList()) }
    var isLoadingMatches by remember { mutableStateOf(false) }

    // Load active matches on open
    fun refreshMatches() {
        val config = MariaDbManager.loadConfig(context)
        if (config.user.isNotBlank()) {
            isLoadingMatches = true
            scope.launch {
                val res = MariaDbManager.listActiveMatches(context)
                isLoadingMatches = false
                res.fold(
                    onSuccess = { matchesList = it },
                    onFailure = { /* silent or show */ }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshMatches()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .padding(12.dp)
                .testTag("correspondence_import_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = "Load Turn",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(32.dp)
                    )

                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = "Database Settings",
                            tint = Color(0xFF10B981)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "LOAD CORRESPONDENCE TURN",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    text = "Fetch opponent's shot from the MariaDB server 'earth' or paste the turn code.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: MariaDB vs Paste Code
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF38BDF8)
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("MARIADB SERVER", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("PASTE CODE", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // MariaDB Tab
                    val config = MariaDbManager.loadConfig(context)
                    if (config.user.isBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF332005)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Credentials Required",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFDE68A)
                                )
                                Text(
                                    text = "MariaDB username and password for database 'earth' on trsbox.net have not been configured yet.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFCD34D),
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showSettingsDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("CONFIGURE MARIADB CREDENTIALS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = matchIdInput,
                            onValueChange = {
                                matchIdInput = it
                                errorMessage = null
                            },
                            label = { Text("Match ID (e.g. EARTH-ALPHA-1)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (matchIdInput.isBlank()) {
                                    errorMessage = "Please enter a Match ID"
                                    return@Button
                                }
                                isFetching = true
                                errorMessage = null
                                successMessage = null
                                scope.launch {
                                    val res = MariaDbManager.fetchLatestTurn(context, matchIdInput.trim())
                                    isFetching = false
                                    res.fold(
                                        onSuccess = { turnPair ->
                                            if (turnPair == null) {
                                                errorMessage = "No match found with ID '${matchIdInput.trim()}'"
                                            } else {
                                                val (data, code) = turnPair
                                                val loaded = onLoadCode(code)
                                                if (loaded) {
                                                    prefs.edit().putString("last_active_match_id", matchIdInput.trim()).apply()
                                                    onDismiss()
                                                } else {
                                                    errorMessage = "Failed to load turn state into battle!"
                                                }
                                            }
                                        },
                                        onFailure = { err ->
                                            errorMessage = "Database error: ${err.localizedMessage}"
                                        }
                                    )
                                }
                            },
                            enabled = !isFetching && matchIdInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            if (isFetching) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CONNECTING TO MARIADB...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("FETCH & LOAD TURN FROM EARTH", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Active matches list from earth db
                        if (matchesList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ACTIVE MATCHES ON SERVER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                                IconButton(
                                    onClick = { refreshMatches() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Refresh Matches",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (match in matchesList.take(4)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                matchIdInput = match.matchId
                                            }
                                            .background(
                                                if (matchIdInput == match.matchId) Color(0xFF0F172A) else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = match.matchId,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Turn #${match.turnNumber} • Tank ${match.activeTankIndex + 1}",
                                                fontSize = 10.sp,
                                                color = Color(0xFF38BDF8)
                                            )
                                        }
                                        Text(
                                            text = "SELECT",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Manual Code Paste Tab
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = {
                            inputCode = it
                            errorMessage = null
                        },
                        label = { Text("Paste SCORCHED-... Turn Code", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("turn_code_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clip = CorrespondenceManager.getFromClipboard(context)
                                if (!clip.isNullOrBlank()) {
                                    inputCode = clip
                                    errorMessage = null
                                } else {
                                    errorMessage = "Clipboard is empty!"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                        ) {
                            Text("PASTE CLIPBOARD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val success = onLoadCode(inputCode)
                                if (!success) {
                                    errorMessage = "Invalid Turn Code! Please verify the text."
                                } else {
                                    onDismiss()
                                }
                            },
                            enabled = inputCode.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Text("LOAD BATTLE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("CANCEL", fontSize = 12.sp, color = Color(0xFF94A3B8))
                }
            }
        }
    }

    if (showSettingsDialog) {
        MariaDbSettingsDialog(
            onDismiss = {
                showSettingsDialog = false
                refreshMatches()
            }
        )
    }
}
