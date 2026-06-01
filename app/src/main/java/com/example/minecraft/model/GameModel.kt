package com.example.minecraft.model

import androidx.compose.ui.graphics.Color
import com.squareup.moshi.JsonClass

enum class ToolType {
    NONE, PICKAXE, AXE, SHOVEL, SWORD, HOE
}

enum class ToolTier(val level: Int, val miningSpeedMultiplier: Float) {
    HAND(0, 1.0f),
    WOOD(1, 2.0f),
    STONE(2, 4.0f),
    IRON(3, 7.0f),
    DIAMOND(4, 12.0f),
    NETHERITE(5, 16.0f)
}

@JsonClass(generateAdapter = true)
data class BlockType(
    val id: String,
    val name: String,
    val isSolid: Boolean = true,
    val isPassable: Boolean = false,
    val hardness: Float = 1.0f, // Ticks or seconds to break
    val requiredToolType: ToolType = ToolType.NONE,
    val requiredToolTier: Int = 0,
    val dropItemId: String? = null,
    val dropCount: Int = 1,
    // Visual colors (Custom Minecraft-like patterns can be rendered using these base/accent colors)
    val colorHex: String,
    val accentColorHex: String? = null,
    val topColorHex: String? = null, // e.g. Grass top green
    val isMineable: Boolean = true,
    val canBurn: Boolean = false
) {
    fun getColor(): Color = Color(android.graphics.Color.parseColor(colorHex))
    fun getAccentColor(): Color? = accentColorHex?.let { Color(android.graphics.Color.parseColor(it)) }
    fun getTopColor(): Color? = topColorHex?.let { Color(android.graphics.Color.parseColor(it)) }
}

@JsonClass(generateAdapter = true)
data class ItemType(
    val id: String,
    val name: String,
    val isBlock: Boolean = false,
    val blockId: String? = null,
    val isTool: Boolean = false,
    val toolType: ToolType = ToolType.NONE,
    val toolTier: ToolTier = ToolTier.HAND,
    val efficiency: Float = 1.0f,
    val colorHex: String = "#CCCCCC",
    val description: String = ""
) {
    fun getColor(): Color = Color(android.graphics.Color.parseColor(colorHex))
}

@JsonClass(generateAdapter = true)
data class ItemStack(
    val itemId: String,
    var count: Int = 1
) {
    fun copyStack(): ItemStack = ItemStack(itemId, count)
}

@JsonClass(generateAdapter = true)
data class CraftingRecipe(
    val id: String,
    val resultItemId: String,
    val resultCount: Int = 1,
    // For simplicity, recipes can be represented as shaped 3x3 grids,
    // or shapeless ingredient counts. Let's do simple shapeless list of list configurations
    // or exact shapeless ingredient list for the custom craft manager.
    // Map of Ingredient Item ID -> Count required
    val ingredients: Map<String, Int>,
    val requiresCraftingTable: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SmeltingRecipe(
    val inputItemId: String,
    val outputItemId: String,
    val experience: Float = 0.1f,
    val smeltingTicks: Int = 200 // standard furnace speed
)

// Represents our Custom Mod Creator structures
@JsonClass(generateAdapter = true)
data class CustomBlockDef(
    val id: String,
    val name: String,
    val colorHex: String,
    val accentColorHex: String = "#000000",
    val hardness: Float = 2.0f,
    val spawnFrequency: String = "Medium", // "None", "Rare", "Medium", "Abundant"
    val spawnDepthMin: Int = 5,
    val spawnDepthMax: Int = 30,
    val dropItemId: String,
    val smeltingResultItemId: String? = null,
    val craftingIngredients: Map<String, Int> = emptyMap() // Recipe to craft this block
)

@JsonClass(generateAdapter = true)
data class CustomToolDef(
    val id: String,
    val name: String,
    val type: String, // "Pickaxe", "Sword", "Axe", "Shovel"
    val tier: String, // "Wood", "Stone", "Iron", "Diamond", "God" (Super Tier)
    val colorHex: String,
    val efficiency: Float = 5.0f,
    val craftingIngredients: Map<String, Int> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class ModDefinition(
    val id: String, // prefix like "custom_mod_"
    val name: String,
    val description: String,
    val blocks: List<CustomBlockDef> = emptyList(),
    val tools: List<CustomToolDef> = emptyList(),
    val isEnabled: Boolean = true
) {
    companion object {
        fun parseMjmScript(script: String): ModDefinition {
            var modId = "custom_mod_mjm"
            var modName = "Custom MJM Scripted Mod"
            var modDesc = "Created or loaded from .mjm script format."
            val blocksList = mutableListOf<CustomBlockDef>()
            val toolsList = mutableListOf<CustomToolDef>()

            val lines = script.lines()
            var currentSection = "" // "BLOCK", "TOOL"
            
            // Temp variables
            var tempId = ""
            var tempName = ""
            var tempColor = "#4CAF50"
            var tempAccent = "#FFFFFF"
            var tempHardness = 1.0f
            var tempDropItem = ""
            var tempToolType = "Pickaxe"
            var tempToolTier = "Diamond"
            var tempEfficiency = 10f
            val tempIngredients = mutableMapOf<String, Int>()

            for (lineRaw in lines) {
                val line = lineRaw.trim()
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue

                if (line.startsWith("MOD_ID:")) {
                    modId = line.substringAfter("MOD_ID:").trim()
                    continue
                }
                if (line.startsWith("MOD_NAME:")) {
                    modName = line.substringAfter("MOD_NAME:").trim()
                    continue
                }
                if (line.startsWith("MOD_DESC:")) {
                    modDesc = line.substringAfter("MOD_DESC:").trim()
                    continue
                }

                if (line == "BLOCK_START") {
                    currentSection = "BLOCK"
                    tempId = ""
                    tempName = ""
                    tempColor = "#4CAF50"
                    tempAccent = "#FFFFFF"
                    tempHardness = 1.0f
                    tempDropItem = ""
                    continue
                }

                if (line == "BLOCK_END") {
                    if (tempId.isNotEmpty()) {
                        blocksList.add(
                            CustomBlockDef(
                                id = tempId,
                                name = tempName.ifEmpty { tempId },
                                colorHex = tempColor,
                                accentColorHex = tempAccent,
                                hardness = tempHardness,
                                dropItemId = tempDropItem.ifEmpty { tempId }
                            )
                        )
                    }
                    currentSection = ""
                    continue
                }

                if (line == "TOOL_START") {
                    currentSection = "TOOL"
                    tempId = ""
                    tempName = ""
                    tempColor = "#4CAF50"
                    tempToolType = "Pickaxe"
                    tempToolTier = "Diamond"
                    tempEfficiency = 10f
                    tempIngredients.clear()
                    continue
                }

                if (line == "TOOL_END") {
                    if (tempId.isNotEmpty()) {
                        toolsList.add(
                            CustomToolDef(
                                id = tempId,
                                name = tempName.ifEmpty { tempId },
                                type = tempToolType,
                                tier = tempToolTier,
                                colorHex = tempColor,
                                efficiency = tempEfficiency,
                                craftingIngredients = tempIngredients.toMap()
                            )
                        )
                    }
                    currentSection = ""
                    continue
                }

                val lowerLine = line.lowercase()
                val suffix = line.substringAfter(":").trim()
                
                if (currentSection == "BLOCK") {
                    when {
                        lowerLine.startsWith("id:") -> tempId = suffix
                        lowerLine.startsWith("name:") -> tempName = suffix
                        lowerLine.startsWith("color_hex:") -> tempColor = suffix
                        lowerLine.startsWith("accent_hex:") -> tempAccent = suffix
                        lowerLine.startsWith("hardness:") -> tempHardness = suffix.toFloatOrNull() ?: 1.0f
                        lowerLine.startsWith("drop_item:") -> tempDropItem = suffix
                    }
                } else if (currentSection == "TOOL") {
                    when {
                        lowerLine.startsWith("id:") -> tempId = suffix
                        lowerLine.startsWith("name:") -> tempName = suffix
                        lowerLine.startsWith("color_hex:") -> tempColor = suffix
                        lowerLine.startsWith("type:") -> tempToolType = suffix
                        lowerLine.startsWith("tier:") -> tempToolTier = suffix
                        lowerLine.startsWith("efficiency:") -> tempEfficiency = suffix.toFloatOrNull() ?: 10f
                        lowerLine.startsWith("ingredients:") -> {
                            val parts = suffix.split(",")
                            for (p in parts) {
                                val subParts = p.trim().split(":")
                                if (subParts.size == 2) {
                                    val ingName = subParts[0].trim()
                                    val ingCount = subParts[1].trim().toIntOrNull() ?: 1
                                    tempIngredients[ingName] = ingCount
                                }
                            }
                        }
                    }
                }
            }

            return ModDefinition(
                id = modId,
                name = modName,
                description = modDesc,
                blocks = blocksList,
                tools = toolsList,
                isEnabled = true
            )
        }
    }
}

// Active states of Chests, Furnaces, etc. in specific world coordinates
@JsonClass(generateAdapter = true)
data class FurnaceState(
    var inputStack: ItemStack? = null,
    var fuelStack: ItemStack? = null,
    var outputStack: ItemStack? = null,
    var burnTimeRemaining: Int = 0,
    var maxBurnTime: Int = 0,
    var cookTimeProgress: Int = 0,
    var isCooking: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ChestState(
    val slots: MutableList<ItemStack?> = MutableList(27) { null }
)

@JsonClass(generateAdapter = true)
data class PlayerState(
    var x: Float = 50000f, // chunk coordinates
    var y: Float = 15f,
    var velocityX: Float = 0f,
    var velocityY: Float = 0f,
    var isGrounded: Boolean = false,
    var health: Float = 20.0f, // 10 full hearts
    var maxHealth: Float = 20.0f,
    var hunger: Float = 20.0f, // 10 chicken legs
    var activeHotbarIndex: Int = 0,
    val inventory: MutableList<ItemStack?> = MutableList(36) { null } // 0-8 hotbar, 9-35 main inventory
)

/**
 * WorldSave structure houses everything regarding a single world state.
 * It is easily serializable to save states.
 */
@JsonClass(generateAdapter = true)
data class WorldSave(
    val name: String,
    val seed: Long,
    val width: Int = 100000,
    val height: Int = 45,
    val gameMode: String = "Survival", // "Survival", "Creative"
    val worldType: String = "Standard", // "Standard", "Flat", "Mountains"
    val playerState: PlayerState = PlayerState(),
    val worldBlocks: Map<String, String> = emptyMap(), // Key: "x,y", Value: Block ID
    val creationTime: Long = System.currentTimeMillis(),
    val furnaceStates: Map<String, FurnaceState> = emptyMap(), // Key: "x,y"
    val chestStates: Map<String, ChestState> = emptyMap(), // Key: "x,y"
    val enabledModIds: List<String> = emptyList(),
    var currentSkyTime: Float = 2000f // 0 to 24000 ticks like Minecraft! Day starts at 0, dusk at 12000, night at 13000, dawn at 22000
)

/**
 * Global Registry for all Blocks, Items, and Crafting recipes in the game.
 * Automatically initialized with vanilla blocks and dynamically expanded when mods are active.
 */
object GameRegistry {
    private val vanillaBlocks = mutableMapOf<String, BlockType>()
    private val vanillaItems = mutableMapOf<String, ItemType>()
    private val vanillaCraftingRecipes = mutableListOf<CraftingRecipe>()
    private val vanillaSmeltingRecipes = mutableListOf<SmeltingRecipe>()

    // Active block and item registries for the current game session
    val blocks = mutableMapOf<String, BlockType>()
    val items = mutableMapOf<String, ItemType>()
    val craftingRecipes = mutableListOf<CraftingRecipe>()
    val smeltingRecipes = mutableListOf<SmeltingRecipe>()

    init {
        registerVanilla()
        resetToVanilla()
    }

    private fun registerVanilla() {
        // Vanilla Blocks
        registerBlock(vanillaBlocks, BlockType("air", "Air", isSolid = false, isPassable = true, isMineable = false, colorHex = "#00000000"))
        registerBlock(vanillaBlocks, BlockType("grass", "Grass Block", hardness = 0.4f, requiredToolType = ToolType.SHOVEL, dropItemId = "dirt", colorHex = "#855C33", topColorHex = "#5E933B", accentColorHex = "#795530"))
        registerBlock(vanillaBlocks, BlockType("dirt", "Dirt Block", hardness = 0.4f, requiredToolType = ToolType.SHOVEL, dropItemId = "dirt", colorHex = "#866043", accentColorHex = "#66462F"))
        registerBlock(vanillaBlocks, BlockType("stone", "Stone", hardness = 1.2f, requiredToolType = ToolType.PICKAXE, requiredToolTier = 0, dropItemId = "cobblestone", colorHex = "#7F7F7F", accentColorHex = "#606060"))
        registerBlock(vanillaBlocks, BlockType("cobblestone", "Cobblestone", hardness = 1.2f, requiredToolType = ToolType.PICKAXE, requiredToolTier = 0, dropItemId = "cobblestone", colorHex = "#737373", accentColorHex = "#535353"))
        registerBlock(vanillaBlocks, BlockType("oak_log", "Oak Wood Log", hardness = 0.8f, requiredToolType = ToolType.AXE, dropItemId = "oak_log", colorHex = "#705435", accentColorHex = "#4A321A", canBurn = true))
        registerBlock(vanillaBlocks, BlockType("oak_leaves", "Oak Leaves", hardness = 0.1f, isSolid = true, isPassable = true, dropItemId = "apple", dropCount = 1, colorHex = "#326227", accentColorHex = "#1D4713", canBurn = true))
        registerBlock(vanillaBlocks, BlockType("oak_planks", "Oak Planks", hardness = 0.6f, requiredToolType = ToolType.AXE, dropItemId = "oak_planks", colorHex = "#9C7C51", accentColorHex = "#755630", canBurn = true))
        registerBlock(vanillaBlocks, BlockType("crafting_table", "Crafting Table", hardness = 1.0f, requiredToolType = ToolType.AXE, dropItemId = "crafting_table", colorHex = "#7F5A39", topColorHex = "#B8915E", accentColorHex = "#493220"))
        registerBlock(vanillaBlocks, BlockType("furnace", "Furnace", hardness = 1.5f, requiredToolType = ToolType.PICKAXE, dropItemId = "furnace", colorHex = "#606060", topColorHex = "#3A3A3A", accentColorHex = "#2B2B2B"))
        registerBlock(vanillaBlocks, BlockType("chest", "Wooden Chest", hardness = 1.0f, requiredToolType = ToolType.AXE, dropItemId = "chest", colorHex = "#7F522A", accentColorHex = "#452C16"))
        registerBlock(vanillaBlocks, BlockType("nano_banana_2", "Nano Banana 2 Block", hardness = 0.5f, dropItemId = "nano_banana_2_item", colorHex = "#FFE57F", topColorHex = "#FFF59D", accentColorHex = "#FFC107"))
        
        // 1.21.1 Tricky Trials Blocks
        registerBlock(vanillaBlocks, BlockType("crafter", "Crafter", hardness = 1.5f, requiredToolType = ToolType.PICKAXE, dropItemId = "crafter", colorHex = "#4D4D4D", topColorHex = "#D94A1A", accentColorHex = "#A67A65"))
        registerBlock(vanillaBlocks, BlockType("trial_spawner", "Trial Spawner", hardness = 5.0f, requiredToolType = ToolType.PICKAXE, dropItemId = "trial_key", colorHex = "#514B5C", topColorHex = "#FF9933", accentColorHex = "#262130"))
        registerBlock(vanillaBlocks, BlockType("vault", "Vault", hardness = 5.0f, requiredToolType = ToolType.PICKAXE, dropItemId = "diamond", colorHex = "#594F4C", topColorHex = "#DEB553", accentColorHex = "#FFD700"))
        registerBlock(vanillaBlocks, BlockType("heavy_core", "Heavy Core", hardness = 2.5f, requiredToolType = ToolType.PICKAXE, dropItemId = "heavy_core", colorHex = "#2F343D", topColorHex = "#4F5B66", accentColorHex = "#1B1E22"))
        registerBlock(vanillaBlocks, BlockType("copper_bulb", "Copper Bulb", hardness = 1.0f, requiredToolType = ToolType.PICKAXE, dropItemId = "copper_bulb", colorHex = "#D97852", topColorHex = "#FFB84D", accentColorHex = "#8A3C22"))
        registerBlock(vanillaBlocks, BlockType("copper_grate", "Copper Grate", hardness = 1.0f, requiredToolType = ToolType.PICKAXE, dropItemId = "copper_grate", colorHex = "#CC704B", accentColorHex = "#592512"))
        registerBlock(vanillaBlocks, BlockType("chiseled_copper", "Chiseled Copper", hardness = 1.2f, requiredToolType = ToolType.PICKAXE, dropItemId = "chiseled_copper", colorHex = "#E67D53", accentColorHex = "#A64E29"))
        registerBlock(vanillaBlocks, BlockType("copper_block", "Copper Block", hardness = 1.2f, requiredToolType = ToolType.PICKAXE, dropItemId = "copper_block", colorHex = "#D36A44", accentColorHex = "#B35A37"))
        registerBlock(vanillaBlocks, BlockType("copper_ore", "Copper Ore", hardness = 1.4f, requiredToolType = ToolType.PICKAXE, requiredToolTier = 1, dropItemId = "raw_copper", colorHex = "#8A766F", accentColorHex = "#54B28D"))
        registerBlock(vanillaBlocks, BlockType("tuff_bricks", "Tuff Bricks", hardness = 1.5f, requiredToolType = ToolType.PICKAXE, dropItemId = "tuff_bricks", colorHex = "#676B6D", accentColorHex = "#434647"))
        registerBlock(vanillaBlocks, BlockType("chiseled_tuff", "Chiseled Tuff", hardness = 1.5f, requiredToolType = ToolType.PICKAXE, dropItemId = "chiseled_tuff", colorHex = "#5F6264", accentColorHex = "#383B3C"))
        
        // 1.13 - 1.20 Historic Blocks
        registerBlock(vanillaBlocks, BlockType("prismarine", "Prismarine Block", hardness = 1.3f, requiredToolType = ToolType.PICKAXE, dropItemId = "prismarine", colorHex = "#315D5F", accentColorHex = "#1D3F41"))
        registerBlock(vanillaBlocks, BlockType("sea_lantern", "Sea Lantern", hardness = 0.3f, dropItemId = "prismarine", colorHex = "#B2EBF2", accentColorHex = "#E0F7FA"))
        registerBlock(vanillaBlocks, BlockType("barrel", "Barrel", hardness = 1.1f, requiredToolType = ToolType.AXE, dropItemId = "barrel", colorHex = "#8E6037", topColorHex = "#6E4521"))
        registerBlock(vanillaBlocks, BlockType("honeycomb_block", "Honeycomb Block", hardness = 0.5f, dropItemId = "honeycomb_block", colorHex = "#E59B12"))
        registerBlock(vanillaBlocks, BlockType("honey_block", "Honey Block", hardness = 0.1f, isSolid = true, isPassable = true, dropItemId = "honey_block", colorHex = "#FFB300"))
        registerBlock(vanillaBlocks, BlockType("beehive", "Beehive", hardness = 0.6f, requiredToolType = ToolType.AXE, dropItemId = "beehive", colorHex = "#FFA726", topColorHex = "#FB8C00"))
        registerBlock(vanillaBlocks, BlockType("netherrack", "Netherrack", hardness = 0.4f, requiredToolType = ToolType.PICKAXE, dropItemId = "netherrack", colorHex = "#661414", accentColorHex = "#400606"))
        registerBlock(vanillaBlocks, BlockType("blackstone", "Blackstone", hardness = 1.5f, requiredToolType = ToolType.PICKAXE, dropItemId = "blackstone", colorHex = "#232026", accentColorHex = "#141217"))
        registerBlock(vanillaBlocks, BlockType("crying_obsidian", "Crying Obsidian", hardness = 10.0f, requiredToolType = ToolType.PICKAXE, requiredToolTier = 4, dropItemId = "crying_obsidian", colorHex = "#1C0D30", accentColorHex = "#8E24AA"))
        registerBlock(vanillaBlocks, BlockType("netherite_block", "Netherite Block", hardness = 15.0f, requiredToolType = ToolType.PICKAXE, requiredToolTier = 4, dropItemId = "netherite_block", colorHex = "#312F35", accentColorHex = "#1C1B1F"))
        registerBlock(vanillaBlocks, BlockType("ancient_debris", "Ancient Debris", hardness = 8.5f, requiredToolType = ToolType.PICKAXE, requiredToolTier = 4, dropItemId = "ancient_debris", colorHex = "#4C3834", topColorHex = "#5E4742"))
        registerBlock(vanillaBlocks, BlockType("deepslate", "Deepslate", hardness = 2.0f, requiredToolType = ToolType.PICKAXE, requiredToolTier = 2, dropItemId = "deepslate", colorHex = "#3F4042", accentColorHex = "#232426"))
        registerBlock(vanillaBlocks, BlockType("sculk", "Sculk Catalyst", hardness = 0.6f, requiredToolType = ToolType.HOE, dropItemId = "sculk", colorHex = "#0C1D24", accentColorHex = "#21EDC4"))
        registerBlock(vanillaBlocks, BlockType("sculk_sensor", "Sculk Sensor", hardness = 0.6f, requiredToolType = ToolType.HOE, dropItemId = "sculk_sensor", colorHex = "#042D3B", topColorHex = "#0DF7F3"))
        registerBlock(vanillaBlocks, BlockType("amethyst_block", "Block of Amethyst", hardness = 1.5f, requiredToolType = ToolType.PICKAXE, dropItemId = "amethyst_shard", colorHex = "#9C27B0", accentColorHex = "#E040FB"))
        registerBlock(vanillaBlocks, BlockType("froglight", "Froglight Block", hardness = 0.3f, dropItemId = "froglight", colorHex = "#FFF9C4", accentColorHex = "#FFE082"))
        registerBlock(vanillaBlocks, BlockType("mud", "Mud Block", hardness = 0.3f, requiredToolType = ToolType.SHOVEL, dropItemId = "mud", colorHex = "#42372E"))
        registerBlock(vanillaBlocks, BlockType("mangrove_log", "Mangrove Log", hardness = 0.8f, requiredToolType = ToolType.AXE, dropItemId = "mangrove_log", colorHex = "#52251B", accentColorHex = "#2E110A"))
        registerBlock(vanillaBlocks, BlockType("cherry_log", "Cherry Log", hardness = 0.8f, requiredToolType = ToolType.AXE, dropItemId = "cherry_log", colorHex = "#2E1E1E", topColorHex = "#DCAEAA"))
        registerBlock(vanillaBlocks, BlockType("cherry_leaves", "Cherry Leaves", hardness = 0.1f, isSolid = true, isPassable = true, dropItemId = "cherry_leaves", colorHex = "#FFB7C5", accentColorHex = "#FF8093"))
        
        // Classic Update & Nether Blocks
        registerBlock(vanillaBlocks, BlockType("soul_sand", "Soul Sand", hardness = 0.5f, requiredToolType = ToolType.SHOVEL, dropItemId = "soul_sand", colorHex = "#5C3E2F", accentColorHex = "#32251D"))
        registerBlock(vanillaBlocks, BlockType("magma_block", "Magma Block", hardness = 0.5f, requiredToolType = ToolType.PICKAXE, dropItemId = "magma_block", colorHex = "#8F3F1F", accentColorHex = "#511D12"))
        registerBlock(vanillaBlocks, BlockType("sponge", "Sponge", hardness = 0.6f, dropItemId = "sponge", colorHex = "#CDCD4C", accentColorHex = "#9E9E2F"))
        registerBlock(vanillaBlocks, BlockType("purpur_block", "Purpur Block", hardness = 1.5f, requiredToolType = ToolType.PICKAXE, dropItemId = "purpur_block", colorHex = "#B886B8", accentColorHex = "#8C5C8C"))
        registerBlock(vanillaBlocks, BlockType("chorus_flower", "Chorus Flower", hardness = 0.8f, requiredToolType = ToolType.AXE, dropItemId = "chorus_flower", colorHex = "#9C5C9C", accentColorHex = "#6E356E"))
        registerBlock(vanillaBlocks, BlockType("sand", "Sand", hardness = 0.5f, requiredToolType = ToolType.SHOVEL, dropItemId = "sand", colorHex = "#DBD2A0", accentColorHex = "#C6B57E"))
        registerBlock(vanillaBlocks, BlockType("sandstone", "Sandstone", hardness = 0.8f, requiredToolType = ToolType.PICKAXE, dropItemId = "sandstone", colorHex = "#C6B57E", accentColorHex = "#98845A"))
        registerBlock(vanillaBlocks, BlockType("end_stone", "End Stone", hardness = 3.0f, requiredToolType = ToolType.PICKAXE, dropItemId = "end_stone", colorHex = "#DCDEB1", accentColorHex = "#A2A481"))
        
        // Classic Essential Blocks (1.0)
        registerBlock(vanillaBlocks, BlockType("glowstone", "Glowstone Block", hardness = 0.3f, dropItemId = "glowstone", colorHex = "#FFEB8A", accentColorHex = "#E6C15C"))
        registerBlock(vanillaBlocks, BlockType("redstone_block", "Block of Redstone", hardness = 1.0f, requiredToolType = ToolType.PICKAXE, dropItemId = "redstone_block", colorHex = "#D50000", accentColorHex = "#FF1744"))
        registerBlock(vanillaBlocks, BlockType("lapis_block", "Lapis Lazuli Block", hardness = 1.5f, requiredToolType = ToolType.PICKAXE, dropItemId = "lapis_block", colorHex = "#1A237E", accentColorHex = "#283593"))
        registerBlock(vanillaBlocks, BlockType("emerald_block", "Block of Emerald", hardness = 1.5f, requiredToolType = ToolType.PICKAXE, dropItemId = "emerald_block", colorHex = "#00C853", accentColorHex = "#69F0AE"))
        registerBlock(vanillaBlocks, BlockType("gold_block", "Block of Gold", hardness = 1.5f, requiredToolType = ToolType.PICKAXE, requiredToolTier = 2, dropItemId = "gold_block", colorHex = "#FFD700", accentColorHex = "#FFEA00"))
        registerBlock(vanillaBlocks, BlockType("iron_block", "Block of Iron", hardness = 1.5f, requiredToolType = ToolType.PICKAXE, requiredToolTier = 1, dropItemId = "iron_block", colorHex = "#E2E2E2", accentColorHex = "#C4C4C4"))
        registerBlock(vanillaBlocks, BlockType("diamond_block", "Block of Diamond", hardness = 2.0f, requiredToolType = ToolType.PICKAXE, requiredToolTier = 2, dropItemId = "diamond_block", colorHex = "#4DEEC7", accentColorHex = "#80F6DC"))
        
        // Ores
        registerBlock(vanillaBlocks, BlockType("coal_ore", "Coal Ore", hardness = 1.4f, requiredToolType = ToolType.PICKAXE, requiredToolTier = 1, dropItemId = "coal", colorHex = "#747474", accentColorHex = "#1D1D1D"))
        registerBlock(vanillaBlocks, BlockType("iron_ore", "Iron Ore", hardness = 1.6f, requiredToolType = ToolType.PICKAXE, requiredToolTier = 2, dropItemId = "raw_iron", colorHex = "#777777", accentColorHex = "#D09476"))
        registerBlock(vanillaBlocks, BlockType("diamond_ore", "Diamond Ore", hardness = 2.0f, requiredToolType = ToolType.PICKAXE, requiredToolTier = 3, dropItemId = "diamond", colorHex = "#707070", accentColorHex = "#4DDEEC"))
        registerBlock(vanillaBlocks, BlockType("obsidian", "Obsidian", hardness = 10.0f, requiredToolType = ToolType.PICKAXE, requiredToolTier = 4, dropItemId = "obsidian", colorHex = "#161124", accentColorHex = "#231B3A"))
        registerBlock(vanillaBlocks, BlockType("bedrock", "Bedrock", isMineable = false, hardness = -1.0f, colorHex = "#323232", accentColorHex = "#1F1F1F"))

        // Vanilla Items & Blocks as Items
        // Blocks as Items first
        vanillaBlocks.forEach { (id, block) ->
            if (id != "air") {
                registerItem(vanillaItems, ItemType(id = id, name = block.name, isBlock = true, blockId = id, colorHex = block.colorHex, description = "Placeable block"))
            }
        }
        
        // Standard Materials
        registerItem(vanillaItems, ItemType("coal", "Coal", colorHex = "#222222", description = "High-efficiency fuel"))
        registerItem(vanillaItems, ItemType("raw_iron", "Raw Iron", colorHex = "#D89E7F", description = "Smelt into ingots"))
        registerItem(vanillaItems, ItemType("iron_ingot", "Iron Ingot", colorHex = "#E5E5E5", description = "Durable forge material"))
        registerItem(vanillaItems, ItemType("diamond", "Diamond", colorHex = "#4FE5E1", description = "Rare celestial gem"))
        registerItem(vanillaItems, ItemType("stick", "Stick", colorHex = "#A05A2C", description = "Crafting handles"))
        registerItem(vanillaItems, ItemType("apple", "Apple", colorHex = "#E52C2C", description = "Restores +4 Health"))
        registerItem(vanillaItems, ItemType("nano_banana_2_item", "Nano Banana 2", colorHex = "#FFD700", description = "Nano-engineered legendary fruit version 2. Direct core healing. Restores max nutrition."))

        // 1.21.1 Tricky Trials Items
        registerItem(vanillaItems, ItemType("mace", "Mace", isTool = true, toolType = ToolType.SWORD, toolTier = ToolTier.DIAMOND, colorHex = "#AF937D", description = "A heavy core on a breeze rod. Crushes foes from high ground! +8 Damage"))
        registerItem(vanillaItems, ItemType("trial_key", "Trial Key", colorHex = "#FFBB33", description = "Unlocks standard Vault blocks for massive loot"))
        registerItem(vanillaItems, ItemType("ominous_trial_key", "Ominous Trial Key", colorHex = "#CE93D8", description = "Unlocks highly dangerous Ominous Vaults with special treasures"))
        registerItem(vanillaItems, ItemType("breeze_rod", "Breeze Rod", colorHex = "#9BE7FF", description = "A core element of the Breeze, used to craft the Mace and Wind Charges"))
        registerItem(vanillaItems, ItemType("wind_charge", "Wind Charge", colorHex = "#E1F5FE", description = "Throws a compressed ball of air to launch yourself or entities!"))
        registerItem(vanillaItems, ItemType("ominous_bottle", "Ominous Bottle", colorHex = "#BA68C8", description = "Drink to gain Bad Omen. Initiates Ominous Trial chambers for high tier loot!"))
        registerItem(vanillaItems, ItemType("raw_copper", "Raw Copper", colorHex = "#D37D5D", description = "Smelt into copper ingots"))
        registerItem(vanillaItems, ItemType("copper_ingot", "Copper Ingot", colorHex = "#E59E81", description = "Used to forge copper blocks, bulbs, and grates"))

        // Pickaxes
        registerItem(vanillaItems, ItemType("netherite_pickaxe", "Netherite Pickaxe", isTool = true, toolType = ToolType.PICKAXE, toolTier = ToolTier.NETHERITE, colorHex = "#312C2C", description = "The ultimate mining tool. Unmatched swiftness."))
        registerItem(vanillaItems, ItemType("netherite_sword", "Netherite Sword", isTool = true, toolType = ToolType.SWORD, toolTier = ToolTier.NETHERITE, colorHex = "#312C2C", description = "Deals extreme damage. Forged in nether fires. +9 Damage"))
        
        // Classic update items
        registerItem(vanillaItems, ItemType("heart_of_the_sea", "Heart of the Sea", colorHex = "#A2D2FF", description = "Rare nautical beacon of the sea"))
        registerItem(vanillaItems, ItemType("nautilus_shell", "Nautilus Shell", colorHex = "#F5CAC3", description = "Sought-after ocean shell casing"))
        registerItem(vanillaItems, ItemType("crossbow", "Crossbow", isTool = true, toolType = ToolType.NONE, colorHex = "#B38B6D", description = "Ranged defense weapon style"))
        registerItem(vanillaItems, ItemType("honey_bottle", "Honey Bottle", colorHex = "#FFB703", description = "Sticky sweet honey. Purges poisons!"))
        registerItem(vanillaItems, ItemType("amethyst_shard", "Amethyst Shard", colorHex = "#C77DFF", description = "Crystalline amethyst chunk"))
        registerItem(vanillaItems, ItemType("brush", "Archaeological Brush", isTool = true, toolType = ToolType.NONE, colorHex = "#DDBDF1", description = "Used to sweep gravel and sand residues"))
        registerItem(vanillaItems, ItemType("netherite_ingot", "Netherite Ingot", colorHex = "#3C3636", description = "Heavy heat-resistant alloy ingot"))

        registerItem(vanillaItems, ItemType("wooden_pickaxe", "Wooden Pickaxe", isTool = true, toolType = ToolType.PICKAXE, toolTier = ToolTier.WOOD, colorHex = "#9C7C51", description = "Mines raw stone"))
        registerItem(vanillaItems, ItemType("stone_pickaxe", "Stone Pickaxe", isTool = true, toolType = ToolType.PICKAXE, toolTier = ToolTier.STONE, colorHex = "#7C7C7C", description = "Mines iron ores"))
        registerItem(vanillaItems, ItemType("iron_pickaxe", "Iron Pickaxe", isTool = true, toolType = ToolType.PICKAXE, toolTier = ToolTier.IRON, colorHex = "#E5E5E5", description = "Mines diamond ores"))
        registerItem(vanillaItems, ItemType("diamond_pickaxe", "Diamond Pickaxe", isTool = true, toolType = ToolType.PICKAXE, toolTier = ToolTier.DIAMOND, colorHex = "#4FE5E1", description = "Shatters obsidian instantly"))

        // Swords
        registerItem(vanillaItems, ItemType("wooden_sword", "Wooden Sword", isTool = true, toolType = ToolType.SWORD, toolTier = ToolTier.WOOD, colorHex = "#9C7C51", description = "Deals moderate damage"))
        registerItem(vanillaItems, ItemType("stone_sword", "Stone Sword", isTool = true, toolType = ToolType.SWORD, toolTier = ToolTier.STONE, colorHex = "#7C7C7C", description = "Deals decent damage"))
        registerItem(vanillaItems, ItemType("iron_sword", "Iron Sword", isTool = true, toolType = ToolType.SWORD, toolTier = ToolTier.IRON, colorHex = "#E5E5E5", description = "Slices through blockades"))
        registerItem(vanillaItems, ItemType("diamond_sword", "Diamond Sword", isTool = true, toolType = ToolType.SWORD, toolTier = ToolTier.DIAMOND, colorHex = "#4FE5E1", description = "Relic of combat dominance"))

        // Shovels
        registerItem(vanillaItems, ItemType("wooden_shovel", "Wooden Shovel", isTool = true, toolType = ToolType.SHOVEL, toolTier = ToolTier.WOOD, colorHex = "#9C7C51"))
        registerItem(vanillaItems, ItemType("stone_shovel", "Stone Shovel", isTool = true, toolType = ToolType.SHOVEL, toolTier = ToolTier.STONE, colorHex = "#7C7C7C"))
        registerItem(vanillaItems, ItemType("iron_shovel", "Iron Shovel", isTool = true, toolType = ToolType.SHOVEL, toolTier = ToolTier.IRON, colorHex = "#E5E5E5"))
        registerItem(vanillaItems, ItemType("diamond_shovel", "Diamond Shovel", isTool = true, toolType = ToolType.SHOVEL, toolTier = ToolTier.DIAMOND, colorHex = "#4FE5E1"))

        // Axes
        registerItem(vanillaItems, ItemType("wooden_axe", "Wooden Axe", isTool = true, toolType = ToolType.AXE, toolTier = ToolTier.WOOD, colorHex = "#9C7C51"))
        registerItem(vanillaItems, ItemType("stone_axe", "Stone Axe", isTool = true, toolType = ToolType.AXE, toolTier = ToolTier.STONE, colorHex = "#7C7C7C"))
        registerItem(vanillaItems, ItemType("iron_axe", "Iron Axe", isTool = true, toolType = ToolType.AXE, toolTier = ToolTier.IRON, colorHex = "#E5E5E5"))
        registerItem(vanillaItems, ItemType("diamond_axe", "Diamond Axe", isTool = true, toolType = ToolType.AXE, toolTier = ToolTier.DIAMOND, colorHex = "#4FE5E1"))

        // Vanilla Crafting Recipes
        // Basic crafting
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("oak_planks_from_log", "oak_planks", 4, mapOf("oak_log" to 1)))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("sticks", "stick", 4, mapOf("oak_planks" to 2)))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("crafting_table", "crafting_table", 1, mapOf("oak_planks" to 4)))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("furnace", "furnace", 1, mapOf("cobblestone" to 8), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("chest", "chest", 1, mapOf("oak_planks" to 8), requiresCraftingTable = true))

        // Tools recipes (requires crafting table)
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("wood_pick", "wooden_pickaxe", 1, mapOf("oak_planks" to 3, "stick" to 2), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("stone_pick", "stone_pickaxe", 1, mapOf("cobblestone" to 3, "stick" to 2), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("iron_pick", "iron_pickaxe", 1, mapOf("iron_ingot" to 3, "stick" to 2), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("diam_pick", "diamond_pickaxe", 1, mapOf("diamond" to 3, "stick" to 2), requiresCraftingTable = true))

        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("wood_sword", "wooden_sword", 1, mapOf("oak_planks" to 2, "stick" to 1), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("stone_sword", "stone_sword", 1, mapOf("cobblestone" to 2, "stick" to 1), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("iron_sword", "iron_sword", 1, mapOf("iron_ingot" to 2, "stick" to 1), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("diam_sword", "diamond_sword", 1, mapOf("diamond" to 2, "stick" to 1), requiresCraftingTable = true))

        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("wood_axe", "wooden_axe", 1, mapOf("oak_planks" to 3, "stick" to 2), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("stone_axe", "stone_axe", 1, mapOf("cobblestone" to 3, "stick" to 2), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("iron_axe", "iron_axe", 1, mapOf("iron_ingot" to 3, "stick" to 2), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("diam_axe", "diamond_axe", 1, mapOf("diamond" to 3, "stick" to 2), requiresCraftingTable = true))

        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("wood_shov", "wooden_shovel", 1, mapOf("oak_planks" to 1, "stick" to 2), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("stone_shov", "stone_shovel", 1, mapOf("cobblestone" to 1, "stick" to 2), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("iron_shov", "iron_shovel", 1, mapOf("iron_ingot" to 1, "stick" to 2), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("diam_shov", "diamond_shovel", 1, mapOf("diamond" to 1, "stick" to 2), requiresCraftingTable = true))

        // Nano Banana 2 Recipes
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("nano_banana_2_craft", "nano_banana_2_item", 4, mapOf("apple" to 1, "diamond" to 1)))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("nano_banana_2_block_craft", "nano_banana_2", 1, mapOf("nano_banana_2_item" to 4)))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("nano_banana_2_unpack", "nano_banana_2_item", 4, mapOf("nano_banana_2" to 1)))

        // 1.21.1 Tricky Trials Crafting Recipes
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("mace", "mace", 1, mapOf("heavy_core" to 1, "breeze_rod" to 1), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("wind_charge", "wind_charge", 4, mapOf("breeze_rod" to 1), requiresCraftingTable = false))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("crafter", "crafter", 1, mapOf("iron_ingot" to 5, "crafting_table" to 1, "stone" to 2, "coal" to 1), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("copper_block", "copper_block", 1, mapOf("copper_ingot" to 4), requiresCraftingTable = false))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("copper_bulb", "copper_bulb", 1, mapOf("copper_ingot" to 3, "stone" to 1, "coal" to 1), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("copper_grate", "copper_grate", 4, mapOf("copper_ingot" to 4), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("chiseled_copper", "chiseled_copper", 1, mapOf("copper_block" to 2), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("tuff_bricks", "tuff_bricks", 4, mapOf("stone" to 4), requiresCraftingTable = true))
        registerRecipe(vanillaCraftingRecipes, CraftingRecipe("chiseled_tuff", "chiseled_tuff", 1, mapOf("tuff_bricks" to 2), requiresCraftingTable = true))

        // Vanilla Smelting Recipes
        registerSmelting(vanillaSmeltingRecipes, SmeltingRecipe("iron_ore", "iron_ingot"))
        registerSmelting(vanillaSmeltingRecipes, SmeltingRecipe("cobblestone", "stone"))
        registerSmelting(vanillaSmeltingRecipes, SmeltingRecipe("copper_ore", "copper_ingot"))
        registerSmelting(vanillaSmeltingRecipes, SmeltingRecipe("raw_copper", "copper_ingot"))
    }

    private fun registerBlock(registry: MutableMap<String, BlockType>, block: BlockType) {
        registry[block.id] = block
    }

    private fun registerItem(registry: MutableMap<String, ItemType>, item: ItemType) {
        registry[item.id] = item
    }

    private fun registerRecipe(registry: MutableList<CraftingRecipe>, recipe: CraftingRecipe) {
        registry.add(recipe)
    }

    private fun registerSmelting(registry: MutableList<SmeltingRecipe>, recipe: SmeltingRecipe) {
        registry.add(recipe)
    }

    fun resetToVanilla() {
        blocks.clear()
        blocks.putAll(vanillaBlocks)
        
        items.clear()
        items.putAll(vanillaItems)
        
        craftingRecipes.clear()
        craftingRecipes.addAll(vanillaCraftingRecipes)
        
        smeltingRecipes.clear()
        smeltingRecipes.addAll(vanillaSmeltingRecipes)
    }

    /**
     * Integrates custom mod definitions into the game's active session registry.
     */
    fun registerMod(mod: ModDefinition) {
        if (!mod.isEnabled) return
        
        if (mod.id.contains("ic2")) {
            // Register all IC2 high tech item parts!
            registerItem(items, ItemType("mod_ic2_lead_ingot", "Lead Ingot", colorHex = "#5C6470", description = "Heavy radioactive radiation shields"))
            registerItem(items, ItemType("mod_ic2_tin_ingot", "Tin Ingot", colorHex = "#D8DADE", description = "Lightweight conductible metal"))
            registerItem(items, ItemType("mod_ic2_raw_tin", "Raw Tin", colorHex = "#CFD8DC", description = "Smelt into tin ingots"))
            registerItem(items, ItemType("mod_ic2_bronze_ingot", "Bronze Ingot", colorHex = "#CC8833", description = "Tough alloy of copper and tin"))
            registerItem(items, ItemType("mod_ic2_copper_plate", "Copper Plate", colorHex = "#D87D56", description = "Pressed copper sheets for mechanics"))
            registerItem(items, ItemType("mod_ic2_re_battery", "RE-Battery", colorHex = "#556B2F", description = "Stores 10,000 EU of portable electric energy"))
            registerItem(items, ItemType("mod_ic2_uranium_rod", "Uranium Fuel Rod", colorHex = "#4FFF5D", description = "Generates immense heat and nuclear power inside IC2 reactors"))
            registerItem(items, ItemType("mod_ic2_rubber", "Industrial Rubber", colorHex = "#2E2E2E", description = "Used for cable insulation coating and machine housings"))
            
            // Add custom recipes for these high tech elements!
            craftingRecipes.add(CraftingRecipe("craft_ic2_bronze", "mod_ic2_bronze_ingot", 4, mapOf("copper_block" to 1, "mod_ic2_tin_ore" to 1), true))
            craftingRecipes.add(CraftingRecipe("craft_ic2_battery", "mod_ic2_re_battery", 1, mapOf("mod_ic2_tin_ingot" to 4, "redstone_block" to 1), true))
            craftingRecipes.add(CraftingRecipe("craft_ic2_cable_item", "mod_ic2_copper_cable", 6, mapOf("mod_ic2_copper_plate" to 3, "mod_ic2_rubber" to 6), true))
        }

        // 1. Register customized Blocks
        for (cb in mod.blocks) {
            val customBlock = BlockType(
                id = cb.id,
                name = cb.name,
                isSolid = true,
                isPassable = false,
                hardness = cb.hardness,
                requiredToolType = ToolType.PICKAXE, // custom modded blocks generally mineable by pickaxe
                requiredToolTier = 0,
                dropItemId = cb.dropItemId,
                colorHex = cb.colorHex,
                accentColorHex = cb.accentColorHex
            )
            blocks[cb.id] = customBlock
            
            // Also register Block itself as a placeable Item
            items[cb.id] = ItemType(
                id = cb.id,
                name = cb.name,
                isBlock = true,
                blockId = cb.id,
                colorHex = cb.colorHex,
                description = "Mod Block: ${mod.name}"
            )
            
            // Register Crafting recipe for custom block if specified
            if (cb.craftingIngredients.isNotEmpty()) {
                craftingRecipes.add(
                    CraftingRecipe(
                        id = "craft_${cb.id}",
                        resultItemId = cb.id,
                        resultCount = 1,
                        ingredients = cb.craftingIngredients,
                        requiresCraftingTable = true
                    )
                )
            }
            
            // Register Smelting recipe if specified
            cb.smeltingResultItemId?.let { result ->
                // First ensure output item is registered, if it matches standard mod items
                smeltingRecipes.add(SmartSmeltingRecipe(cb.id, result))
            }
        }

        // 2. Register customized Tools / Swords
        for (ct in mod.tools) {
            val mappedToolType = when (ct.type.lowercase()) {
                "pickaxe" -> ToolType.PICKAXE
                "sword" -> ToolType.SWORD
                "axe" -> ToolType.AXE
                "shovel" -> ToolType.SHOVEL
                else -> ToolType.NONE
            }
            
            val mappedTier = when (ct.tier.lowercase()) {
                "wood" -> ToolTier.WOOD
                "stone" -> ToolTier.STONE
                "iron" -> ToolTier.IRON
                "diamond" -> ToolTier.DIAMOND
                else -> ToolTier.DIAMOND // God/Super Tier uses high speed diamond tier logic
            }

            // Create customizable sub-item
            val customTool = ItemType(
                id = ct.id,
                name = ct.name,
                isBlock = false,
                isTool = true,
                toolType = mappedToolType,
                toolTier = mappedTier,
                efficiency = ct.efficiency,
                colorHex = ct.colorHex,
                description = "Mod Tool: ${mod.name}. Speed: ${ct.efficiency}x"
            )
            items[ct.id] = customTool

            // Register craft recipe if ingredients set
            if (ct.craftingIngredients.isNotEmpty()) {
                craftingRecipes.add(
                    CraftingRecipe(
                        id = "craft_${ct.id}",
                        resultItemId = ct.id,
                        resultCount = 1,
                        ingredients = ct.craftingIngredients,
                        requiresCraftingTable = true
                    )
                )
            }
        }
    }

    private fun SmartSmeltingRecipe(input: String, output: String) : SmeltingRecipe {
        // Automatically make sure the smelting output is some sort of registered item.
        // If output doesn't exist, register a placeholder gem automatically so it never crashes!
        if (!items.containsKey(output)) {
            val formattedName = output.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            items[output] = ItemType(
                id = output,
                name = formattedName,
                colorHex = "#FF007F", // radiant pink placeholder
                description = "Mod Smelting Output"
            )
        }
        return SmeltingRecipe(input, output)
    }

    /**
     * Scans active mod registries or craft inventories to see if block can be crafted.
     */
    fun findRecipe(invMatrix: Map<String, Int>): CraftingRecipe? {
        return craftingRecipes.find { recipe ->
            recipe.ingredients == invMatrix
        }
    }
}
