package com.example.minecraft.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minecraft.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

data class BlockParticle(
    val id: Long = java.util.UUID.randomUUID().mostSignificantBits,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var alpha: Float = 1.0f,
    val size: Float = 6f + Math.random().toFloat() * 8f,
    var age: Int = 0,
    val maxAge: Int = 15 + (Math.random() * 12).toInt()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    world: WorldSave,
    onQuitToMenu: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Grab StateFlows from ViewModel
    val isPaused by viewModel.isPaused.collectAsState()
    val inventoryOpen by viewModel.inventoryOpen.collectAsState()
    val isPlaceMode by viewModel.isPlaceMode.collectAsState()
    val openFurnaceCoord by viewModel.openFurnaceCoord.collectAsState()
    val openChestCoord by viewModel.openChestCoord.collectAsState()
    val isCraftingNear by viewModel.isCraftingTableNear.collectAsState()
    val miningProgress by viewModel.miningProgress.collectAsState()

    // Dynamic Options from VM settings
    val particlesEnabled by viewModel.particlesEnabled.collectAsState()
    val particleDensity by viewModel.particleDensity.collectAsState()
    val renderDistanceSetting by viewModel.renderDistanceSetting.collectAsState()
    val isFirstPersonSetting by viewModel.isFirstPersonSetting.collectAsState()
    val buttonSizeMultiplier by viewModel.buttonSizeMultiplier.collectAsState()
    val touchSensitivity by viewModel.touchSensitivity.collectAsState()
    val soundEffectsEnabled by viewModel.soundEffectsEnabled.collectAsState()

    // Active screen and dialog configs
    var showSettingsDialog by remember { mutableStateOf(false) }
    val particles = remember { mutableStateListOf<BlockParticle>() }

    // Coroutine-driven 60FPS physics step simulator
    LaunchedEffect(isPaused) {
        while (true) {
            delay(16) // ~60fps ticks
            if (!isPaused && particles.isNotEmpty()) {
                val listSnapshot = particles.toList()
                particles.clear()
                listSnapshot.forEach { p ->
                    p.x += p.vx
                    p.y += p.vy
                    p.vy += 0.4f // Gravitational constant
                    p.age++
                    p.alpha = (1.0f - p.age.toFloat() / p.maxAge).coerceIn(0f, 1f)
                    if (p.age < p.maxAge) {
                        particles.add(p)
                    }
                }
            }
        }
    }

    val player = world.playerState
    val activeHotItem = player.inventory[player.activeHotbarIndex]

    // 3D Engine Variables
    var is3DMode by remember { mutableStateOf(true) } // State-of-the-art 3D OpenGL enabled by default!
    var pitchAngle by remember { mutableStateOf(-12f) }
    var yawAngle by remember { mutableStateOf(14f) }
    val currentYaw by androidx.compose.runtime.rememberUpdatedState(yawAngle)
    var zoomVal by remember { mutableStateOf(8.5f) }

    // Screen-space scaling factors (40dp tiles)
    val tileSizeDp = 44.dp
    
    // Joystick timers to reset movement values
    DisposableEffect(Unit) {
        onDispose {
            viewModel.joystickX = 0f
            viewModel.isJumpingPressed = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("game_screen")
            .background(SkyDay)
    ) {
        
        // ==========================================
        // 1. GAME MAP VOXEL CANVAS RENDERING ENGINE
        // ==========================================
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val viewWidthPx = constraints.maxWidth.toFloat()
            val viewHeightPx = constraints.maxHeight.toFloat()
            val tileSizePx = tileSizeDp.value * (viewHeightPx / 480f) // Responsive tile calculations

            // Player coordinates are voxel-space. Set camera center to Steve!
            val camOffsetX = player.x * tileSizePx - (viewWidthPx / 2f)
            val camOffsetY = (world.height - player.y) * tileSizePx - (viewHeightPx / 2f)

            // Dynamic background interpolation based on world.currentSkyTime (0 to 24000)
            val skyColor = remember(world.currentSkyTime) {
                val tick = world.currentSkyTime
                when {
                    tick < 10000 -> SkyDay
                    tick < 12000 -> {
                        // Blend Day to Dusk
                        val ratio = (tick - 10000) / 2000f
                        lerpColor(SkyDay, SkyDusk, ratio)
                    }
                    tick < 13000 -> {
                        // Blend Dusk to Night
                        val ratio = (tick - 12000) / 1000f
                        lerpColor(SkyDusk, SkyNight, ratio)
                    }
                    tick < 21000 -> SkyNight
                    tick < 22000 -> {
                        // Blend Night to Dawn
                        val ratio = (tick - 21000) / 1000f
                        lerpColor(SkyNight, SkyDawn, ratio)
                    }
                    else -> {
                        // Blend Dawn to Day
                        val ratio = (tick - 22000) / 2000f
                        lerpColor(SkyDawn, SkyDay, ratio)
                    }
                }
            }

            if (is3DMode) {
                // RENDER GORGEOUS HARDWARE-ACCELERATED 3D OPENGL VOXEL SANDBOX!
                Minecraft3DView(
                    modifier = Modifier.fillMaxSize(),
                    world = world,
                    viewModel = viewModel,
                    pitchAngle = pitchAngle,
                    yawAngle = yawAngle,
                    zoomVal = zoomVal,
                    onBlockTap = { vx, vy -> }
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(skyColor)
                ) {
                    // Background star overlays during night!
                    if (world.currentSkyTime in 12500f..21500f) {
                        val rand = java.util.Random(42) // Stable stars
                        for (i in 0..60) {
                            val sx = rand.nextFloat() * size.width
                            val sy = rand.nextFloat() * size.height
                            val brightness = rand.nextFloat()
                            drawCircle(
                                color = Color.White.copy(alpha = brightness),
                                radius = 2f,
                                center = Offset(sx, sy)
                            )
                        }
                    }

                    // Calculate visible world column boundaries
                    val startCol = (camOffsetX / tileSizePx).toInt().coerceIn(0, world.width - 1)
                    val endCol = ((camOffsetX + viewWidthPx) / tileSizePx).toInt().coerceIn(0, world.width - 1)
                    val startRow = (camOffsetY / tileSizePx).toInt().coerceIn(0, world.height - 1)
                    val endRow = ((camOffsetY + viewHeightPx) / tileSizePx).toInt().coerceIn(0, world.height - 1)

                    // RENDER BLOCKS GRID
                    for (cx in startCol..endCol) {
                        for (cyRow in startRow..endRow) {
                            val cy = world.height - 1 - cyRow
                            val bId = world.worldBlocks["$cx,$cy"] ?: "air"
                            if (bId == "air") continue

                            val bType = GameRegistry.blocks[bId] ?: continue
                            val vx = cx * tileSizePx - camOffsetX
                            val vy = cyRow * tileSizePx - camOffsetY

                            // Solid block filled drawing
                            drawRect(
                                color = bType.getColor(),
                                topLeft = Offset(vx, vy),
                                size = Size(tileSizePx, tileSizePx)
                            )

                            // 3D Highlight top edge / shadows patterns
                            bType.getTopColor()?.let { topCol ->
                                drawRect(
                                    color = topCol,
                                    topLeft = Offset(vx, vy),
                                    size = Size(tileSizePx, tileSizePx * 0.18f)
                                )
                            }

                            // Crack accent/border sketches inside pixels
                            bType.getAccentColor()?.let { accCol ->
                                // Left outline highlight
                                drawRect(
                                    color = accCol,
                                    topLeft = Offset(vx, vy),
                                    size = Size(tileSizePx * 0.08f, tileSizePx)
                                )
                                // Right bottom frame shadow
                                drawRect(
                                    color = accCol,
                                    topLeft = Offset(vx, vy + tileSizePx - (tileSizePx * 0.08f)),
                                    size = Size(tileSizePx, tileSizePx * 0.08f)
                                )
                            }

                            // Decorate distinct blocks visually (crosses, boxes, rings)
                            when (bId) {
                                "oak_log" -> {
                                    drawRect(color = Color(0xFF4A321A), topLeft = Offset(vx + tileSizePx*0.25f, vy), size = Size(tileSizePx*0.5f, tileSizePx))
                                }
                                "oak_leaves" -> {
                                    drawCircle(color = Color(0xFF1D4713), radius = tileSizePx*0.3f, center = Offset(vx + tileSizePx/2f, vy + tileSizePx/2f))
                                }
                                "coal_ore" -> {
                                    drawCircle(color = Color.Black, radius = tileSizePx*0.12f, center = Offset(vx + tileSizePx*0.3f, vy + tileSizePx*0.4f))
                                    drawCircle(color = Color.Black, radius = tileSizePx*0.09f, center = Offset(vx + tileSizePx*0.7f, vy + tileSizePx*0.65f))
                                }
                                "iron_ore" -> {
                                    drawCircle(color = Color(0xFFD09476), radius = tileSizePx*0.13f, center = Offset(vx + tileSizePx*0.4f, vy + tileSizePx*0.3f))
                                    drawCircle(color = Color(0xFFD09476), radius = tileSizePx*0.1f, center = Offset(vx + tileSizePx*0.65f, vy + tileSizePx*0.65f))
                                }
                                "diamond_ore" -> {
                                    drawCircle(color = Color(0xFF4DDEEC), radius = tileSizePx*0.11f, center = Offset(vx + tileSizePx*0.35f, vy + tileSizePx*0.6f))
                                    drawCircle(color = Color(0xFF4DDEEC), radius = tileSizePx*0.14f, center = Offset(vx + tileSizePx*0.65f, vy + tileSizePx*0.35f))
                                }
                                "crafting_table" -> {
                                    drawRect(color = Color(0xFF5A3E26), topLeft = Offset(vx + tileSizePx*0.2f, vy + tileSizePx*0.2f), size = Size(tileSizePx*0.6f, tileSizePx*0.6f))
                                }
                                "furnace" -> {
                                    drawRect(color = Color.Black, topLeft = Offset(vx + tileSizePx*0.25f, vy + tileSizePx*0.4f), size = Size(tileSizePx*0.5f, tileSizePx*0.4f))
                                }
                                "chest" -> {
                                    drawRect(color = Color(0xFFFFD700), topLeft = Offset(vx + tileSizePx*0.4f, vy + tileSizePx*0.4f), size = Size(tileSizePx*0.2f, tileSizePx*0.2f))
                                }
                            }

                            // Draw Grid Border Wireframe
                            drawRect(
                                color = Color(0x22000000),
                                topLeft = Offset(vx, vy),
                                size = Size(tileSizePx, tileSizePx),
                                style = Stroke(width = 1f)
                            )
                        }
                    }

                    // ==========================================
                    // 2. RENDER STEVE PLAYER MODEL
                    // ==========================================
                    val steveWidth = 0.6f * tileSizePx
                    val steveHeight = 1.8f * tileSizePx
                    val sx = (player.x * tileSizePx) - camOffsetX
                    val sy = ((world.height - player.y - 1.8f) * tileSizePx) - camOffsetY

                    // Head (Peach skin color box)
                    drawRect(
                        color = Color(0xFFE8B68E),
                        topLeft = Offset(sx + (steveWidth * 0.15f), sy),
                        size = Size(steveWidth * 0.7f, steveHeight * 0.25f)
                    )
                    // Hair decoration (Dark brown cap)
                    drawRect(
                        color = Color(0xFF52331E),
                        topLeft = Offset(sx + (steveWidth * 0.15f), sy),
                        size = Size(steveWidth * 0.7f, steveHeight * 0.08f)
                    )

                    // Body torso (Steve Cyan clothes)
                    drawRect(
                        color = Color(0xFF00ADB5),
                        topLeft = Offset(sx, sy + (steveHeight * 0.25f)),
                        size = Size(steveWidth, steveHeight * 0.42f)
                    )

                    // Trousers/Legs (Purple)
                    drawRect(
                        color = Color(0xFF3F3B6C),
                        topLeft = Offset(sx + (steveWidth * 0.08f), sy + (steveHeight * 0.67f)),
                        size = Size(steveWidth * 0.84f, steveHeight * 0.33f)
                    )

                    // Left & Right Shoes (Grey outline top ground)
                    drawRect(
                        color = Color(0xFF222831),
                        topLeft = Offset(sx + (steveWidth * 0.08f), sy + (steveHeight * 0.95f)),
                        size = Size(steveWidth * 0.35f, steveHeight * 0.05f)
                    )
                    drawRect(
                        color = Color(0xFF222831),
                        topLeft = Offset(sx + (steveWidth * 0.57f), sy + (steveHeight * 0.95f)),
                        size = Size(steveWidth * 0.35f, steveHeight * 0.05f)
                    )
                }
            }

            // --- UNIFIED TOUCH GESTURE CONTROLLER (HOLD-TO-BREAK & TAP-TO-PLACE) ---
            var activeMiningX by remember { mutableStateOf(-1) }
            var activeMiningY by remember { mutableStateOf(-1) }
            var miningJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
            var lastTouchX by remember { mutableStateOf(0f) }
            var lastTouchY by remember { mutableStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(world, camOffsetX, camOffsetY, tileSizePx) {
                        detectTapGestures(
                            onTap = { offset ->
                                val hitVx = ((offset.x + camOffsetX) / tileSizePx).toInt()
                                val hitVy = (world.height - ((offset.y + camOffsetY) / tileSizePx)).toInt()
                                
                                val existingBlock = world.worldBlocks["$hitVx,$hitVy"] ?: "air"
                                if (existingBlock == "air") {
                                    viewModel.forcePlaceBlock(context, hitVx, hitVy)
                                } else if (existingBlock == "furnace" || existingBlock == "chest" || existingBlock == "crafter" || existingBlock == "vault" || existingBlock == "trial_spawner") {
                                    viewModel.handleBlockInteraction(context, hitVx, hitVy)
                                } else {
                                    // Tapped on solid block -> Place ADJACENT to the clicked side like Pocket Edition
                                    val clickRelX = (offset.x + camOffsetX) - (hitVx * tileSizePx)
                                    val clickRelY = (offset.y + camOffsetY) - ((world.height - 1 - hitVy) * tileSizePx)
                                    
                                    var targetX = hitVx
                                    var targetY = hitVy
                                    
                                    val distLeft = clickRelX
                                    val distRight = tileSizePx - clickRelX
                                    val distTop = clickRelY
                                    val distBottom = tileSizePx - clickRelY
                                    
                                    val minDist = minOf(distLeft, distRight, distTop, distBottom)
                                    when (minDist) {
                                        distLeft -> targetX = hitVx - 1
                                        distRight -> targetX = hitVx + 1
                                        distTop -> targetY = hitVy + 1
                                        distBottom -> targetY = hitVy - 1
                                    }
                                    
                                    val targetBlock = world.worldBlocks["$targetX,$targetY"] ?: "air"
                                    if (targetBlock == "air") {
                                        viewModel.forcePlaceBlock(context, targetX, targetY)
                                    } else {
                                        // Top fallback
                                        val topBlock = world.worldBlocks["$hitVx,${hitVy + 1}"] ?: "air"
                                        if (topBlock == "air") {
                                            viewModel.forcePlaceBlock(context, hitVx, hitVy + 1)
                                        }
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(world, camOffsetX, camOffsetY, tileSizePx) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val hitVx = ((offset.x + camOffsetX) / tileSizePx).toInt()
                                val hitVy = (world.height - ((offset.y + camOffsetY) / tileSizePx)).toInt()
                                val existingBlock = world.worldBlocks["$hitVx,$hitVy"] ?: "air"
                                
                                if (existingBlock != "air" && existingBlock != "bedrock") {
                                    activeMiningX = hitVx
                                    activeMiningY = hitVy
                                    lastTouchX = offset.x
                                    lastTouchY = offset.y
                                    
                                    miningJob?.cancel()
                                    miningJob = coroutineScope.launch {
                                        val bType = GameRegistry.blocks[existingBlock] ?: return@launch
                                        val speed = viewModel.getToolMiningSpeed(player.inventory[player.activeHotbarIndex], existingBlock)
                                        val totalTicks = (bType.hardness * 16f / speed).coerceAtLeast(3f)
                                        var tick = 0f
                                        
                                        while (tick < totalTicks) {
                                            delay(50) // 50ms ticks
                                            tick += 1.0f
                                            viewModel.setMiningProgress(tick / totalTicks)
                                            
                                            // Emit mining dust particles
                                            if (viewModel.particlesEnabled.value) {
                                                val bCol = Color(android.graphics.Color.parseColor(bType.colorHex))
                                                repeat((3 * viewModel.particleDensity.value).toInt()) {
                                                    val px = lastTouchX + (Math.random().toFloat() - 0.5f) * 30f
                                                    val py = lastTouchY + (Math.random().toFloat() - 0.5f) * 30f
                                                    val vx = (Math.random().toFloat() - 0.5f) * 5f
                                                    val vy = -2f - Math.random().toFloat() * 3f
                                                    particles.add(BlockParticle(x = px, y = py, vx = vx, vy = vy, color = bCol))
                                                }
                                            }
                                        }
                                        
                                        // Block broken!
                                        viewModel.mineBlockAction(context, hitVx, hitVy, existingBlock)
                                        
                                        // Exploding burst particles
                                        if (viewModel.particlesEnabled.value) {
                                            val bCol = Color(android.graphics.Color.parseColor(bType.colorHex))
                                            repeat((14 * viewModel.particleDensity.value).toInt()) {
                                                val vx = (Math.random().toFloat() - 0.5f) * 10f
                                                val vy = -4f - Math.random().toFloat() * 6f
                                                particles.add(BlockParticle(x = lastTouchX, y = lastTouchY, vx = vx, vy = vy, color = bCol))
                                            }
                                        }
                                        
                                        activeMiningX = -1
                                        activeMiningY = -1
                                        viewModel.resetMiningProgress()
                                    }
                                }
                            },
                            onDragEnd = {
                                miningJob?.cancel()
                                activeMiningX = -1
                                activeMiningY = -1
                                viewModel.resetMiningProgress()
                            },
                            onDragCancel = {
                                miningJob?.cancel()
                                activeMiningX = -1
                                activeMiningY = -1
                                viewModel.resetMiningProgress()
                            },
                            onDrag = { change, dragAmount ->
                                lastTouchX = change.position.x
                                lastTouchY = change.position.y
                                
                                val hitVx = ((lastTouchX + camOffsetX) / tileSizePx).toInt()
                                val hitVy = (world.height - ((lastTouchY + camOffsetY) / tileSizePx)).toInt()
                                if (hitVx != activeMiningX || hitVy != activeMiningY) {
                                    miningJob?.cancel()
                                    activeMiningX = -1
                                    activeMiningY = -1
                                    viewModel.resetMiningProgress()
                                }
                                
                                // Touch swipe-to-look camera rotation controls in 3D Mode
                                if (is3DMode && activeMiningX == -1) {
                                    yawAngle = (yawAngle - dragAmount.x * 0.22f * touchSensitivity) % 360f
                                    pitchAngle = (pitchAngle - dragAmount.y * 0.18f * touchSensitivity).coerceIn(-45f, 15f)
                                }
                            }
                        )
                    }
            )

            // --- EXQUISITE HIGH-PERFORMANCE BLOCK SHARD PARTICLES CANVAS CHANNEL ---
            Canvas(modifier = Modifier.fillMaxSize()) {
                particles.forEach { p ->
                    drawCircle(
                        color = p.color.copy(alpha = p.alpha),
                        radius = p.size,
                        center = Offset(p.x, p.y)
                    )
                }
            }

            // --- HUD COLOURED MINING RADIAL PROGRESS BAR EMBEDDED DYNAMICALLY ---
            if (miningProgress > 0f && lastTouchX > 0f && lastTouchY > 0f) {
                Box(
                    modifier = Modifier
                        .offset(
                            x = (lastTouchX / LocalContext.current.resources.displayMetrics.density).dp - 24.dp,
                            y = (lastTouchY / LocalContext.current.resources.displayMetrics.density).dp - 24.dp
                        )
                        .size(48.dp)
                        .background(Color(0x7F000000), shape = CircleShape)
                        .border(3.dp, MinecraftGreen.copy(alpha = 0.8f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = miningProgress,
                        color = MinecraftTextYellow,
                        strokeWidth = 4.dp,
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                    Text(
                        text = "${(miningProgress * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // ==========================================
        // 3. SURVIVAL STATUS HUD (Hearts & Hunger Legs)
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TopAppBarDefaults.windowInsets.asPaddingValues())
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left Column: Player Health Hearts (10 Hearts total)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val fullHearts = (player.health / 2f).toInt()
                val hasHalf = (player.health % 2f) >= 1.0f

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (i in 0 until 10) {
                        val color = when {
                            i < fullHearts -> HeartRed
                            i == fullHearts && hasHalf -> Color(0xFFD84B4B)
                            else -> Color(0x44220000)
                        }
                        // Custom heart drawing
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val path = Path().apply {
                                moveTo(size.width / 2f, size.height * 0.85f)
                                cubicTo(0f, size.height * 0.4f, 0f, 0f, size.width / 4f, 0f)
                                cubicTo(size.width / 2f, 0f, size.width / 2f, size.height * 0.3f, size.width / 2f, size.height * 0.3f)
                                cubicTo(size.width / 2f, size.height * 0.3f, size.width / 2f, 0f, size.width * 0.75f, 0f)
                                cubicTo(size.width, 0f, size.width, size.height * 0.4f, size.width / 2f, size.height * 0.85f)
                                close()
                            }
                            drawPath(path = path, color = color)
                        }
                    }
                }

                // Row of hunger legs
                val hungerTicks = (player.hunger / 2f).toInt()
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (i in 0 until 10) {
                        val color = if (i < hungerTicks) BreadBrown else Color(0x443E2723)
                        Canvas(modifier = Modifier.size(14.dp)) {
                            drawCircle(color = color, radius = size.width / 2f)
                        }
                    }
                }
            }

            // Central: Active holding Item details
            Column(
                modifier = Modifier
                    .background(Color(0x99000000))
                    .padding(vertical = 4.dp, horizontal = 12.dp)
                    .widthIn(max = 240.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val itemText = activeHotItem?.let {
                    val type = GameRegistry.items[it.itemId]
                    "${type?.name} (x${it.count})"
                } ?: "Bare Hands"
                
                Text(
                    text = itemText.uppercase(),
                    color = MinecraftTextYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )

                // Mini day progression dial
                val skyPercent = (world.currentSkyTime / 24000f) * 100f
                val periodName = if (world.currentSkyTime in 12000f..22000f) "Night Shift" else "Daylight"
                Text(
                    text = "$periodName [${skyPercent.toInt()}%]",
                    color = Color.LightGray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Top-right Menu actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // If holding consumable/usable, render immediate action trigger!
                if (activeHotItem?.itemId == "apple") {
                    MinecraftButton(
                        text = "Eat Apple",
                        onClick = { viewModel.consumeActiveFood(context) },
                        modifier = Modifier.height(34.dp)
                    )
                } else if (activeHotItem?.itemId == "nano_banana_2_item") {
                    MinecraftButton(
                        text = "Eat Nano Banana 2",
                        onClick = { viewModel.consumeActiveFood(context) },
                        modifier = Modifier.height(34.dp)
                    )
                } else if (activeHotItem?.itemId == "ominous_bottle") {
                    MinecraftButton(
                        text = "Drink Ominous Bottle",
                        onClick = { viewModel.consumeActiveFood(context) },
                        modifier = Modifier.height(34.dp)
                    )
                } else if (activeHotItem?.itemId == "wind_charge") {
                    MinecraftButton(
                        text = "Launch Wind Charge",
                        onClick = { viewModel.consumeActiveFood(context) },
                        modifier = Modifier.height(34.dp)
                    )
                } else if (activeHotItem?.itemId == "mace") {
                    MinecraftButton(
                        text = "Smash Mace!",
                        onClick = { viewModel.consumeActiveFood(context) },
                        modifier = Modifier.height(34.dp)
                    )
                }

                // 2D <-> 3D Mode Switcher
                MinecraftButton(
                    text = if (is3DMode) "Switch 2D" else "Switch 3D",
                    onClick = { is3DMode = !is3DMode },
                    modifier = Modifier.height(34.dp)
                )

                // Render dynamic 3D camera presets button
                if (is3DMode) {
                    MinecraftButton(
                        text = "Cam Mode",
                        onClick = {
                            when {
                                pitchAngle == -12f && yawAngle == 14f -> {
                                    // 3D Front Scroll View
                                    pitchAngle = 0f
                                    yawAngle = 0f
                                    zoomVal = 8.0f
                                }
                                pitchAngle == 0f && yawAngle == 0f -> {
                                    // 3D Wide Cinematic view
                                    pitchAngle = -25f
                                    yawAngle = -25f
                                    zoomVal = 10.5f
                                }
                                else -> {
                                    // Back to Stunning Axonometric Isometric
                                    pitchAngle = -12f
                                    yawAngle = 14f
                                    zoomVal = 8.5f
                                }
                            }
                        },
                        modifier = Modifier.height(34.dp)
                    )
                    MinecraftButton(
                        text = "Zoom +",
                        onClick = { zoomVal = (zoomVal - 1f).coerceIn(3.5f, 16f) },
                        modifier = Modifier.height(34.dp)
                    )
                    MinecraftButton(
                        text = "Zoom -",
                        onClick = { zoomVal = (zoomVal + 1f).coerceIn(3.5f, 16f) },
                        modifier = Modifier.height(34.dp)
                    )
                }

                MinecraftButton(
                    text = "Inventory",
                    onClick = { viewModel.toggleInventory() },
                    modifier = Modifier.height(34.dp)
                )

                IconButton(
                    onClick = { viewModel.togglePause() },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xAA000000))
                        .border(1.dp, Color.Gray)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // ==========================================
        // 4. ERGONOMIC ON-SCREEN SURVIVAL CONTROLS (TACTILE 4-WAY CROSS D-PAD)
        // ==========================================
        // Bottom-Left Side: Beautiful, Responsive 4-Way D-Pad Cross
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 16.dp, start = 24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Top Arrow (▲) - MOVE FORWARD (Camera-relative, 100% non-sticky)
                Box(
                    modifier = Modifier
                        .size((48 * buttonSizeMultiplier).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xE6222222))
                        .border(2.dp, Color(0xFF8A8A8A), RoundedCornerShape(8.dp))
                        .pointerInput(buttonSizeMultiplier) {
                            detectTapGestures(
                                onPress = {
                                    val lookDirX = Math.sin(Math.toRadians(currentYaw.toDouble())).toFloat()
                                    viewModel.joystickX = if (lookDirX >= 0f) 1f else -1f
                                    try {
                                        awaitRelease()
                                    } finally {
                                        viewModel.joystickX = 0f
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("▲", color = Color.White, fontSize = (18 * buttonSizeMultiplier).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Arrow (◀)
                    Box(
                        modifier = Modifier
                            .size((48 * buttonSizeMultiplier).dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xE6222222))
                            .border(2.dp, Color(0xFF8A8A8A), RoundedCornerShape(8.dp))
                            .pointerInput(buttonSizeMultiplier) {
                                detectTapGestures(
                                    onPress = {
                                        viewModel.joystickX = -1f
                                        try {
                                            awaitRelease()
                                        } finally {
                                            viewModel.joystickX = 0f
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("◀", color = Color.White, fontSize = (18 * buttonSizeMultiplier).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    // Touch Center Decorative Space
                    Box(
                        modifier = Modifier
                            .size((48 * buttonSizeMultiplier).dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33FFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                    )

                    // Right Arrow (▶)
                    Box(
                        modifier = Modifier
                            .size((48 * buttonSizeMultiplier).dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xE6222222))
                            .border(2.dp, Color(0xFF8A8A8A), RoundedCornerShape(8.dp))
                            .pointerInput(buttonSizeMultiplier) {
                                detectTapGestures(
                                    onPress = {
                                        viewModel.joystickX = 1f
                                        try {
                                            awaitRelease()
                                        } finally {
                                            viewModel.joystickX = 0f
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▶", color = Color.White, fontSize = (18 * buttonSizeMultiplier).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                // Bottom Arrow (▼) - MOVE BACKWARD (Camera-relative, 100% non-sticky)
                Box(
                    modifier = Modifier
                        .size((48 * buttonSizeMultiplier).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xE6222222))
                        .border(2.dp, Color(0xFF8A8A8A), RoundedCornerShape(8.dp))
                        .pointerInput(buttonSizeMultiplier) {
                            detectTapGestures(
                                onPress = {
                                    val lookDirX = Math.sin(Math.toRadians(currentYaw.toDouble())).toFloat()
                                    viewModel.joystickX = if (lookDirX >= 0f) -1f else 1f
                                    try {
                                        awaitRelease()
                                    } finally {
                                        viewModel.joystickX = 0f
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("▼", color = Color.White, fontSize = (18 * buttonSizeMultiplier).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Bottom-Right Side: Jump Control (100% non-sticky, build/mine toggle removed!)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 24.dp)
        ) {
            // Tactical JUMP action with instantaneous touch response
            Box(
                modifier = Modifier
                    .size((54 * buttonSizeMultiplier).dp)
                    .clip(CircleShape)
                    .background(Color(0xBB222222))
                    .border(2.dp, Color(0xFF8A8A8A), CircleShape)
                    .pointerInput(buttonSizeMultiplier) {
                        detectTapGestures(
                            onPress = {
                                viewModel.isJumpingPressed = true
                                try {
                                    awaitRelease()
                                } finally {
                                    viewModel.isJumpingPressed = false
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("JUMP", color = Color.White, fontSize = (11 * buttonSizeMultiplier).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        // ==========================================
        // 5. THE ACTIVE QUICK HOTBAR DOCKED PANEL
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
                .background(Color(0xE62E2E2E))
                .border(2.dp, Color(0xFF8A8A8A))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0..8) {
                    val isSelected = player.activeHotbarIndex == i
                    val stack = player.inventory[i]
                    val bBorder = if (isSelected) Color.White else Color(0xFF555555)

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF222222))
                            .border(width = if (isSelected) 2.dp else 1.dp, color = bBorder)
                            .clickable { viewModel.selectHotbarSlot(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (stack != null) {
                            val type = GameRegistry.items[stack.itemId]
                            // Simple visual render overlay stack
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(0.6f)
                                    .background(type?.getColor() ?: Color.Gray)
                            )
                            if (stack.count > 1) {
                                Text(
                                    text = "${stack.count}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 6. INVENTORY OVERLAY SLATE (STONE BACKDROP)
        // ==========================================
        if (inventoryOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD9000000))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Stone gray chest tiled border popup layout
                Column(
                    modifier = Modifier
                        .width(480.dp)
                        .background(Color(0xFF333333))
                        .border(2.dp, Color(0xFF8A8A8A))
                        .padding(14.dp)
                ) {
                    // Header container
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (openFurnaceCoord != null) "FURNACE CONTAINER" else if (openChestCoord != null) "WOODEN CHEST" else "SURVIVAL INVENTORY",
                            color = MinecraftTextYellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        IconButton(
                            onClick = { viewModel.closeContainer(context) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left Pane: Player Main Inventory slots (9 to 35) + Hotbar (0 to 8)
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text("STORAGE BLOCKBAG", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Render 3x9 Inventory Grid
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(9),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Full inventory indexed 0 to 35
                                items(36) { index ->
                                    val stack = player.inventory[index]
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .background(Color.Black)
                                            .border(1.dp, Color(0xFF555555))
                                            .clickable {
                                                // Handle container logic if active
                                                if (openFurnaceCoord != null) {
                                                    viewModel.moveFurnaceItem(context, "INPUT", index)
                                                } else if (openChestCoord != null) {
                                                    viewModel.moveChestItem(context, 0, index, true) // quick load
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (stack != null) {
                                            val itemType = GameRegistry.items[stack.itemId]
                                            Box(modifier = Modifier.fillMaxSize(0.65f).background(itemType?.getColor() ?: Color.Gray))
                                            Text(
                                                text = "${stack.count}",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Right Pane: Crafting Recipes or Furnace refining / Chest Slots
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF222222))
                                .border(1.dp, Color(0xFF555555))
                                .padding(8.dp)
                        ) {
                            if (openFurnaceCoord != null) {
                                val coord = openFurnaceCoord!!
                                val furnace = world.furnaceStates[coord]
                                
                                if (furnace != null) {
                                    Text("FURNACE MILL", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Input Display Slot
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("INPUT", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                            Box(modifier = Modifier.size(32.dp).background(Color.Black).border(1.dp, Color.Gray), contentAlignment = Alignment.Center) {
                                                val stack = furnace.inputStack
                                                if (stack != null) {
                                                    Box(modifier = Modifier.size(16.dp).background(GameRegistry.items[stack.itemId]?.getColor() ?: Color.White))
                                                    Text("${stack.count}", color = Color.White, fontSize = 8.sp, modifier = Modifier.align(Alignment.BottomEnd))
                                                }
                                            }
                                        }

                                        // Fuel Display Slot
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("FUEL", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                            Box(modifier = Modifier.size(32.dp).background(Color.Black).border(1.dp, Color.Gray), contentAlignment = Alignment.Center) {
                                                val stack = furnace.fuelStack
                                                if (stack != null) {
                                                    Box(modifier = Modifier.size(16.dp).background(GameRegistry.items[stack.itemId]?.getColor() ?: Color.White))
                                                    Text("${stack.count}", color = Color.White, fontSize = 8.sp, modifier = Modifier.align(Alignment.BottomEnd))
                                                }
                                            }
                                        }
                                        
                                        // Burn/Cook Indicators
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            val burnPercent = if (furnace.maxBurnTime > 0) (furnace.burnTimeRemaining.toFloat() / furnace.maxBurnTime) * 100 else 0f
                                            Text("FIRE: ${burnPercent.toInt()}%", color = Color(0xFFFFA500), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                            Text("COOK: ${furnace.cookTimeProgress}/200", color = Color.Yellow, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                        }

                                        // Smelted Output Slot
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("OUTPUT", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(Color(0xFF151515))
                                                    .border(2.dp, Color.Yellow)
                                                    .clickable { viewModel.takeFurnaceOutput(context, 0) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val stack = furnace.outputStack
                                                if (stack != null) {
                                                    Box(modifier = Modifier.size(20.dp).background(GameRegistry.items[stack.itemId]?.getColor() ?: Color.White))
                                                    Text("${stack.count}", color = Color.White, fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomEnd))
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (openChestCoord != null) {
                                val chest = world.chestStates[openChestCoord]
                                if (chest != null) {
                                    Text("CHEST SLOTS (TAP TRSF)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(4),
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(20) { index ->
                                            val stack = chest.slots[index]
                                            Box(
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .background(Color.Black)
                                                    .border(1.dp, Color.Gray)
                                                    .clickable { viewModel.moveChestItem(context, index, 0, false) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (stack != null) {
                                                    Box(modifier = Modifier.fillMaxSize(0.6f).background(GameRegistry.items[stack.itemId]?.getColor() ?: Color.White))
                                                    Text("${stack.count}", color = Color.White, fontSize = 8.sp, modifier = Modifier.align(Alignment.BottomEnd))
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Default Crafting Table Table Recipes
                                Text("CRAFTING TABLE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(6.dp))

                                // Filter recipes based on table proximity
                                val activeRecipes = remember(isCraftingNear) {
                                    GameRegistry.craftingRecipes.filter { !it.requiresCraftingTable || isCraftingNear }
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (recipe in activeRecipes) {
                                        val targetType = GameRegistry.items[recipe.resultItemId]
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF1A1A1A))
                                                .border(1.dp, Color(0xFF333333))
                                                .clickable { viewModel.craftItem(context, recipe) }
                                                .padding(6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(20.dp).background(targetType?.getColor() ?: Color.Gray))
                                                Text(targetType?.name ?: "", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            }
                                            Text("Craft x${recipe.resultCount}", color = MinecraftTextYellow, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 7. GAME PAUSE OVERLAY MENU
        // ==========================================
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD9000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .background(Color(0xFF2E2E2E))
                        .border(2.dp, Color(0xFF8A8A8A))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "GAME PAUSED",
                        color = MinecraftTextYellow,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    MinecraftButton(
                        text = "Resume Game",
                        onClick = { viewModel.togglePause() },
                        modifier = Modifier.fillMaxWidth()
                    )

                    MinecraftButton(
                        text = "Save State",
                        onClick = {
                            viewModel.addToInventory("apple", 2) // grant bonus apple for fun on save!
                            viewModel.closeContainer(context)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    MinecraftButton(
                        text = "Options & Settings",
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    MinecraftButton(
                        text = "Quit to Title",
                        onClick = {
                            viewModel.closeContainer(context)
                            onQuitToMenu()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ==========================================
        // 8. MINECRAFT OPTIONS & SETTINGS POPUP MODAL
        // ==========================================
        if (showSettingsDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(360.dp)
                        .background(Color(0xFF2E2E2E))
                        .border(2.dp, Color(0xFF8A8A8A))
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "OPTIONS & SETTINGS",
                        color = MinecraftTextYellow,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 1. Particle System Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Particles Engine", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(if (particlesEnabled) Color(0xFF3F6C3F) else Color(0xFF444444))
                                    .border(1.dp, Color.White)
                                    .clickable { viewModel.updateParticlesEnabled(true) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("ON", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .background(if (!particlesEnabled) Color(0xFF8A3A3A) else Color(0xFF444444))
                                    .border(1.dp, Color.White)
                                    .clickable { viewModel.updateParticlesEnabled(false) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("OFF", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 2. Particle Density Slider
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Particle Quantity", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text("${String.format("%.1f", particleDensity)}x", color = MinecraftTextYellow, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = particleDensity,
                            onValueChange = { viewModel.updateParticleDensity(it) },
                            valueRange = 0.5f..2.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = MinecraftTextYellow,
                                activeTrackColor = MinecraftGreen
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0xFF555555), thickness = 1.dp)

                    // 3. Render Distance Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Render Distance", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Low", "Medium", "High").forEach { dist ->
                                Box(
                                    modifier = Modifier
                                        .background(if (renderDistanceSetting == dist) Color(0xFF3B5B8A) else Color(0xFF444444))
                                        .border(1.dp, Color.White)
                                        .clickable { viewModel.updateRenderDistance(dist) }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(dist, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    // 4. Perspective Mode Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gameplay Camera", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Box(
                            modifier = Modifier
                                .background(if (isFirstPersonSetting) Color(0xFF5C335C) else Color(0xFF335C5C))
                                .border(1.dp, Color.White)
                                .clickable { viewModel.updateFirstPerson(!isFirstPersonSetting) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isFirstPersonSetting) "1st Person [Real]" else "Axonometric 3D",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF555555), thickness = 1.dp)

                    // 5. Button Sizing Slider
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("D-Pad Button Size", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text("${String.format("%.2f", buttonSizeMultiplier)}x", color = MinecraftTextYellow, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = buttonSizeMultiplier,
                            onValueChange = { viewModel.updateButtonMultiplier(it) },
                            valueRange = 0.7f..1.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = MinecraftTextYellow,
                                activeTrackColor = MinecraftGreen
                            )
                        )
                    }

                    // 6. Sound effects toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mine Sound Effects", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Box(
                            modifier = Modifier
                                .background(if (soundEffectsEnabled) Color(0xFF2D6A4F) else Color(0xFF800F2F))
                                .border(1.dp, Color.White)
                                .clickable { viewModel.updateSoundEnabled(!soundEffectsEnabled) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (soundEffectsEnabled) "UNMUTED [ON]" else "MUTED [OFF]",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    MinecraftButton(
                        text = "Save & Apply Settings",
                        onClick = { showSettingsDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// Custom linear interpolation between two Jetpack Compose Colors!
fun lerpColor(c1: Color, c2: Color, ratio: Float): Color {
    val r = c1.red + (c2.red - c1.red) * ratio
    val g = c1.green + (c2.green - c1.green) * ratio
    val b = c1.blue + (c2.blue - c1.blue) * ratio
    return Color(r, g, b, 1f)
}

// Custom safety helper for ViewModel actions
fun GameViewModel.规律ActionToggle() {
    togglePlacementMode()
}
