package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.correspondence.CorrespondenceManager
import com.example.correspondence.CorrespondenceTurnData
import com.example.model.WeaponType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Scorched Tanks", appName)
    }

    @Test
    fun testCorrespondenceSerialization() {
        val data = CorrespondenceTurnData(
            turnNumber = 4,
            roundNumber = 2,
            seed = 99999L,
            windSpeed = 32.5f,
            activeTankIndex = 1,
            p1Health = 85,
            p1Shield = 30,
            p1Cash = 350,
            p1X = 140f,
            p2Health = 60,
            p2Shield = 0,
            p2Cash = 210,
            p2X = 860f,
            lastFiredPlayerId = 0,
            lastFiredAngle = 52f,
            lastFiredPower = 64f,
            lastWeaponName = WeaponType.BABY_MISSILE.name
        )

        val code = CorrespondenceManager.encodeTurn(data)
        assertTrue(code.startsWith("SCORCHED-"))

        val decoded = CorrespondenceManager.decodeTurn(code)
        assertNotNull(decoded)
        assertEquals(4, decoded!!.turnNumber)
        assertEquals(2, decoded.roundNumber)
        assertEquals(85, decoded.p1Health)
        assertEquals(30, decoded.p1Shield)
        assertEquals(60, decoded.p2Health)
        assertEquals(52f, decoded.lastFiredAngle, 0.1f)
    }
}
