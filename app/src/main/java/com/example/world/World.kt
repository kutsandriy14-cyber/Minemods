package com.example.world

import com.example.engine.Chunk
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
    
    // Calculate the exact terrain height at a world (x, z) coordinate
    fun getTerrainHeight(x: Int, z: Int): Int {
        // 1.18 style terrain generation with larger scale and higher base
        val nx = x.toFloat() * 0.005f
        val nz = z.toFloat() * 0.005f
        
        val continentalness = noise.noise2D(nx, nz) * 20f
        val erosion = noise.noise2D(nx * 2.0f + 100f, nz * 2.0f + 100f) * 10f
        val peaks = noise.noise2D(nx * 5.0f + 200f, nz * 5.0f + 200f) * 15f
        
        // Final height shifted up to allow deep caves below
        return (80f + continentalness + erosion + peaks).toInt().coerceIn(30, 150)
    }

    fun getBiomeAt(x: Int, z: Int): Byte {
        // Biome selector: humidity/moisture noise
        val moisture = noise.noise2D(x.toFloat() * 0.01f + 500f, z.toFloat() * 0.01f + 500f)
        return if (moisture < -0.15f) {
            BlockRegistry.SAND // Desert biome
        } else {
            BlockRegistry.GRASS // Plains biome
        }
    }

    fun isCave(x: Int, y: Int, z: Int): Boolean {
        if (y > 130) return false // No caves too close to highest peaks
        
        // 1.18 Cheese caves (large open caverns)
        val cheese = noise.noise3D(x * 0.02f, y * 0.02f, z * 0.02f)
        if (cheese > 0.35f) return true
        
        // 1.18 Spaghetti caves (long winding tunnels)
        val spaghetti1 = noise.noise3D(x * 0.04f, y * 0.04f, z * 0.04f)
        val spaghetti2 = noise.noise3D(x * 0.04f + 100f, y * 0.04f + 100f, z * 0.04f + 100f)
        
        // A tunnel is where two noises are close to 0 (intersection of two planes)
        val isSpaghetti = Math.abs(spaghetti1) < 0.06f && Math.abs(spaghetti2) < 0.06f
        if (isSpaghetti) return true
        
        return false
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
        val WATER_LEVEL = 70
        
        for (x in 0..15) {
            val wx = worldXOffset + x
            for (z in 0..15) {
                val wz = worldZOffset + z
                
                val height = getTerrainHeight(wx, wz)
                val biomeBlock = getBiomeAt(wx, wz)
                
                for (y in 0..255) {
                    if (y > height) {
                        if (y <= WATER_LEVEL) {
                            chunk.setBlock(x, y, z, BlockRegistry.WATER)
                        } else {
                            chunk.setBlock(x, y, z, BlockRegistry.AIR)
                        }
                    } else {
                        // Cave check with 3D noise threshold - don't break surface
                        if (y > 5 && y < height - 3 && isCave(wx, y, wz)) {
                            chunk.setBlock(x, y, z, BlockRegistry.AIR)
                        } else {
                            val blockType = when {
                                y == height -> {
                                    if (height < WATER_LEVEL) {
                                        BlockRegistry.SAND
                                    } else {
                                        biomeBlock
                                    }
                                }
                                y > height - 4 -> {
                                    if (biomeBlock == BlockRegistry.SAND || height < WATER_LEVEL) BlockRegistry.SAND else BlockRegistry.DIRT
                                }
                                else -> BlockRegistry.STONE
                            }
                            chunk.setBlock(x, y, z, blockType)
                        }
                    }
                }
                
                // Add trees randomly
                if (height > WATER_LEVEL && biomeBlock == BlockRegistry.GRASS) {
                    val treeChance = noise.noise2D((wx * 10).toFloat(), (wz * 10).toFloat())
                    if (treeChance > 0.85f && x > 2 && x < 13 && z > 2 && z < 13) {
                        // Trunk
                        for (ty in 1..4) {
                            if (height + ty <= 255) chunk.setBlock(x, height + ty, z, BlockRegistry.WOOD)
                        }
                        // Leaves
                        for (lx in -2..2) {
                            for (ly in 3..5) {
                                for (lz in -2..2) {
                                    if (Math.abs(lx) + Math.abs(lz) + Math.abs(ly - 3) <= 4) {
                                        if (height + ly <= 255) {
                                            val b = chunk.getBlock(x + lx, height + ly, z + lz)
                                            if (b == BlockRegistry.AIR) {
                                                chunk.setBlock(x + lx, height + ly, z + lz, BlockRegistry.LEAVES)
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
        val playerCx = Math.floor(playerX.toDouble() / 16.0).toInt()
        val playerCz = Math.floor(playerZ.toDouble() / 16.0).toInt()
        
        // Generate chunks within radius
        for (cx in (playerCx - radius)..(playerCx + radius)) {
            for (cz in (playerCz - radius)..(playerCz + radius)) {
                val pair = Pair(cx, cz)
                if (!chunks.containsKey(pair)) {
                    generateChunk(cx, cz)
                }
            }
        }
        
        // Unload chunks that are too far away to conserve memory and maintain smooth FPS
        val unloadThreshold = radius + 2
        val iterator = chunks.keys.iterator()
        while (iterator.hasNext()) {
            val coords = iterator.next()
            val dx = Math.abs(coords.first - playerCx)
            val dz = Math.abs(coords.second - playerCz)
            if (dx > unloadThreshold || dz > unloadThreshold) {
                val removed = chunks[coords]
                if (removed != null) {
                    meshesToDestroy.add(removed.mesh)
                }
                iterator.remove()
            }
        }
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
