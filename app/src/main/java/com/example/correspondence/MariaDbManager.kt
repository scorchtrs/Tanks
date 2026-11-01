package com.example.correspondence

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager

data class MariaDbConfig(
    val host: String = "trsbox.net",
    val port: Int = 3306,
    val database: String = "earth",
    val user: String = "earth_user",
    val pass: String = "YourSecurePasswordHere"
)

data class MariaDbMatchSummary(
    val matchId: String,
    val turnNumber: Int,
    val roundNumber: Int = 1,
    val activeTankIndex: Int = 0,
    val p1Name: String = "Player 1",
    val p2Name: String = "Player 2",
    val isGameOver: Boolean = false,
    val winnerName: String = "",
    val lastUpdated: String = ""
)

data class MariaDbTurnResult(
    val turnData: CorrespondenceTurnData,
    val turnCode: String,
    val p1Name: String = "Player 1",
    val p2Name: String = "Player 2",
    val isGameOver: Boolean = false,
    val winnerName: String = ""
)

object MariaDbManager {
    private const val TAG = "MariaDbManager"
    private const val PREFS_NAME = "scorched_mariadb_prefs"
    private const val KEY_HOST = "mariadb_host"
    private const val KEY_PORT = "mariadb_port"
    private const val KEY_DB = "mariadb_db"
    private const val KEY_USER = "mariadb_user"
    private const val KEY_PASS = "mariadb_pass"

    private var cachedConfig: MariaDbConfig? = null

    init {
        try {
            Class.forName("org.mariadb.jdbc.Driver")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load MariaDB JDBC Driver", e)
        }
    }

    fun loadConfig(context: Context): MariaDbConfig {
        cachedConfig?.let { return it }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val config = MariaDbConfig(
            host = prefs.getString(KEY_HOST, "trsbox.net") ?: "trsbox.net",
            port = prefs.getInt(KEY_PORT, 3306),
            database = prefs.getString(KEY_DB, "earth") ?: "earth",
            user = prefs.getString(KEY_USER, "earth_user") ?: "earth_user",
            pass = prefs.getString(KEY_PASS, "YourSecurePasswordHere") ?: "YourSecurePasswordHere"
        )
        cachedConfig = config
        return config
    }

    fun saveConfig(context: Context, config: MariaDbConfig) {
        cachedConfig = config
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_HOST, config.host)
            .putInt(KEY_PORT, config.port)
            .putString(KEY_DB, config.database)
            .putString(KEY_USER, config.user)
            .putString(KEY_PASS, config.pass)
            .apply()
    }

    private fun getConnection(config: MariaDbConfig): Connection {
        val url = "jdbc:mariadb://${config.host}:${config.port}/${config.database}?connectTimeout=5000&socketTimeout=8000"
        DriverManager.setLoginTimeout(5)
        return DriverManager.getConnection(url, config.user, config.pass)
    }

    /**
     * Tests connectivity to the MariaDB server.
     */
    suspend fun testConnection(config: MariaDbConfig): Result<String> = withContext(Dispatchers.IO) {
        try {
            getConnection(config).use { conn ->
                val meta = conn.metaData
                val version = "${meta.databaseProductName} ${meta.databaseProductVersion}"
                initTables(conn)
                Result.success("Connected successfully to $version on ${config.database}@${config.host}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "MariaDB connection test failed", e)
            Result.failure(e)
        }
    }

    private fun initTables(conn: Connection) {
        val createMatchesTable = """
            CREATE TABLE IF NOT EXISTS scorched_matches (
                match_id VARCHAR(64) PRIMARY KEY,
                turn_number INT NOT NULL,
                round_number INT NOT NULL DEFAULT 1,
                active_tank_index INT NOT NULL,
                p1_name VARCHAR(64) NOT NULL DEFAULT 'Player 1',
                p2_name VARCHAR(64) NOT NULL DEFAULT 'Player 2',
                last_weapon VARCHAR(64) NOT NULL DEFAULT '',
                turn_code MEDIUMTEXT NOT NULL,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """.trimIndent()

        val createHistoryTable = """
            CREATE TABLE IF NOT EXISTS scorched_turn_history (
                id INT AUTO_INCREMENT PRIMARY KEY,
                match_id VARCHAR(64) NOT NULL,
                turn_number INT NOT NULL,
                round_number INT NOT NULL DEFAULT 1,
                active_tank_index INT NOT NULL,
                turn_code MEDIUMTEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_match_id (match_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """.trimIndent()

        try {
            conn.createStatement().use { stmt ->
                stmt.executeUpdate(createMatchesTable)
                stmt.executeUpdate(createHistoryTable)
            }
        } catch (e: Exception) {
            Log.w(TAG, "initTables note: ${e.message}")
        }
    }

    /**
     * Saves a completed turn to MariaDB, using the verified schema on earth@trsbox.net.
     * Round number, player names, and weapons are persisted to SQL columns;
     * game over status and winner are safely embedded within the encoded turn_code.
     */
    suspend fun saveTurn(
        context: Context,
        matchId: String,
        turnData: CorrespondenceTurnData,
        turnCode: String,
        p1Name: String = "Player 1",
        p2Name: String = "Player 2",
        isGameOver: Boolean = false,
        winnerName: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val config = loadConfig(context)
        if (config.user.isBlank()) {
            return@withContext Result.failure(IllegalStateException("MariaDB username is not configured"))
        }

        try {
            getConnection(config).use { conn ->
                initTables(conn)

                val effectiveTurnCode = if (turnData.isGameOver != isGameOver || turnData.winnerName != winnerName) {
                    CorrespondenceManager.encodeTurn(turnData.copy(isGameOver = isGameOver, winnerName = winnerName))
                } else {
                    turnCode
                }

                val upsertSql = """
                    INSERT INTO scorched_matches 
                        (match_id, turn_number, round_number, active_tank_index, p1_name, p2_name, last_weapon, turn_code, last_updated)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    ON DUPLICATE KEY UPDATE
                        turn_number = VALUES(turn_number),
                        round_number = VALUES(round_number),
                        active_tank_index = VALUES(active_tank_index),
                        p1_name = VALUES(p1_name),
                        p2_name = VALUES(p2_name),
                        last_weapon = VALUES(last_weapon),
                        turn_code = VALUES(turn_code),
                        last_updated = CURRENT_TIMESTAMP;
                """.trimIndent()

                conn.prepareStatement(upsertSql).use { ps ->
                    ps.setString(1, matchId.trim())
                    ps.setInt(2, turnData.turnNumber)
                    ps.setInt(3, maxOf(1, turnData.roundNumber))
                    ps.setInt(4, turnData.activeTankIndex)
                    ps.setString(5, p1Name.ifBlank { "Player 1" })
                    ps.setString(6, p2Name.ifBlank { "Player 2" })
                    ps.setString(7, turnData.lastWeaponName)
                    ps.setString(8, effectiveTurnCode)
                    ps.executeUpdate()
                }

                // Add to history log
                try {
                    val historySql = """
                        INSERT INTO scorched_turn_history 
                            (match_id, turn_number, round_number, active_tank_index, turn_code)
                        VALUES (?, ?, ?, ?, ?);
                    """.trimIndent()

                    conn.prepareStatement(historySql).use { ps ->
                        ps.setString(1, matchId.trim())
                        ps.setInt(2, turnData.turnNumber)
                        ps.setInt(3, maxOf(1, turnData.roundNumber))
                        ps.setInt(4, turnData.activeTankIndex)
                        ps.setString(5, effectiveTurnCode)
                        ps.executeUpdate()
                    }
                } catch (eh: Exception) {
                    Log.w(TAG, "History log insert note: ${eh.message}")
                }

                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save turn to MariaDB", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves the latest turn code for a specific match from MariaDB.
     */
    suspend fun fetchLatestTurn(
        context: Context,
        matchId: String
    ): Result<MariaDbTurnResult?> = withContext(Dispatchers.IO) {
        val config = loadConfig(context)
        if (config.user.isBlank()) {
            return@withContext Result.failure(IllegalStateException("MariaDB username is not configured"))
        }

        try {
            getConnection(config).use { conn ->
                initTables(conn)
                val selectSql = """
                    SELECT match_id, turn_number, round_number, active_tank_index, p1_name, p2_name, last_weapon, turn_code, last_updated 
                    FROM scorched_matches 
                    WHERE match_id = ?
                """.trimIndent()

                conn.prepareStatement(selectSql).use { ps ->
                    ps.setString(1, matchId.trim())
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            val code = rs.getString("turn_code") ?: ""
                            val p1 = rs.getString("p1_name") ?: "Player 1"
                            val p2 = rs.getString("p2_name") ?: "Player 2"

                            val decoded = CorrespondenceManager.decodeTurn(code)
                            if (decoded != null) {
                                val isGameOver = decoded.isGameOver || (decoded.p1Health <= 0 || decoded.p2Health <= 0)
                                var winner = decoded.winnerName
                                if (winner.isBlank() && isGameOver) {
                                    winner = if (decoded.p1Health > 0) p1 else if (decoded.p2Health > 0) p2 else "Draw"
                                }
                                Result.success(MariaDbTurnResult(decoded, code, p1, p2, isGameOver, winner))
                            } else {
                                Result.failure(IllegalArgumentException("Corrupted turn data in database for match $matchId"))
                            }
                        } else {
                            Result.success(null)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch turn from MariaDB", e)
            Result.failure(e)
        }
    }

    /**
     * Updates player names for a match in MariaDB.
     */
    suspend fun updateMatchPlayerNames(
        context: Context,
        matchId: String,
        p1Name: String,
        p2Name: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val config = loadConfig(context)
        try {
            getConnection(config).use { conn ->
                initTables(conn)
                val sql = "UPDATE scorched_matches SET p1_name = ?, p2_name = ? WHERE match_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, p1Name.trim().ifBlank { "Player 1" })
                    ps.setString(2, p2Name.trim().ifBlank { "Player 2" })
                    ps.setString(3, matchId.trim())
                    ps.executeUpdate()
                }
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update player names in MariaDB", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches status of multiple matches (e.g. for the 5 slots) in a single query.
     */
    suspend fun fetchMatchesForSlots(
        context: Context,
        matchIds: List<String>
    ): Result<Map<String, MariaDbMatchSummary>> = withContext(Dispatchers.IO) {
        val config = loadConfig(context)
        if (config.user.isBlank()) {
            return@withContext Result.failure(IllegalStateException("MariaDB credentials not configured"))
        }

        try {
            getConnection(config).use { conn ->
                initTables(conn)
                if (matchIds.isEmpty()) return@withContext Result.success(emptyMap())

                val placeholders = matchIds.joinToString(",") { "?" }
                val sql = """
                    SELECT match_id, turn_number, round_number, active_tank_index, p1_name, p2_name, last_weapon, turn_code, last_updated 
                    FROM scorched_matches 
                    WHERE match_id IN ($placeholders);
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    matchIds.forEachIndexed { index, id ->
                        ps.setString(index + 1, id.trim())
                    }
                    ps.executeQuery().use { rs ->
                        val map = mutableMapOf<String, MariaDbMatchSummary>()
                        while (rs.next()) {
                            val id = rs.getString("match_id") ?: continue
                            val p1Name = rs.getString("p1_name") ?: "Player 1"
                            val p2Name = rs.getString("p2_name") ?: "Player 2"
                            val code = rs.getString("turn_code") ?: ""

                            var isGameOver = false
                            var winnerName = ""

                            if (code.isNotBlank()) {
                                try {
                                    val decoded = CorrespondenceManager.decodeTurn(code)
                                    if (decoded != null) {
                                        isGameOver = decoded.isGameOver || (decoded.p1Health <= 0 || decoded.p2Health <= 0)
                                        winnerName = if (decoded.winnerName.isNotBlank()) {
                                            decoded.winnerName
                                        } else if (decoded.p1Health > 0 && decoded.p2Health <= 0) {
                                            p1Name
                                        } else if (decoded.p2Health > 0 && decoded.p1Health <= 0) {
                                            p2Name
                                        } else if (decoded.p1Health <= 0 && decoded.p2Health <= 0) {
                                            "Draw"
                                        } else {
                                            ""
                                        }
                                    }
                                } catch (_: Exception) {}
                            }

                            val lastUpdated = try {
                                rs.getTimestamp("last_updated")?.toString() ?: ""
                            } catch (_: Exception) { "" }

                            map[id] = MariaDbMatchSummary(
                                matchId = id,
                                turnNumber = rs.getInt("turn_number"),
                                roundNumber = rs.getInt("round_number"),
                                activeTankIndex = rs.getInt("active_tank_index"),
                                p1Name = p1Name,
                                p2Name = p2Name,
                                isGameOver = isGameOver,
                                winnerName = winnerName,
                                lastUpdated = lastUpdated
                            )
                        }
                        Result.success(map)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch matches for slots", e)
            Result.failure(e)
        }
    }

    /**
     * Resets or deletes a match when starting a fresh game in that slot.
     */
    suspend fun resetMatch(
        context: Context,
        matchId: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val config = loadConfig(context)
        if (config.user.isBlank()) return@withContext Result.failure(IllegalStateException("No credentials"))
        try {
            getConnection(config).use { conn ->
                val sql = "DELETE FROM scorched_matches WHERE match_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, matchId.trim())
                    ps.executeUpdate()
                }
                try {
                    val sqlHist = "DELETE FROM scorched_turn_history WHERE match_id = ?"
                    conn.prepareStatement(sqlHist).use { ps ->
                        ps.setString(1, matchId.trim())
                        ps.executeUpdate()
                    }
                } catch (_: Exception) {}
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lists active matches from MariaDB.
     */
    suspend fun listActiveMatches(
        context: Context
    ): Result<List<MariaDbMatchSummary>> = withContext(Dispatchers.IO) {
        val config = loadConfig(context)
        if (config.user.isBlank()) {
            return@withContext Result.failure(IllegalStateException("MariaDB username is not configured"))
        }

        try {
            getConnection(config).use { conn ->
                initTables(conn)
                val listSql = """
                    SELECT match_id, turn_number, round_number, active_tank_index, p1_name, p2_name, last_weapon, turn_code, last_updated 
                    FROM scorched_matches 
                    ORDER BY last_updated DESC 
                    LIMIT 20;
                """.trimIndent()

                conn.createStatement().use { stmt ->
                    stmt.executeQuery(listSql).use { rs ->
                        val list = mutableListOf<MariaDbMatchSummary>()
                        while (rs.next()) {
                            val id = rs.getString("match_id") ?: continue
                            val p1Name = rs.getString("p1_name") ?: "Player 1"
                            val p2Name = rs.getString("p2_name") ?: "Player 2"
                            val code = rs.getString("turn_code") ?: ""

                            var isGameOver = false
                            var winnerName = ""

                            if (code.isNotBlank()) {
                                try {
                                    val decoded = CorrespondenceManager.decodeTurn(code)
                                    if (decoded != null) {
                                        isGameOver = decoded.isGameOver || (decoded.p1Health <= 0 || decoded.p2Health <= 0)
                                        winnerName = if (decoded.winnerName.isNotBlank()) {
                                            decoded.winnerName
                                        } else if (decoded.p1Health > 0 && decoded.p2Health <= 0) {
                                            p1Name
                                        } else if (decoded.p2Health > 0 && decoded.p1Health <= 0) {
                                            p2Name
                                        } else if (decoded.p1Health <= 0 && decoded.p2Health <= 0) {
                                            "Draw"
                                        } else {
                                            ""
                                        }
                                    }
                                } catch (_: Exception) {}
                            }

                            val lastUpdated = try {
                                rs.getTimestamp("last_updated")?.toString() ?: ""
                            } catch (_: Exception) { "" }

                            list.add(
                                MariaDbMatchSummary(
                                    matchId = id,
                                    turnNumber = rs.getInt("turn_number"),
                                    roundNumber = rs.getInt("round_number"),
                                    activeTankIndex = rs.getInt("active_tank_index"),
                                    p1Name = p1Name,
                                    p2Name = p2Name,
                                    isGameOver = isGameOver,
                                    winnerName = winnerName,
                                    lastUpdated = lastUpdated
                                )
                            )
                        }
                        Result.success(list)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list matches from MariaDB", e)
            Result.failure(e)
        }
    }
}
