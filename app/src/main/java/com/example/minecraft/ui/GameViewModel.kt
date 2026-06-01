package com.example.minecraft.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minecraft.data.WorldSaver
import com.example.minecraft.engine.PhysicsEngine
import com.example.minecraft.engine.TerrainGenerator
import com.example.minecraft.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.random.Random

sealed class Screen {
    object Launcher : Screen()
    object WorldSelect : Screen()
    object Game : Screen()
    object ModManager : Screen()
}

class GameViewModel : ViewModel() {

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Launcher)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Listed saves & mods
    private val _worlds = MutableStateFlow<List<WorldSave>>(emptyList())
    val worlds: StateFlow<List<WorldSave>> = _worlds.asStateFlow()

    private val _mods = MutableStateFlow<List<ModDefinition>>(emptyList())
    val mods: StateFlow<List<ModDefinition>> = _mods.asStateFlow()

    // Active world & UI States
    private val _activeWorld = MutableStateFlow<WorldSave?>(null)
    val activeWorld: StateFlow<WorldSave?> = _activeWorld.asStateFlow()

    // Temporary mod creator active definition
    private val _editingMod = MutableStateFlow<ModDefinition?>(null)
    val editingMod: StateFlow<ModDefinition?> = _editingMod.asStateFlow()

    // In-game temporary interactive states
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _inventoryOpen = MutableStateFlow(false)
    val inventoryOpen: StateFlow<Boolean> = _inventoryOpen.asStateFlow()

    // Is client in Mine or Place mode
    // false = Mine/Break Mode, true = Place Mode
    private val _isPlaceMode = MutableStateFlow(false)
    val isPlaceMode: StateFlow<Boolean> = _isPlaceMode.asStateFlow()

    // Selected block to inspect/interact container
    // E.g. "x,y" coordinate string or null
    private val _openFurnaceCoord = MutableStateFlow<String?>(null)
    val openFurnaceCoord: StateFlow<String?> = _openFurnaceCoord.asStateFlow()

    private val _openChestCoord = MutableStateFlow<String?>(null)
    val openChestCoord: StateFlow<String?> = _openChestCoord.asStateFlow()

    private val _isCraftingTableNear = MutableStateFlow(false)
    val isCraftingTableNear: StateFlow<Boolean> = _isCraftingTableNear.asStateFlow()

    // Block mining state
    private val _miningCoord = MutableStateFlow<String?>(null)
    val miningCoord: StateFlow<String?> = _openFurnaceCoord.asStateFlow()
    private val _miningProgress = MutableStateFlow(0f) // 0.0 to 1.0f
    val miningProgress: StateFlow<Float> = _miningProgress.asStateFlow()

    // --- GAME OPTIONS & GUI SETTINGS (Particles, UI, Performance) ---
    val particlesEnabled = MutableStateFlow(true)
    val particleDensity = MutableStateFlow(1.0f) // 0.5f = Low, 1.0f = Medium, 1.5f = High
    val renderDistanceSetting = MutableStateFlow("Medium") // "Low", "Medium", "High"
    val isFirstPersonSetting = MutableStateFlow(true)
    val buttonSizeMultiplier = MutableStateFlow(1.00f) // 0.8f = Small, 1.0f = Normal, 1.25f = Large
    val touchSensitivity = MutableStateFlow(1.0f)
    val soundEffectsEnabled = MutableStateFlow(true)

    fun updateParticlesEnabled(enabled: Boolean) { particlesEnabled.value = enabled }
    fun updateParticleDensity(density: Float) { particleDensity.value = density }
    fun updateRenderDistance(level: String) { renderDistanceSetting.value = level }
    fun updateFirstPerson(enabled: Boolean) { isFirstPersonSetting.value = enabled }
    fun updateButtonMultiplier(multiplier: Float) { buttonSizeMultiplier.value = multiplier }
    fun updateTouchSensitivity(sens: Float) { touchSensitivity.value = sens }
    fun updateSoundEnabled(enabled: Boolean) { soundEffectsEnabled.value = enabled }

    fun setMiningProgress(progress: Float) {
        _miningProgress.value = progress
    }

    fun resetMiningProgress() {
        _miningProgress.value = 0f
    }

    // Game loop control
    private var gameLoopJob: Job? = null
    var joystickX = 0f
    var isJumpingPressed = false

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun loadLauncherData(context: Context) {
        _worlds.value = WorldSaver.listWorlds(context)
        _mods.value = WorldSaver.listMods(context)
    }

    // --- MODDING CONTROLLER METHODS ---

    fun startNewMod() {
        val newId = "mod_${System.currentTimeMillis() % 100000}"
        _editingMod.value = ModDefinition(
            id = newId,
            name = "My Awesome Mod",
            description = "Unleash customized textures and elements into the sandbox!",
            blocks = emptyList(),
            tools = emptyList()
        )
        _currentScreen.value = Screen.ModManager
    }

    fun selectEditMod(mod: ModDefinition) {
        _editingMod.value = mod
        _currentScreen.value = Screen.ModManager
    }

    fun updateEditingMod(updated: ModDefinition) {
        _editingMod.value = updated
    }

    fun saveEditingMod(context: Context) {
        val mod = _editingMod.value ?: return
        WorldSaver.saveMod(context, mod)
        _editingMod.value = null
        loadLauncherData(context)
        _currentScreen.value = Screen.Launcher
    }

    fun deleteMod(context: Context, modId: String) {
        WorldSaver.deleteMod(context, modId)
        loadLauncherData(context)
    }

    fun compileAndSaveMjmMod(context: Context, scriptText: String) {
        try {
            val parsed = ModDefinition.parseMjmScript(scriptText)
            WorldSaver.saveMjmScript(context, parsed.id, scriptText)
            WorldSaver.saveMod(context, parsed)
            _editingMod.value = null
            loadLauncherData(context)
            _currentScreen.value = Screen.Launcher
        } catch (e: Exception) {
            android.util.Log.e("GameViewModel", "Error compiling MJM script", e)
        }
    }

    fun toggleModEnabled(context: Context, modId: String) {
        val list = _mods.value
        val target = list.find { it.id == modId } ?: return
        val updated = target.copy(isEnabled = !target.isEnabled)
        WorldSaver.saveMod(context, updated)
        loadLauncherData(context)
    }

    // --- WORLD CONTROLLER METHODS ---

    fun createNewWorld(context: Context, name: String, mode: String, terrainType: String) {
        GameRegistry.resetToVanilla()
        
        // Register active mods
        val activeMods = _mods.value.filter { it.isEnabled }
        activeMods.forEach { GameRegistry.registerMod(it) }

        val seed = Random.nextLong()
        val defaultInventory = MutableList<ItemStack?>(36) { null }
        
        // Creative mode grants full vanilla blocks stack by default!
        if (mode == "Creative") {
            var i = 0
            GameRegistry.items.values.sortedBy { it.isBlock }.forEach { item ->
                if (i < 36) {
                    defaultInventory[i] = ItemStack(item.id, 64)
                    i++
                }
            }
        } else {
            // Survival grants some structural items
            defaultInventory[0] = ItemStack("wooden_pickaxe", 1)
            defaultInventory[1] = ItemStack("apple", 5)
            defaultInventory[2] = ItemStack("oak_log", 4)
        }

        // Generate procedural map blocks Map
        val worldBlocks = TerrainGenerator.generateWorld(
            name = name,
            seed = seed,
            worldType = terrainType,
            enabledModIds = activeMods.map { it.id }
        )

        // Find a safe spawn Y (top surface above grass block near X=50000)
        val spawnX = 50000
        var spawnY = 15f
        for (y in (44) downTo 0) {
            val key = "$spawnX,$y"
            if (worldBlocks.containsKey(key)) {
                val bId = worldBlocks[key]
                if (bId != "air" && bTypeIsSolid(bId ?: "air")) {
                    spawnY = y.toFloat() + 1.2f
                    break
                }
            }
        }

        val freshWorld = WorldSave(
            name = name,
            seed = seed,
            width = 100000,
            gameMode = mode,
            worldType = terrainType,
            playerState = PlayerState(
                x = spawnX.toFloat(),
                y = spawnY,
                inventory = defaultInventory
            ),
            worldBlocks = worldBlocks,
            enabledModIds = activeMods.map { it.id }
        )

        WorldSaver.saveWorld(context, freshWorld)
        loadLauncherData(context)
    }

    fun playWorld(context: Context, save: WorldSave) {
        // Reset registries first
        GameRegistry.resetToVanilla()
        
        // Load enabled mods
        _mods.value.forEach { mod ->
            if (save.enabledModIds.contains(mod.id)) {
                GameRegistry.registerMod(mod)
            }
        }

        // Auto-upgrade width to 100000 and adjust player position if it was 200
        val upgradedSave = if (save.width < 1000) {
            val offset = 50000 - 50
            val upgradedBlocks = mutableMapOf<String, String>()
            save.worldBlocks.forEach { (coord, blockId) ->
                val parts = coord.split(",")
                if (parts.size == 2) {
                    val originalX = parts[0].toIntOrNull() ?: 0
                    val y = parts[1]
                    val newX = originalX + offset
                    upgradedBlocks["$newX,$y"] = blockId
                }
            }
            save.copy(
                width = 100000,
                playerState = save.playerState.copy(
                    x = save.playerState.x + offset
                ),
                worldBlocks = upgradedBlocks
            )
        } else {
            save
        }

        _activeWorld.value = upgradedSave
        _isPaused.value = false
        _inventoryOpen.value = false
        _openFurnaceCoord.value = null
        _openChestCoord.value = null
        _isPlaceMode.value = false
        
        _currentScreen.value = Screen.Game

        // Launch Game Loop Thread
        startGameLoop(context)
    }

    fun deleteWorld(context: Context, name: String) {
        WorldSaver.deleteWorld(context, name)
        loadLauncherData(context)
    }

    // --- GAME ENGINE CLOCK LOOP & INTERACTION WORK ---

    private fun startGameLoop(context: Context) {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (_currentScreen.value == Screen.Game) {
                // If paused, don't run physical/smelting clock updates
                if (!_isPaused.value) {
                    tickGameEngine(context)
                }
                delay(33) // ~30 Game ticks / physics updates per second (super fluid!)
            }
        }
    }

    private fun tickGameEngine(context: Context) {
        var world = _activeWorld.value ?: return
        val player = world.playerState

        // 1. Tick Day/Night Sky time
        world.currentSkyTime = (world.currentSkyTime + 1.0f) % 24000
        
        // 1.5. Dynamic Endless world block generation chunk trigger
        val playerXInt = player.x.toInt()
        val checkRadius = 60
        var worldChanged = false
        val mutableBlocks = world.worldBlocks.toMutableMap()
        
        for (x in (playerXInt - checkRadius)..(playerXInt + checkRadius)) {
            if (x in 2 until world.width - 2) {
                val sentinelKey = "$x,0"
                if (!mutableBlocks.containsKey(sentinelKey)) {
                    val columnBlocks = TerrainGenerator.generateColumn(
                        x = x,
                        seed = world.seed,
                        height = world.height,
                        worldType = world.worldType,
                        enabledModIds = world.enabledModIds
                    )
                    mutableBlocks.putAll(columnBlocks)
                    worldChanged = true
                }
            }
        }
        if (worldChanged) {
            world = world.copy(worldBlocks = mutableBlocks)
            _activeWorld.value = world
        }
        
        // 2. Apply player physics & movement via joystick / d-pad
        val damage = PhysicsEngine.updatePlayer(
            player = player,
            moveX = joystickX,
            jumpPressed = isJumpingPressed,
            worldBlocks = world.worldBlocks,
            worldWidth = world.width,
            worldHeight = world.height
        )
        if (damage > 0f && world.gameMode == "Survival") {
            player.health = (player.health - damage).coerceAtLeast(0f)
            checkPlayerDeath(context)
        }

        // Over-time hunger depletion & natural health regeneration (Survival)
        if (world.gameMode == "Survival") {
            if (player.health > 0f) {
                // Decay hunger slowly: 20 -> 0 over roughly 5 minutes (approx 9000 engine ticks)
                player.hunger = (player.hunger - 0.0015f).coerceAtLeast(0f)
                
                // Natural regeneration: if hunger is high (>18) and player is injured, heal!
                if (player.hunger > 17f && player.health < player.maxHealth) {
                    player.health = (player.health + 0.015f).coerceAtMost(player.maxHealth)
                }
                
                // Starvation: if hunger is completely empty, take slow ticks damage!
                if (player.hunger <= 0f && player.health > 2f) {
                    player.health = (player.health - 0.015f).coerceAtLeast(2f)
                }
            }
        }

        // 3. Scan & update active furnaces smelting operations
        val mutableFurnaces = world.furnaceStates.toMutableMap()
        var furnacesChanged = false

        mutableFurnaces.forEach { (coord, furnace) ->
            if (tickFurnace(furnace, world.worldBlocks)) {
                furnacesChanged = true
            }
        }

        // 4. Check block table triggers (like standing coordinates near crafting stations)
        val playerGridX = player.x.toInt()
        val playerGridY = player.y.toInt()
        var craftingTableFound = false
        
        for (dx in -2..2) {
            for (dy in -2..2) {
                val blockId = world.worldBlocks["${playerGridX + dx},${playerGridY + dy}"]
                if (blockId == "crafting_table") {
                    craftingTableFound = true
                }
            }
        }
        _isCraftingTableNear.value = craftingTableFound

        // Trigger updates to composables
        val updatedWorld = world.copy(
            furnaceStates = if (furnacesChanged) mutableFurnaces else world.furnaceStates,
            currentSkyTime = world.currentSkyTime
        )
        _activeWorld.value = updatedWorld
    }

    private fun checkPlayerDeath(context: Context) {
        val world = _activeWorld.value ?: return
        val player = world.playerState
        if (player.health <= 0f) {
            // Respawn player
            player.health = player.maxHealth
            player.hunger = player.maxHealth
            
            // Clear survival inventory as death penalty
            if (world.gameMode == "Survival") {
                player.inventory.forEachIndexed { idx, _ -> player.inventory[idx] = null }
                // Grant initial tools
                player.inventory[0] = ItemStack("wooden_pickaxe", 1)
            }
            
            // Reset coordinates to top surface spawn
            player.x = 50000f
            player.velocityY = 0f
            player.velocityX = 0f
            
            var ySurface = 15f
            for (y in (world.height - 1) downTo 0) {
                val key = "50000,$y"
                if (world.worldBlocks.containsKey(key)) {
                    val bId = world.worldBlocks[key]
                    if (bId != "air" && bTypeIsSolid(bId ?: "air")) {
                        ySurface = y.toFloat() + 1.2f
                        break
                    }
                }
            }
            player.y = ySurface
            
            // Auto save state
            WorldSaver.saveWorld(context, world)
        }
    }

    /**
     * Ticks a single furnace smelting state. Returns true if state changed (requiring map save update).
     */
    private fun tickFurnace(furnace: FurnaceState, worldBlocks: Map<String, String>): Boolean {
        var changed = false
        
        // 1. Fire Fuel burning countdown
        if (furnace.burnTimeRemaining > 0) {
            furnace.burnTimeRemaining--
            changed = true
        }

        val hasInput = furnace.inputStack != null && (furnace.inputStack?.count ?: 0) > 0
        val recipe = if (hasInput) GameRegistry.smeltingRecipes.find { it.inputItemId == furnace.inputStack?.itemId } else null

        // 2. Refuel if needed and cooking is achievable
        if (furnace.burnTimeRemaining <= 0 && recipe != null) {
            val canOutput = canFurnaceOutputAccept(furnace, recipe.outputItemId)
            val hasFuel = furnace.fuelStack != null && (furnace.fuelStack?.count ?: 0) > 0

            if (canOutput && hasFuel) {
                // Consume 1 fuel
                val fuel = furnace.fuelStack!!
                val fuelValue = getFuelBurnTime(fuel.itemId)
                
                if (fuelValue > 0) {
                    fuel.count--
                    if (fuel.count <= 0) furnace.fuelStack = null
                    
                    furnace.burnTimeRemaining = fuelValue
                    furnace.maxBurnTime = fuelValue
                    changed = true
                }
            }
        }

        // 3. Process smelting cooking progress
        if (furnace.burnTimeRemaining > 0 && recipe != null && canFurnaceOutputAccept(furnace, recipe.outputItemId)) {
            furnace.isCooking = true
            furnace.cookTimeProgress++
            changed = true

            // Reach cook threshold (standard 200 ticks)
            if (furnace.cookTimeProgress >= recipe.smeltingTicks) {
                furnace.cookTimeProgress = 0
                
                // Deduct 1 Input
                furnace.inputStack?.let {
                    it.count--
                    if (it.count <= 0) furnace.inputStack = null
                }

                // Add 1 Output
                if (furnace.outputStack == null) {
                    furnace.outputStack = ItemStack(recipe.outputItemId, 1)
                } else {
                    furnace.outputStack?.let {
                        if (it.itemId == recipe.outputItemId) {
                            it.count = (it.count + 1).coerceAtMost(64)
                        }
                    }
                }
            }
        } else {
            if (furnace.cookTimeProgress > 0) {
                furnace.cookTimeProgress = (furnace.cookTimeProgress - 2).coerceAtLeast(0) // progress cooling down
                furnace.isCooking = false
                changed = true
            }
        }

        return changed
    }

    private fun canFurnaceOutputAccept(furnace: FurnaceState, outItemId: String): Boolean {
        val out = furnace.outputStack ?: return true
        return out.itemId == outItemId && out.count < 64
    }

    private fun getFuelBurnTime(itemId: String): Int {
        return when (itemId) {
            "coal" -> 800 // smelts 4 items
            "oak_log" -> 200 // smelts 1 item
            "oak_planks" -> 150
            "stick" -> 50
            else -> 0
        }
    }

    // --- MANIPULATE BLOCKS (BREAKER & PLACER EVENTS) ---

    fun togglePlacementMode() {
        _isPlaceMode.value = !_isPlaceMode.value
    }

    fun handleBlockInteraction(context: Context, bx: Int, by: Int) {
        val world = _activeWorld.value ?: return
        val player = world.playerState

        // Validate player interaction reaches target (within 4 block radius)
        val distance = Math.hypot((player.x - bx).toDouble(), (player.y - by).toDouble())
        if (distance > 4.5) return // Too far!

        val blockKey = "$bx,$by"
        val existingBlock = world.worldBlocks[blockKey] ?: "air"

        if (_isPlaceMode.value) {
            // PLACE MODE: Put item block into empty air coords
            if (existingBlock == "air") {
                val selectedItem = player.inventory[player.activeHotbarIndex] ?: return
                
                // Ensure item type is a placeable Block!
                val iType = GameRegistry.items[selectedItem.itemId]
                if (iType != null && iType.isBlock && iType.blockId != null) {
                    
                    // Don't wrap solid block directly inside Steve bounding box!
                    val isSolid = bTypeIsSolid(iType.blockId)
                    val overlapsPlayer = isSolid && PhysicsEngine.checkCollision(player.x, player.y, mapOf(blockKey to iType.blockId))
                    if (!overlapsPlayer) {
                        // Place block!
                        val mutableBlocks = world.worldBlocks.toMutableMap()
                        mutableBlocks[blockKey] = iType.blockId
                        
                        // Deduct inventory
                        if (world.gameMode == "Survival") {
                            selectedItem.count--
                            if (selectedItem.count <= 0) {
                                player.inventory[player.activeHotbarIndex] = null
                            }
                        }

                        // Save World trigger
                        val newWorld = world.copy(worldBlocks = mutableBlocks)
                        _activeWorld.value = newWorld
                        WorldSaver.saveWorld(context, newWorld)
                    }
                }
            }
        } else {
            // BREAK/MINE MODE: Double check custom containers click first
            if (existingBlock == "furnace") {
                openFurnaceContainer(blockKey)
                return
            } else if (existingBlock == "chest") {
                openChestContainer(blockKey)
                return
            } else if (existingBlock == "vault") {
                val heldItem = player.inventory[player.activeHotbarIndex]
                if (heldItem?.itemId == "trial_key") {
                    // Consume key
                    if (world.gameMode == "Survival") {
                        heldItem.count--
                        if (heldItem.count <= 0) player.inventory[player.activeHotbarIndex] = null
                    }
                    // Reward high tier loot
                    addToInventory("heavy_core", 1)
                    addToInventory("breeze_rod", 2)
                    addToInventory("ominous_bottle", 1)
                    _activeWorld.value = world.copy()
                    WorldSaver.saveWorld(context, world)
                    android.widget.Toast.makeText(context, "🔑 VAULT UNLOCKED! Received: Heavy Core, 2x Breeze Rods, 1x Ominous Bottle! 🔑", android.widget.Toast.LENGTH_LONG).show()
                } else if (heldItem?.itemId == "ominous_trial_key") {
                    // Consume ominous key
                    if (world.gameMode == "Survival") {
                        heldItem.count--
                        if (heldItem.count <= 0) player.inventory[player.activeHotbarIndex] = null
                    }
                    // Reward legendary mace loot!
                    addToInventory("mace", 1)
                    addToInventory("wind_charge", 8)
                    _activeWorld.value = world.copy()
                    WorldSaver.saveWorld(context, world)
                    android.widget.Toast.makeText(context, "☠️ OMINOUS VAULT UNLOCKED! Discovered legendary Mace! Received: 1x Mace, 8x Wind Charges! ☠️", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "🔒 Vault is sealed. Tap with a Trial Key or Ominous Trial Key to open!", android.widget.Toast.LENGTH_LONG).show()
                }
                return
            } else if (existingBlock == "trial_spawner") {
                // Simulate Trial Spawner battle
                if (world.gameMode == "Survival" && player.health <= 4.0f) {
                    android.widget.Toast.makeText(context, "⚠️ You are too weak! Heal up first before starting the Trial!", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    if (world.gameMode == "Survival") {
                        player.health -= 3.0f // take damage from simulated fight
                    }
                    // Determine drops (random chances)
                    val randVal = java.util.Random().nextFloat()
                    val keyToGive = if (randVal < 0.25f) "ominous_trial_key" else "trial_key"
                    addToInventory(keyToGive, 1)
                    addToInventory("breeze_rod", 2)
                    _activeWorld.value = world.copy()
                    WorldSaver.saveWorld(context, world)
                    val spawnerName = if (keyToGive == "ominous_trial_key") "OMINOUS SPAWNER" else "TRIAL SPAWNER"
                    android.widget.Toast.makeText(context, "⚔️ $spawnerName CLEARED! Fought off the Breeze. Received: ${if (keyToGive == "ominous_trial_key") "Ominous" else "Standard"} Trial Key, 2x Breeze Rods! (-3 HP) ⚔️", android.widget.Toast.LENGTH_LONG).show()
                }
                return
            } else if (existingBlock == "crafter") {
                // Automated craft trigger: check available ingredients in player inventory to auto-craft items
                // This searches the inventory and tries to automatically craft premium 1.21.1 items!
                var autoCrafted = false
                
                // Check 1: Can craft standard Wind Charge from Breeze Rods?
                val breezeCount = countInventoryItem("breeze_rod")
                val copperCount = countInventoryItem("copper_ingot")
                val rawCopperCount = countInventoryItem("raw_copper")
                val coreCount = countInventoryItem("heavy_core")
                
                if (coreCount >= 1 && breezeCount >= 1) {
                    deductInventoryItem("heavy_core", 1)
                    deductInventoryItem("breeze_rod", 1)
                    addToInventory("mace", 1)
                    autoCrafted = true
                    android.widget.Toast.makeText(context, "⚙️ CRAFTER AUTO-COMBINED Heavy Core + Breeze Rod -> Crafted Mace! ⚙️", android.widget.Toast.LENGTH_LONG).show()
                } else if (breezeCount >= 1) {
                    deductInventoryItem("breeze_rod", 1)
                    addToInventory("wind_charge", 4)
                    autoCrafted = true
                    android.widget.Toast.makeText(context, "⚙️ CRAFTER AUTO-CRAFTED: Breeze Rod -> 4x Wind Charges! ⚙️", android.widget.Toast.LENGTH_LONG).show()
                } else if (rawCopperCount >= 1) {
                    deductInventoryItem("raw_copper", 1)
                    addToInventory("copper_ingot", 1)
                    autoCrafted = true
                    android.widget.Toast.makeText(context, "⚙️ CRAFTER AUTO-SMELTED: Raw Copper -> 1x Copper Ingot! ⚙️", android.widget.Toast.LENGTH_LONG).show()
                } else if (copperCount >= 4) {
                    deductInventoryItem("copper_ingot", 4)
                    addToInventory("copper_block", 1)
                    autoCrafted = true
                    android.widget.Toast.makeText(context, "⚙️ CRAFTER AUTO-COMPRESSED: 4x Copper Ingots -> 1x Copper Block! ⚙️", android.widget.Toast.LENGTH_LONG).show()
                } else if (copperCount >= 3) {
                    deductInventoryItem("copper_ingot", 3)
                    addToInventory("copper_bulb", 1)
                    autoCrafted = true
                    android.widget.Toast.makeText(context, "⚙️ CRAFTER AUTO-CRAFTED: 3x Copper -> 1x Copper Bulb! ⚙️", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "⚙️ Crafter idle. Place Breeze Rods, Heavy Cores, or Copper in your inventory to auto-craft 1.21.1 items!", android.widget.Toast.LENGTH_LONG).show()
                }
                
                if (autoCrafted) {
                    _activeWorld.value = world.copy()
                    WorldSaver.saveWorld(context, world)
                }
                return
            }

            // Mine standard breakable target block
            if (existingBlock != "air" && existingBlock != "bedrock") {
                // Apply tool-based efficiency mining
                val activeTool = player.inventory[player.activeHotbarIndex]
                val speedCoeff = getToolMiningSpeed(activeTool, existingBlock)

                // Mining takes ticks. In-game, we can immediately mine or implement a short delayed mine duration.
                // For direct, highly responsive playing, we mine it immediately and yield items,
                // which is extremely satisfying, or introduce a 1-turn mine progress.
                // Let's implement immediate responsive mining for standard blocks, and larger delays for hard ones!
                mineBlockAction(context, bx, by, existingBlock)
            }
        }
    }

    fun mineBlockAction(context: Context, bx: Int, by: Int, blockId: String) {
        val world = _activeWorld.value ?: return
        val player = world.playerState
        val blockKey = "$bx,$by"

        val bType = GameRegistry.blocks[blockId] ?: return
        
        // Break Block & clear coordinate
        val mutableBlocks = world.worldBlocks.toMutableMap()
        mutableBlocks.remove(blockKey)

        // Drop item stack
        val dropId = bType.dropItemId ?: blockId
        if (dropId != "air" && world.gameMode != "Creative") {
            addToInventory(dropId, bType.dropCount)
        }

        // Clean registered container state if breaking a Container block!
        val mutableFurnaces = world.furnaceStates.toMutableMap()
        mutableFurnaces.remove(blockKey)

        val mutableChests = world.chestStates.toMutableMap()
        mutableChests.remove(blockKey)

        val newWorld = world.copy(
            worldBlocks = mutableBlocks,
            furnaceStates = mutableFurnaces,
            chestStates = mutableChests
        )
        _activeWorld.value = newWorld
        WorldSaver.saveWorld(context, newWorld)
    }

    fun getToolMiningSpeed(toolStack: ItemStack?, targetBlockId: String): Float {
        val bType = GameRegistry.blocks[targetBlockId] ?: return 1.0f
        val tItem = toolStack?.let { GameRegistry.items[it.itemId] } ?: return 1.0f

        if (!tItem.isTool) return 1.0f

        // Check if correct tool type (e.g. pickaxe on stone)
        if (bType.requiredToolType != ToolType.NONE && bType.requiredToolType != tItem.toolType) {
            return 0.5f // highly inefficient
        }

        // Check if sufficient level
        if (tItem.toolTier.level < bType.requiredToolTier) {
            return 0.5f
        }

        return tItem.efficiency
    }

    fun forcePlaceBlock(context: Context, bx: Int, by: Int) {
        val world = _activeWorld.value ?: return
        val player = world.playerState
        val blockKey = "$bx,$by"

        // Validate player interaction range (4.5 block radius)
        val distance = Math.hypot((player.x - bx).toDouble(), (player.y - by).toDouble())
        if (distance > 4.5) return // Reach limit

        val existingBlock = world.worldBlocks[blockKey] ?: "air"
        if (existingBlock == "air") {
            val selectedItem = player.inventory[player.activeHotbarIndex] ?: return
            
            // Ensure item is block
            val iType = GameRegistry.items[selectedItem.itemId]
            if (iType != null && iType.isBlock && iType.blockId != null) {
                val isSolid = bTypeIsSolid(iType.blockId)
                val overlapsPlayer = isSolid && PhysicsEngine.checkCollision(player.x, player.y, mapOf(blockKey to iType.blockId))
                if (!overlapsPlayer) {
                    val mutableBlocks = world.worldBlocks.toMutableMap()
                    mutableBlocks[blockKey] = iType.blockId
                    
                    if (world.gameMode == "Survival") {
                        selectedItem.count--
                        if (selectedItem.count <= 0) {
                            player.inventory[player.activeHotbarIndex] = null
                        }
                    }
                    val newWorld = world.copy(worldBlocks = mutableBlocks)
                    _activeWorld.value = newWorld
                    WorldSaver.saveWorld(context, newWorld)
                }
            }
        }
    }

    fun toggleGameMode(context: Context, mode: String) {
        val world = _activeWorld.value ?: return
        val updatedWorld = world.copy(gameMode = mode)
        _activeWorld.value = updatedWorld
        WorldSaver.saveWorld(context, updatedWorld)
    }

    fun spawnCreativeItem(context: Context, itemId: String, count: Int) {
        val world = _activeWorld.value ?: return
        val player = world.playerState
        
        // Find existing stack or first empty slot
        var targetIndex = -1
        for (i in 0 until 36) {
            val s = player.inventory[i]
            if (s != null && s.itemId == itemId && s.count < 64) {
                targetIndex = i
                break
            }
        }
        if (targetIndex == -1) {
            for (i in 0 until 36) {
                if (player.inventory[i] == null) {
                    targetIndex = i
                    break
                }
            }
        }
        if (targetIndex == -1) {
            targetIndex = player.activeHotbarIndex
        }
        
        player.inventory[targetIndex] = ItemStack(itemId, count)
        
        val newWorld = world.copy(playerState = player)
        _activeWorld.value = newWorld
        WorldSaver.saveWorld(context, newWorld)
    }

    private fun bTypeIsSolid(blockId: String): Boolean {
        return GameRegistry.blocks[blockId]?.isSolid ?: true
    }

    // --- CONTAINER CONTROLLERS ---

    private fun openFurnaceContainer(coord: String) {
        val world = _activeWorld.value ?: return
        val existing = world.furnaceStates[coord]
        
        if (existing == null) {
            val mutableFurnaces = world.furnaceStates.toMutableMap()
            mutableFurnaces[coord] = FurnaceState()
            _activeWorld.value = world.copy(furnaceStates = mutableFurnaces)
        }
        _openFurnaceCoord.value = coord
        _inventoryOpen.value = true
    }

    private fun openChestContainer(coord: String) {
        val world = _activeWorld.value ?: return
        val existing = world.chestStates[coord]

        if (existing == null) {
            val mutableChests = world.chestStates.toMutableMap()
            val newChest = ChestState()
            
            // Generate procedural structure loot depending on location
            val parts = coord.split(",")
            if (parts.size == 2) {
                val bx = parts[0].toIntOrNull() ?: 0
                val rand = java.util.Random((world.seed + bx).toLong())
                
                val houseInterval = 140
                val houseCx = ((bx - 70).toFloat() / houseInterval.toFloat()).let { java.lang.Math.round(it) } * houseInterval + 70
                val isVillageChest = Math.abs(bx - houseCx) <= 3
                
                val pyramidInterval = 320
                val pyramidCx = ((bx - 160).toFloat() / pyramidInterval.toFloat()).let { java.lang.Math.round(it) } * pyramidInterval + 160
                val isPyramidChest = Math.abs(bx - pyramidCx) <= 4

                if (isVillageChest) {
                    // Populate village loot
                    newChest.slots[0] = ItemStack("apple", rand.nextInt(4) + 2)
                    newChest.slots[1] = ItemStack("oak_log", rand.nextInt(12) + 4)
                    newChest.slots[2] = ItemStack("iron_ingot", rand.nextInt(3) + 1)
                    newChest.slots[4] = ItemStack("coal", rand.nextInt(8) + 2)
                    if (rand.nextFloat() < 0.25f) {
                        newChest.slots[8] = ItemStack("trial_key", 1)
                    }
                } else if (isPyramidChest) {
                    // Populate desert pyramid treasure loot!
                    newChest.slots[0] = ItemStack("diamond", rand.nextInt(3) + 1)
                    newChest.slots[2] = ItemStack("gold_block", rand.nextInt(2) + 1)
                    newChest.slots[4] = ItemStack("mace", 1)
                    newChest.slots[8] = ItemStack("mod_ic2_uranium_rod", rand.nextInt(2) + 1)
                    newChest.slots[12] = ItemStack("trial_key", rand.nextInt(2) + 1)
                    if (rand.nextFloat() < 0.35f) {
                        newChest.slots[15] = ItemStack("mod_ic2_nano_saber", 1)
                    }
                } else {
                    // Random wilderness chest loot
                    newChest.slots[0] = ItemStack("cobblestone", rand.nextInt(20) + 10)
                    newChest.slots[4] = ItemStack("stick", rand.nextInt(6) + 2)
                }
            }
            
            mutableChests[coord] = newChest
            _activeWorld.value = world.copy(chestStates = mutableChests)
        }
        _openChestCoord.value = coord
        _inventoryOpen.value = true
    }

    fun closeContainer(context: Context) {
        _openFurnaceCoord.value = null
        _openChestCoord.value = null
        _inventoryOpen.value = false
        _activeWorld.value?.let { WorldSaver.saveWorld(context, it) }
    }

    fun moveFurnaceItem(context: Context, slotType: String, playerInvIndex: Int) {
        val world = _activeWorld.value ?: return
        val coord = _openFurnaceCoord.value ?: return
        val furnace = world.furnaceStates[coord] ?: return
        val playerStack = world.playerState.inventory[playerInvIndex] ?: return

        when (slotType) {
            "INPUT" -> {
                // Transfer player item to furnace input slot
                val target = furnace.inputStack
                if (target == null) {
                    furnace.inputStack = playerStack.copyStack()
                    world.playerState.inventory[playerInvIndex] = null
                } else if (target.itemId == playerStack.itemId) {
                    val add = (64 - target.count).coerceAtMost(playerStack.count)
                    target.count += add
                    playerStack.count -= add
                    if (playerStack.count <= 0) world.playerState.inventory[playerInvIndex] = null
                }
            }
            "FUEL" -> {
                // Is this a valid fuel item?
                if (getFuelBurnTime(playerStack.itemId) > 0) {
                    val target = furnace.fuelStack
                    if (target == null) {
                        furnace.fuelStack = playerStack.copyStack()
                        world.playerState.inventory[playerInvIndex] = null
                    } else if (target.itemId == playerStack.itemId) {
                        val add = (64 - target.count).coerceAtMost(playerStack.count)
                        target.count += add
                        playerStack.count -= add
                        if (playerStack.count <= 0) world.playerState.inventory[playerInvIndex] = null
                    }
                }
            }
        }
        _activeWorld.value = world.copy()
        WorldSaver.saveWorld(context, world)
    }

    fun takeFurnaceOutput(context: Context, playerInvIndexSpace: Int) {
        val world = _activeWorld.value ?: return
        val coord = _openFurnaceCoord.value ?: return
        val furnace = world.furnaceStates[coord] ?: return
        val outStack = furnace.outputStack ?: return

        val playerDest = world.playerState.inventory[playerInvIndexSpace]
        if (playerDest == null) {
            world.playerState.inventory[playerInvIndexSpace] = outStack.copyStack()
            furnace.outputStack = null
        } else if (playerDest.itemId == outStack.itemId) {
            val add = (64 - playerDest.count).coerceAtMost(outStack.count)
            playerDest.count += add
            outStack.count -= add
            if (outStack.count <= 0) furnace.outputStack = null
        }

        _activeWorld.value = world.copy()
        WorldSaver.saveWorld(context, world)
    }

    fun moveChestItem(context: Context, chestSlotIndex: Int, playerInvIndex: Int, toChest: Boolean) {
        val world = _activeWorld.value ?: return
        val coord = _openChestCoord.value ?: return
        val chest = world.chestStates[coord] ?: return

        if (toChest) {
            val pStack = world.playerState.inventory[playerInvIndex] ?: return
            val cStack = chest.slots[chestSlotIndex]
            
            if (cStack == null) {
                chest.slots[chestSlotIndex] = pStack.copyStack()
                world.playerState.inventory[playerInvIndex] = null
            } else if (cStack.itemId == pStack.itemId) {
                val space = (64 - cStack.count).coerceAtMost(pStack.count)
                cStack.count += space
                pStack.count -= space
                if (pStack.count <= 0) world.playerState.inventory[playerInvIndex] = null
            }
        } else {
            val cStack = chest.slots[chestSlotIndex] ?: return
            val pStack = world.playerState.inventory[playerInvIndex]

            if (pStack == null) {
                world.playerState.inventory[playerInvIndex] = cStack.copyStack()
                chest.slots[chestSlotIndex] = null
            } else if (pStack.itemId == cStack.itemId) {
                val space = (64 - pStack.count).coerceAtMost(cStack.count)
                pStack.count += space
                cStack.count -= space
                if (cStack.count <= 0) chest.slots[chestSlotIndex] = null
            }
        }

        _activeWorld.value = world.copy()
        WorldSaver.saveWorld(context, world)
    }

    // --- INVENTORY & CRAFTING CONTROLLERS ---

    fun selectHotbarSlot(index: Int) {
        val world = _activeWorld.value ?: return
        world.playerState.activeHotbarIndex = index
        _activeWorld.value = world.copy()
    }

    fun consumeActiveFood(context: Context) {
        val world = _activeWorld.value ?: return
        val player = world.playerState
        val currentStack = player.inventory[player.activeHotbarIndex] ?: return

        if (currentStack.itemId == "apple" && world.gameMode == "Survival") {
            player.health = (player.health + 4f).coerceAtMost(player.maxHealth)
            player.hunger = (player.hunger + 4f).coerceAtMost(player.maxHealth)
            
            currentStack.count--
            if (currentStack.count <= 0) {
                player.inventory[player.activeHotbarIndex] = null
            }

            _activeWorld.value = world.copy()
            WorldSaver.saveWorld(context, world)
        } else if (currentStack.itemId == "nano_banana_2_item") {
            // Nano Banana 2 restores full health and saturation!
            player.health = player.maxHealth
            player.hunger = player.maxHealth
            
            if (world.gameMode == "Survival") {
                currentStack.count--
                if (currentStack.count <= 0) {
                    player.inventory[player.activeHotbarIndex] = null
                }
            }

            _activeWorld.value = world.copy()
            WorldSaver.saveWorld(context, world)
            android.widget.Toast.makeText(context, "🍌 NANO BANANA 2 POWER DETECTED! Max HP & Hunger Restored! 🍌", android.widget.Toast.LENGTH_LONG).show()
        } else if (currentStack.itemId == "ominous_bottle") {
            // Give bad omen effect
            if (world.gameMode == "Survival") {
                currentStack.count--
                if (currentStack.count <= 0) {
                    player.inventory[player.activeHotbarIndex] = null
                }
            }
            _activeWorld.value = world.copy()
            WorldSaver.saveWorld(context, world)
            android.widget.Toast.makeText(context, "☠️ BAD OMEN INITIATED! Trial spawners are now highly dangerous OMINOUS SPAWNERS! ☠️", android.widget.Toast.LENGTH_LONG).show()
        } else if (currentStack.itemId == "wind_charge") {
            // Rocket jump launch!
            player.velocityY = 10.5f
            player.isGrounded = false
            if (world.gameMode == "Survival") {
                currentStack.count--
                if (currentStack.count <= 0) {
                    player.inventory[player.activeHotbarIndex] = null
                }
            }
            _activeWorld.value = world.copy()
            WorldSaver.saveWorld(context, world)
            android.widget.Toast.makeText(context, "💨 WIND CHARGED! Compressed air blast launched you skyward! 💨", android.widget.Toast.LENGTH_SHORT).show()
        } else if (currentStack.itemId == "mace") {
            // Smashing slam attack!
            if (!player.isGrounded) {
                android.widget.Toast.makeText(context, "⚡️ CRUSHING SMASH! Critical high-fall Mace strike landed! ⚡️", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "⚔️ Mace sweep strike deals +8 heavy damage! ⚔️", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleInventory() {
        _inventoryOpen.value = !_inventoryOpen.value
    }

    fun togglePause() {
        _isPaused.value = !_isPaused.value
    }

    fun craftItem(context: Context, recipe: CraftingRecipe): Boolean {
        val world = _activeWorld.value ?: return false
        val player = world.playerState

        // Validate recipe ingredients in player inventory
        // Group inputs inside player full inventory
        val counts = mutableMapOf<String, Int>()
        player.inventory.forEach { stack ->
            if (stack != null) {
                counts[stack.itemId] = (counts[stack.itemId] ?: 0) + stack.count
            }
        }

        // Verify counts
        for ((needId, needQty) in recipe.ingredients) {
            val has = counts[needId] ?: 0
            if (has < needQty) return false
        }

        // Deduct ingredients!
        for ((needId, needQty) in recipe.ingredients) {
            var leftToDeduct = needQty
            for (i in 0 until 36) {
                val stack = player.inventory[i]
                if (stack != null && stack.itemId == needId) {
                    if (stack.count >= leftToDeduct) {
                        stack.count -= leftToDeduct
                        leftToDeduct = 0
                        if (stack.count <= 0) player.inventory[i] = null
                        break
                    } else {
                        leftToDeduct -= stack.count
                        player.inventory[i] = null
                    }
                }
            }
        }

        // Deliver result item stack
        addToInventory(recipe.resultItemId, recipe.resultCount)
        
        _activeWorld.value = world.copy()
        WorldSaver.saveWorld(context, world)
        return true
    }

    fun addToInventory(itemId: String, amount: Int): Boolean {
        val world = _activeWorld.value ?: return false
        val player = world.playerState
        var remaining = amount

        // First pass: accumulate into current matching stacks
        for (i in 0 until 36) {
            val stack = player.inventory[i]
            if (stack != null && stack.itemId == itemId) {
                val addable = (64 - stack.count).coerceAtMost(remaining)
                stack.count += addable
                remaining -= addable
                if (remaining <= 0) break
            }
        }

        // Second pass: fill empty slots
        if (remaining > 0) {
            for (i in 0 until 36) {
                if (player.inventory[i] == null) {
                    player.inventory[i] = ItemStack(itemId, remaining.coerceAtMost(64))
                    remaining -= player.inventory[i]!!.count
                    if (remaining <= 0) break
                }
            }
        }

        return remaining <= 0
    }

    fun countInventoryItem(itemId: String): Int {
        val world = _activeWorld.value ?: return 0
        var total = 0
        for (i in 0 until 36) {
            val s = world.playerState.inventory[i]
            if (s != null && s.itemId == itemId) {
                total += s.count
            }
        }
        return total
    }

    fun deductInventoryItem(itemId: String, amount: Int): Boolean {
        val world = _activeWorld.value ?: return false
        var remaining = amount
        for (i in 0 until 36) {
            val s = world.playerState.inventory[i]
            if (s != null && s.itemId == itemId) {
                val deductBytes = s.count.coerceAtMost(remaining)
                s.count -= deductBytes
                remaining -= deductBytes
                if (s.count <= 0) {
                    world.playerState.inventory[i] = null
                }
                if (remaining <= 0) break
            }
        }
        return remaining <= 0
    }
}
