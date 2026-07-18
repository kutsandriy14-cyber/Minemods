package com.example.engine

import com.example.game.BlockRegistry
import com.example.world.World

object RenderPhysicsEngine {
    
    data class RaycastResult(
        val bx: Int,
        val by: Int,
        val bz: Int,
        val face: Int, // 0: +Y, 1: -Y, 2: -X, 3: +X, 4: -Z, 5: +Z
        val hitX: Float,
        val hitY: Float,
        val hitZ: Float
    )

    // Raycasts from player's eye to detect block intersection for highlighting and placing
    fun raycast(
        world: World,
        startX: Float,
        startY: Float,
        startZ: Float,
        dirX: Float,
        dirY: Float,
        dirZ: Float,
        maxDistance: Float = 5.0f
    ): RaycastResult? {
        var rx = startX
        var ry = startY
        var rz = startZ
        
        val step = 0.05f
        val stepsCount = (maxDistance / step).toInt()
        
        var lastBx = Math.floor(startX.toDouble()).toInt()
        var lastBy = Math.floor(startY.toDouble()).toInt()
        var lastBz = Math.floor(startZ.toDouble()).toInt()
        
        for (i in 0..stepsCount) {
            rx += dirX * step
            ry += dirY * step
            rz += dirZ * step
            
            val bx = Math.floor(rx.toDouble()).toInt()
            val by = Math.floor(ry.toDouble()).toInt()
            val bz = Math.floor(rz.toDouble()).toInt()
            
            val block = world.getBlock(bx, by, bz)
            if (BlockRegistry.isSolid(block)) {
                // Determine face based on the previous block position
                val face = when {
                    lastBy > by -> 0 // hit top (+Y)
                    lastBy < by -> 1 // hit bottom (-Y)
                    lastBx < bx -> 2 // hit left side (-X)
                    lastBx > bx -> 3 // hit right side (+X)
                    lastBz < bz -> 4 // hit front (-Z)
                    lastBz > bz -> 5 // hit back (+Z)
                    else -> 0
                }
                return RaycastResult(bx, by, bz, face, rx, ry, rz)
            }
            
            lastBx = bx
            lastBy = by
            lastBz = bz
        }
        return null
    }

    // Computes selection box vertex coordinates for the highlighted block
    fun getSelectionBoxVertices(bx: Int, by: Int, bz: Int): FloatArray {
        val o = -0.002f // offset outward slightly to avoid Z-fighting
        val s = 1.004f
        val x = bx.toFloat() + o
        val y = by.toFloat() + o
        val z = bz.toFloat() + o
        
        return floatArrayOf(
            // Wireframe cube lines (24 coordinates for 12 lines)
            x, y, z, x + s, y, z,
            x + s, y, z, x + s, y, z + s,
            x + s, y, z + s, x, y, z + s,
            x, y, z + s, x, y, z,
            
            x, y + s, z, x + s, y + s, z,
            x + s, y + s, z, x + s, y + s, z + s,
            x + s, y + s, z + s, x, y + s, z + s,
            x, y + s, z + s, x, y + s, z,
            
            x, y, z, x, y + s, z,
            x + s, y, z, x + s, y + s, z,
            x + s, y, z + s, x + s, y + s, z + s,
            x, y, z + s, x, y + s, z + s
        )
    }
}
