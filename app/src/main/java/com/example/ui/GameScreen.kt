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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectTapGestures

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
    val world = remember { 
        World().apply { 
            if (mode != "client") {
                // Try loading world; generate new one if file doesn't exist
                val playerPlaceholder = Player(this)
                val success = WorldSaveManager.loadWorld(context, worldName, this, playerPlaceholder)
                if (!success) {
                    seed = worldName.hashCode().toLong()
                    generateInitialWorld()
                }
            } else {
                // Client gets seed and chunks from host over socket!
                seed = 123456L
            }
        }
    }
    val player = remember { 
        Player(world).apply {
            if (mode != "client") {
                val saveFile = File(WorldSaveManager.getWorldsDir(context), "$worldName/level.dat")
                if (!saveFile.exists()) {
                    val spawnY = world.getSpawnHeight(0, 0)
                    camera.position.set(0.5f, spawnY, 0.5f)
                } else {
                    WorldSaveManager.loadWorld(context, worldName, world, this)
                }
            } else {
                // Client initial spawn placement
                camera.position.set(8.0f, 65f, 8.0f)
            }
        }
    }
    
    var moveForward by remember { mutableStateOf(false) }
    var moveBackward by remember { mutableStateOf(false) }
    var moveLeft by remember { mutableStateOf(false) }
    var moveRight by remember { mutableStateOf(false) }
    var moveUp by remember { mutableStateOf(false) }
    var moveDown by remember { mutableStateOf(false) }
    var jump by remember { mutableStateOf(false) }
    var isBreaking by remember { mutableStateOf(false) }
    
    var showAdminPanel by remember { mutableStateOf(false) }
    
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
    var renderDistance by remember { mutableStateOf(preferences.getFloat("renderDistance", 8f)) }
    var textureQuality by remember { mutableStateOf(preferences.getString("textureQuality", "HIGH") ?: "HIGH") }
    var cameraSensitivity by remember { mutableStateOf(preferences.getFloat("sensitivity", 1.0f)) }
    
    var isConnecting by remember { mutableStateOf(mode == "client") }
    var connectionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (mode == "host") {
            NetworkManager.startHost(world) {
                // Host server started!
            }
        } else if (mode == "client") {
            NetworkManager.startClient(
                hostIp = ip,
                world = world,
                onConnectSuccess = {
                    isConnecting = false
                },
                onConnectFailed = {
                    connectionError = "Failed to connect to Host at $ip.\nCheck IP address, Wi-Fi network, and make sure the Host has started the game."
                }
            )
        }

        // Register network callback for real-time block synchronization
        NetworkManager.setOnBlockChangeCallback { bx, by, bz, type ->
            val cx = bx shr 4
            val cz = bz shr 4
            world.chunks[Pair(cx, cz)]?.isModified = true
        }

        // Asynchronous background thread loop for generating/loading infinite chunks around the player
        launch(Dispatchers.IO) {
            while (isActive) {
                val currentRadius = preferences.getFloat("renderDistance", 8f).toInt()
                world.updateChunksAroundPlayer(player.camera.position.x, player.camera.position.z, currentRadius)
                delay(500)
            }
        }

        // Asynchronous background thread loop for broadcasting our player position to multiplayer peers
        launch(Dispatchers.IO) {
            while (isActive) {
                NetworkManager.sendPlayerPosition(
                    player.camera.position.x,
                    player.camera.position.y,
                    player.camera.position.z
                )
                delay(50)
            }
        }

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
                if(moveUp) 1f else if(moveDown) -1f else 0f,
                if(moveForward) 1f else if(moveBackward) -1f else 0f
            )
            player.update(dt, dirs, jump)
            player.updateBreaking(dt, isBreaking)
            
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
            NetworkManager.stop()
        }
    }
    
    if (isConnecting) {
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
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 3D Viewport
        AndroidView(
            factory = { GameSurfaceView(it, world, player) },
            modifier = Modifier.fillMaxSize()
        )
        
        // 1. Classic Crosshair in the exact center of screen
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showAdminPanel = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Text("⚙️ Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = onBackClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.6f))
                ) {
                    Text("Lobby", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        
        // 3. Hotbar & Active Item Display (Bottom Center)
        var triggerRecompose by remember { mutableStateOf(0) } // Used to trigger recomposition when inventory changes
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Selected item name overlay
            val selectedBlockId = player.inventory.getSelectedBlock()
            val blockName = when(selectedBlockId) {
                BlockRegistry.GRASS -> "Grass Block"
                BlockRegistry.DIRT -> "Dirt"
                BlockRegistry.STONE -> "Stone"
                BlockRegistry.SAND -> "Sand"
                BlockRegistry.WOOD -> "Wood"
                BlockRegistry.PLANKS -> "Wooden Planks"
                BlockRegistry.LEAVES -> "Leaves"
                BlockRegistry.COBBLESTONE -> "Cobblestone"
                BlockRegistry.WATER -> "Water"
                BlockRegistry.LAVA -> "Lava"
                BlockRegistry.GLASS -> "Glass"
                BlockRegistry.WOODEN_PICKAXE -> "Wooden Pickaxe"
                BlockRegistry.STONE_PICKAXE -> "Stone Pickaxe"
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
            
            // Hotbar row
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
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
                                    player.inventory.selectedHotbarSlot = slot
                                    triggerRecompose++ // force UI update
                                }
                                .padding(4.dp)
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
                                BlockRegistry.WOOD -> {
                                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF5C4033)))
                                }
                                BlockRegistry.PLANKS -> {
                                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFB5885C)))
                                }
                                BlockRegistry.LEAVES -> {
                                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF228B22)))
                                }
                                BlockRegistry.COBBLESTONE -> {
                                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF555555)))
                                }
                                BlockRegistry.WATER -> {
                                    Box(modifier = Modifier.fillMaxSize().background(Color(0x803366FF)))
                                }
                                BlockRegistry.LAVA -> {
                                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFF5500)))
                                }
                                BlockRegistry.GLASS -> {
                                    Box(modifier = Modifier.fillMaxSize().background(Color(0x60EEEEFF)))
                                }
                                BlockRegistry.WOODEN_PICKAXE -> {
                                    Text("⛏️", fontSize = 20.sp, modifier = Modifier.align(Alignment.Center))
                                }
                                BlockRegistry.STONE_PICKAXE -> {
                                    Text("⛏", color = Color.Gray, fontSize = 20.sp, modifier = Modifier.align(Alignment.Center))
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
            // Jump or Fly UP / DN buttons
            if (player.canFly) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {},
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1).copy(alpha = 0.8f)),
                        modifier = Modifier
                            .size(56.dp)
                            .pointerInputHoverLike { down -> moveUp = down }
                    ) {
                        Text("UP ▲", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    
                    Button(
                        onClick = {},
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF303F9F).copy(alpha = 0.8f)),
                        modifier = Modifier
                            .size(56.dp)
                            .pointerInputHoverLike { down -> moveDown = down }
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
                        .pointerInputHoverLike { down -> jump = down }
                ) {
                    Text("JUMP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
            
            // Break and Place side by side actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .pointerInputHoverLike { down -> isBreaking = down }
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

        if (showAdminPanel) {
            var activeSettingsTab by remember { mutableStateOf(0) } // 0 = General Settings, 1 = Operator (OP) Tools

            AlertDialog(
                onDismissRequest = { showAdminPanel = false },
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
                        IconButton(onClick = { showAdminPanel = false }) {
                            Text("❌", color = Color.White, fontSize = 16.sp)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tab Selector Headers
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

                        // Content Based on Active Tab
                        if (activeSettingsTab == 0) {
                            // --- SETTINGS TAB ---
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // 1. FPS Limit Slider
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
                                            fpsLimit = it.toInt()
                                            preferences.edit().putInt("fpsLimit", it.toInt()).apply()
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

                                // 2. Render Distance Slider
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
                                            renderDistance = it
                                            preferences.edit().putFloat("renderDistance", it).apply()
                                        },
                                        valueRange = 4f..512f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFFFFB300),
                                            activeTrackColor = Color(0xFFFFB300),
                                            inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                                        )
                                    )
                                }

                                // 3. Camera Sensitivity Slider
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
                                            cameraSensitivity = it
                                            preferences.edit().putFloat("sensitivity", it).apply()
                                        },
                                        valueRange = 0.2f..2.0f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFFFFB300),
                                            activeTrackColor = Color(0xFFFFB300),
                                            inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                                        )
                                    )
                                }

                                // 4. Texture Quality Selector
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
                                                    textureQuality = quality
                                                    preferences.edit().putString("textureQuality", quality).apply()
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
                            // --- OPERATOR TOOLS TAB ---
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. Operator Mode Toggle
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
                                    Switch(
                                        checked = player.isOp,
                                        onCheckedChange = { player.isOp = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFFFFB300),
                                            checkedTrackColor = Color(0xFFFFB300).copy(alpha = 0.5f)
                                        )
                                    )
                                }

                                if (player.isOp) {
                                    // 2. Select Game Mode Row
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
                                            Player.GameMode.values().forEach { gMode ->
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
                                                            Player.GameMode.SURVIVAL -> "SURVIVAL"
                                                            Player.GameMode.CREATIVE -> "CREATIVE"
                                                            Player.GameMode.SPECTATOR -> "SPECTATOR"
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

                                    // 3. Permissions Checklist
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
                                        
                                        // Fly switch
                                        Row(
                                            modifier = Modifier.fillMaxWidth().height(28.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Can Fly (Полет)", color = Color.White, fontSize = 11.sp)
                                            Switch(
                                                checked = player.canFly,
                                                onCheckedChange = { player.canFly = it }
                                            )
                                        }
                                        
                                        // Keep Inventory switch
                                        Row(
                                            modifier = Modifier.fillMaxWidth().height(28.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Keep Inventory (Сохр. инвентаря)", color = Color.White, fontSize = 11.sp)
                                            Switch(
                                                checked = true, // Default true for this engine currently
                                                onCheckedChange = { }
                                            )
                                        }
                                        
                                        // Time of day toggle
                                        Row(
                                            modifier = Modifier.fillMaxWidth().height(28.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Set Night Time", color = Color.White, fontSize = 11.sp)
                                            Switch(
                                                checked = world.isNight,
                                                onCheckedChange = { world.isNight = it }
                                            )
                                        }
                                        
                                        // Build permission switch
                                        Row(
                                            modifier = Modifier.fillMaxWidth().height(28.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Can Place (Ставить блоки)", color = Color.White, fontSize = 11.sp)
                                            Switch(
                                                checked = player.canBuild,
                                                onCheckedChange = { player.canBuild = it }
                                            )
                                        }
                                        
                                        // Break permission switch
                                        Row(
                                            modifier = Modifier.fillMaxWidth().height(28.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Can Break (Ломать блоки)", color = Color.White, fontSize = 11.sp)
                                            Switch(
                                                checked = player.canBreak,
                                                onCheckedChange = { player.canBreak = it }
                                            )
                                        }
                                    }

                                    // 4. Special commands
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
                                            // Day command
                                            Button(
                                                onClick = { world.isNight = false },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                                                shape = RoundedCornerShape(4.dp),
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.weight(1f).height(30.dp)
                                            ) {
                                                Text("☀️ SET DAY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            
                                            // Night command
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
                                            // Teleport Spawn command
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
                                            
                                            // Unstuck / Teleport up command
                                            Button(
                                                onClick = {
                                                    val curX = Math.floor(player.camera.position.x.toDouble()).toInt()
                                                    val curY = Math.floor(player.camera.position.y.toDouble()).toInt()
                                                    val curZ = Math.floor(player.camera.position.z.toDouble()).toInt()
                                                    
                                                    var foundY = -1
                                                    for (y in curY..250) {
                                                        if (!BlockRegistry.isSolid(world.getBlock(curX, y, curZ)) &&
                                                            !BlockRegistry.isSolid(world.getBlock(curX, y + 1, curZ))) {
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
                        onClick = { showAdminPanel = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
                    ) {
                        Text("DONE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        }
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
