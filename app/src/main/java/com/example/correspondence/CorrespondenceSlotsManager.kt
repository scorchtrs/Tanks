package com.example.correspondence

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class CorrespondenceSlotState(
    val slotNumber: Int, // 1..5
    val matchId: String,
    val title: String,
    val turnNumber: Int = 0,
    val roundNumber: Int = 1,
    val activeTankIndex: Int = 0,
    val p1Name: String = "Player 1",
    val p2Name: String = "Player 2",
    val isGameOver: Boolean = false,
    val winnerName: String = "",
    val lastUpdated: String = "",
    val existsOnServer: Boolean = false,
    val isLoading: Boolean = false
)

object CorrespondenceSlotsManager {
    private const val PREFS_NAME = "scorched_slots_prefs"
    private const val KEY_SLOT_PREFIX = "slot_match_id_"
    const val TOTAL_SLOTS = 5

    private val _slotsState = MutableStateFlow<List<CorrespondenceSlotState>>(
        (1..TOTAL_SLOTS).map { slot ->
            CorrespondenceSlotState(
                slotNumber = slot,
                matchId = "EARTH-GAME-$slot",
                title = "Game $slot"
            )
        }
    )
    val slotsState: StateFlow<List<CorrespondenceSlotState>> = _slotsState.asStateFlow()

    fun getSlotMatchId(context: Context, slotNumber: Int): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("$KEY_SLOT_PREFIX$slotNumber", "EARTH-GAME-$slotNumber") ?: "EARTH-GAME-$slotNumber"
    }

    fun setSlotMatchId(context: Context, slotNumber: Int, matchId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("$KEY_SLOT_PREFIX$slotNumber", matchId.trim()).apply()

        val updated = _slotsState.value.map { slot ->
            if (slot.slotNumber == slotNumber) slot.copy(matchId = matchId.trim()) else slot
        }
        _slotsState.value = updated
    }

    /**
     * Updates player names for a specific slot match in MariaDB.
     */
    suspend fun updateSlotPlayerNames(
        context: Context,
        slotNumber: Int,
        p1Name: String,
        p2Name: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val matchId = getSlotMatchId(context, slotNumber)
        val res = MariaDbManager.updateMatchPlayerNames(context, matchId, p1Name, p2Name)
        refreshAllSlots(context)
        res
    }

    /**
     * Resets a slot for a brand-new match.
     */
    suspend fun resetSlotGame(context: Context, slotNumber: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        val matchId = getSlotMatchId(context, slotNumber)
        val res = MariaDbManager.resetMatch(context, matchId)
        refreshAllSlots(context)
        res
    }

    /**
     * Initializes slots from preferences and fetches their live status from MariaDB earth.
     */
    suspend fun refreshAllSlots(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentSlots = (1..TOTAL_SLOTS).map { slotNum ->
            val matchId = prefs.getString("$KEY_SLOT_PREFIX$slotNum", "EARTH-GAME-$slotNum") ?: "EARTH-GAME-$slotNum"
            CorrespondenceSlotState(
                slotNumber = slotNum,
                matchId = matchId,
                title = "Game $slotNum",
                isLoading = true
            )
        }
        _slotsState.value = currentSlots

        val matchIds = currentSlots.map { it.matchId }
        val result = MariaDbManager.fetchMatchesForSlots(context, matchIds)

        result.fold(
            onSuccess = { map ->
                val updated = currentSlots.map { slot ->
                    val serverSummary = map[slot.matchId]
                    if (serverSummary != null) {
                        slot.copy(
                            turnNumber = serverSummary.turnNumber,
                            roundNumber = serverSummary.roundNumber,
                            activeTankIndex = serverSummary.activeTankIndex,
                            p1Name = serverSummary.p1Name,
                            p2Name = serverSummary.p2Name,
                            isGameOver = serverSummary.isGameOver,
                            winnerName = serverSummary.winnerName,
                            lastUpdated = serverSummary.lastUpdated,
                            existsOnServer = true,
                            isLoading = false
                        )
                    } else {
                        slot.copy(
                            turnNumber = 0,
                            existsOnServer = false,
                            isLoading = false
                        )
                    }
                }
                _slotsState.value = updated
            },
            onFailure = {
                val updated = currentSlots.map { it.copy(isLoading = false) }
                _slotsState.value = updated
            }
        )
    }
}
