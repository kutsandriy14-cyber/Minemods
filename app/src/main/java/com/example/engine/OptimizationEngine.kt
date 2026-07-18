package com.example.engine

import com.example.world.World
import kotlin.math.cos
import kotlin.math.sin

object OptimizationEngine {
    // Frustum culling check to see if a chunk at (cx, cz) is in front of the camera
    fun isChunkInFrustum(
        cx: Int,
        cz: Int,
        camX: Float,
        camZ: Float,
        yawRad: Float,
        viewDistanceChunks: Int
    ): Boolean {
        // Simple 2D angle check relative to camera direction
        val dx = (cx * 16 + 8) - camX
        val dz = (cz * 16 + 8) - camZ
        val distSq = dx * dx + dz * dz
        
        // If within immediate radius (1.5 chunks), always render
        if (distSq < 24 * 24) return true
        
        // If outside render distance, cull
        val maxDist = viewDistanceChunks * 16
        if (distSq > maxDist * maxDist) return false
        
        // Calculate camera forward direction vector
        val dirX = -sin(yawRad)
        val dirZ = cos(yawRad)
        
        // Normalize chunk direction vector
        val len = Math.sqrt(distSq.toDouble()).toFloat()
        val ndx = dx / len
        val ndz = dz / len
        
        // Dot product to see if chunk is in front (within ~110 degrees field of view)
        val dot = ndx * dirX + ndz * dirZ
        return dot > -0.2f
    }

    // High performance sorting of chunks based on distance to player (front-to-back for opaque, back-to-front for transparent)
    fun sortChunksByDistance(chunksList: List<Chunk>, playerX: Float, playerZ: Float): List<Chunk> {
        val px = playerX / 16f
        val pz = playerZ / 16f
        return chunksList.sortedBy { chunk ->
            val dx = chunk.chunkX - px
            val dz = chunk.chunkZ - pz
            dx * dx + dz * dz
        }
    }
}
