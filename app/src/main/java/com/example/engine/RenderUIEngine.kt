package com.example.engine

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import kotlinx.coroutines.*

object RenderUIEngine {
    
    // Configures clean styling colors for the UI HUD elements
    val colorBackgroundDark = Color(0xFF0C131D)
    val colorTextGold = Color(0xFFFFC107)
    val colorHeartRed = Color(0xFFFF3D00)
    val colorHeartGold = Color(0xFFFFD600)
    val colorActiveEffectSpeed = Color(0xFF00E676)
    val colorActiveEffectRegen = Color(0xFFEC407A)

    fun getStatusEffectColor(type: EffectsEngine.EffectType): Color {
        return when (type) {
            EffectsEngine.EffectType.SPEED -> colorActiveEffectSpeed
            EffectsEngine.EffectType.REGENERATION -> colorActiveEffectRegen
            EffectsEngine.EffectType.JUMP_BOOST -> Color(0xFF29B6F6)
            EffectsEngine.EffectType.ABSORPTION -> colorHeartGold
        }
    }

    fun getStatusEffectName(type: EffectsEngine.EffectType): String {
        return when (type) {
            EffectsEngine.EffectType.SPEED -> "Speed Boost"
            EffectsEngine.EffectType.REGENERATION -> "Regeneration"
            EffectsEngine.EffectType.JUMP_BOOST -> "Jump Boost"
            EffectsEngine.EffectType.ABSORPTION -> "Absorption"
        }
    }

    // Calculates standard FPS displays
    fun getFpsLabel(fps: Int): String {
        return "FPS: $fps"
    }

    // Formats standard coordinate displays
    fun getCoordinatesLabel(px: Float, py: Float, pz: Float): String {
        return String.format("XYZ: %.2f / %.2f / %.2f", px, py, pz)
    }

    @Composable
    fun RetroSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        Box(
            modifier = Modifier
                .size(40.dp, 20.dp)
                .background(Color(0xFF333333), RoundedCornerShape(2.dp))
                .border(1.dp, Color(0xFF111111), RoundedCornerShape(2.dp))
                .clickable { onCheckedChange(!checked) }
        ) {
            val alignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
            val knobColor = if (checked) Color(0xFF55AA55) else Color(0xFF888888)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(alignment)
                    .background(knobColor, RoundedCornerShape(2.dp))
                    .border(1.dp, Color.Black, RoundedCornerShape(2.dp))
            )
        }
    }

    fun Modifier.pointerInputHoverLike(onAction: (Boolean) -> Unit): Modifier = this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val hasDown = event.changes.any { it.pressed }
                onAction(hasDown)
            }
        }
    }

    @Composable
    fun ConnectingOverlay(
        ip: String,
        connectionError: String?,
        onBackClick: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF14304A)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "CONNECTING TO HOST...",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFFFB300),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Syncing world seed and generating synchronized chunks...",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(color = Color(0xFFFFB300))
                
                connectionError?.let { err ->
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = err,
                        color = Color.Red,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("BACK TO LOBBY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    @Composable
    fun WorldGenerationOverlay(world: com.example.world.World) {
        var isGenerating by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            while (isActive) {
                if (world.chunks.size > 8) {
                    isGenerating = false
                    break
                }
                delay(100)
            }
        }
        
        if (isGenerating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF14304A)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "GENERATING WORLD...",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFB300),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    CircularProgressIndicator(color = Color(0xFFFFB300))
                }
            }
        }
    }

    @Composable
    fun DeathOverlay(
        player: com.example.game.Player,
        world: com.example.world.World
    ) {
        if (player.health <= 0 && player.gameMode == com.example.game.Player.GameMode.SURVIVAL) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "YOU DIED",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            player.health = player.maxHealth
                            val spawnY = world.getSpawnHeight(0, 0)
                            player.camera.position.set(0.5f, spawnY, 0.5f)
                            if (player.gameMode == com.example.game.Player.GameMode.SURVIVAL) {
                                for (i in 0 until player.inventory.size) {
                                    player.inventory.items[i] = com.example.game.BlockRegistry.AIR
                                }
                                player.inventory.items[0] = com.example.game.BlockRegistry.WOODEN_PICKAXE
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text("Respawn", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    @Composable
    fun CrosshairOverlay(player: com.example.game.Player) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(16.dp)
                ) {
                    // Horizontal bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color.White.copy(alpha = 0.8f))
                    )
                    // Vertical bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxHeight()
                            .width(2.dp)
                            .background(Color.White.copy(alpha = 0.8f))
                    )
                }
                
                // Break Progress
                if (player.breakProgress > 0f) {
                    LinearProgressIndicator(
                        progress = { player.breakProgress },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 24.dp)
                            .width(48.dp)
                            .height(4.dp),
                        color = Color.White,
                        trackColor = Color.Black.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    @Composable
    fun StatsHUDPanel(
        currentFps: Int,
        posX: Float,
        posY: Float,
        posZ: Float
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.wrapContentSize()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "MineMods Sandbox",
                    color = Color(0xFFFFB300),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "FPS: $currentFps",
                    color = if (currentFps >= 50) Color.Green else if (currentFps >= 30) Color.Yellow else Color.Red,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Text(
                    text = "X: ${String.format("%.1f", posX)}",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Text(
                    text = "Y: ${String.format("%.1f", posY)}",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Text(
                    text = "Z: ${String.format("%.1f", posZ)}",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }

    @Composable
    fun TopButtonsHUDPanel(
        onTerminalClick: () -> Unit,
        onSettingsClick: () -> Unit,
        onLobbyClick: () -> Unit
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onTerminalClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
            ) {
                Text("💬 Terminal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = onSettingsClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
            ) {
                Text("⚙️ Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = onLobbyClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.6f))
            ) {
                Text("Lobby", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }

    @Composable
    fun HotbarHUDPanel(
        player: com.example.game.Player,
        triggerRecompose: Int,
        onSlotSelected: (Int) -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Health Bar (Survival only)
            if (player.gameMode == com.example.game.Player.GameMode.SURVIVAL) {
                Row(
                    modifier = Modifier.padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val fullHearts = player.health / 2
                    val halfHeart = player.health % 2 != 0
                    
                    for (i in 0 until 10) {
                        val isGold = i < player.goldHearts / 2
                        val color = if (isGold) Color(0xFFFFD700) else Color.Red
                        
                        if (i < fullHearts) {
                            Text("♥", color = color, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 1.dp))
                        } else if (i == fullHearts && halfHeart) {
                            Text("♡", color = color, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 1.dp))
                        } else {
                            Text("♡", color = Color.Gray, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 1.dp))
                        }
                    }
                }
            }

            // Selected item name overlay
            val selectedBlockId = player.inventory.getSelectedBlock()
            val blockName = com.example.game.BlockRegistry.getBlockName(selectedBlockId)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = blockName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            // Hotbar row
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .padding(4.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    for (slot in 0 until player.inventory.hotbarSize) {
                        val bId = player.inventory.getBlock(slot)
                        val isSelected = player.inventory.selectedHotbarSlot == slot
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Color.White.copy(alpha = if (isSelected) 0.25f else 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFFFB300) else Color.White.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable {
                                    onSlotSelected(slot)
                                }
                                .padding(4.dp)
                        ) {
                            if (bId != com.example.game.BlockRegistry.AIR) {
                                val atlas = TextureManager.atlasBitmap
                                if (atlas != null) {
                                    if (com.example.game.BlockRegistry.isItem(bId) || bId == com.example.game.BlockRegistry.ROSE || bId == com.example.game.BlockRegistry.DANDELION || bId == com.example.game.BlockRegistry.TALL_GRASS) {
                                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                            val w = size.width
                                            val h = size.height
                                            drawIntoCanvas { canvas ->
                                                com.example.engine.RenderItemEngine.drawFlatItem(canvas.nativeCanvas, atlas, bId, w, h)
                                            }
                                        }
                                    } else {
                                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                            val w = size.width
                                            val h = size.height
                                            drawIntoCanvas { canvas ->
                                                com.example.engine.RenderItemEngine.draw3DBlockItem(canvas.nativeCanvas, atlas, bId, w, h)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun VirtualJoystickHUD(
        onDirectionChange: (forward: Boolean, backward: Boolean, left: Boolean, right: Boolean) -> Unit
    ) {
        var joyOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
        val joyRadius = 120f
        
        LaunchedEffect(joyOffset) {
            val thresh = joyRadius * 0.25f
            val fwd = joyOffset.y < -thresh
            val bwd = joyOffset.y > thresh
            val rgt = joyOffset.x > thresh
            val lft = joyOffset.x < -thresh
            onDirectionChange(fwd, bwd, lft, rgt)
        }

        Box(
            modifier = Modifier
                .size(160.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.pressed }) {
                                val change = event.changes.first { it.pressed }
                                val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                                val pos = change.position - center
                                val length = pos.getDistance()
                                if (length > joyRadius) {
                                     joyOffset = pos * (joyRadius / length)
                                } else {
                                     joyOffset = pos
                                }
                            } else {
                                joyOffset = androidx.compose.ui.geometry.Offset.Zero
                            }
                        }
                    }
                }
        ) {
            // Stick
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { androidx.compose.ui.unit.IntOffset(joyOffset.x.toInt(), joyOffset.y.toInt()) }
                    .size(60.dp)
                    .background(Color.White.copy(alpha = 0.6f), CircleShape)
            )
        }
    }

    @Composable
    fun ActionControlsHUD(
        player: com.example.game.Player,
        onJumpAction: (Boolean) -> Unit,
        onMoveDownAction: (Boolean) -> Unit,
        onBreakAction: (Boolean) -> Unit,
        onPlaceUseAction: () -> Unit
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (player.isFlying) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {},
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1).copy(alpha = 0.8f)),
                        modifier = Modifier
                            .size(56.dp)
                            .pointerInputHoverLike { down -> onJumpAction(down) }
                    ) {
                        Text("UP ▲", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    
                    Button(
                        onClick = {},
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF303F9F).copy(alpha = 0.8f)),
                        modifier = Modifier
                            .size(56.dp)
                            .pointerInputHoverLike { down -> onMoveDownAction(down) }
                    ) {
                        Text("DN ▼", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            } else {
                Button(
                    onClick = {},
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300).copy(alpha = 0.7f)),
                    modifier = Modifier
                        .size(56.dp)
                        .pointerInputHoverLike { down -> onJumpAction(down) }
                ) {
                    Text("JUMP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .pointerInputHoverLike { down -> onBreakAction(down) }
                ) {
                    Text("BREAK", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                
                Button(
                    onClick = onPlaceUseAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    val blockToPlace = player.inventory.getSelectedBlock()
                    val buttonText = if (com.example.game.BlockRegistry.isItem(blockToPlace) || blockToPlace == com.example.game.BlockRegistry.GOLDEN_APPLE || blockToPlace == com.example.game.BlockRegistry.POTION_HEALING) "USE" else "PLACE"
                    Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }

    @Composable
    fun AdminPanelDialogHUD(
        player: com.example.game.Player,
        world: com.example.world.World,
        fpsLimit: Int,
        onFpsLimitChange: (Int) -> Unit,
        renderDistance: Float,
        onRenderDistanceChange: (Float) -> Unit,
        cameraSensitivity: Float,
        onCameraSensitivityChange: (Float) -> Unit,
        textureQuality: String,
        onTextureQualityChange: (String) -> Unit,
        onDismiss: () -> Unit
    ) {
        var activeSettingsTab by remember { mutableStateOf(0) }

        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.92f)
                .border(2.dp, Color(0xFFFFB300), RoundedCornerShape(12.dp)),
            containerColor = Color(0xFF101B2B),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ SETTINGS",
                        color = Color(0xFFFFB300),
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Text("❌", color = Color.White, fontSize = 16.sp)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { activeSettingsTab = 0 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeSettingsTab == 0) Color(0xFFFFB300) else Color.Transparent
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "⚙️ SETTINGS",
                                color = if (activeSettingsTab == 0) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = { activeSettingsTab = 1 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeSettingsTab == 1) Color(0xFFFFB300) else Color.Transparent
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "👑 CHEATS",
                                color = if (activeSettingsTab == 1) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (activeSettingsTab == 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "FPS LIMIT:",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "${fpsLimit} FPS",
                                        color = Color(0xFFFFB300),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                }
                                Slider(
                                    value = fpsLimit.toFloat(),
                                    onValueChange = { 
                                        onFpsLimitChange(it.toInt())
                                    },
                                    valueRange = 30f..120f,
                                    steps = 2,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFB300),
                                        activeTrackColor = Color(0xFFFFB300),
                                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                                    )
                                )
                            }

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "RENDER DISTANCE:",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "${renderDistance.toInt()} Chunks",
                                        color = Color(0xFFFFB300),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                }
                                Slider(
                                    value = renderDistance,
                                    onValueChange = { 
                                        onRenderDistanceChange(it)
                                    },
                                    valueRange = 4f..512f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFB300),
                                        activeTrackColor = Color(0xFFFFB300),
                                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                                    )
                                )
                            }

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "CAMERA SENSITIVITY:",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        String.format("%.1fx", cameraSensitivity),
                                        color = Color(0xFFFFB300),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                }
                                Slider(
                                    value = cameraSensitivity,
                                    onValueChange = { 
                                        onCameraSensitivityChange(it)
                                    },
                                    valueRange = 0.2f..2.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFB300),
                                        activeTrackColor = Color(0xFFFFB300),
                                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "TEXTURE QUALITY:",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("LOW", "MEDIUM", "HIGH").forEach { quality ->
                                        val isSelected = textureQuality == quality
                                        Button(
                                            onClick = {
                                                onTextureQualityChange(quality)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) Color(0xFFFFB300) else Color(0xFF334155)
                                            ),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(
                                                text = quality,
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "OPERATOR STATUS (OP)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Enables creative mode commands and block tools.",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                RetroSwitch(
                                    checked = player.isOp,
                                    onCheckedChange = { player.isOp = it }
                                )
                            }

                            if (player.isOp) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "SELECT GAME MODE:",
                                        color = Color(0xFFFFB300),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        com.example.game.Player.GameMode.values().forEach { gMode ->
                                            val isCurrent = player.gameMode == gMode
                                            Button(
                                                onClick = { player.gameMode = gMode },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isCurrent) Color(0xFFFFB300) else Color(0xFF334155)
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f).height(36.dp)
                                            ) {
                                                Text(
                                                    text = when(gMode) {
                                                        com.example.game.Player.GameMode.SURVIVAL -> "SURVIVAL"
                                                        com.example.game.Player.GameMode.CREATIVE -> "CREATIVE"
                                                        com.example.game.Player.GameMode.SPECTATOR -> "SPECTATOR"
                                                    },
                                                    color = if (isCurrent) Color.Black else Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "ABILITIES & PERMISSIONS:",
                                        color = Color(0xFFFFB300),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().height(28.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Can Fly (Полет)", color = Color.White, fontSize = 11.sp)
                                        RetroSwitch(
                                            checked = player.canFly,
                                            onCheckedChange = { player.canFly = it }
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().height(28.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Keep Inventory (Сохр. инвентаря)", color = Color.White, fontSize = 11.sp)
                                        RetroSwitch(
                                            checked = true,
                                            onCheckedChange = { }
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().height(28.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Set Night Time", color = Color.White, fontSize = 11.sp)
                                        RetroSwitch(
                                            checked = world.isNight,
                                            onCheckedChange = { world.isNight = it }
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().height(28.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Can Place (Ставить блоки)", color = Color.White, fontSize = 11.sp)
                                        RetroSwitch(
                                            checked = player.canBuild,
                                            onCheckedChange = { player.canBuild = it }
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().height(28.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Can Break (Ломать блоки)", color = Color.White, fontSize = 11.sp)
                                        RetroSwitch(
                                            checked = player.canBreak,
                                            onCheckedChange = { player.canBreak = it }
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "COMMAND CENTER:",
                                        color = Color(0xFFFFB300),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { world.isNight = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.weight(1f).height(30.dp)
                                        ) {
                                            Text("☀️ SET DAY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        
                                        Button(
                                            onClick = { world.isNight = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF311B92)),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.weight(1f).height(30.dp)
                                        ) {
                                            Text("🌙 SET NIGHT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val spawnY = world.getSpawnHeight(0, 0)
                                                player.camera.position.set(0.5f, spawnY, 0.5f)
                                                player.velocity.set(0f, 0f, 0f)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.weight(1f).height(30.dp)
                                        ) {
                                            Text("🏠 TO SPAWN", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        
                                        Button(
                                            onClick = {
                                                val curX = Math.floor(player.camera.position.x.toDouble()).toInt()
                                                val curY = Math.floor(player.camera.position.y.toDouble()).toInt()
                                                val curZ = Math.floor(player.camera.position.z.toDouble()).toInt()
                                                
                                                var foundY = -1
                                                for (y in curY..250) {
                                                    if (!com.example.game.BlockRegistry.isSolid(world.getBlock(curX, y, curZ)) &&
                                                        !com.example.game.BlockRegistry.isSolid(world.getBlock(curX, y + 1, curZ))) {
                                                        foundY = y
                                                        break
                                                    }
                                                }
                                                if (foundY != -1) {
                                                    player.camera.position.y = foundY.toFloat() + 0.1f
                                                } else {
                                                    player.camera.position.y += 5.0f
                                                }
                                                player.velocity.set(0f, 0f, 0f)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE64A19)),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.weight(1f).height(30.dp)
                                        ) {
                                            Text("🆘 UNSTUCK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "You must be an Operator (OP) to use these settings.",
                                        color = Color.LightGray,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
                ) {
                    Text("DONE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        )
    }

    @Composable
    fun TerminalDialogHUD(
        player: com.example.game.Player,
        world: com.example.world.World,
        onDismiss: () -> Unit,
        onCommandRun: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.6f)
                .border(2.dp, Color(0xFFFFB300), RoundedCornerShape(12.dp)),
            containerColor = Color(0xFF0C131D),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💬 IN-GAME TERMINAL",
                        color = Color(0xFFFFB300),
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Text("❌", color = Color.White, fontSize = 16.sp)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(6.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (msg in com.example.engine.UIEngine.chatMessages) {
                            Text(
                                text = msg,
                                color = if (msg.contains("[System]")) Color(0xFFFFC107) else Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = com.example.engine.UIEngine.chatInputText,
                            onValueChange = { com.example.engine.UIEngine.chatInputText = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)), RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                        
                        Button(
                            onClick = {
                                val input = com.example.engine.UIEngine.chatInputText
                                if (input.isNotEmpty()) {
                                    if (input.startsWith("/")) {
                                        val response = com.example.engine.CommandEngine.executeCommand(input, player, world)
                                        com.example.engine.UIEngine.addSystemMessage(response)
                                    } else {
                                        com.example.engine.NetworkManager.sendChatMessage(input)
                                    }
                                    com.example.engine.UIEngine.chatInputText = ""
                                    onCommandRun()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("RUN", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

