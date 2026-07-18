package com.example.world

import com.example.engine.*
import com.example.game.BlockRegistry
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class World {
    val chunks = ConcurrentHashMap<Pair<Int, Int>, Chunk>()
    
    var isNight: Boolean = false
    
    var seed: Long = 1337L
        set(value) {
            field = value
            noise = SimplexNoise(value)
        }
    
    var noise = SimplexNoise(seed)
    
    // Tracks all user block modifications (broken/placed) to sync with joining LAN clients
    val blockUpdates = ConcurrentHashMap<String, Byte>()
    
    val mobs = java.util.concurrent.CopyOnWriteArrayList<com.example.game.Mob>()
    
    // Calculate the exact terrain height at a world (x, z) coordinate
    fun getTerrainHeight(x: Int, z: Int): Int {
        return BiomeEngine.getTerrainHeight(x, z, noise)
    }

    fun getBiomeAt(x: Int, z: Int): Byte {
        val biomeType = BiomeEngine.getBiomeType(x, z, noise)
        return BiomeEngine.getSurfaceBlockForBiome(biomeType, getTerrainHeight(x, z))
    }

    fun isCave(x: Int, y: Int, z: Int): Boolean {
        return WorldEngine.isCave(x, y, z, noise)
    }
    
    fun getBlock(x: Int, y: Int, z: Int): Byte {
        if (y < 0 || y > 255) return BlockRegistry.AIR
        
        val cx = x shr 4
        val cz = z shr 4
        
        val chunk = chunks[Pair(cx, cz)] ?: return BlockRegistry.AIR
        
        val lx = x and 15
        val lz = z and 15
        
        return chunk.getBlock(lx, y, lz)
    }
    
    fun setBlock(x: Int, y: Int, z: Int, type: Byte) {
        if (y < 0 || y > 255) return
        val cx = x shr 4
        val cz = z shr 4
        
        // Track the block update for LAN sync
        blockUpdates["$x,$y,$z"] = type
        
        val chunk = chunks[Pair(cx, cz)] ?: return
        val lx = x and 15
        val lz = z and 15
        chunk.setBlock(lx, y, lz, type)
        
        // If on chunk border, mark neighbor chunks for update
        if (lx == 0) chunks[Pair(cx - 1, cz)]?.isModified = true
        if (lx == 15) chunks[Pair(cx + 1, cz)]?.isModified = true
        if (lz == 0) chunks[Pair(cx, cz - 1)]?.isModified = true
        if (lz == 15) chunks[Pair(cx, cz + 1)]?.isModified = true
    }
    
    fun getSpawnHeight(x: Int, z: Int): Float {
        val startY = getTerrainHeight(x, z)
        for (y in startY + 10 downTo 0) {
            val block = getBlock(x, y, z)
            if (BlockRegistry.isSolid(block)) {
                return y.toFloat() + 2.8f // standing on top of block
            }
        }
        return 40f // Default fallback
    }

    fun generateChunk(cx: Int, cz: Int): Chunk {
        val chunk = Chunk(cx, cz)
        val worldXOffset = cx * 16
        val worldZOffset = cz * 16
        val WATER_LEVEL = WorldEngine.WATER_LEVEL
        
        for (x in 0..15) {
            val wx = worldXOffset + x
            for (z in 0..15) {
                val wz = worldZOffset + z
                
                val height = getTerrainHeight(wx, wz)
                val biomeType = BiomeEngine.getBiomeType(wx, wz, noise)
                val biomeBlock = BiomeEngine.getSurfaceBlockForBiome(biomeType, height)
                val underBlock = BiomeEngine.getUndergroundBlockForBiome(biomeType)
                
                for (y in 0..255) {
                    if (y > height) {
                        if (y <= WATER_LEVEL) {
                            // Freeze water surface in Snowy Tundra biome
                            if (biomeType == BiomeEngine.BIOME_SNOWY_TUNDRA && y == WATER_LEVEL) {
                                chunk.setBlock(x, y, z, BlockRegistry.ICE)
                            } else {
                                chunk.setBlock(x, y, z, BlockRegistry.WATER)
                            }
                        } else {
                            chunk.setBlock(x, y, z, BlockRegistry.AIR)
                        }
                    } else {
                        // Cave check with 3D noise threshold - don't break surface
                        if (y > 5 && y < height - 3 && isCave(wx, y, wz)) {
                            chunk.setBlock(x, y, z, BlockRegistry.AIR)
                        } else {
                            val blockType = when {
                                y == 0 -> BlockRegistry.BEDROCK
                                y < 4 && Math.random() < 0.5 -> BlockRegistry.BEDROCK
                                y == height -> {
                                    if (height < WATER_LEVEL) {
                                        if (biomeType == BiomeEngine.BIOME_VOLCANIC_WASTELAND) BlockRegistry.BASALT else BlockRegistry.SAND
                                    } else {
                                        biomeBlock
                                    }
                                }
                                y > height - 4 -> {
                                    if (height < WATER_LEVEL) {
                                        if (biomeType == BiomeEngine.BIOME_VOLCANIC_WASTELAND) BlockRegistry.BASALT else BlockRegistry.SAND
                                    } else {
                                        underBlock
                                    }
                                }
                                else -> {
                                    // Generate realistic interconnected ore veins in 3D using simplex noise
                                    val coalNoise = noise.noise3D(wx * 0.12f, y * 0.12f, wz * 0.12f)
                                    val ironNoise = noise.noise3D(wx * 0.16f + 100f, y * 0.16f + 100f, wz * 0.16f + 100f)
                                    val goldNoise = noise.noise3D(wx * 0.22f + 200f, y * 0.22f + 200f, wz * 0.22f + 200f)
                                    val diamondNoise = noise.noise3D(wx * 0.28f + 300f, y * 0.28f + 300f, wz * 0.28f + 300f)
                                    
                                    when {
                                        y < 16 && diamondNoise > 0.65f -> BlockRegistry.DIAMOND_ORE
                                        y < 32 && goldNoise > 0.60f -> BlockRegistry.GOLD_ORE
                                        y < 64 && ironNoise > 0.55f -> BlockRegistry.IRON_ORE
                                        coalNoise > 0.50f -> BlockRegistry.COAL_ORE
                                        else -> BlockRegistry.STONE
                                    }
                                }
                            }
                            chunk.setBlock(x, y, z, blockType)
                        }
                    }
                }
                
                // Add structures randomly based on Biome types
                if (height > WATER_LEVEL) {
                    val structChance = noise.noise2D((wx * 11).toFloat(), (wz * 11).toFloat())
                    when (biomeType) {
                        BiomeEngine.BIOME_JUNGLE -> {
                            if (structChance > 0.82f) {
                                StructureEngine.generateStructure(chunk, x, height, z, StructureEngine.StructureType.JUNGLE_TREE)
                            } else if (structChance < -0.6f) {
                                StructureEngine.generateStructure(chunk, x, height, z, StructureEngine.StructureType.FLOWER_PATCH)
                            }
                        }
                        BiomeEngine.BIOME_VOLCANIC_WASTELAND -> {
                            if (structChance > 0.90f) {
                                StructureEngine.generateStructure(chunk, x, height, z, StructureEngine.StructureType.VOLCANIC_SPIRE)
                            } else if (structChance in -0.8f..-0.75f) {
                                StructureEngine.generateStructure(chunk, x, height, z, StructureEngine.StructureType.OBSIDIAN_OBELISK)
                            } else if (structChance in 0.42f..0.44f) {
                                StructureEngine.generateStructure(chunk, x, height, z, StructureEngine.StructureType.GLOWSTONE_CRYSTAL)
                            }
                        }
                        BiomeEngine.BIOME_SNOWY_TUNDRA -> {
                            if (structChance > 0.88f) {
                                StructureEngine.generateStructure(chunk, x, height, z, StructureEngine.StructureType.SNOWY_TREE)
                            } else if (structChance in 0.40f..0.43f) {
                                StructureEngine.generateStructure(chunk, x, height, z, StructureEngine.StructureType.GLOWSTONE_CRYSTAL)
                            }
                        }
                        BiomeEngine.BIOME_MOUNTAINS -> {
                            if (structChance > 0.85f) {
                                StructureEngine.generateStructure(chunk, x, height, z, StructureEngine.StructureType.PINE_TREE)
                            }
                        }
                        BiomeEngine.BIOME_DESERT -> {
                            if (structChance > 0.88f) {
                                StructureEngine.generateStructure(chunk, x, height, z, StructureEngine.StructureType.CACTUS)
                            }
                        }
                        BiomeEngine.BIOME_FOREST -> {
                            if (structChance > 0.78f) {
                                val t = if (Math.random() > 0.4) StructureEngine.StructureType.OAK_TREE else StructureEngine.StructureType.PINE_TREE
                                StructureEngine.generateStructure(chunk, x, height, z, t)
                            }
                        }
                        else -> { // BIOME_PLAINS
                            if (structChance > 0.90f) {
                                StructureEngine.generateStructure(chunk, x, height, z, StructureEngine.StructureType.OAK_TREE)
                            } else if (structChance < -0.55f) {
                                StructureEngine.generateStructure(chunk, x, height, z, StructureEngine.StructureType.FLOWER_PATCH)
                            } else if (structChance in 0.42f..0.44f && x in 3..12 && z in 3..12) {
                                StructureEngine.generateStructure(chunk, x, height, z, StructureEngine.StructureType.DUNGEON_RUIN)
                            }
                        }
                    }
                }
            }
        }
        
        // Apply any recorded block modifications to this chunk
        for ((coords, type) in blockUpdates) {
            val parts = coords.split(",")
            if (parts.size == 3) {
                val bx = parts[0].toInt()
                val by = parts[1].toInt()
                val bz = parts[2].toInt()
                val bcx = bx shr 4
                val bcz = bz shr 4
                if (bcx == cx && bcz == cz) {
                    chunk.setBlock(bx and 15, by, bz and 15, type)
                }
            }
        }
        
        chunk.isModified = true
        chunks[Pair(cx, cz)] = chunk
        return chunk
    }

    val meshesToDestroy = java.util.concurrent.ConcurrentLinkedQueue<com.example.engine.ChunkMesh>()

    fun updateChunksAroundPlayer(playerX: Float, playerZ: Float, radius: Int = 4) {
        ChunkLoadEngine.updatePlayerLoadRadius(this, playerX, playerZ, radius)
    }

    fun generateInitialWorld() {
        noise = SimplexNoise(seed)
        chunks.clear()
        
        // Load initial 5x5 chunk grid
        for (cx in -2..2) {
            for (cz in -2..2) {
                generateChunk(cx, cz)
            }
        }
    }
}
