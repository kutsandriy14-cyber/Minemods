package com.example.engine

import com.example.game.BlockRegistry
import com.example.world.World

object PhysicalEngine {
    data class AABB(
        var minX: Float, var minY: Float, var minZ: Float,
        var maxX: Float, var maxY: Float, var maxZ: Float
    ) {
        fun intersects(o: AABB): Boolean {
            return this.maxX > o.minX && this.minX < o.maxX &&
                   this.maxY > o.minY && this.minY < o.maxY &&
                   this.maxZ > o.minZ && this.minZ < o.maxZ
        }
        
        fun offset(dx: Float, dy: Float, dz: Float) {
            minX += dx; maxX += dx
            minY += dy; maxY += dy
            minZ += dz; maxZ += dz
        }

        fun calculateXOffset(other: AABB, dx: Float): Float {
            if (other.maxY > this.minY && other.minY < this.maxY && other.maxZ > this.minZ && other.minZ < this.maxZ) {
                if (dx > 0 && other.minX >= this.maxX) {
                    val d = other.minX - this.maxX
                    if (d < dx) return d - 0.001f
                } else if (dx < 0 && other.maxX <= this.minX) {
                    val d = other.maxX - this.minX
                    if (d > dx) return d + 0.001f
                }
            }
            return dx
        }

        fun calculateYOffset(other: AABB, dy: Float): Float {
            if (other.maxX > this.minX && other.minX < this.maxX && other.maxZ > this.minZ && other.minZ < this.maxZ) {
                if (dy > 0 && other.minY >= this.maxY) {
                    val d = other.minY - this.maxY
                    if (d < dy) return d - 0.001f
                } else if (dy < 0 && other.maxY <= this.minY) {
                    val d = other.maxY - this.minY
                    if (d > dy) return d + 0.001f
                }
            }
            return dy
        }

        fun calculateZOffset(other: AABB, dz: Float): Float {
            if (other.maxX > this.minX && other.minX < this.maxX && other.maxY > this.minY && other.minY < this.maxY) {
                if (dz > 0 && other.minZ >= this.maxZ) {
                    val d = other.minZ - this.maxZ
                    if (d < dz) return d - 0.001f
                } else if (dz < 0 && other.maxZ <= this.minX) {
                    val d = other.maxZ - this.minZ
                    if (d > dz) return d + 0.001f
                }
            }
            return dz
        }
    }

    fun getEntityAABB(px: Float, py: Float, pz: Float, w: Float, h: Float): AABB {
        val r = w / 2.0f
        return AABB(
            px - r, py - (h * 0.88f), pz - r,
            px + r, py + (h * 0.12f), pz + r
        )
    }

    fun getSurroundingCollisionBoxes(world: World, aabb: AABB): List<AABB> {
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

    // Resolves standard physical entity motion with collision detection, updates position and reports if grounded
    fun resolveCollision(
        world: World,
        pos: Vector3f,
        vel: Vector3f,
        dt: Float,
        w: Float,
        h: Float,
        noclip: Boolean
    ): Boolean {
        if (noclip) {
            pos.x += vel.x * dt
            pos.y += vel.y * dt
            pos.z += vel.z * dt
            return false
        }

        val dx = vel.x * dt
        val dy = vel.y * dt
        val dz = vel.z * dt
        
        var moveX = dx
        var moveY = dy
        var moveZ = dz
        
        val entityBox = getEntityAABB(pos.x, pos.y, pos.z, w, h)
        
        val expandedBox = AABB(
            entityBox.minX + Math.min(0f, moveX) - 0.2f, entityBox.minY + Math.min(0f, moveY) - 0.2f, entityBox.minZ + Math.min(0f, moveZ) - 0.2f,
            entityBox.maxX + Math.max(0f, moveX) + 0.2f, entityBox.maxY + Math.max(0f, moveY) + 0.2f, entityBox.maxZ + Math.max(0f, moveZ) + 0.2f
        )
        
        val colliders = getSurroundingCollisionBoxes(world, expandedBox)
        
        // Resolve Y axis first
        for (box in colliders) {
            moveY = entityBox.calculateYOffset(box, moveY)
        }
        entityBox.offset(0f, moveY, 0f)
        
        // Resolve X axis
        for (box in colliders) {
            moveX = entityBox.calculateXOffset(box, moveX)
        }
        entityBox.offset(moveX, 0f, 0f)
        
        // Resolve Z axis
        for (box in colliders) {
            moveZ = entityBox.calculateZOffset(box, moveZ)
        }
        entityBox.offset(0f, 0f, moveZ)
        
        pos.x += moveX
        pos.y += moveY
        pos.z += moveZ
        
        var grounded = false
        if (moveY != dy) {
            if (dy < 0) {
                grounded = true
            }
            vel.y = 0f
        }
        
        if (moveX != dx) vel.x = 0f
        if (moveZ != dz) vel.z = 0f
        
        return grounded
    }
}
