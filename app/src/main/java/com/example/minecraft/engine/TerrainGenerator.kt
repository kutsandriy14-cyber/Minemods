package com.example.minecraft.engine

import com.example.minecraft.model.GameRegistry
import kotlin.random.Random

object TerrainGenerator {

    /**
     * Generates world blocks as a map of "x,y" coordinates -> block ID.
     * x is horizontal (0 to width-1), y is vertical (0 to height-1).
     * y = 0 is bedrock (bottom), higher y is skyward.
     */
    fun generateWorld(
        name: String,
        seed: Long,
        width: Int = 200,
        height: Int = 45,
        worldType: String = "Standard", // "Standard", "Flat", "Mountains"
        enabledModIds: List<String> = emptyList()
    ): Map<String, String> {
        val blocks = mutableMapOf<String, String>()
        val rand = Random(seed)

        // Find active mod block configurations to spawn
        val modBlocksToSpawn = GameRegistry.blocks.values.filter { block ->
            block.id.startsWith("mod_") || !listOf(
                "air", "grass", "dirt", "stone", "cobblestone", "oak_log", 
                "oak_leaves", "crafting_table", "furnace", "chest", 
                "coal_ore", "iron_ore", "diamond_ore", "obsidian", "bedrock"
            ).contains(block.id)
        }

        // 1. Generate base layers
        for (x in 0 until width) {
            // Determine ground height (surface level) for this column
            val surfaceY = when (worldType) {
                "Flat" -> 15
                "Mountains" -> {
                    val base = 12
                    val wave1 = (Math.sin(x * 0.05) * 8).toInt()
                    val wave2 = (Math.cos(x * 0.15) * 3).toInt()
                    base + wave1 + wave2
                }
                else -> { // "Standard"
                    val base = 14
                    val wave1 = (Math.sin(x * 0.07) * 4).toInt()
                    val wave2 = (Math.sin(x * 0.02) * 2).toInt()
                    base + wave1 + wave2
                }
            }.coerceIn(5, height - 8)

            // Bedrock layer
            blocks["$x,0"] = "bedrock"
            if (rand.nextFloat() < 0.45f) {
                blocks["$x,1"] = "bedrock"
            }

            // Generate layers up to sky
            for (y in 1 until height) {
                if (y > surfaceY) {
                    // Sky / Air (don't write to save map density, empty = air)
                    continue
                }

                when {
                    y == 1 -> {
                        // Already bedrock
                    }
                    y == surfaceY -> {
                        // Grass
                        blocks["$x,$y"] = "grass"
                    }
                    y >= surfaceY - 3 -> {
                        // Dirt
                        blocks["$x,$y"] = "dirt"
                    }
                    else -> {
                        // Default Stone
                        blocks["$x,$y"] = "stone"

                        // Procedural Vanilla Ores veins
                        val depthFromSurface = surfaceY - y
                        val roll = rand.nextFloat()
                        
                        when {
                            // Diamonds spawn deep
                            depthFromSurface > 10 && roll < 0.015f -> {
                                blocks["$x,$y"] = "diamond_ore"
                            }
                            // Iron is moderately deep
                            depthFromSurface > 5 && roll < 0.035f -> {
                                blocks["$x,$y"] = "iron_ore"
                            }
                            // Copper ore is also moderately deep
                            depthFromSurface > 4 && roll < 0.045f -> {
                                blocks["$x,$y"] = "copper_ore"
                            }
                            // Coal is common everywhere
                            roll < 0.06f -> {
                                blocks["$x,$y"] = "coal_ore"
                            }
                            // Obsidian pockets deep down
                            depthFromSurface > 12 && roll < 0.01f -> {
                                blocks["$x,$y"] = "obsidian"
                            }
                        }

                        // Support spawning of custom active Mod Ores
                        for (cb in modBlocksToSpawn) {
                            // Match depth requirement
                            // Since standard depths are represented in height coordinates,
                            // let's say y-level between 2 (bedrock) to 15 (typical stone/dirt border)
                            val blockRoll = rand.nextFloat()
                            val spawnRate = when (rand.nextInt(3)) {
                                0 -> 0.01f  // Rare
                                1 -> 0.03f  // Medium
                                else -> 0.05f // Abundant
                            }
                            if (y in 2..16 && blockRoll < spawnRate) {
                                blocks["$x,$y"] = cb.id
                            }
                        }
                    }
                }
            }

            // 2. Spawn Trees (Only on standard/mountain types, on grass blocks)
            // Leave a gap between trees
            if (worldType != "Flat" && x % 9 == 4 && rand.nextFloat() < 0.7f && surfaceY < height - 7) {
                // Spawn Oak Tree!
                val treeBaseY = surfaceY + 1
                val treeHeight = rand.nextInt(3) + 4 // 4 to 6 tall logs

                // Log column
                for (ty in 0 until treeHeight) {
                    blocks["$x,${treeBaseY + ty}"] = "oak_log"
                }

                // Dynamic leaf crown
                val leafCenterY = treeBaseY + treeHeight - 1
                for (lx in -2..2) {
                    for (ly in -2..2) {
                        for (lz in -1..1) {
                            val targetX = x + lx
                            val targetY = leafCenterY + ly
                            // Don't overwrite trunk logs
                            if (targetX == x && targetY <= treeBaseY + treeHeight - 1 && targetY >= treeBaseY) {
                                continue
                            }
                            // Rounded leaf design
                            if (Math.abs(lx) + Math.abs(ly) <= 3) {
                                blocks["$targetX,$targetY"] = "oak_leaves"
                            }
                        }
                    }
                }
            }
        }

        // 3. Procedurally generate subterranean Trial Chambers (dungeons from Tricky Trials 1.21.1)
        if (worldType != "Flat") {
            val chamberLocations = listOf(55, 135)
            for (cx in chamberLocations) {
                val cy = 5 // Subterranean depth layer coordinate
                
                // Build a 11x6 hollow dungeon room made of tuff bricks and chiseled tuff
                for (dx in -5..5) {
                    for (dy in -1..4) {
                        val tcX = cx + dx
                        val tcY = cy + dy
                        
                        if (tcX in 0 until width && tcY in 1 until height) {
                            val isBorder = dx == -5 || dx == 5 || dy == -1 || dy == 4
                            if (isBorder) {
                                // Stylized brick patterns
                                if ((dx + dy) % 3 == 0) {
                                    blocks["$tcX,$tcY"] = "chiseled_tuff"
                                } else {
                                    blocks["$tcX,$tcY"] = "tuff_bricks"
                                }
                            } else {
                                // Hollow cavity interior
                                blocks["$tcX,$tcY"] = "air"
                            }
                        }
                    }
                }
                
                // Decorate Trial Chamber interior with 1.21.1 items and functional blocks
                if (cx - 3 in 0 until width && cy in 1 until height) blocks["${cx - 3},$cy"] = "vault"
                if (cx in 0 until width && cy in 1 until height) blocks["$cx,$cy"] = "trial_spawner"
                if (cx + 3 in 0 until width && cy in 1 until height) blocks["${cx + 3},$cy"] = "vault"
                
                // Copper items
                if (cx - 4 in 0 until width && cy + 2 in 1 until height) blocks["${cx - 4},${cy + 2}"] = "copper_bulb"
                if (cx - 2 in 0 until width && cy + 3 in 1 until height) blocks["${cx - 2},${cy + 3}"] = "copper_grate"
                if (cx + 2 in 0 until width && cy + 3 in 1 until height) blocks["${cx + 2},${cy + 3}"] = "chiseled_copper"
                if (cx + 4 in 0 until width && cy + 2 in 1 until height) blocks["${cx + 4},${cy + 2}"] = "copper_bulb"
                
                // The heavy core block resting on the ground
                if (cx - 1 in 0 until width && cy in 1 until height) blocks["${cx - 1},$cy"] = "heavy_core"
            }
        }

        return blocks
    }
}
