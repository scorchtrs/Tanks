package com.example.correspondence

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.example.model.PowerUpType
import com.example.model.WeaponType
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

data class CorrespondenceTurnData(
    val turnNumber: Int,
    val roundNumber: Int,
    val seed: Long,
    val windSpeed: Float,
    val activeTankIndex: Int,
    val p1Health: Int,
    val p1Shield: Int,
    val p1Cash: Int,
    val p1X: Float,
    val p2Health: Int,
    val p2Shield: Int,
    val p2Cash: Int,
    val p2X: Float,
    // Last fired action for replay
    val lastFiredPlayerId: Int = -1,
    val lastFiredAngle: Float = 0f,
    val lastFiredPower: Float = 0f,
    val lastWeaponName: String = "",
    // Ground modifications (crater list cx, cy, radius, isAddDirt)
    val craters: List<CraterRecord> = emptyList(),
    // Active crates (type, x, isGrounded)
    val crates: List<CrateRecord> = emptyList(),
    val isGameOver: Boolean = false,
    val winnerName: String = ""
)

data class CraterRecord(val cx: Float, val cy: Float, val radius: Float, val isDirt: Boolean)
data class CrateRecord(val type: PowerUpType, val x: Float, val isGrounded: Boolean)

object SimpleBase64 {
    private const val TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun encode(data: ByteArray): String {
        val out = StringBuilder((data.size * 4 + 2) / 3)
        var i = 0
        while (i < data.size) {
            val b0 = data[i].toInt() and 0xFF
            val b1 = if (i + 1 < data.size) data[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < data.size) data[i + 2].toInt() and 0xFF else 0

            out.append(TABLE[b0 ushr 2])
            out.append(TABLE[((b0 and 0x03) shl 4) or (b1 ushr 4)])
            if (i + 1 < data.size) {
                out.append(TABLE[((b1 and 0x0F) shl 2) or (b2 ushr 6)])
            } else {
                out.append('=')
            }
            if (i + 2 < data.size) {
                out.append(TABLE[b2 and 0x3F])
            } else {
                out.append('=')
            }
            i += 3
        }
        return out.toString()
    }

    fun decode(input: String): ByteArray {
        val clean = input.filter { it in TABLE || it == '=' }
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < clean.length) {
            val c0 = TABLE.indexOf(clean[i])
            val c1 = if (i + 1 < clean.length) TABLE.indexOf(clean[i + 1]) else -1
            val c2 = if (i + 2 < clean.length && clean[i + 2] != '=') TABLE.indexOf(clean[i + 2]) else -1
            val c3 = if (i + 3 < clean.length && clean[i + 3] != '=') TABLE.indexOf(clean[i + 3]) else -1

            if (c0 != -1 && c1 != -1) {
                out.write((c0 shl 2) or (c1 ushr 4))
                if (c2 != -1) {
                    out.write(((c1 and 0x0F) shl 4) or (c2 ushr 2))
                    if (c3 != -1) {
                        out.write(((c2 and 0x03) shl 6) or c3)
                    }
                }
            }
            i += 4
        }
        return out.toByteArray()
    }
}

object CorrespondenceManager {

    fun encodeTurn(data: CorrespondenceTurnData): String {
        val json = JSONObject()
        json.put("v", 1)
        json.put("turn", data.turnNumber)
        json.put("rnd", data.roundNumber)
        json.put("seed", data.seed)
        json.put("wind", data.windSpeed.toDouble())
        json.put("activeIdx", data.activeTankIndex)

        // P1
        val p1 = JSONObject()
        p1.put("hp", data.p1Health)
        p1.put("sh", data.p1Shield)
        p1.put("cash", data.p1Cash)
        p1.put("x", data.p1X.toDouble())
        json.put("p1", p1)

        // P2
        val p2 = JSONObject()
        p2.put("hp", data.p2Health)
        p2.put("sh", data.p2Shield)
        p2.put("cash", data.p2Cash)
        p2.put("x", data.p2X.toDouble())
        json.put("p2", p2)

        // Last action
        if (data.lastFiredPlayerId != -1) {
            val act = JSONObject()
            act.put("pid", data.lastFiredPlayerId)
            act.put("ang", data.lastFiredAngle.toDouble())
            act.put("pwr", data.lastFiredPower.toDouble())
            act.put("wpn", data.lastWeaponName)
            json.put("act", act)
        }

        // Craters
        val cratersArr = JSONArray()
        for (c in data.craters.takeLast(25)) { // store recent deformations
            val cj = JSONObject()
            cj.put("x", c.cx.toInt())
            cj.put("y", c.cy.toInt())
            cj.put("r", c.radius.toInt())
            cj.put("d", c.isDirt)
            cratersArr.put(cj)
        }
        json.put("craters", cratersArr)

        // Crates
        val cratesArr = JSONArray()
        for (cr in data.crates) {
            val crj = JSONObject()
            crj.put("t", cr.type.name)
            crj.put("x", cr.x.toInt())
            crj.put("g", cr.isGrounded)
            cratesArr.put(crj)
        }
        json.put("crates", cratesArr)
        json.put("over", data.isGameOver)
        json.put("win", data.winnerName)

        val rawBytes = json.toString().toByteArray(Charsets.UTF_8)
        val base64 = SimpleBase64.encode(rawBytes)
        return "SCORCHED-$base64"
    }

    fun decodeTurn(turnCode: String): CorrespondenceTurnData? {
        try {
            val cleanCode = turnCode.trim().removePrefix("SCORCHED-").trim()
            val rawBytes = SimpleBase64.decode(cleanCode)
            val json = JSONObject(String(rawBytes, Charsets.UTF_8))

            val p1 = json.getJSONObject("p1")
            val p2 = json.getJSONObject("p2")

            var lastPid = -1
            var lastAngle = 0f
            var lastPower = 0f
            var lastWeapon = ""
            if (json.has("act")) {
                val act = json.getJSONObject("act")
                lastPid = act.optInt("pid", -1)
                lastAngle = act.optDouble("ang", 0.0).toFloat()
                lastPower = act.optDouble("pwr", 0.0).toFloat()
                lastWeapon = act.optString("wpn", "")
            }

            val cratersList = mutableListOf<CraterRecord>()
            if (json.has("craters")) {
                val carr = json.getJSONArray("craters")
                for (i in 0 until carr.length()) {
                    val c = carr.getJSONObject(i)
                    cratersList.add(
                        CraterRecord(
                            cx = c.getInt("x").toFloat(),
                            cy = c.getInt("y").toFloat(),
                            radius = c.getInt("r").toFloat(),
                            isDirt = c.optBoolean("d", false)
                        )
                    )
                }
            }

            val cratesList = mutableListOf<CrateRecord>()
            if (json.has("crates")) {
                val crarr = json.getJSONArray("crates")
                for (i in 0 until crarr.length()) {
                    val cr = crarr.getJSONObject(i)
                    val type = try {
                        PowerUpType.valueOf(cr.getString("t"))
                    } catch (e: Exception) {
                        PowerUpType.SHIELD_GENERATOR
                    }
                    cratesList.add(
                        CrateRecord(
                            type = type,
                            x = cr.getInt("x").toFloat(),
                            isGrounded = cr.optBoolean("g", true)
                        )
                    )
                }
            }

            val isOver = json.optBoolean("over", false) || (p1.optInt("hp", 100) <= 0 || p2.optInt("hp", 100) <= 0)
            val winnerName = json.optString("win", "")

            return CorrespondenceTurnData(
                turnNumber = json.optInt("turn", 1),
                roundNumber = json.optInt("rnd", 1),
                seed = json.optLong("seed", 12345L),
                windSpeed = json.optDouble("wind", 0.0).toFloat(),
                activeTankIndex = json.optInt("activeIdx", 0),
                p1Health = p1.optInt("hp", 100),
                p1Shield = p1.optInt("sh", 0),
                p1Cash = p1.optInt("cash", 200),
                p1X = p1.optDouble("x", 120.0).toFloat(),
                p2Health = p2.optInt("hp", 100),
                p2Shield = p2.optInt("sh", 0),
                p2Cash = p2.optInt("cash", 200),
                p2X = p2.optDouble("x", 880.0).toFloat(),
                lastFiredPlayerId = lastPid,
                lastFiredAngle = lastAngle,
                lastFiredPower = lastPower,
                lastWeaponName = lastWeapon,
                craters = cratersList,
                crates = cratesList,
                isGameOver = isOver,
                winnerName = winnerName
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun shareTurnIntent(context: Context, turnCode: String, turnNumber: Int) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Scorched Tanks - Turn $turnNumber")
            putExtra(
                Intent.EXTRA_TEXT,
                "It's your turn in Scorched Tanks! Turn #$turnNumber code:\n\n$turnCode\n\nCopy this code and load it in Scorched Tanks to take your shot!"
            )
        }
        val chooser = Intent.createChooser(shareIntent, "Send Turn to Opponent")
        context.startActivity(chooser)
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Scorched Tanks Turn Code", text)
        clipboard?.setPrimaryClip(clip)
    }

    fun getFromClipboard(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val item = clipboard?.primaryClip?.getItemAt(0)
        return item?.text?.toString()
    }
}
