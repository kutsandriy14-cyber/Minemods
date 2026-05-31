package com.example.minecraft.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minecraft.model.ModDefinition
import com.example.ui.theme.MinecraftGreen
import com.example.ui.theme.MinecraftTextYellow
import kotlinx.coroutines.delay

@Composable
fun LauncherScreen(
    viewModel: GameViewModel,
    onNavigateToWorldSelect: () -> Unit,
    onNavigateToModManager: () -> Unit,
    mods: List<ModDefinition>
) {
    val context = LocalContext.current
    var showCreditsDialog by remember { mutableStateOf(false) }

    // Splash bouncing animation using floating point math!
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val scaleMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Randomized Splash titles checklist
    val splashTexts = remember {
        listOf(
            "Also try Terraria!",
            "Kotlin Powered!",
            "Dynamic Mod Loader!",
            "Voxel 2.5D Science!",
            "Steve approved!",
            "Mines like Java!",
            "Smelt raw iron!",
            "100% blocky vibes!"
        )
    }
    var currentSplash by remember { mutableStateOf(splashTexts[0]) }
    LaunchedEffect(Unit) {
        while (true) {
            currentSplash = splashTexts.random()
            delay(4000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TiledDirtBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Giant Minecraft 3D Block Styled Logo Title!
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "MINEMODS",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 48.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            shadow = Shadow(
                                color = Color.Black,
                                blurRadius = 0.1f
                            )
                        )
                    )
                    Text(
                        text = "mods moble java",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = MinecraftGreen,
                            textAlign = TextAlign.Center,
                            shadow = Shadow(
                                color = Color.Black,
                                blurRadius = 0.1f
                            )
                        )
                    )
                }

                // Rotated bouncing Yellow Slash credits text next to Title!
                Box(
                    modifier = Modifier
                        .offset(x = 180.dp, y = 24.dp)
                        .rotate(-15f)
                ) {
                    Text(
                        text = currentSplash,
                        color = MinecraftTextYellow,
                        fontSize = (15.sp.value * scaleMultiplier).sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xFF333300),
                                offset = androidx.compose.ui.geometry.Offset(2f, 2f)
                            )
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Selection Buttons Grid
            Column(
                modifier = Modifier
                    .width(360.dp)
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MinecraftButton(
                    text = "Singleplayer",
                    onClick = onNavigateToWorldSelect,
                    testTag = "singleplayer_btn",
                    modifier = Modifier.fillMaxWidth()
                )

                MinecraftButton(
                    text = "Mod Builder & Registry",
                    onClick = {
                        viewModel.startNewMod()
                    },
                    testTag = "mod_builder_btn",
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MinecraftButton(
                        text = "Credits / Guide",
                        onClick = { showCreditsDialog = true },
                        testTag = "credits_btn",
                        modifier = Modifier.weight(1f)
                    )
                    MinecraftButton(
                        text = "Exit",
                        onClick = { System.exit(0) },
                        testTag = "exit_btn",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Active Loadouts Panel (showing total compiled mods)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .background(Color(0x99000000))
                .border(1.dp, Color(0xFF444444))
                .padding(8.dp)
        ) {
            val count = mods.count { it.isEnabled }
            Text(
                text = "Minecraft Java Engine v1.12_Kotlin\nActive Mods Loaded: $count / ${mods.size}\nTap Singleplayer to launch a world!",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )
        }

        // Custom guide dialog overlay
        if (showCreditsDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6000000))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(480.dp)
                        .background(Color(0xFF2E2E2E))
                        .border(2.dp, Color(0xFF8A8A8A))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GAME GUIDE",
                        color = MinecraftTextYellow,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "This application implements a fully functional 2D Minecraft Engine in beautiful Compose Canvas!\n\n" +
                                "- CONTROLS: Use Left/Right D-pad to move, Joy-up to Jump. Tap block to Mine it. Toggle Mode to Place blocks!\n" +
                                "- CONTAINERS: Double tap a Furnace to smelt Iron Ore using Coal fuel. Double tap a Chest to store item stacks!\n" +
                                "- MODDING: Create blocks (like custom emerald ore) or diamond-tier laser pickaxes. Toggle them in the launcher, and they will procedurally generate in newly generated Singleplayer worlds!",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MinecraftButton(
                        text = "Back to Menu",
                        onClick = { showCreditsDialog = false },
                        modifier = Modifier.width(160.dp)
                    )
                }
            }
        }
    }
}
