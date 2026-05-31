package com.example.minecraft.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minecraft.model.*
import com.example.ui.theme.MinecraftGreen
import com.example.ui.theme.MinecraftTextYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModManagerScreen(
    viewModel: GameViewModel,
    mods: List<ModDefinition>,
    activeMod: ModDefinition?, // Mod being edited or created
    onBackToLauncher: () -> Unit
) {
    val context = LocalContext.current
    
    // Dialog triggers
    var showBlockCreator by remember { mutableStateOf(false) }
    var showToolCreator by remember { mutableStateOf(false) }

    // --- Active Form inputs ---
    var modName by remember(activeMod) { mutableStateOf(activeMod?.name ?: "") }
    var modDesc by remember(activeMod) { mutableStateOf(activeMod?.description ?: "") }

    // Custom Block inputs
    var cbName by remember { mutableStateOf("Ruby Ore") }
    var cbColor by remember { mutableStateOf("#E91E63") } // Ruby pink
    var cbHardness by remember { mutableStateOf(1.5f) }
    var cbDropItemId by remember { mutableStateOf("mod_ruby_crystal") }

    // Custom Tool inputs
    var ctName by remember { mutableStateOf("Ruby Pickaxe") }
    var ctType by remember { mutableStateOf("Pickaxe") } // Pickaxe, Sword, Axe, Shovel
    var ctTier by remember { mutableStateOf("Diamond") } // Wood, Stone, Iron, Diamond
    var ctColor by remember { mutableStateOf("#E91E63") }
    var ctEfficiency by remember { mutableStateOf(12.0f) }

    // Base layout toggle
    // If activeMod is null, we list all available mods; if not null, we render the Mod Builder edit form!
    if (activeMod == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            TiledDirtBackground()
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header List screen
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MinecraftButton(text = "Back", onClick = onBackToLauncher, modifier = Modifier.width(100.dp))
                    Text(
                        text = "MODDING PLATFORM",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    MinecraftButton(text = "New Mod", onClick = { viewModel.startNewMod() }, modifier = Modifier.width(140.dp))
                }

                // Listing Mods
                if (mods.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0x99000000)).padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No custom modular expansions found.\nTap 'New Mod' to craft customized ores, blocks, and hyperpickaxes!",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0x66000000)).border(1.dp, Color(0xFF444444)).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(mods) { mod ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF222222))
                                    .border(1.dp, Color(0xFF555555))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mod.name.uppercase(),
                                        color = if (mod.isEnabled) MinecraftTextYellow else Color.Gray,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = mod.description,
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Prefix: ${mod.id}_  |  Blocks: ${mod.blocks.size}  |  Tools: ${mod.tools.size}",
                                        color = Color.DarkGray,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Enable/Disable Active Switch
                                    Checkbox(
                                        checked = mod.isEnabled,
                                        onCheckedChange = { viewModel.toggleModEnabled(context, mod.id) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MinecraftGreen,
                                            uncheckedColor = Color.Gray
                                        )
                                    )

                                    // Edit Button
                                    IconButton(
                                        onClick = { viewModel.selectEditMod(mod) },
                                        modifier = Modifier.size(36.dp).background(Color(0xFF333333)).border(1.dp, Color.Gray)
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Mod", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }

                                    // Trash Button
                                    IconButton(
                                        onClick = { viewModel.deleteMod(context, mod.id) },
                                        modifier = Modifier.size(36.dp).background(Color(0xFF5A1E1E)).border(1.dp, Color(0xFFC04040))
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // --- RENDERING MOD CREATOR FORM SCREEN LAYOUT ---
        var isScriptMode by remember { mutableStateOf(false) }
        var scriptText by remember(activeMod) {
            val initial = """
            # Mods Mobile Java Mod (.mjm) Script
            MOD_ID: ${activeMod.id}
            MOD_NAME: ${if (activeMod.name.startsWith("My Awesome Mod")) "My Special Trial Mod" else activeMod.name}
            MOD_DESC: ${activeMod.description}

            BLOCK_START
              ID: trial_scaffold
              NAME: Obsidian Trial Scaffolding
              COLOR_HEX: #1A122B
              ACCENT_HEX: #E91E63
              HARDNESS: 1.5
              DROP_ITEM: trial_scaffold
            BLOCK_END

            TOOL_START
              ID: fiery_saber
              NAME: Nether Fiery Saber
              TYPE: Sword
              TIER: Diamond
              COLOR_HEX: #D32F2F
              EFFICIENCY: 16.0
              INGREDIENTS: blaze_rod:1, diamond:2
            TOOL_END
            """.trimIndent()
            mutableStateOf(initial)
        }
        var compileLogs by remember { mutableStateOf("Mods Mobile Java Compiler (MJC) v1.21.1 - Status: Idle.") }

        Box(modifier = Modifier.fillMaxSize()) {
            TiledDirtBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Builder Screen
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MinecraftButton(
                        text = "Cancel",
                        onClick = { viewModel.navigateTo(Screen.Launcher) },
                        modifier = Modifier.width(100.dp)
                    )

                    Text(
                        text = if (isScriptMode) "MJM SCRIPT COMPILER" else "MOD BUILDER REGISTRY",
                        color = MinecraftTextYellow,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MinecraftButton(
                            text = if (isScriptMode) "Visual ID" else ".mjm Script",
                            onClick = { isScriptMode = !isScriptMode },
                            modifier = Modifier.width(110.dp)
                        )
                        
                        if (isScriptMode) {
                            MinecraftButton(
                                text = "Compile",
                                onClick = {
                                    try {
                                        val parsed = ModDefinition.parseMjmScript(scriptText)
                                        compileLogs = """
                                        [MJC-Compiler] Scanning tokens... SUCCESS!
                                        [MJC-Compiler] Compiled Mod: ${parsed.name} (${parsed.id})
                                        [MJC-Compiler] Custom blocks loaded: ${parsed.blocks.size}
                                        [MJC-Compiler] Custom items loaded: ${parsed.tools.size}
                                        [MJC-Compiler] Build Status: 100% SUCCESS and 0 compilation errors! Ready to install.
                                        """.trimIndent()
                                        
                                        viewModel.updateEditingMod(parsed)
                                    } catch (e: Exception) {
                                        compileLogs = "[MJC-Compiler] FAILED TO COMPILE: ${e.message}"
                                    }
                                },
                                modifier = Modifier.width(110.dp)
                            )
                        } else {
                            MinecraftButton(
                                text = "Save Mod",
                                onClick = {
                                    val savedMod = activeMod.copy(name = modName, description = modDesc)
                                    viewModel.updateEditingMod(savedMod)
                                    viewModel.saveEditingMod(context)
                                },
                                modifier = Modifier.width(110.dp)
                            )
                        }
                    }
                }

                if (isScriptMode) {
                    // SCRIPT COMPILER PANEL
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .background(Color(0xFF1E1E1E))
                                .border(1.dp, Color(0xFF3C3C3C))
                                .padding(12.dp)
                        ) {
                            Text("EDITING MOD SCRIPT FILE (EXTENSION: .MJM)", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            TextField(
                                value = scriptText,
                                onValueChange = { scriptText = it },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFFD4D4D4),
                                    unfocusedTextColor = Color(0xFFD4D4D4),
                                    focusedContainerColor = Color(0xFF151515),
                                    unfocusedContainerColor = Color(0xFF151515)
                                ),
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                shape = RectangleShape,
                                modifier = Modifier.fillMaxSize().border(1.dp, Color(0xFF444444))
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(0.8f)
                                .background(Color(0xFF2D2D2D))
                                .border(1.dp, Color(0xFF434343))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("COMPILER BUILD OUTPUTS & TOKENS", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(Color.Black)
                                        .border(1.dp, Color(0xFF555555))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = compileLogs,
                                        color = if (compileLogs.contains("FAILED")) Color.Red else Color.Green,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("SYS-MODLOADER: mods moble java", color = MinecraftTextYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Coordinates custom compiled classes and dynamic block injection straight into the voxel pipeline, complying with modern 1.21.1 protocols.", color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                MinecraftButton(
                                    text = "INSTALL MOD FILE",
                                    onClick = {
                                        viewModel.compileAndSaveMjmMod(context, scriptText)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                )
                                Text("Note: Installing will save your code to direct file system as .mjm file and mount it to current launcher registry.", color = Color.DarkGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left Side: Base Definition details
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xAA000000))
                                .border(1.dp, Color(0xFF444444))
                                .padding(12.dp)
                        ) {
                        Text("MOD METADATA", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Mod Name
                        Text("Name:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        TextField(
                            value = modName,
                            onValueChange = { modName = it },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black
                            ),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            shape = RectangleShape,
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Mod Description
                        Text("Description:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        TextField(
                            value = modDesc,
                            onValueChange = { modDesc = it },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black
                            ),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            shape = RectangleShape,
                            modifier = Modifier.fillMaxWidth().height(80.dp).border(1.dp, Color.Gray)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("RESOURCES", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MinecraftButton(
                                text = "Add Block",
                                onClick = { showBlockCreator = true },
                                modifier = Modifier.weight(1f)
                            )
                            MinecraftButton(
                                text = "Add Tool",
                                onClick = { showToolCreator = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Right Side: List of custom items created inside this mod
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .background(Color(0x99000000))
                            .border(1.dp, Color(0xFF444444))
                            .padding(12.dp)
                    ) {
                        Text("REGISTERED SHARDS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Render Blocks list
                            if (activeMod.blocks.isEmpty() && activeMod.tools.isEmpty()) {
                                Text(
                                    "No custom blocks or pickaxes added yet.\nTap 'Add Block' or 'Add Tool' on the left side to define modular resources!",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            } else {
                                // Dynamic Custom Blocks
                                for (cb in activeMod.blocks) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(Color(0xFF2C2C2C)).border(1.dp, Color(0xFF555555)).padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Tiny Color Canvas representing texture
                                            Box(modifier = Modifier.size(24.dp).background(Color(android.graphics.Color.parseColor(cb.colorHex))).border(1.dp, Color.White))
                                            Column {
                                                Text(cb.name, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                                Text("Voxel Hardness: ${cb.hardness} | Drops: ${cb.dropItemId}", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                val remainingBlocks = activeMod.blocks.filter { it.id != cb.id }
                                                viewModel.updateEditingMod(activeMod.copy(blocks = remainingBlocks))
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove block", tint = Color.Red, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }

                                // Dynamic Custom Tools
                                for (ct in activeMod.tools) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(Color(0xFF2C2C2C)).border(1.dp, Color(0xFF555555)).padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(24.dp).background(Color(android.graphics.Color.parseColor(ct.colorHex))))
                                            Column {
                                                Text(ct.name, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                                Text("${ct.type} tier: ${ct.tier} | speed: ${ct.efficiency}x", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                val remainingTools = activeMod.tools.filter { it.id != ct.id }
                                                viewModel.updateEditingMod(activeMod.copy(tools = remainingTools))
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove tool", tint = Color.Red, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- DIALOG MODAL: Custom Block Visual Designer ---
            if (showBlockCreator) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xE6000000)).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .width(380.dp)
                            .background(Color(0xFF2E2E2E))
                            .border(2.dp, Color(0xFF8A8A8A))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("DESIGN CUSTOM BLOCK", color = MinecraftTextYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Block Name
                        Text("Block Name:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.align(Alignment.Start))
                        TextField(
                            value = cbName,
                            onValueChange = { cbName = it },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            shape = RectangleShape,
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Texture Colors Row (Emerald, Ruby, Sapphire, Amber)
                        Text("Texture Color (Hex):", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.align(Alignment.Start))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("#4CAF50", "#E91E63", "#00BCD4", "#FFC107", "#9C27B0").forEach { colHex ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(android.graphics.Color.parseColor(colHex)))
                                        .border(
                                            width = if (cbColor == colHex) 2.dp else 0.dp,
                                            color = Color.White
                                        )
                                        .clickable { cbColor = colHex }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Voxel Hardness slider coefficients
                        Text("Voxel Hardness: $cbHardness (${if (cbHardness <= 0.5f) "Soft (Dirt)" else if (cbHardness <= 1.5f) "Average (Stone)" else "Hard (Iron/Obsidian)"})", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.align(Alignment.Start))
                        Slider(
                            value = cbHardness,
                            onValueChange = { cbHardness = it },
                            valueRange = 0.2f..4.0f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = MinecraftGreen,
                                thumbColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MinecraftButton(
                                text = "Cancel",
                                onClick = { showBlockCreator = false },
                                modifier = Modifier.weight(1f)
                            )
                            MinecraftButton(
                                text = "Register",
                                onClick = {
                                    val bId = "mod_${cbName.lowercase().replace(" ", "_")}"
                                    val dropId = "mod_${cbName.lowercase().replace(" ", "_")}_gem"
                                    val newBlock = CustomBlockDef(
                                        id = bId,
                                        name = cbName,
                                        colorHex = cbColor,
                                        accentColorHex = "#FFFFFF",
                                        hardness = cbHardness,
                                        dropItemId = dropId,
                                        smeltingResultItemId = dropId
                                    )
                                    val updatedBlocks = activeMod.blocks + newBlock
                                    viewModel.updateEditingMod(activeMod.copy(blocks = updatedBlocks))
                                    showBlockCreator = false
                                    // Reset inputs
                                    cbName = "Mod Block"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // --- DIALOG MODAL: Custom Tool Visual Designer ---
            if (showToolCreator) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xE6000000)).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .width(380.dp)
                            .background(Color(0xFF2E2E2E))
                            .border(2.dp, Color(0xFF8A8A8A))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("DESIGN CUSTOM TOOL", color = MinecraftTextYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Tool Name
                        Text("Tool Name:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.align(Alignment.Start))
                        TextField(
                            value = ctName,
                            onValueChange = { ctName = it },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            shape = RectangleShape,
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tool Type Select
                        Text("Tool Type:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.align(Alignment.Start))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Pickaxe", "Sword", "Axe", "Shovel").forEach { t ->
                                MinecraftButton(
                                    text = t,
                                    onClick = { ctType = t },
                                    modifier = Modifier.weight(1f),
                                    enabled = ctType != t
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tool Color select
                        Text("Tool Material Color:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.align(Alignment.Start))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("#4CAF50", "#E91E63", "#00BCD4", "#FFC107", "#9C27B0").forEach { colHex ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(android.graphics.Color.parseColor(colHex)))
                                        .border(
                                            width = if (ctColor == colHex) 2.dp else 0.dp,
                                            color = Color.White
                                        )
                                        .clickable { ctColor = colHex }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tool Mining Efficiency slide
                        Text("Mining Speed: $ctEfficiency x", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.align(Alignment.Start))
                        Slider(
                            value = ctEfficiency,
                            onValueChange = { ctEfficiency = it },
                            valueRange = 2f..30f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = MinecraftGreen,
                                thumbColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MinecraftButton(
                                text = "Cancel",
                                onClick = { showToolCreator = false },
                                modifier = Modifier.weight(1f)
                            )
                            MinecraftButton(
                                text = "Register",
                                onClick = {
                                    val toolId = "mod_${ctName.lowercase().replace(" ", "_")}"
                                    val materialInputId = activeMod.blocks.firstOrNull()?.dropItemId ?: "diamond"
                                    
                                    // Generate craft formula using first registered mod ore if any, otherwise diamond!
                                    val recipe = if (ctType == "Sword") {
                                        mapOf(materialInputId to 2, "stick" to 1)
                                    } else {
                                        mapOf(materialInputId to 3, "stick" to 2)
                                    }

                                    val newTool = CustomToolDef(
                                        id = toolId,
                                        name = ctName,
                                        type = ctType,
                                        tier = ctTier,
                                        colorHex = ctColor,
                                        efficiency = ctEfficiency,
                                        craftingIngredients = recipe
                                    )
                                    val updatedTools = activeMod.tools + newTool
                                    viewModel.updateEditingMod(activeMod.copy(tools = updatedTools))
                                    showToolCreator = false
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
}
