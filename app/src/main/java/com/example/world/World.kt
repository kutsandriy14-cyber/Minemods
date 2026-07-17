package com.example.world

import com.example.engine.Chunk
import com.example.game.BlockRegistry
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class World {
    val chunks = ConcurrentHashMap<Pair<Int, Int>, Chunk>()
    
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
        // Base elevation noise (frequencies and octaves)
        val nx = x.toFloat() * 0.015f
        val nz = z.toFloat() * 0.015f
        
        val baseNoise = noise.noise2D(nx, nz) * 12f
        val detailNoise = noise.noise2D(nx * 4.0f, nz * 4.0f) * 3f
        
        // Final height between 10 and 60
        return (28f + baseNoise + detailNoise).toInt().coerceIn(10, 80)
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
        if (y > 30) return false // No caves too close to surface to prevent ruined landscape
        val caveNoise = noise.noise3D(x.toFloat() * 0.07f, y.toFloat() * 0.07f, z.toFloat() * 0.07f)
        return caveNoise > 0.45f
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
        
        for (x in 0..15) {
            val wx = worldXOffset + x
            for (z in 0..15) {
                val wz = worldZOffset + z
                
                val height = getTerrainHeight(wx, wz)
                val biomeBlock = getBiomeAt(wx, wz)
                
                for (y in 0..255) {
                    if (y > height) {
                        chunk.setBlock(x, y, z, BlockRegistry.AIR)
                    } else {
                        // Cave check with 3D noise threshold
                        if (y > 2 && isCave(wx, y, wz)) {
                            chunk.setBlock(x, y, z, BlockRegistry.AIR)
                        } else {
                            val blockType = when {
                                y == height -> biomeBlock
                                y > height - 4 -> {
                                    if (biomeBlock == BlockRegistry.SAND) BlockRegistry.SAND else BlockRegistry.DIRT
                                }
                                else -> BlockRegistry.STONE
                            }
                            chunk.setBlock(x, y, z, blockType)
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

    fun updateChunksAroundPlayer(playerX: Float, playerZ: Float) {
        val playerCx = Math.floor(playerX.toDouble() / 16.0).toInt()
        val playerCz = Math.floor(playerZ.toDouble() / 16.0).toInt()
        val radius = 4 // 9x9 chunk area around player
        
        for (cx in (playerCx - radius)..(playerCx + radius)) {
            for (cz in (playerCz - radius)..(playerCz + radius)) {
                val pair = Pair(cx, cz)
                if (!chunks.containsKey(pair)) {
                    generateChunk(cx, cz)
                }
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
