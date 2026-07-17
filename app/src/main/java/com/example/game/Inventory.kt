package com.example.game

class Inventory(val size: Int = 36, val hotbarSize: Int = 12) {
    val items = ByteArray(size)
    var selectedHotbarSlot = 0

    init {
        // Initialize with default blocks
        items[0] = BlockRegistry.WOODEN_PICKAXE
        items[1] = BlockRegistry.GRASS
        items[2] = BlockRegistry.STONE
        items[3] = BlockRegistry.WOOD
        items[4] = BlockRegistry.PLANKS
        items[5] = BlockRegistry.LEAVES
        items[6] = BlockRegistry.GLASS
        items[7] = BlockRegistry.COBBLESTONE
        items[8] = BlockRegistry.WATER
        items[9] = BlockRegistry.LAVA
        items[10] = BlockRegistry.DIRT
        items[11] = BlockRegistry.SAND
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
