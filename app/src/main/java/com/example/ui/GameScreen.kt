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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.game.Player
import com.example.world.World
import com.example.game.BlockRegistry
import kotlinx.coroutines.delay
import com.example.engine.Vector3f
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.ExperimentalComposeUiApi
import android.content.Context
import com.example.world.WorldSaveManager
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameScreen(worldName: String, onSettingsClick: () -> Unit) {
    val context = LocalContext.current
    val world = remember { 
        World().apply { 
            // Try loading world; generate new one if file doesn't exist
            val playerPlaceholder = Player(this)
            val success = WorldSaveManager.loadWorld(context, worldName, this, playerPlaceholder)
            if (!success) {
                seed = worldName.hashCode().toLong()
                generateInitialWorld()
            }
        }
    }
    val player = remember { 
        Player(world).apply {
            val saveFile = File(WorldSaveManager.getWorldsDir(context), "$worldName/level.dat")
            if (!saveFile.exists()) {
                val spawnY = world.getSpawnHeight(0, 0)
                camera.position.set(0.5f, spawnY, 0.5f)
            } else {
                WorldSaveManager.loadWorld(context, worldName, world, this)
            }
        }
    }
    
    var moveForward by remember { mutableStateOf(false) }
    var moveBackward by remember { mutableStateOf(false) }
    var moveLeft by remember { mutableStateOf(false) }
    var moveRight by remember { mutableStateOf(false) }
    var jump by remember { mutableStateOf(false) }
    
    // Position state for HUD
    var posX by remember { mutableStateOf(0f) }
    var posY by remember { mutableStateOf(0f) }
    var posZ by remember { mutableStateOf(0f) }
    
    // Block selection state
    var selectedBlock by remember { mutableStateOf(BlockRegistry.STONE) }
    
    // FPS counter
    var currentFps by remember { mutableStateOf(0) }
    
    // Settings state
    val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var fpsLimit by remember { mutableStateOf(preferences.getInt("fpsLimit", 60)) }
    
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        var frames = 0
        var lastFpsTime = lastTime
        var lastSaveTime = lastTime
        
        while(true) {
            val now = System.nanoTime()
            val dt = (now - lastTime) / 1_000_000_000f
            lastTime = now
            
            val dirs = Vector3f(
                if(moveRight) 1f else if(moveLeft) -1f else 0f,
                0f,
                if(moveForward) 1f else if(moveBackward) -1f else 0f
            )
            player.update(dt, dirs, jump)
            
            // Update HUD values
            posX = player.camera.position.x
            posY = player.camera.position.y
            posZ = player.camera.position.z
            
            frames++
            if (now - lastFpsTime >= 1_000_000_000) {
                currentFps = frames
                frames = 0
                lastFpsTime = now
                fpsLimit = preferences.getInt("fpsLimit", 60)
            }
            
            // Auto-save every 10 seconds on background IO
            if (now - lastSaveTime >= 10_000_000_000L) {
                lastSaveTime = now
                WorldSaveManager.saveWorld(context, worldName, world, player)
            }
            
            val targetDelay = if (fpsLimit >= 120) 8L else if (fpsLimit == 30) 33L else 16L
            delay(targetDelay)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            WorldSaveManager.saveWorld(context, worldName, world, player)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 3D Viewport
        AndroidView(
            factory = { GameSurfaceView(it, world, player) },
            modifier = Modifier.fillMaxSize()
        )
        
        // 1. Classic Crosshair in the exact center of screen
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
        
        // 2. HUD - Left-aligned debug details & Right-aligned menus
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Stats Panel
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
            
            // Buttons Top Right
            Row {
                Button(
                    onClick = onSettingsClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Settings", color = Color.White)
                }
            }
        }
        
        // 3. Hotbar & Active Item Display (Bottom Center)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Selected item name overlay
            val blockName = when(selectedBlock) {
                BlockRegistry.GRASS -> "Grass Block"
                BlockRegistry.DIRT -> "Dirt"
                BlockRegistry.STONE -> "Stone"
                BlockRegistry.SAND -> "Sand"
                else -> "None"
            }
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
            
            // 4-slot Hotbar row
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(4.dp)
                ) {
                    val blocks = listOf(BlockRegistry.GRASS, BlockRegistry.DIRT, BlockRegistry.STONE, BlockRegistry.SAND)
                    blocks.forEach { bId ->
                        val isSelected = selectedBlock == bId
                        Box(
                            modifier = Modifier
                                .size(48.dp)
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
                                    selectedBlock = bId
                                    player.selectedBlockType = bId
                                }
                                .padding(6.dp)
                        ) {
                            // Custom voxel preview graphic
                            when (bId) {
                                BlockRegistry.GRASS -> {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF55AA55)))
                                        Box(modifier = Modifier.weight(1.5f).fillMaxWidth().background(Color(0xFF866043)))
                                    }
                                }
                                BlockRegistry.DIRT -> {
                                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF866043)))
                                }
                                BlockRegistry.STONE -> {
                                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF808080)))
                                }
                                BlockRegistry.SAND -> {
                                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFDBD19F)))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 4. Custom D-pad (Bottom Left)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 24.dp)
                .size(130.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            // Forward
            IconButton(
                onClick = {},
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(40.dp)
                    .pointerInputHoverLike { down -> moveForward = down }
            ) {
                Text("▲", color = Color.White, fontSize = 20.sp)
            }
            
            // Left
            IconButton(
                onClick = {},
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .pointerInputHoverLike { down -> moveLeft = down }
            ) {
                Text("◀", color = Color.White, fontSize = 20.sp)
            }
            
            // Right
            IconButton(
                onClick = {},
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(40.dp)
                    .pointerInputHoverLike { down -> moveRight = down }
            ) {
                Text("▶", color = Color.White, fontSize = 20.sp)
            }
            
            // Backward
            IconButton(
                onClick = {},
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(40.dp)
                    .pointerInputHoverLike { down -> moveBackward = down }
            ) {
                Text("▼", color = Color.White, fontSize = 20.sp)
            }
            
            // Center decorative hub
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            )
        }
        
        // 5. Action controls (Bottom Right)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Jump button
            Button(
                onClick = {},
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300).copy(alpha = 0.7f)),
                modifier = Modifier
                    .size(56.dp)
                    .pointerInputHoverLike { down -> jump = down }
            ) {
                Text("JUMP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            
            // Break and Place side by side actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { player.raycastBlock(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("BREAK", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                
                Button(
                    onClick = { player.raycastBlock(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("PLACE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.pointerInputHoverLike(onAction: (Boolean) -> Unit): Modifier = this.pointerInteropFilter {
    when (it.action) {
        android.view.MotionEvent.ACTION_DOWN -> { onAction(true); true }
        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> { onAction(false); true }
        else -> false
    }
}
