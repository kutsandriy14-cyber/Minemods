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
    
    // Tools as placeholder blocks in inventory (won't be placed in world as blocks)
    const val WOODEN_PICKAXE: Byte = 100
    const val STONE_PICKAXE: Byte = 101
    
    fun isItem(type: Byte): Boolean {
        return type == WOODEN_PICKAXE || type == STONE_PICKAXE
    }
    
    fun isSolid(type: Byte): Boolean {
        return type != AIR && type != WATER && type != LAVA && !isItem(type)
    }

    fun isTransparent(type: Byte): Boolean {
        return type == AIR || type == WATER || type == GLASS || type == LEAVES
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
            PLANKS -> 4 to 0 // using sand texture temporarily
            GLASS -> 1 to 3
            COBBLESTONE -> 0 to 1
            else -> 0 to 0
        }
    }
}
