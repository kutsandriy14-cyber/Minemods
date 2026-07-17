package com.example.game

import com.example.engine.Camera
import com.example.world.World
import com.example.engine.Vector3f
import com.example.engine.NetworkManager

class Player(val world: World) {
    val camera = Camera()
    
    enum class GameMode {
        SURVIVAL,
        CREATIVE,
        SPECTATOR
    }

    var gameMode: GameMode = GameMode.CREATIVE
        set(value) {
            field = value
            when (value) {
                GameMode.SURVIVAL -> {
                    canFly = false
                    noclip = false
                    canBuild = true
                    canBreak = true
                }
                GameMode.CREATIVE -> {
                    canFly = true
                    noclip = false
                    canBuild = true
                    canBreak = true
                }
                GameMode.SPECTATOR -> {
                    canFly = true
                    noclip = true
                    canBuild = false
                    canBreak = false
                }
            }
        }

    var isOp: Boolean = true
    var canFly: Boolean = false
    var noclip: Boolean = false
    var canBuild: Boolean = true
    var canBreak: Boolean = true

    val inventory = Inventory()
    
    // Position is camera.position
    var velocity = Vector3f()
    var isGrounded = false
    var selectedBlockType: Byte = BlockRegistry.STONE
    
    val width = 0.6f
    val height = 1.8f

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
                } else if (dz < 0 && other.maxZ <= this.minZ) {
                    val d = other.maxZ - this.minZ
                    if (d > dz) return d + 0.001f
                }
            }
            return dz
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
    
    var isFlying = false
    private var lastJumpTime = 0L
    private var wasJumping = false

    fun update(dt: Float, inputDirs: Vector3f, jump: Boolean, isSneaking: Boolean = false) {
        val jumpPressedThisFrame = jump && !wasJumping
        wasJumping = jump

        // Double jump detection
        if (jumpPressedThisFrame && canFly) {
            val now = System.currentTimeMillis()
            if (now - lastJumpTime < 300) {
                isFlying = !isFlying
                lastJumpTime = 0L // consume
            } else {
                lastJumpTime = now
            }
        }

        val speed = if (isFlying) 12.5f else 5.5f
        
        if (isFlying) {
            // Flight inertia and control
            val targetY = if (jump) speed else if (isSneaking) -speed else 0f
            velocity.y += (targetY - velocity.y) * 10f * dt
        } else {
            // Apply gravity to velocity
            velocity.y -= 22f * dt
            if (velocity.y < -30f) velocity.y = -30f // terminal velocity
        }
        
        // Input applied to horizontal velocity
        val fw = camera.front.copy()
        fw.y = 0f; fw.normalize()
        val rt = camera.right.copy()
        rt.y = 0f; rt.normalize()
        
        val moveDir = fw * inputDirs.z + rt * inputDirs.x
        
        val targetX = moveDir.x * speed
        val targetZ = moveDir.z * speed
        
        if (isFlying) {
             velocity.x += (targetX - velocity.x) * 10f * dt
             velocity.z += (targetZ - velocity.z) * 10f * dt
        } else {
             velocity.x = targetX
             velocity.z = targetZ
        }
        
        if (!isFlying && jump && isGrounded) {
            velocity.y = 7.5f
            isGrounded = false
        }
        
        if (noclip) {
            // No collision resolution, just free motion!
            camera.position.x += velocity.x * dt
            camera.position.y += velocity.y * dt
            camera.position.z += velocity.z * dt
            isGrounded = false
        } else {
            val dx = velocity.x * dt
            val dy = velocity.y * dt
            val dz = velocity.z * dt
            
            var moveX = dx
            var moveY = dy
            var moveZ = dz
            
            val playerBox = getPlayerAABB(camera.position.x, camera.position.y, camera.position.z)
            
            // Expand AABB to include the destination region for broadphase checking
            val expandedBox = AABB(
                playerBox.minX + Math.min(0f, moveX) - 0.1f, playerBox.minY + Math.min(0f, moveY) - 0.1f, playerBox.minZ + Math.min(0f, moveZ) - 0.1f,
                playerBox.maxX + Math.max(0f, moveX) + 0.1f, playerBox.maxY + Math.max(0f, moveY) + 0.1f, playerBox.maxZ + Math.max(0f, moveZ) + 0.1f
            )
            
            val colliders = getSurroundingCollisionBoxes(expandedBox)
            
            // Resolve Y
            for (box in colliders) {
                moveY = playerBox.calculateYOffset(box, moveY)
            }
            playerBox.offset(0f, moveY, 0f)
            
            // Resolve X
            for (box in colliders) {
                moveX = playerBox.calculateXOffset(box, moveX)
            }
            playerBox.offset(moveX, 0f, 0f)
            
            // Resolve Z
            for (box in colliders) {
                moveZ = playerBox.calculateZOffset(box, moveZ)
            }
            playerBox.offset(0f, 0f, moveZ)
            
            camera.position.x += moveX
            camera.position.y += moveY
            camera.position.z += moveZ
            
            if (moveY != dy) {
                if (dy < 0) {
                    isGrounded = true
                    isFlying = false
                }
                velocity.y = 0f
            } else {
                isGrounded = false
            }
            
            if (moveX != dx) velocity.x = 0f
            if (moveZ != dz) velocity.z = 0f
        }
        
        // Out of bounds safety check (void fallback)
        if (camera.position.y < -10f) {
            val spawnY = world.getSpawnHeight(0, 0)
            camera.position.set(0.5f, spawnY, 0.5f)
            velocity.set(0f, 0f, 0f)
            isGrounded = true
        }
    }

    var breakingBx = -1
    var breakingBy = -1
    var breakingBz = -1
    var breakProgress = 0f

    fun updateBreaking(dt: Float, isBreaking: Boolean) {
        if (!isBreaking || !canBreak) {
            breakProgress = 0f
            breakingBy = -1
            return
        }

        var rx = camera.position.x
        var ry = camera.position.y
        var rz = camera.position.z
        
        val dx = camera.front.x
        val dy = camera.front.y
        val dz = camera.front.z
        
        var foundBx = -1
        var foundBy = -1
        var foundBz = -1
        
        for (i in 0..100) {
            rx += dx * 0.05f
            ry += dy * 0.05f
            rz += dz * 0.05f
            
            val bx = Math.floor(rx.toDouble()).toInt()
            val by = Math.floor(ry.toDouble()).toInt()
            val bz = Math.floor(rz.toDouble()).toInt()
            
            val block = world.getBlock(bx, by, bz)
            if (BlockRegistry.isSolid(block)) {
                foundBx = bx
                foundBy = by
                foundBz = bz
                break
            }
        }
        
        if (foundBy != -1) {
            if (foundBx != breakingBx || foundBy != breakingBy || foundBz != breakingBz) {
                breakingBx = foundBx
                breakingBy = foundBy
                breakingBz = foundBz
                breakProgress = 0f
            }
            
            // Depends on tool and block type, but simple 1s break time for now
            val equippedTool = inventory.getSelectedBlock()
            var speedMultiplier = 1.0f 
            if (equippedTool == BlockRegistry.WOODEN_PICKAXE) speedMultiplier = 2.0f
            if (equippedTool == BlockRegistry.STONE_PICKAXE) speedMultiplier = 4.0f
            
            breakProgress += dt * speedMultiplier
            
            if (breakProgress >= 1.0f) {
                world.setBlock(breakingBx, breakingBy, breakingBz, BlockRegistry.AIR)
                NetworkManager.sendBlockChange(breakingBx, breakingBy, breakingBz, BlockRegistry.AIR)
                breakProgress = 0f
                breakingBy = -1
            }
        } else {
            breakProgress = 0f
            breakingBy = -1
        }
    }

    fun raycastBlock(breakBlock: Boolean) {
        if (breakBlock) return // Breaking is handled by updateBreaking now
        if (!canBuild) return

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
                if (lastBy != -1) {
                    val blockBox = AABB(
                        lastBx.toFloat(), lastBy.toFloat(), lastBz.toFloat(),
                        lastBx.toFloat() + 1f, lastBy.toFloat() + 1f, lastBz.toFloat() + 1f
                    )
                    val playerBox = getPlayerAABB(camera.position.x, camera.position.y, camera.position.z)
                    if (noclip || !playerBox.intersects(blockBox)) {
                        val blockToPlace = inventory.getSelectedBlock()
                        if (!BlockRegistry.isItem(blockToPlace)) {
                            world.setBlock(lastBx, lastBy, lastBz, blockToPlace)
                            NetworkManager.sendBlockChange(lastBx, lastBy, lastBz, blockToPlace)
                        }
                    }
                }
                return
            }
            lastBx = bx
            lastBy = by
            lastBz = bz
        }
    }
}
