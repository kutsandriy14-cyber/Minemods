package com.example.engine

import com.example.game.BlockRegistry

object StructureEngine {
    enum class StructureType {
        OAK_TREE,
        PINE_TREE,
        CACTUS,
        DUNGEON_RUIN,
        FLOWER_PATCH,
        JUNGLE_TREE,
        VOLCANIC_SPIRE,
        OBSIDIAN_OBELISK,
        GLOWSTONE_CRYSTAL,
        SNOWY_TREE
    }

    fun generateStructure(chunk: Chunk, lx: Int, height: Int, lz: Int, type: StructureType) {
        when (type) {
            StructureType.OAK_TREE -> {
                if (lx in 2..13 && lz in 2..13) {
                    // Trunk
                    for (ty in 1..5) {
                        if (height + ty <= 255) chunk.setBlock(lx, height + ty, lz, BlockRegistry.WOOD)
                    }
                    // Leaves spherical shape
                    for (dx in -2..2) {
                        for (dy in 3..6) {
                            for (dz in -2..2) {
                                if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy - 4) <= 4) {
                                    if (height + dy <= 255) {
                                        val b = chunk.getBlock(lx + dx, height + dy, lz + dz)
                                        if (b == BlockRegistry.AIR) {
                                            chunk.setBlock(lx + dx, height + dy, lz + dz, BlockRegistry.LEAVES)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            StructureType.PINE_TREE -> {
                if (lx in 2..13 && lz in 2..13) {
                    // Tall Pine Tree for Mountains
                    for (ty in 1..7) {
                        if (height + ty <= 255) chunk.setBlock(lx, height + ty, lz, BlockRegistry.WOOD)
                    }
                    // Pine leaves (conical layers)
                    for (ly in 3..8) {
                        val radius = if (ly == 3 || ly == 4) 2 else if (ly == 5 || ly == 6) 1 else 0
                        for (dx in -radius..radius) {
                            for (dz in -radius..radius) {
                                if (Math.abs(dx) + Math.abs(dz) <= radius + 1) {
                                    if (height + ly <= 255) {
                                        val b = chunk.getBlock(lx + dx, height + ly, lz + dz)
                                        if (b == BlockRegistry.AIR) {
                                            chunk.setBlock(lx + dx, height + ly, lz + dz, BlockRegistry.LEAVES)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            StructureType.CACTUS -> {
                if (lx in 1..14 && lz in 1..14) {
                    val cactusHeight = 2 + (Math.random() * 2).toInt()
                    for (cy in 1..cactusHeight) {
                        if (height + cy <= 255) chunk.setBlock(lx, height + cy, lz, BlockRegistry.CACTUS)
                    }
                }
            }
            StructureType.DUNGEON_RUIN -> {
                if (lx in 3..12 && lz in 3..12) {
                    // Small underground/surface cobblestone ruin with a chest/spawner-like block, mossy cobble
                    val w = 4
                    val h = 4
                    val d = 4
                    for (dx in 0..w) {
                        for (dy in -2..h) {
                            for (dz in 0..d) {
                                val ry = height + dy
                                if (ry in 0..255) {
                                    val isWall = dx == 0 || dx == w || dz == 0 || dz == d
                                    val isRoof = dy == h
                                    val isFloor = dy == -2
                                    if (isWall || isRoof || isFloor) {
                                        // Random gaps to make it look ruined
                                        if (Math.random() > 0.15 || isFloor) {
                                            chunk.setBlock(lx + dx - 2, ry, lz + dz - 2, BlockRegistry.COBBLESTONE)
                                        } else {
                                            chunk.setBlock(lx + dx - 2, ry, lz + dz - 2, BlockRegistry.AIR)
                                        }
                                    } else {
                                        // Inside is air
                                        chunk.setBlock(lx + dx - 2, ry, lz + dz - 2, BlockRegistry.AIR)
                                    }
                                }
                            }
                        }
                    }
                    // Place a center gold block or obsidian block inside as loot
                    if (height + 1 <= 255) {
                        chunk.setBlock(lx, height - 1, lz, BlockRegistry.GOLD_ORE)
                    }
                }
            }
            StructureType.FLOWER_PATCH -> {
                if (height + 1 <= 255) {
                    val plantType = when ((Math.random() * 3).toInt()) {
                        0 -> BlockRegistry.ROSE
                        1 -> BlockRegistry.DANDELION
                        else -> BlockRegistry.TALL_GRASS
                    }
                    chunk.setBlock(lx, height + 1, lz, plantType)
                }
            }
            StructureType.JUNGLE_TREE -> {
                if (lx in 3..12 && lz in 3..12) {
                    val trunkHeight = 10 + (Math.random() * 5).toInt()
                    // Wide trunk (2x2)
                    for (ty in 1..trunkHeight) {
                        if (height + ty <= 255) {
                            chunk.setBlock(lx, height + ty, lz, BlockRegistry.JUNGLE_WOOD)
                            chunk.setBlock(lx + 1, height + ty, lz, BlockRegistry.JUNGLE_WOOD)
                            chunk.setBlock(lx, height + ty, lz + 1, BlockRegistry.JUNGLE_WOOD)
                            chunk.setBlock(lx + 1, height + ty, lz + 1, BlockRegistry.JUNGLE_WOOD)
                        }
                    }
                    // Canopy layers
                    for (dy in (trunkHeight - 4)..(trunkHeight + 3)) {
                        val radius = if (dy < trunkHeight - 1) 3 else if (dy < trunkHeight + 1) 2 else 1
                        for (dx in -radius..radius + 1) {
                            for (dz in -radius..radius + 1) {
                                if (height + dy <= 255) {
                                    val b = chunk.getBlock(lx + dx, height + dy, lz + dz)
                                    if (b == BlockRegistry.AIR) {
                                        chunk.setBlock(lx + dx, height + dy, lz + dz, BlockRegistry.JUNGLE_LEAVES)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            StructureType.VOLCANIC_SPIRE -> {
                if (lx in 2..13 && lz in 2..13) {
                    val spireHeight = 6 + (Math.random() * 6).toInt()
                    for (sy in 1..spireHeight) {
                        val ry = height + sy
                        if (ry <= 255) {
                            val block = if (sy == spireHeight) BlockRegistry.GLOWSTONE else if (Math.random() > 0.3) BlockRegistry.BASALT else BlockRegistry.OBSIDIAN
                            chunk.setBlock(lx, ry, lz, block)
                            // lava source on top
                            if (sy == spireHeight - 1 && Math.random() > 0.5) {
                                chunk.setBlock(lx, ry, lz, BlockRegistry.LAVA)
                            }
                        }
                    }
                }
            }
            StructureType.OBSIDIAN_OBELISK -> {
                val obHeight = 3 + (Math.random() * 3).toInt()
                for (oy in 1..obHeight) {
                    if (height + oy <= 255) {
                        chunk.setBlock(lx, height + oy, lz, BlockRegistry.OBSIDIAN)
                    }
                }
            }
            StructureType.GLOWSTONE_CRYSTAL -> {
                if (height + 2 <= 255) {
                    chunk.setBlock(lx, height + 1, lz, BlockRegistry.GLOWSTONE)
                    chunk.setBlock(lx + 1, height + 1, lz, BlockRegistry.GLASS)
                    chunk.setBlock(lx - 1, height + 1, lz, BlockRegistry.GLASS)
                    chunk.setBlock(lx, height + 1, lz + 1, BlockRegistry.GLASS)
                    chunk.setBlock(lx, height + 1, lz - 1, BlockRegistry.GLASS)
                    chunk.setBlock(lx, height + 2, lz, BlockRegistry.GLOWSTONE)
                }
            }
            StructureType.SNOWY_TREE -> {
                if (lx in 2..13 && lz in 2..13) {
                    // Wood trunk
                    for (ty in 1..6) {
                        if (height + ty <= 255) chunk.setBlock(lx, height + ty, lz, BlockRegistry.WOOD)
                    }
                    // Conical layers with Snow accents on top
                    for (ly in 3..7) {
                        val radius = if (ly == 3 || ly == 4) 2 else if (ly == 5) 1 else 0
                        for (dx in -radius..radius) {
                            for (dz in -radius..radius) {
                                if (Math.abs(dx) + Math.abs(dz) <= radius + 1) {
                                    val ry = height + ly
                                    if (ry <= 255) {
                                        val b = chunk.getBlock(lx + dx, ry, lz + dz)
                                        if (b == BlockRegistry.AIR) {
                                            chunk.setBlock(lx + dx, ry, lz + dz, BlockRegistry.LEAVES)
                                            // Place snow cap on top of leaves
                                            if (ry + 1 <= 255 && chunk.getBlock(lx + dx, ry + 1, lz + dz) == BlockRegistry.AIR) {
                                                chunk.setBlock(lx + dx, ry + 1, lz + dz, BlockRegistry.SNOW)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
