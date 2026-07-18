package com.example.game

object BlockRegistry {
    const val AIR: Byte = 0
    const val DIRT: Byte = 1
    const val GRASS: Byte = 2
    const val STONE: Byte = 3
    const val SAND: Byte = 4
    const val WATER: Byte = 5
    const val LAVA: Byte = 6
    const val WOOD: Byte = 7
    const val LEAVES: Byte = 8
    const val PLANKS: Byte = 9
    const val GLASS: Byte = 10
    const val COBBLESTONE: Byte = 11
    const val BEDROCK: Byte = 12
    const val COAL_ORE: Byte = 13
    const val IRON_ORE: Byte = 14
    const val GOLD_ORE: Byte = 15
    const val DIAMOND_ORE: Byte = 16
    const val ROSE: Byte = 17
    const val DANDELION: Byte = 18
    const val TALL_GRASS: Byte = 19
    const val CACTUS: Byte = 20
    const val SNOW: Byte = 21
    const val ICE: Byte = 22
    const val RED_SAND: Byte = 23
    const val BASALT: Byte = 24
    const val JUNGLE_WOOD: Byte = 25
    const val JUNGLE_LEAVES: Byte = 26
    const val OBSIDIAN: Byte = 27
    const val GLOWSTONE: Byte = 28

    // Tools and items as placeholder blocks in inventory (won't be placed in world as blocks)
    const val WOODEN_PICKAXE: Byte = 100
    const val STONE_PICKAXE: Byte = 101
    const val IRON_PICKAXE: Byte = 102
    const val DIAMOND_PICKAXE: Byte = 103
    const val WOODEN_SWORD: Byte = 104
    const val STONE_SWORD: Byte = 105
    const val IRON_SWORD: Byte = 106
    const val DIAMOND_SWORD: Byte = 107
    const val COAL: Byte = 108
    const val IRON_INGOT: Byte = 109
    const val GOLD_INGOT: Byte = 110
    const val DIAMOND: Byte = 111
    const val GOLDEN_APPLE: Byte = 112
    const val POTION_HEALING: Byte = 113

    fun isItem(type: Byte): Boolean {
        return type >= 100
    }

    fun isSolid(type: Byte): Boolean {
        return type != AIR && type != WATER && type != LAVA && type != ROSE && type != DANDELION && type != TALL_GRASS && !isItem(type)
    }

    fun isTransparent(type: Byte): Boolean {
        return type == AIR || type == WATER || type == GLASS || type == LEAVES || type == ROSE || type == DANDELION || type == TALL_GRASS || type == CACTUS || type == JUNGLE_LEAVES || type == ICE
    }

    fun getTextureUV(type: Byte, face: Int): Pair<Int, Int> {
        return when (type) {
            DIRT -> 1 to 0
            GRASS -> {
                when (face) {
                    0 -> 0 to 0 // top
                    1 -> 1 to 0 // bottom
                    else -> 2 to 0 // sides
                }
            }
            STONE -> 3 to 0
            SAND -> 4 to 0
            WATER -> 13 to 12 // Using generic water-like coords (assuming terrain.png)
            LAVA -> 13 to 14
            WOOD -> {
                when (face) {
                    0, 1 -> 5 to 1 // top/bottom
                    else -> 4 to 1 // sides
                }
            }
            LEAVES -> 4 to 3
            PLANKS -> 5 to 0 // 5 to 0 is planks
            GLASS -> 1 to 3
            COBBLESTONE -> 0 to 1
            BEDROCK -> 1 to 1
            COAL_ORE -> 2 to 2
            IRON_ORE -> 1 to 2
            GOLD_ORE -> 0 to 2
            DIAMOND_ORE -> 2 to 3
            ROSE -> 12 to 0
            DANDELION -> 13 to 0
            TALL_GRASS -> 7 to 2
            CACTUS -> {
                when (face) {
                    0, 1 -> 5 to 4
                    else -> 6 to 4
                }
            }
            SNOW -> 2 to 4
            ICE -> 3 to 4
            RED_SAND -> 8 to 0
            BASALT -> 3 to 1
            JUNGLE_WOOD -> {
                when (face) {
                    0, 1 -> 5 to 1
                    else -> 4 to 1
                }
            }
            JUNGLE_LEAVES -> 4 to 3
            OBSIDIAN -> 2 to 1
            GLOWSTONE -> 9 to 1
            // Items
            WOODEN_PICKAXE -> 0 to 5
            STONE_PICKAXE -> 1 to 5
            IRON_PICKAXE -> 2 to 5
            DIAMOND_PICKAXE -> 3 to 5
            WOODEN_SWORD -> 0 to 4
            STONE_SWORD -> 1 to 4
            IRON_SWORD -> 2 to 4
            DIAMOND_SWORD -> 3 to 4
            COAL -> 7 to 0
            IRON_INGOT -> 7 to 1
            GOLD_INGOT -> 7 to 2
            DIAMOND -> 7 to 3
            GOLDEN_APPLE -> 10 to 0
            POTION_HEALING -> 9 to 0
            else -> 0 to 0
        }
    }

    fun getBlockName(type: Byte): String {
        return when (type) {
            AIR -> "Air"
            DIRT -> "Dirt"
            GRASS -> "Grass Block"
            STONE -> "Stone"
            SAND -> "Sand"
            WATER -> "Water"
            LAVA -> "Lava"
            WOOD -> "Wood"
            LEAVES -> "Leaves"
            PLANKS -> "Wooden Planks"
            GLASS -> "Glass"
            COBBLESTONE -> "Cobblestone"
            BEDROCK -> "Bedrock"
            COAL_ORE -> "Coal Ore"
            IRON_ORE -> "Iron Ore"
            GOLD_ORE -> "Gold Ore"
            DIAMOND_ORE -> "Diamond Ore"
            ROSE -> "Rose"
            DANDELION -> "Dandelion"
            TALL_GRASS -> "Tall Grass"
            CACTUS -> "Cactus"
            SNOW -> "Snow"
            ICE -> "Ice"
            RED_SAND -> "Red Sand"
            BASALT -> "Basalt"
            JUNGLE_WOOD -> "Jungle Wood"
            JUNGLE_LEAVES -> "Jungle Leaves"
            OBSIDIAN -> "Obsidian"
            GLOWSTONE -> "Glowstone"
            WOODEN_PICKAXE -> "Wooden Pickaxe"
            STONE_PICKAXE -> "Stone Pickaxe"
            IRON_PICKAXE -> "Iron Pickaxe"
            DIAMOND_PICKAXE -> "Diamond Pickaxe"
            WOODEN_SWORD -> "Wooden Sword"
            STONE_SWORD -> "Stone Sword"
            IRON_SWORD -> "Iron Sword"
            DIAMOND_SWORD -> "Diamond Sword"
            COAL -> "Coal"
            IRON_INGOT -> "Iron Ingot"
            GOLD_INGOT -> "Gold Ingot"
            DIAMOND -> "Diamond"
            GOLDEN_APPLE -> "Golden Apple"
            POTION_HEALING -> "Potion of Healing"
            else -> "Unknown Block"
        }
    }
}
