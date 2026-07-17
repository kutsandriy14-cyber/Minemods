package com.example.game

object BlockRegistry {
    const val AIR: Byte = 0
    const val DIRT: Byte = 1
    const val GRASS: Byte = 2
    const val STONE: Byte = 3
    const val SAND: Byte = 4
    
    // UV mappings (x, y) assuming 16x16 pixel blocks in a 256x256 atlas (16x16 tiles)
    // Each tile is 1f / 16f size
    // grass top: 0, 0
    // dirt: 1, 0
    // grass side: 2, 0
    // stone: 3, 0
    // sand: 4, 0
    
    fun isSolid(type: Byte): Boolean {
        return type != AIR
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
            else -> 0 to 0
        }
    }
}
