package com.example.game

class Inventory(val size: Int = 36, val hotbarSize: Int = 9) {
    val items = ByteArray(size)
    var selectedHotbarSlot = 0

    init {
        // Initialize with default blocks
        items[0] = BlockRegistry.DIRT
        items[1] = BlockRegistry.STONE
        items[2] = BlockRegistry.GRASS
        items[3] = BlockRegistry.SAND
    }

    fun getSelectedBlock(): Byte {
        return items[selectedHotbarSlot]
    }

    fun setBlock(slot: Int, block: Byte) {
        if (slot in 0 until size) {
            items[slot] = block
        }
    }

    fun getBlock(slot: Int): Byte {
        if (slot in 0 until size) {
            return items[slot]
        }
        return BlockRegistry.AIR
    }
}
