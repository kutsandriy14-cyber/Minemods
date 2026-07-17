package com.example.engine

import com.example.game.BlockRegistry

class Chunk(val chunkX: Int, val chunkZ: Int) {
    val blocks = ByteArray(16 * 256 * 16)
    var isModified = true
    var mesh = ChunkMesh()
    
    fun getBlock(x: Int, y: Int, z: Int): Byte {
        if (x < 0 || x >= 16 || y < 0 || y >= 256 || z < 0 || z >= 16) return BlockRegistry.AIR
        return blocks[(y * 256) + (z * 16) + x]
    }
    
    fun setBlock(x: Int, y: Int, z: Int, type: Byte) {
        if (x < 0 || x >= 16 || y < 0 || y >= 256 || z < 0 || z >= 16) return
        blocks[(y * 256) + (z * 16) + x] = type
        isModified = true
    }
}
