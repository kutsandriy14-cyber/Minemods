package com.example.engine

import com.example.game.BlockRegistry

object ItemEngine {
    
    data class CraftingRecipe(
        val ingredient1: Byte,
        val ingredient2: Byte,
        val result: Byte,
        val resultAmount: Int = 1
    )

    val recipes = listOf(
        // Coal + Wood -> Torch placeholder or Diamond_Pickaxe
        CraftingRecipe(BlockRegistry.COBBLESTONE, BlockRegistry.WOOD, BlockRegistry.STONE_PICKAXE),
        CraftingRecipe(BlockRegistry.IRON_INGOT, BlockRegistry.WOOD, BlockRegistry.IRON_PICKAXE),
        CraftingRecipe(BlockRegistry.DIAMOND, BlockRegistry.WOOD, BlockRegistry.DIAMOND_PICKAXE),
        CraftingRecipe(BlockRegistry.COBBLESTONE, BlockRegistry.WOOD, BlockRegistry.STONE_SWORD),
        CraftingRecipe(BlockRegistry.IRON_INGOT, BlockRegistry.WOOD, BlockRegistry.IRON_SWORD),
        CraftingRecipe(BlockRegistry.DIAMOND, BlockRegistry.WOOD, BlockRegistry.DIAMOND_SWORD),
        // Ores to ingots
        CraftingRecipe(BlockRegistry.COAL, BlockRegistry.IRON_ORE, BlockRegistry.IRON_INGOT),
        CraftingRecipe(BlockRegistry.COAL, BlockRegistry.GOLD_ORE, BlockRegistry.GOLD_INGOT),
        // Planks
        CraftingRecipe(BlockRegistry.WOOD, BlockRegistry.AIR, BlockRegistry.PLANKS, 4)
    )

    fun getToolMiningSpeed(item: Byte, block: Byte): Float {
        val baseSpeed = 1.0f
        
        val isOre = block == BlockRegistry.COAL_ORE || block == BlockRegistry.IRON_ORE || block == BlockRegistry.GOLD_ORE || block == BlockRegistry.DIAMOND_ORE
        val isStone = block == BlockRegistry.STONE || block == BlockRegistry.COBBLESTONE || isOre
        
        return when (item) {
            BlockRegistry.WOODEN_PICKAXE -> if (isStone) 2.5f else 1.0f
            BlockRegistry.STONE_PICKAXE -> if (isStone) 4.5f else 1.0f
            BlockRegistry.IRON_PICKAXE -> if (isStone) 7.5f else 1.0f
            BlockRegistry.DIAMOND_PICKAXE -> if (isStone) 12.0f else 1.0f
            else -> baseSpeed
        }
    }

    fun getWeaponDamage(item: Byte): Int {
        return when (item) {
            BlockRegistry.WOODEN_SWORD -> 4
            BlockRegistry.STONE_SWORD -> 5
            BlockRegistry.IRON_SWORD -> 7
            BlockRegistry.DIAMOND_SWORD -> 9
            BlockRegistry.WOODEN_PICKAXE -> 2
            BlockRegistry.STONE_PICKAXE -> 3
            BlockRegistry.IRON_PICKAXE -> 4
            BlockRegistry.DIAMOND_PICKAXE -> 5
            else -> 1 // Punch damage
        }
    }

    fun isFoodOrPotion(item: Byte): Boolean {
        return item == BlockRegistry.GOLDEN_APPLE || item == BlockRegistry.POTION_HEALING
    }
}
