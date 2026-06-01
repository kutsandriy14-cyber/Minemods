package com.example.minecraft.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minecraft.model.WorldSave
import com.example.ui.theme.MinecraftGreen
import com.example.ui.theme.MinecraftTextYellow
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldSelectScreen(
    viewModel: GameViewModel,
    worlds: List<WorldSave>,
    onBackToLauncher: () -> Unit
) {
    val context = LocalContext.current
    var showCreateForm by remember { mutableStateOf(false) }

    // Create World configurations
    var worldName by remember { mutableStateOf("New World") }
    var gameMode by remember { mutableStateOf("Survival") } // Survival vs Creative
    var terrainType by remember { mutableStateOf("Standard") } // Standard, Flat, Mountains

    Box(modifier = Modifier.fillMaxSize()) {
        TiledDirtBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MinecraftButton(
                    text = "Back",
                    onClick = onBackToLauncher,
                    modifier = Modifier.width(100.dp)
                )

                Text(
                    text = "SELECT WORLD",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )

                MinecraftButton(
                    text = "Create World",
                    onClick = { showCreateForm = true },
                    modifier = Modifier.width(140.dp)
                )
            }

            // Worlds list
            if (worlds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0x99000000))
                        .border(1.dp, Color(0xFF444444))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No worlds discovered.\nTap 'Create World' above to generate your first procedurally voxelized map!",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0x88000000))
                        .border(1.dp, Color(0xFF444444))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(worlds) { save ->
                        WorldCard(
                            save = save,
                            onPlay = { viewModel.playWorld(context, save) },
                            onDelete = { viewModel.deleteWorld(context, save) }
                        )
                    }
                }
            }
        }

        // VISUAL MODAL OVERLAY: Create New World Builder Settings Formula
        if (showCreateForm) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6000000))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(460.dp)
                        .background(Color(0xFF2C2C2C))
                        .border(2.dp, Color(0xFF8A8A8A))
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CREATE NEW WORLD",
                        color = MinecraftTextYellow,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Name Input
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("World Name:", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        TextField(
                            value = worldName,
                            onValueChange = { worldName = it },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            shape = RectangleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.Gray)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Game Mode SELECT
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Game Mode:", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MinecraftButton(
                                text = "Survival",
                                onClick = { gameMode = "Survival" },
                                modifier = Modifier.weight(1f),
                                enabled = gameMode != "Survival"
                            )
                            MinecraftButton(
                                text = "Creative",
                                onClick = { gameMode = "Creative" },
                                modifier = Modifier.weight(1f),
                                enabled = gameMode != "Creative"
                            )
                        }
                        Text(
                            text = if (gameMode == "Survival") "Survival: Search, harvest materials, craft tools, and manage hunger/health!" else "Creative: Unlimited blocks grid, instant breaking, flight, and no danger!",
                            color = Color.DarkGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Terrain Type selector Standard, Flat, Mountains
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("World Terrain Type:", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MinecraftButton(
                                text = "Standard",
                                onClick = { terrainType = "Standard" },
                                modifier = Modifier.weight(1f),
                                enabled = terrainType != "Standard"
                            )
                            MinecraftButton(
                                text = "Flat",
                                onClick = { terrainType = "Flat" },
                                modifier = Modifier.weight(1f),
                                enabled = terrainType != "Flat"
                            )
                            MinecraftButton(
                                text = "Mountains",
                                onClick = { terrainType = "Mountains" },
                                modifier = Modifier.weight(1f),
                                enabled = terrainType != "Mountains"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Save actions container
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MinecraftButton(
                            text = "Cancel",
                            onClick = { showCreateForm = false },
                            modifier = Modifier.weight(1f)
                        )
                        MinecraftButton(
                            text = "Create!",
                            onClick = {
                                viewModel.createNewWorld(context, worldName, gameMode, terrainType)
                                showCreateForm = false
                            },
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorldCard(
    save: WorldSave,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val formattedDate = remember(save.creationTime) { formatter.format(Date(save.creationTime)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2C2C2C))
            .border(1.dp, Color(0xFF5A5A5A))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = save.name.uppercase(),
                color = MinecraftTextYellow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Mode: ${save.gameMode}",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Created: $formattedDate",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .background(Color(0xFF5F2323))
                    .border(1.dp, Color(0xFFC04040))
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Save",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .background(Color(0xFF335F23))
                    .border(1.dp, Color(0xFF5FAF40))
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Load World Save",
                    tint = Color.White
                )
            }
        }
    }
}
