package com.example.minecraft.engine

import com.example.minecraft.model.GameRegistry
import kotlin.random.Random

object TerrainGenerator {

    fun getSurfaceY(x: Int, worldType: String, height: Int): Int {
        return when (worldType) {
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
    }

    /**
     * Generates a single column of the world.
     */
    fun generateColumn(
        x: Int,
        seed: Long,
        height: Int = 45,
        worldType: String = "Standard",
        enabledModIds: List<String> = emptyList()
    ): Map<String, String> {
        val blocks = mutableMapOf<String, String>()
        val rand = Random(seed + x)

        // Find active mod block configurations to spawn
        val modBlocksToSpawn = GameRegistry.blocks.values.filter { block ->
            block.id.startsWith("mod_") || !listOf(
                "air", "grass", "dirt", "stone", "cobblestone", "oak_log", 
                "oak_leaves", "crafting_table", "furnace", "chest", 
                "coal_ore", "iron_ore", "diamond_ore", "obsidian", "bedrock"
            ).contains(block.id)
        }

        val surfaceY = getSurfaceY(x, worldType, height)

        // Bedrock layer
        blocks["$x,0"] = "bedrock"
        if (rand.nextFloat() < 0.45f) {
            blocks["$x,1"] = "bedrock"
        }

        // Generate layers up to sky
        for (y in 1 until height) {
            if (y > surfaceY) {
                continue
            }

            when {
                y == 1 -> {
                    // Already bedrock (if randomized)
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

        // --- 1. PROCEDURAL SURFACE STRUCTURES (VILLAGES & PYRAMIDS) ---
        val houseInterval = 140
        val houseCx = ((x - 70).toFloat() / houseInterval.toFloat()).let { java.lang.Math.round(it) } * houseInterval + 70
        val houseDx = x - houseCx
        val isNearHouse = Math.abs(houseDx) <= 3

        val pyramidInterval = 320
        val pyramidCx = ((x - 160).toFloat() / pyramidInterval.toFloat()).let { java.lang.Math.round(it) } * pyramidInterval + 160
        val pyramidDx = x - pyramidCx
        val isNearPyramid = Math.abs(pyramidDx) <= 4

        if (isNearHouse) {
            val hY = getSurfaceY(houseCx, worldType, height)
            // Clear space above the foundation
            for (ty in (hY + 1) until height) {
                blocks["$x,$ty"] = "air"
            }
            // Floor foundation
            blocks["$x,$hY"] = "cobblestone"

            // Build vertical walls & features
            val floorY = hY + 1
            if (houseDx == -3 || houseDx == 3) {
                blocks["$x,$floorY"] = "cobblestone"
                blocks["$x,${floorY + 1}"] = "oak_log"
                blocks["$x,${floorY + 2}"] = "oak_planks"
            } else if (houseDx == -2 || houseDx == 2) {
                blocks["$x,$floorY"] = "oak_planks"
                blocks["$x,${floorY + 1}"] = "glass"
                blocks["$x,${floorY + 2}"] = "oak_planks"
            } else {
                // Interior hollow area
                blocks["$x,$floorY"] = "air"
                blocks["$x,${floorY + 1}"] = "air"
                blocks["$x,${floorY + 2}"] = "air"

                // Specific furniture
                if (houseDx == -1) {
                    blocks["$x,$floorY"] = "chest"
                } else if (houseDx == 1) {
                    blocks["$x,$floorY"] = "crafting_table"
                } else if (houseDx == 0) {
                    // Door opening is clear air
                }
            }

            // Oak flat roof
            blocks["$x,${hY + 4}"] = "oak_planks"
            // Cobblestone roof peak
            if (houseDx in -1..1) {
                blocks["$x,${hY + 5}"] = "cobblestone"
            }
        } else if (isNearPyramid) {
            val pY = getSurfaceY(pyramidCx, worldType, height)
            // Clear sky above
            for (ty in (pY + 1) until height) {
                blocks["$x,$ty"] = "air"
            }

            // Bottom layer (Y+1)
            if (Math.abs(pyramidDx) <= 4) {
                blocks["$x,${pY + 1}"] = "sandstone"
            }
            // Second layer (Y+2)
            if (Math.abs(pyramidDx) <= 3) {
                blocks["$x,${pY + 2}"] = "sandstone"
            }
            // Third layer (Y+3)
            if (Math.abs(pyramidDx) <= 2) {
                blocks["$x,${pY + 3}"] = "sandstone"
            }
            // Peak layer (Y+4)
            if (Math.abs(pyramidDx) <= 1) {
                if (pyramidDx == 0) {
                    blocks["$x,${pY + 4}"] = "gold_block"
                } else {
                    blocks["$x,${pY + 4}"] = "sandstone"
                }
            }

            // Hollow out treasure chamber
            if (Math.abs(pyramidDx) <= 1) {
                blocks["$x,${pY + 1}"] = "air"
                blocks["$x,${pY + 2}"] = "air"
                if (pyramidDx == 0) {
                    // Reward chest in middle of pyramid room!
                    blocks["$x,${pY + 1}"] = "chest"
                }
            }
        }

        // Tree generation
        if (worldType != "Flat" && !isNearHouse && !isNearPyramid) {
            // Check if a tree is spawned in any column tx from x-2 to x+2
            for (tx in (x - 2)..(x + 2)) {
                val txRand = Random(seed + tx)
                val txSpawnsTree = tx % 9 == 4 && txRand.nextFloat() < 0.7f
                if (txSpawnsTree) {
                    val txTreeHeight = txRand.nextInt(3) + 4
                    val txSurfaceY = getSurfaceY(tx, worldType, height)
                    val treeBaseY = txSurfaceY + 1
                    
                    // 1. Trunk
                    if (x == tx) {
                        for (ty in 0 until txTreeHeight) {
                            blocks["$x,${treeBaseY + ty}"] = "oak_log"
                        }
                    }

                    // 2. Leaves
                    val leafCenterY = treeBaseY + txTreeHeight - 1
                    for (ly in -2..2) {
                        val targetY = leafCenterY + ly
                        val lx = x - tx
                        if (targetY >= 1 && targetY < height) {
                            // Don't overwrite trunk logs
                            if (x == tx && targetY <= treeBaseY + txTreeHeight - 1 && targetY >= treeBaseY) {
                                continue
                            }
                            // Rounded leaf design
                            if (Math.abs(lx) + Math.abs(ly) <= 3) {
                                blocks["$x,$targetY"] = "oak_leaves"
                            }
                        }
                    }
                }
            }
        }

        // Subterranean Trial Chambers (spawn periodically along the endless coordinate line)
        if (worldType != "Flat") {
            // Find closest Chamber center (every 200 blocks, e.g. at ... -145, 55, 255, 455 ...)
            val cx = ((x - 55).toFloat() / 200f).let { java.lang.Math.round(it) } * 200 + 55
            val dx = x - cx
            if (Math.abs(dx) <= 5) {
                val cy = 5 // Subterranean depth layer coordinate
                for (dy in -1..4) {
                    val tcY = cy + dy
                    if (tcY in 1 until height) {
                        val isBorder = dx == -5 || dx == 5 || dy == -1 || dy == 4
                        if (isBorder) {
                            if ((dx + dy) % 3 == 0) {
                                blocks["$x,$tcY"] = "chiseled_tuff"
                            } else {
                                blocks["$x,$tcY"] = "tuff_bricks"
                            }
                        } else {
                            blocks["$x,$tcY"] = "air"
                        }
                    }
                }

                // Decorate Trial Chamber interior deterministically based on dx
                if (dx == -3 && cy in 1 until height) blocks["$x,$cy"] = "vault"
                if (dx == 0 && cy in 1 until height) blocks["$x,$cy"] = "trial_spawner"
                if (dx == 3 && cy in 1 until height) blocks["$x,$cy"] = "vault"
                
                // Copper items
                if (dx == -4 && cy + 2 in 1 until height) blocks["$x,${cy + 2}"] = "copper_bulb"
                if (dx == -2 && cy + 3 in 1 until height) blocks["$x,${cy + 3}"] = "copper_grate"
                if (dx == 2 && cy + 3 in 1 until height) blocks["$x,${cy + 3}"] = "chiseled_copper"
                if (dx == 4 && cy + 2 in 1 until height) blocks["$x,${cy + 2}"] = "copper_bulb"
                
                // Heavy core
                if (dx == -1 && cy in 1 until height) blocks["$x,$cy"] = "heavy_core"
            }
        }

        return blocks
    }

    /**
     * Generates initial world starting section.
     */
    fun generateWorld(
        name: String,
        seed: Long,
        width: Int = 100000,
        height: Int = 45,
        worldType: String = "Standard", // "Standard", "Flat", "Mountains"
        enabledModIds: List<String> = emptyList()
    ): Map<String, String> {
        val blocks = mutableMapOf<String, String>()
        
        // Let's generate from 49900 to 50100 (200 columns) initially centered around 50000 spawn point
        val startX = 49900
        val endX = 50100
        
        for (x in startX..endX) {
            blocks.putAll(generateColumn(x, seed, height, worldType, enabledModIds))
        }
        
        return blocks
    }
}
