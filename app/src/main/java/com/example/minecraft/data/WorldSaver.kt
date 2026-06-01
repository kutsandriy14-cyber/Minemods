package com.example.minecraft.data

import android.content.Context
import android.util.Log
import com.example.minecraft.model.ModDefinition
import com.example.minecraft.model.WorldSave
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

object WorldSaver {
    private const val TAG = "WorldSaver"
    private const val WORLDS_DIR = "minecraft_worlds"
    private const val MODS_DIR = "minecraft_mods"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val worldAdapter = moshi.adapter(WorldSave::class.java)
    private val modAdapter = moshi.adapter(ModDefinition::class.java)
    private val modListAdapter = moshi.adapter<List<ModDefinition>>(
        Types.newParameterizedType(List::class.java, ModDefinition::class.java)
    )

    /**
     * Get list of all saved world names/files.
     */
    fun listWorlds(context: Context): List<WorldSave> {
        val dir = File(context.filesDir, WORLDS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
            return emptyList()
        }

        val files = dir.listFiles { _, name -> name.endsWith(".json") } ?: return emptyList()
        val worlds = mutableListOf<WorldSave>()
        
        for (f in files) {
            try {
                val json = f.readText()
                val world = worldAdapter.fromJson(json)
                if (world != null) {
                    worlds.add(world.sanitize())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing world file ${f.name}", e)
            }
        }
        
        return worlds.sortedByDescending { it.creationTime }
    }

    /**
     * Serializes and writes a world state to local files.
     */
    fun saveWorld(context: Context, world: WorldSave) {
        try {
            val dir = File(context.filesDir, WORLDS_DIR)
            if (!dir.exists()) dir.mkdirs()

            // Safe alphanumeric name for file
            val safeName = world.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(dir, "${safeName}_${world.creationTime}.json")
            
            val json = worldAdapter.toJson(world)
            file.writeText(json)

            // Delete legacy file if we migrated it
            val legacyFile = File(dir, "$safeName.json")
            if (legacyFile.exists()) {
                legacyFile.delete()
            }
            Log.d(TAG, "Successfully saved world ${world.name} to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving world ${world.name}", e)
        }
    }

    /**
     * Deletes a world save file.
     */
    fun deleteWorld(context: Context, world: WorldSave) {
        try {
            val dir = File(context.filesDir, WORLDS_DIR)
            val safeName = world.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            var file = File(dir, "${safeName}_${world.creationTime}.json")
            if (!file.exists()) {
                file = File(dir, "$safeName.json")
            }
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting world ${world.name}", e)
        }
    }

    /**
     * List all custom mods created by the user.
     */
    fun listMods(context: Context): List<ModDefinition> {
        val dir = File(context.filesDir, MODS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val files = dir.listFiles { _, name -> name.endsWith(".json") || name.endsWith(".mjm") } ?: emptyArray()
        val mods = mutableListOf<ModDefinition>()

        for (f in files) {
            try {
                if (f.name.endsWith(".mjm")) {
                    val script = f.readText()
                    val mod = ModDefinition.parseMjmScript(script)
                    mods.add(mod)
                } else {
                    val json = f.readText()
                    val mod = modAdapter.fromJson(json)
                    if (mod != null) {
                        mods.add(mod)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing mod file ${f.name}", e)
            }
        }

        // Always ensure both default and IC2 mod configurations are created/loaded
        if (mods.none { it.id == "mod_emerald_plus" }) {
            val emerald = createExampleMod()
            saveMod(context, emerald)
            mods.add(emerald)
        }
        if (mods.none { it.id == "mod_ic2" }) {
            val ic2 = createIC2Mod()
            saveMod(context, ic2)
            mods.add(ic2)
        }

        return mods
    }

    /**
     * Saves a mod definition.
     */
    fun saveMod(context: Context, mod: ModDefinition) {
        try {
            val dir = File(context.filesDir, MODS_DIR)
            if (!dir.exists()) dir.mkdirs()

            val safeName = mod.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(dir, "$safeName.json")

            val json = modAdapter.toJson(mod)
            file.writeText(json)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving mod ${mod.name}", e)
        }
    }

    fun saveMjmScript(context: Context, modId: String, scriptText: String) {
        try {
            val dir = File(context.filesDir, MODS_DIR)
            if (!dir.exists()) dir.mkdirs()

            val safeName = modId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(dir, "$safeName.mjm")
            file.writeText(scriptText)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving MJM script mod $modId", e)
        }
    }

    /**
     * Deletes a mod.
     */
    fun deleteMod(context: Context, modId: String) {
        try {
            val dir = File(context.filesDir, MODS_DIR)
            val safeName = modId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val fileJson = File(dir, "$safeName.json")
            if (fileJson.exists()) fileJson.delete()
            val fileMjm = File(dir, "$safeName.mjm")
            if (fileMjm.exists()) fileMjm.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting mod $modId", e)
        }
    }

    private fun createExampleMod(): ModDefinition {
        return ModDefinition(
            id = "mod_emerald_plus",
            name = "Emerald Plus Mod",
            description = "Brings raw Emerald ores to life, introducing Emerald tools and crafting recipes!",
            blocks = listOf(
                com.example.minecraft.model.CustomBlockDef(
                    id = "mod_emerald_ore",
                    name = "Emerald Ore",
                    colorHex = "#2C2C2C",
                    accentColorHex = "#2EBF6A", // Radiant green accent
                    hardness = 2.4f,
                    spawnFrequency = "Medium",
                    spawnDepthMin = 4,
                    spawnDepthMax = 20,
                    dropItemId = "mod_emerald_gem",
                    smeltingResultItemId = "mod_emerald_ingot",
                    craftingIngredients = mapOf("cobblestone" to 8, "coal" to 1) // Also craftable with cobblestone surrounding coal!
                )
            ),
            tools = listOf(
                com.example.minecraft.model.CustomToolDef(
                    id = "mod_emerald_sword",
                    name = "Emerald Broadsword",
                    type = "Sword",
                    tier = "Diamond",
                    colorHex = "#2EBF6A",
                    efficiency = 12.0f,
                    craftingIngredients = mapOf("mod_emerald_gem" to 2, "stick" to 1)
                ),
                com.example.minecraft.model.CustomToolDef(
                    id = "mod_emerald_pickaxe",
                    name = "Emerald Pickaxe",
                    type = "Pickaxe",
                    tier = "Diamond",
                    colorHex = "#2EBF6A",
                    efficiency = 15.0f, // extremely fast!
                    craftingIngredients = mapOf("mod_emerald_gem" to 3, "stick" to 2)
                )
            ),
            isEnabled = true
        )
    }

    private fun createIC2Mod(): ModDefinition {
        return ModDefinition(
            id = "mod_ic2",
            name = "IndustrialCraft 2",
            description = "Advanced machinery, generators, automated macerators, nuclear reactors, energy storage blocks, and high-tech alloy tools!",
            blocks = listOf(
                com.example.minecraft.model.CustomBlockDef(
                    id = "mod_ic2_uranium_ore",
                    name = "Uranium Ore",
                    colorHex = "#23331F",
                    accentColorHex = "#66FF00",
                    hardness = 4.0f,
                    spawnFrequency = "Rare",
                    spawnDepthMin = 1,
                    spawnDepthMax = 12,
                    dropItemId = "mod_ic2_uranium_rod",
                    craftingIngredients = emptyMap()
                ),
                com.example.minecraft.model.CustomBlockDef(
                    id = "mod_ic2_tin_ore",
                    name = "Tin Ore",
                    colorHex = "#5E6C75",
                    accentColorHex = "#CFD8DC",
                    hardness = 1.6f,
                    spawnFrequency = "Medium",
                    spawnDepthMin = 5,
                    spawnDepthMax = 25,
                    dropItemId = "mod_ic2_tin_ingot",
                    craftingIngredients = emptyMap()
                ),
                com.example.minecraft.model.CustomBlockDef(
                    id = "mod_ic2_generator",
                    name = "IC2 Coal Generator",
                    colorHex = "#37474F",
                    accentColorHex = "#FF3D00",
                    hardness = 2.0f,
                    dropItemId = "mod_ic2_generator",
                    craftingIngredients = mapOf("iron_block" to 1, "furnace" to 1, "copper_block" to 3)
                ),
                com.example.minecraft.model.CustomBlockDef(
                    id = "mod_ic2_macerator",
                    name = "IC2 Crusher Macerator",
                    colorHex = "#2E3D2F",
                    accentColorHex = "#00FFFF",
                    hardness = 2.0f,
                    dropItemId = "mod_ic2_macerator",
                    craftingIngredients = mapOf("iron_block" to 1, "cobblestone" to 6, "redstone_block" to 1)
                ),
                com.example.minecraft.model.CustomBlockDef(
                    id = "mod_ic2_electric_furnace",
                    name = "IC2 Electric Furnace",
                    colorHex = "#31353D",
                    accentColorHex = "#FFD54F",
                    hardness = 2.0f,
                    dropItemId = "mod_ic2_electric_furnace",
                    craftingIngredients = mapOf("furnace" to 1, "redstone_block" to 2, "iron_block" to 1)
                ),
                com.example.minecraft.model.CustomBlockDef(
                    id = "mod_ic2_batbox",
                    name = "IC2 BatBox Energy Saver",
                    colorHex = "#4E3629",
                    accentColorHex = "#FFFF00",
                    hardness = 1.5f,
                    dropItemId = "mod_ic2_batbox",
                    craftingIngredients = mapOf("oak_planks" to 5, "redstone_block" to 1, "copper_block" to 1)
                ),
                com.example.minecraft.model.CustomBlockDef(
                    id = "mod_ic2_copper_cable",
                    name = "IC2 Copper Wire Cable",
                    colorHex = "#D84315",
                    accentColorHex = "#212121",
                    hardness = 0.5f,
                    dropItemId = "mod_ic2_copper_cable",
                    craftingIngredients = mapOf("wood" to 3, "copper_block" to 1)
                )
            ),
            tools = listOf(
                com.example.minecraft.model.CustomToolDef(
                    id = "mod_ic2_bronze_sword",
                    name = "IC2 Bronze Broadsword",
                    type = "Sword",
                    tier = "Iron",
                    colorHex = "#B58A30",
                    efficiency = 8.0f,
                    craftingIngredients = mapOf("copper_block" to 1, "stick" to 1)
                ),
                com.example.minecraft.model.CustomToolDef(
                    id = "mod_ic2_bronze_pickaxe",
                    name = "IC2 Bronze Heavy Pickaxe",
                    type = "Pickaxe",
                    tier = "Iron",
                    colorHex = "#B58A30",
                    efficiency = 10.0f,
                    craftingIngredients = mapOf("copper_block" to 2, "stick" to 2)
                ),
                com.example.minecraft.model.CustomToolDef(
                    id = "mod_ic2_nano_saber",
                    name = "⚡ Elite Nano Saber ⚡",
                    type = "Sword",
                    tier = "Diamond",
                    colorHex = "#00E5FF",
                    efficiency = 28.0f,
                    craftingIngredients = mapOf("diamond_block" to 1, "redstone_block" to 2, "obsidian" to 1)
                )
            ),
            isEnabled = true
        )
    }
}

fun WorldSave.sanitize(): WorldSave {
    val sName = (this.name as? String) ?: "World"
    val sGameMode = (this.gameMode as? String) ?: "Survival"
    val sWorldType = (this.worldType as? String) ?: "Standard"
    
    // Sanitize player state safely
    val rawPlayer = this.playerState as com.example.minecraft.model.PlayerState?
    val sPlayerState = if (rawPlayer != null) {
        val rawInventory = rawPlayer.inventory as List<*>?
        val sInventory = mutableListOf<com.example.minecraft.model.ItemStack?>()
        if (rawInventory != null) {
            for (item in rawInventory) {
                if (item is com.example.minecraft.model.ItemStack) {
                    sInventory.add(item)
                } else {
                    sInventory.add(null)
                }
            }
        }
        while (sInventory.size < 36) {
            sInventory.add(null)
        }
        rawPlayer.copy(inventory = sInventory)
    } else {
        com.example.minecraft.model.PlayerState()
    }

    val sWorldBlocks = (this.worldBlocks as? Map<String, String>) ?: emptyMap()
    val sFurnaceStates = (this.furnaceStates as? Map<String, com.example.minecraft.model.FurnaceState>) ?: emptyMap()
    val sChestStates = (this.chestStates as? Map<String, com.example.minecraft.model.ChestState>) ?: emptyMap()
    val sEnabledModIds = (this.enabledModIds as? List<String>) ?: emptyList()
    
    return this.copy(
        name = sName,
        gameMode = sGameMode,
        worldType = sWorldType,
        playerState = sPlayerState,
        worldBlocks = sWorldBlocks,
        furnaceStates = sFurnaceStates,
        chestStates = sChestStates,
        enabledModIds = sEnabledModIds
    )
}

