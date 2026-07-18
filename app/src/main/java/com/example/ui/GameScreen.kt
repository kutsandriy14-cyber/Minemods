package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import com.example.game.Player
import com.example.world.World
import com.example.game.BlockRegistry
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToDown
import com.example.engine.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.engine.Vector3f
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.example.world.WorldSaveManager
import java.io.File
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.engine.TextureManager

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameScreen(
    worldName: String,
    mode: String = "single",
    ip: String = "",
    onSettingsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Initialize GameEngine
    LaunchedEffect(worldName, mode, ip) {
        com.example.engine.GameEngine.initGame(context, worldName, mode, ip)
    }

    val world = com.example.engine.GameEngine.world
    val player = com.example.engine.GameEngine.player

    if (world == null || player == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF14304A)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFFFB300))
        }
        return
    }

    LaunchedEffect(world, player) {
        // Load data
        com.example.engine.GameEngine.loadWorldData(context, worldName, mode)
        
        // Start networking
        com.example.engine.GameEngine.startNetworking(mode, ip)

        // Asynchronous background thread loop for generating/loading infinite chunks around the player via ChunkLoadEngine
        launch(Dispatchers.IO) {
            while (isActive) {
                val currentRadius = com.example.engine.GameEngine.renderDistance.toInt()
                com.example.engine.ChunkLoadEngine.updatePlayerLoadRadius(
                    world,
                    player.camera.position.x,
                    player.camera.position.z,
                    currentRadius
                )
                delay(400)
            }
        }

        // Asynchronous background thread loop for broadcasting our player position to multiplayer peers
        launch(Dispatchers.IO) {
            while (isActive) {
                com.example.engine.NetworkManager.sendPlayerPosition(
                    player.camera.position.x,
                    player.camera.position.y,
                    player.camera.position.z
                )
                delay(50)
            }
        }

        var lastTime = System.nanoTime()
        var lastFpsTime = lastTime
        var lastSaveTime = lastTime
        val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

        while (isActive) {
            val now = System.nanoTime()
            val dt = (now - lastTime) / 1_000_000_000f
            lastTime = now
            
            com.example.engine.GameEngine.updateGameTick(dt)
            
            if (now - lastFpsTime >= 1_000_000_000) {
                lastFpsTime = now
                com.example.engine.GameEngine.fpsLimit = preferences.getInt("fpsLimit", 60)
            }
            
            // Auto-save every 10 seconds on background IO
            if (now - lastSaveTime >= 10_000_000_000L) {
                lastSaveTime = now
                WorldSaveManager.saveWorld(context, worldName, world, player)
            }
            
            val targetDelay = if (com.example.engine.GameEngine.fpsLimit >= 120) 8L else if (com.example.engine.GameEngine.fpsLimit == 30) 33L else 16L
            delay(targetDelay)
        }
    }

    DisposableEffect(world, player) {
        com.example.engine.ChunkLoadEngine.startEngine(
            world,
            player.camera.position.x,
            player.camera.position.z,
            com.example.engine.GameEngine.renderDistance.toInt()
        )
        onDispose {
            com.example.engine.GameEngine.shutdown(context, worldName)
        }
    }
    
    if (com.example.engine.GameEngine.isConnecting) {
        com.example.engine.RenderUIEngine.ConnectingOverlay(
            ip = ip,
            connectionError = com.example.engine.GameEngine.connectionError,
            onBackClick = onBackClick
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 3D Viewport
        AndroidView(
            factory = { GameSurfaceView(it, world, player) },
            modifier = Modifier.fillMaxSize()
        )
        
        // World Generation Overlay
        com.example.engine.RenderUIEngine.WorldGenerationOverlay(world)
        
        // Death Overlay
        com.example.engine.RenderUIEngine.DeathOverlay(player, world)
        
        // Crosshair
        com.example.engine.RenderUIEngine.CrosshairOverlay(player)
        
        // HUD - Debug Stats (Top Left) & Top Buttons (Top Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            com.example.engine.RenderUIEngine.StatsHUDPanel(
                currentFps = com.example.engine.GameEngine.currentFps,
                posX = com.example.engine.GameEngine.posX,
                posY = com.example.engine.GameEngine.posY,
                posZ = com.example.engine.GameEngine.posZ
            )
            
            com.example.engine.RenderUIEngine.TopButtonsHUDPanel(
                onTerminalClick = { com.example.engine.UIEngine.showChat = !com.example.engine.UIEngine.showChat },
                onSettingsClick = { com.example.engine.GameEngine.showAdminPanel = true },
                onLobbyClick = onBackClick
            )
        }
        
        // Hotbar (Bottom Center)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            com.example.engine.RenderUIEngine.HotbarHUDPanel(
                player = player,
                triggerRecompose = com.example.engine.GameEngine.triggerRecompose,
                onSlotSelected = { slot ->
                    player.inventory.selectedHotbarSlot = slot
                    com.example.engine.GameEngine.triggerRecompose++
                }
            )
        }
        
        // Virtual Joystick (Bottom Left)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 32.dp, bottom = 32.dp)
        ) {
            com.example.engine.RenderUIEngine.VirtualJoystickHUD(
                onDirectionChange = { fwd, bwd, lft, rgt ->
                    com.example.engine.GameEngine.moveForward = fwd
                    com.example.engine.GameEngine.moveBackward = bwd
                    com.example.engine.GameEngine.moveLeft = lft
                    com.example.engine.GameEngine.moveRight = rgt
                }
            )
        }
        
        // Action Controls (Bottom Right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
        ) {
            com.example.engine.RenderUIEngine.ActionControlsHUD(
                player = player,
                onJumpAction = { down -> com.example.engine.GameEngine.jump = down },
                onMoveDownAction = { down -> com.example.engine.GameEngine.moveDown = down },
                onBreakAction = { down -> com.example.engine.GameEngine.isBreaking = down },
                onPlaceUseAction = {
                    val blockToPlace = player.inventory.getSelectedBlock()
                    if (com.example.game.BlockRegistry.isItem(blockToPlace) || blockToPlace == com.example.game.BlockRegistry.GOLDEN_APPLE || blockToPlace == com.example.game.BlockRegistry.POTION_HEALING) {
                        player.useItem()
                    } else {
                        player.raycastBlock(false)
                    }
                }
            )
        }
        
        // Settings Dialog
        if (com.example.engine.GameEngine.showAdminPanel) {
            val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            com.example.engine.RenderUIEngine.AdminPanelDialogHUD(
                player = player,
                world = world,
                fpsLimit = com.example.engine.GameEngine.fpsLimit,
                onFpsLimitChange = {
                    com.example.engine.GameEngine.fpsLimit = it
                    preferences.edit().putInt("fpsLimit", it).apply()
                },
                renderDistance = com.example.engine.GameEngine.renderDistance,
                onRenderDistanceChange = {
                    com.example.engine.GameEngine.renderDistance = it
                    preferences.edit().putFloat("renderDistance", it).apply()
                },
                cameraSensitivity = com.example.engine.GameEngine.cameraSensitivity,
                onCameraSensitivityChange = {
                    com.example.engine.GameEngine.cameraSensitivity = it
                    preferences.edit().putFloat("sensitivity", it).apply()
                },
                textureQuality = com.example.engine.GameEngine.textureQuality,
                onTextureQualityChange = {
                    com.example.engine.GameEngine.textureQuality = it
                    preferences.edit().putString("textureQuality", it).apply()
                },
                onDismiss = { com.example.engine.GameEngine.showAdminPanel = false }
            )
        }
        
        // Terminal Dialog
        if (com.example.engine.UIEngine.showChat) {
            com.example.engine.RenderUIEngine.TerminalDialogHUD(
                player = player,
                world = world,
                onDismiss = { com.example.engine.UIEngine.showChat = false },
                onCommandRun = { com.example.engine.GameEngine.triggerRecompose++ }
            )
        }
    }
}
