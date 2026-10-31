package com.example.ui

import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PowerUpType
import com.example.model.Tank
import com.example.model.WeaponType

@Composable
fun TacticalControlDeck(
    activeTank: Tank,
    isFiring: Boolean,
    onMove: (Int) -> Unit,
    onAngleChange: (Float) -> Unit,
    onAngleAdjust: (Float) -> Unit,
    onPowerChange: (Float) -> Unit,
    onPowerAdjust: (Float) -> Unit,
    onSelectWeapon: (WeaponType) -> Unit,
    onFire: () -> Unit,
    onOpenShop: () -> Unit,
    isLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isLandscape) {
        LandscapeTacticalControlDeck(
            activeTank = activeTank,
            isFiring = isFiring,
            onMove = onMove,
            onAngleChange = onAngleChange,
            onAngleAdjust = onAngleAdjust,
            onPowerChange = onPowerChange,
            onPowerAdjust = onPowerAdjust,
            onSelectWeapon = onSelectWeapon,
            onFire = onFire,
            onOpenShop = onOpenShop,
            modifier = modifier
        )
    } else {
        PortraitTacticalControlDeck(
            activeTank = activeTank,
            isFiring = isFiring,
            onMove = onMove,
            onAngleChange = onAngleChange,
            onAngleAdjust = onAngleAdjust,
            onPowerChange = onPowerChange,
            onPowerAdjust = onPowerAdjust,
            onSelectWeapon = onSelectWeapon,
            onFire = onFire,
            onOpenShop = onOpenShop,
            modifier = modifier
        )
    }
}

@Composable
fun LandscapeTacticalControlDeck(
    activeTank: Tank,
    isFiring: Boolean,
    onMove: (Int) -> Unit,
    onAngleChange: (Float) -> Unit,
    onAngleAdjust: (Float) -> Unit,
    onPowerChange: (Float) -> Unit,
    onPowerAdjust: (Float) -> Unit,
    onSelectWeapon: (WeaponType) -> Unit,
    onFire: () -> Unit,
    onOpenShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var weaponMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xF20F172A),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("tactical_control_deck")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left Column: Mobility & Weapon Selector
            Column(
                modifier = Modifier.widthIn(min = 210.dp, max = 270.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Drive Left / Drive Right + Fuel Gauge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onMove(-1)
                        },
                        enabled = !isFiring && activeTank.fuel > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("drive_left_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move Left", tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("DRIVE", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onMove(1)
                        },
                        enabled = !isFiring && activeTank.fuel > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("drive_right_button")
                    ) {
                        Text("DRIVE", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move Right", tint = Color.White, modifier = Modifier.size(15.dp))
                    }

                    // Fuel gauge
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalGasStation, contentDescription = "Fuel", tint = Color(0xFF38BDF8), modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("${activeTank.fuel.toInt()}%", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { activeTank.fuel / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF334155)
                        )
                    }
                }

                // Weapon Selector Dropdown Button
                Box {
                    val currentWeapon = activeTank.selectedWeapon
                    val ammo = activeTank.inventory[currentWeapon] ?: 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(currentWeapon.color), RoundedCornerShape(8.dp))
                            .clickable(enabled = !isFiring) { weaponMenuExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("weapon_selector_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color(currentWeapon.color))
                            )
                            Text(
                                text = currentWeapon.displayName,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (currentWeapon.isDefault) "∞" else "x$ammo",
                                color = Color(currentWeapon.color),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select Weapon",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = weaponMenuExpanded,
                        onDismissRequest = { weaponMenuExpanded = false },
                        modifier = Modifier
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF334155))
                    ) {
                        WeaponType.values().forEach { weapon ->
                            val count = activeTank.inventory[weapon] ?: 0
                            val isAvailable = weapon.isDefault || count > 0

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(Color(weapon.color))
                                        )
                                        Column {
                                            Text(
                                                text = weapon.displayName,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isAvailable) Color.White else Color(0xFF64748B),
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "Dmg: ${weapon.baseDamage} • Rad: ${weapon.blastRadius.toInt()}",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 9.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = if (weapon.isDefault) "∞" else "x$count",
                                            fontWeight = FontWeight.Bold,
                                            color = if (count > 0 || weapon.isDefault) Color(weapon.color) else Color(0xFF64748B),
                                            fontSize = 11.sp
                                        )
                                    }
                                },
                                onClick = {
                                    if (isAvailable) {
                                        onSelectWeapon(weapon)
                                        weaponMenuExpanded = false
                                    }
                                },
                                enabled = isAvailable
                            )
                        }
                    }
                }
            }

            // Center Column: Ballistics (Angle & Power Sliders)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Angle row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "ANG",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.width(28.dp)
                    )
                    StepButton(text = "-5", onClick = { onAngleAdjust(-5f) }, enabled = !isFiring)
                    StepButton(text = "-1", onClick = { onAngleAdjust(-1f) }, enabled = !isFiring)

                    Slider(
                        value = activeTank.aimAngle,
                        onValueChange = { onAngleChange(it) },
                        valueRange = 0f..180f,
                        enabled = !isFiring,
                        modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                            .testTag("angle_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )

                    StepButton(text = "+1", onClick = { onAngleAdjust(1f) }, enabled = !isFiring)
                    StepButton(text = "+5", onClick = { onAngleAdjust(5f) }, enabled = !isFiring)

                    Text(
                        text = "${activeTank.aimAngle.toInt()}°",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.width(32.dp)
                    )
                }

                // Power row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "PWR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.width(28.dp)
                    )
                    StepButton(text = "-5", onClick = { onPowerAdjust(-5f) }, enabled = !isFiring)
                    StepButton(text = "-1", onClick = { onPowerAdjust(-1f) }, enabled = !isFiring)

                    Slider(
                        value = activeTank.aimPower,
                        onValueChange = { onPowerChange(it) },
                        valueRange = 5f..100f,
                        enabled = !isFiring,
                        modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                            .testTag("power_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFF59E0B),
                            activeTrackColor = Color(0xFFF59E0B),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )

                    StepButton(text = "+1", onClick = { onPowerAdjust(1f) }, enabled = !isFiring)
                    StepButton(text = "+5", onClick = { onPowerAdjust(5f) }, enabled = !isFiring)

                    Text(
                        text = "${activeTank.aimPower.toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.width(32.dp)
                    )
                }
            }

            // Right Column: Big Glowing FIRE! Button
            val fireBrush = if (isFiring) {
                androidx.compose.ui.graphics.SolidColor(Color(0xFF475569))
            } else {
                Brush.verticalGradient(listOf(Color(0xFFEF4444), Color(0xFFB91C1C)))
            }
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 58.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(fireBrush)
                    .border(
                        2.dp,
                        if (isFiring) Color(0xFF64748B) else Color(0xFFFCA5A5),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = !isFiring) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onFire()
                    }
                    .testTag("fire_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Whatshot,
                        contentDescription = "Fire Cannon",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isFiring) "FIRING" else "FIRE!",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PortraitTacticalControlDeck(
    activeTank: Tank,
    isFiring: Boolean,
    onMove: (Int) -> Unit,
    onAngleChange: (Float) -> Unit,
    onAngleAdjust: (Float) -> Unit,
    onPowerChange: (Float) -> Unit,
    onPowerAdjust: (Float) -> Unit,
    onSelectWeapon: (WeaponType) -> Unit,
    onFire: () -> Unit,
    onOpenShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var weaponMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xF20F172A),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("tactical_control_deck")
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Row 0: Active Buffs & Tank Stats Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Turn Tag
                    Box(
                        modifier = Modifier
                            .background(activeTank.color.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .border(1.dp, activeTank.color, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${activeTank.name.uppercase()}'S TURN",
                            color = activeTank.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Cash
                    Text(
                        text = "$${activeTank.cash}",
                        color = Color(0xFFFFD700),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Active Buff Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeTank.isShieldActive) {
                        BuffBadge(
                            icon = Icons.Default.Shield,
                            text = "${activeTank.shield} SHIELD",
                            color = Color(0xFF00E5FF)
                        )
                    }
                    if (activeTank.hasDoubleDamage) {
                        BuffBadge(
                            icon = Icons.Default.Whatshot,
                            text = "2x DMG",
                            color = Color(0xFFFF1744)
                        )
                    }
                    if (activeTank.hasSuperPropulsion) {
                        BuffBadge(
                            icon = Icons.Default.ElectricBolt,
                            text = "HYPER",
                            color = Color(0xFFFFEA00)
                        )
                    }
                    if (activeTank.hasLaserGuide) {
                        BuffBadge(
                            icon = Icons.Default.Whatshot,
                            text = "TRACER",
                            color = Color(0xFFD500F9)
                        )
                    }

                    // Armory Shop Button
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF475569), RoundedCornerShape(8.dp))
                            .clickable(enabled = !isFiring) { onOpenShop() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("open_shop_button")
                    ) {
                        Text(
                            text = "ARMORY",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 1: Weapon Selector & Mobility Drive Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Mobility / Fuel Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onMove(-1)
                        },
                        enabled = !isFiring && activeTank.fuel > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("drive_left_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move Left", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("DRIVE", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onMove(1)
                        },
                        enabled = !isFiring && activeTank.fuel > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("drive_right_button")
                    ) {
                        Text("DRIVE", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move Right", tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    // Fuel gauge
                    Column(modifier = Modifier.width(60.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalGasStation, contentDescription = "Fuel", tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("${activeTank.fuel.toInt()}%", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { activeTank.fuel / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF334155)
                        )
                    }
                }

                // Weapon Selector Dropdown Button
                Box {
                    val currentWeapon = activeTank.selectedWeapon
                    val ammo = activeTank.inventory[currentWeapon] ?: 0

                    Row(
                        modifier = Modifier
                            .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(currentWeapon.color), RoundedCornerShape(10.dp))
                            .clickable(enabled = !isFiring) { weaponMenuExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("weapon_selector_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(currentWeapon.color))
                        )
                        Column {
                            Text(
                                text = currentWeapon.displayName,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (currentWeapon.isDefault) "Infinite" else "$ammo in stock",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Weapon",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = weaponMenuExpanded,
                        onDismissRequest = { weaponMenuExpanded = false },
                        modifier = Modifier
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF334155))
                    ) {
                        WeaponType.values().forEach { weapon ->
                            val count = activeTank.inventory[weapon] ?: 0
                            val isAvailable = weapon.isDefault || count > 0

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(weapon.color))
                                        )
                                        Column {
                                            Text(
                                                text = weapon.displayName,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isAvailable) Color.White else Color(0xFF64748B),
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "Dmg: ${weapon.baseDamage} • Radius: ${weapon.blastRadius.toInt()} • Spd: ${weapon.speedMultiplier}x",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = if (weapon.isDefault) "∞" else "x$count",
                                            fontWeight = FontWeight.Bold,
                                            color = if (count > 0 || weapon.isDefault) Color(weapon.color) else Color(0xFF64748B),
                                            fontSize = 12.sp
                                        )
                                    }
                                },
                                onClick = {
                                    if (isAvailable) {
                                        onSelectWeapon(weapon)
                                        weaponMenuExpanded = false
                                    }
                                },
                                enabled = isAvailable
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Angle & Power Adjusters + Big FIRE Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Angle & Power Sliders Block
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Angle Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "ANGLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.width(42.dp)
                        )
                        StepButton(text = "-5", onClick = { onAngleAdjust(-5f) }, enabled = !isFiring)
                        StepButton(text = "-1", onClick = { onAngleAdjust(-1f) }, enabled = !isFiring)

                        Slider(
                            value = activeTank.aimAngle,
                            onValueChange = { onAngleChange(it) },
                            valueRange = 0f..180f,
                            enabled = !isFiring,
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                                .testTag("angle_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF38BDF8),
                                activeTrackColor = Color(0xFF38BDF8),
                                inactiveTrackColor = Color(0xFF334155)
                            )
                        )

                        StepButton(text = "+1", onClick = { onAngleAdjust(1f) }, enabled = !isFiring)
                        StepButton(text = "+5", onClick = { onAngleAdjust(5f) }, enabled = !isFiring)

                        Text(
                            text = "${activeTank.aimAngle.toInt()}°",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.width(36.dp)
                        )
                    }

                    // Power Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "POWER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.width(42.dp)
                        )
                        StepButton(text = "-5", onClick = { onPowerAdjust(-5f) }, enabled = !isFiring)
                        StepButton(text = "-1", onClick = { onPowerAdjust(-1f) }, enabled = !isFiring)

                        Slider(
                            value = activeTank.aimPower,
                            onValueChange = { onPowerChange(it) },
                            valueRange = 5f..100f,
                            enabled = !isFiring,
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                                .testTag("power_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFF59E0B),
                                activeTrackColor = Color(0xFFF59E0B),
                                inactiveTrackColor = Color(0xFF334155)
                            )
                        )

                        StepButton(text = "+1", onClick = { onPowerAdjust(1f) }, enabled = !isFiring)
                        StepButton(text = "+5", onClick = { onPowerAdjust(5f) }, enabled = !isFiring)

                        Text(
                            text = "${activeTank.aimPower.toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }

                // Big Glowing FIRE Button
                val fireBrush = if (isFiring) {
                    androidx.compose.ui.graphics.SolidColor(Color(0xFF475569))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFFEF4444), Color(0xFFB91C1C)))
                }
                Box(
                    modifier = Modifier
                        .size(width = 82.dp, height = 66.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(fireBrush)
                        .border(
                            2.dp,
                            if (isFiring) Color(0xFF64748B) else Color(0xFFFCA5A5),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable(enabled = !isFiring) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onFire()
                        }
                        .testTag("fire_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Whatshot,
                            contentDescription = "Fire Cannon",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (isFiring) "FIRING" else "FIRE!",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Box(
        modifier = Modifier
            .size(width = 28.dp, height = 26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF475569), RoundedCornerShape(6.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else Color(0xFF64748B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BuffBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .border(1.dp, color, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Text(text = text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
