package com.example.game

import com.example.engine.Camera
import com.example.world.World
import com.example.engine.Vector3f

class Player(val world: World) {
    val camera = Camera()
    
    // Position is camera.position
    var velocity = Vector3f()
    var isGrounded = false
    var selectedBlockType: Byte = BlockRegistry.STONE
    
    val width = 0.6f
    val height = 1.8f

    data class AABB(
        val minX: Float, val minY: Float, val minZ: Float,
        val maxX: Float, val maxY: Float, val maxZ: Float
    ) {
        fun intersects(o: AABB): Boolean {
            return this.maxX > o.minX && this.minX < o.maxX &&
                   this.maxY > o.minY && this.minY < o.maxY &&
                   this.maxZ > o.minZ && this.minZ < o.maxZ
        }
    }

    private fun getPlayerAABB(px: Float, py: Float, pz: Float): AABB {
        val r = width / 2.0f
        return AABB(
            px - r, py - 1.6f, pz - r,
            px + r, py + 0.2f, pz + r
        )
    }

    private fun getSurroundingCollisionBoxes(aabb: AABB): List<AABB> {
        val boxes = mutableListOf<AABB>()
        val minX = Math.floor(aabb.minX.toDouble()).toInt()
        val maxX = Math.floor(aabb.maxX.toDouble()).toInt()
        val minY = Math.floor(aabb.minY.toDouble()).toInt()
        val maxY = Math.floor(aabb.maxY.toDouble()).toInt()
        val minZ = Math.floor(aabb.minZ.toDouble()).toInt()
        val maxZ = Math.floor(aabb.maxZ.toDouble()).toInt()
        
        for (y in minY..maxY) {
            for (z in minZ..maxZ) {
                for (x in minX..maxX) {
                    val block = world.getBlock(x, y, z)
                    if (BlockRegistry.isSolid(block)) {
                        boxes.add(AABB(
                            x.toFloat(), y.toFloat(), z.toFloat(),
                            x + 1f, y + 1f, z + 1f
                        ))
                    }
                }
            }
        }
        return boxes
    }
    
    fun update(dt: Float, inputDirs: Vector3f, jump: Boolean) {
        // Apply gravity to velocity
        velocity.y -= 22f * dt
        if (velocity.y < -30f) velocity.y = -30f // terminal velocity
        
        // Input applied to horizontal velocity
        val speed = 5.5f
        val fw = camera.front.copy()
        fw.y = 0f; fw.normalize()
        val rt = camera.right.copy()
        rt.y = 0f; rt.normalize()
        
        val moveDir = fw * inputDirs.z + rt * inputDirs.x
        
        velocity.x = moveDir.x * speed
        velocity.z = moveDir.z * speed
        
        if (jump && isGrounded) {
            velocity.y = 7.5f
            isGrounded = false
        }
        
        // 1. Move and resolve X
        camera.position.x += velocity.x * dt
        var playerBox = getPlayerAABB(camera.position.x, camera.position.y, camera.position.z)
        var colliders = getSurroundingCollisionBoxes(playerBox)
        for (box in colliders) {
            if (playerBox.intersects(box)) {
                if (velocity.x > 0) {
                    camera.position.x = box.minX - (width / 2f) - 0.001f
                } else if (velocity.x < 0) {
                    camera.position.x = box.maxX + (width / 2f) + 0.001f
                }
                velocity.x = 0f
                playerBox = getPlayerAABB(camera.position.x, camera.position.y, camera.position.z)
            }
        }
        
        // 2. Move and resolve Z
        camera.position.z += velocity.z * dt
        playerBox = getPlayerAABB(camera.position.x, camera.position.y, camera.position.z)
        colliders = getSurroundingCollisionBoxes(playerBox)
        for (box in colliders) {
            if (playerBox.intersects(box)) {
                if (velocity.z > 0) {
                    camera.position.z = box.minZ - (width / 2f) - 0.001f
                } else if (velocity.z < 0) {
                    camera.position.z = box.maxZ + (width / 2f) + 0.001f
                }
                velocity.z = 0f
                playerBox = getPlayerAABB(camera.position.x, camera.position.y, camera.position.z)
            }
        }
        
        // 3. Move and resolve Y
        isGrounded = false
        camera.position.y += velocity.y * dt
        playerBox = getPlayerAABB(camera.position.x, camera.position.y, camera.position.z)
        colliders = getSurroundingCollisionBoxes(playerBox)
        for (box in colliders) {
            if (playerBox.intersects(box)) {
                if (velocity.y > 0) { // Hit ceiling
                    camera.position.y = box.minY - 0.2f - 0.001f
                    velocity.y = 0f
                } else if (velocity.y < 0) { // Hit ground
                    camera.position.y = box.maxY + 1.6f + 0.001f
                    velocity.y = 0f
                    isGrounded = true
                }
                playerBox = getPlayerAABB(camera.position.x, camera.position.y, camera.position.z)
            }
        }
        
        // Out of bounds safety check (void fallback)
        if (camera.position.y < -10f) {
            val spawnY = world.getSpawnHeight(0, 0)
            camera.position.set(0.5f, spawnY, 0.5f)
            velocity.set(0f, 0f, 0f)
            isGrounded = true
        }
    }

    fun raycastBlock(breakBlock: Boolean) {
        // Simple DDA raycast up to 5 blocks
        var rx = camera.position.x
        var ry = camera.position.y
        var rz = camera.position.z
        
        val dx = camera.front.x
        val dy = camera.front.y
        val dz = camera.front.z
        
        var lastBx = -1
        var lastBy = -1
        var lastBz = -1
        
        for (i in 0..100) {
            rx += dx * 0.05f
            ry += dy * 0.05f
            rz += dz * 0.05f
            
            val bx = Math.floor(rx.toDouble()).toInt()
            val by = Math.floor(ry.toDouble()).toInt()
            val bz = Math.floor(rz.toDouble()).toInt()
            
            val block = world.getBlock(bx, by, bz)
            if (BlockRegistry.isSolid(block)) {
                if (breakBlock) {
                    world.setBlock(bx, by, bz, BlockRegistry.AIR)
                } else if (lastBy != -1) {
                    // Place block at previous empty position
                    world.setBlock(lastBx, lastBy, lastBz, selectedBlockType)
                }
                return
            }
            lastBx = bx
            lastBy = by
            lastBz = bz
        }
    }
}
