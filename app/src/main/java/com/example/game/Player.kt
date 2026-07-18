package com.example.game

import com.example.engine.*
import com.example.world.World

class Player(val world: World) {
    val camera = Camera()
    
    enum class GameMode {
        SURVIVAL,
        CREATIVE,
        SPECTATOR
    }

    var health: Int = 20
    var maxHealth: Int = 20
    var goldHearts: Int = 0 // Absorption hearts
    
    var gameMode: GameMode = GameMode.SURVIVAL
        set(value) {
            field = value
            when (value) {
                GameMode.SURVIVAL -> {
                    canFly = false
                    noclip = false
                    canBuild = true
                    canBreak = true
                    for (i in 0 until inventory.size) inventory.items[i] = BlockRegistry.AIR
                    inventory.items[0] = BlockRegistry.WOODEN_PICKAXE
                }
                GameMode.CREATIVE -> {
                    canFly = true
                    noclip = false
                    canBuild = true
                    canBreak = true
                    val creativeItems = byteArrayOf(
                        BlockRegistry.WOODEN_PICKAXE, BlockRegistry.STONE_PICKAXE, BlockRegistry.IRON_PICKAXE, BlockRegistry.DIAMOND_PICKAXE,
                        BlockRegistry.WOODEN_SWORD, BlockRegistry.STONE_SWORD, BlockRegistry.IRON_SWORD, BlockRegistry.DIAMOND_SWORD,
                        BlockRegistry.GRASS, BlockRegistry.DIRT, BlockRegistry.STONE, BlockRegistry.COBBLESTONE,
                        BlockRegistry.WOOD, BlockRegistry.PLANKS, BlockRegistry.LEAVES, BlockRegistry.GLASS,
                        BlockRegistry.SAND, BlockRegistry.WATER, BlockRegistry.LAVA, BlockRegistry.BEDROCK,
                        BlockRegistry.COAL_ORE, BlockRegistry.IRON_ORE, BlockRegistry.GOLD_ORE, BlockRegistry.DIAMOND_ORE,
                        BlockRegistry.COAL, BlockRegistry.IRON_INGOT, BlockRegistry.GOLD_INGOT, BlockRegistry.DIAMOND,
                        BlockRegistry.ROSE, BlockRegistry.DANDELION, BlockRegistry.TALL_GRASS, BlockRegistry.CACTUS,
                        BlockRegistry.GOLDEN_APPLE, BlockRegistry.POTION_HEALING
                    )
                    for (i in 0 until Math.min(inventory.size, creativeItems.size)) {
                        inventory.items[i] = creativeItems[i]
                    }
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

        // Active speed effect update
        val speedMultiplier = EffectsEngine.getSpeedMultiplier()
        val jumpMultiplier = EffectsEngine.getJumpMultiplier()

        val speed = if (isFlying) 12.5f * speedMultiplier else 5.5f * speedMultiplier
        
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
            velocity.y = 7.5f * jumpMultiplier
            isGrounded = false
            // Spawn jumping dust/grass particles at player's feet
            com.example.engine.ParticleEngine.spawnJumpParticles(camera.position.x, camera.position.y, camera.position.z)
        }
        
        // Resolve movement and collisions using PhysicalEngine
        isGrounded = PhysicalEngine.resolveCollision(
            world = world,
            pos = camera.position,
            vel = velocity,
            dt = dt,
            w = 0.6f,
            h = 1.8f,
            noclip = noclip
        )
        if (isGrounded) {
            isFlying = false
        }
        
        // Update status effects tick via EffectsEngine
        EffectsEngine.updateEffects(dt, this)
        
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

        val dx = camera.front.x
        val dy = camera.front.y
        val dz = camera.front.z
        
        // Check for mobs in front first to attack them
        for (mob in world.mobs) {
            if (mob.isDead) continue
            val mdx = mob.position.x - camera.position.x
            val mdy = mob.position.y + 0.9f - camera.position.y
            val mdz = mob.position.z - camera.position.z
            val dist = Math.sqrt((mdx * mdx + mdy * mdy + mdz * mdz).toDouble()).toFloat()
            if (dist < 3f) {
                // Dot product to see if looking at it
                val dot = (mdx / dist) * dx + (mdy / dist) * dy + (mdz / dist) * dz
                if (dot > 0.9f) {
                    // Attack mob with ItemEngine damage calculations!
                    val weapon = inventory.getSelectedBlock()
                    val damage = ItemEngine.getWeaponDamage(weapon)
                    mob.health -= damage
                    if (mob.health <= 0) {
                        mob.isDead = true
                    }
                    breakProgress = -1f // Cooldown delay
                    return
                }
            }
        }
        
        if (breakProgress < 0f) {
            breakProgress += dt
            if (breakProgress >= 0f) breakProgress = 0f
            return
        }

        // Raycast blocks using RenderPhysicsEngine
        val ray = RenderPhysicsEngine.raycast(
            world, camera.position.x, camera.position.y, camera.position.z,
            dx, dy, dz, 5.0f
        )
        
        if (ray != null) {
            val foundBx = ray.bx
            val foundBy = ray.by
            val foundBz = ray.bz
            
            if (foundBx != breakingBx || foundBy != breakingBy || foundBz != breakingBz) {
                breakingBx = foundBx
                breakingBy = foundBy
                breakingBz = foundBz
                breakProgress = 0f
            }
            
            val equippedTool = inventory.getSelectedBlock()
            val brokenBlock = world.getBlock(breakingBx, breakingBy, breakingBz)
            val speedMultiplier = ItemEngine.getToolMiningSpeed(equippedTool, brokenBlock)
            
            breakProgress += dt * speedMultiplier
            
            if (breakProgress >= 1.0f) {
                // Spawn beautiful block debris break particles matching the block type
                com.example.engine.ParticleEngine.spawnBlockBreakParticles(breakingBx, breakingBy, breakingBz, brokenBlock)
                
                world.setBlock(breakingBx, breakingBy, breakingBz, BlockRegistry.AIR)
                NetworkManager.sendBlockChange(breakingBx, breakingBy, breakingBz, BlockRegistry.AIR)
                
                // Add to inventory in survival
                if (gameMode == GameMode.SURVIVAL && !BlockRegistry.isItem(brokenBlock)) {
                    for (i in 0 until inventory.size) {
                        if (inventory.items[i] == BlockRegistry.AIR || inventory.items[i] == brokenBlock) {
                            inventory.items[i] = brokenBlock
                            break
                        }
                    }
                }
                
                breakProgress = 0f
                breakingBy = -1
            }
        } else {
            breakProgress = 0f
            breakingBy = -1
        }
    }

    fun useItem() {
        val selectedBlock = inventory.getSelectedBlock()
        if (ItemEngine.isFoodOrPotion(selectedBlock)) {
            EffectsEngine.consumeItem(selectedBlock, this)
        }
    }

    fun raycastBlock(breakBlock: Boolean) {
        if (breakBlock) return // Breaking is handled by updateBreaking now
        if (!canBuild) return

        // Raycast blocks using RenderPhysicsEngine
        val ray = RenderPhysicsEngine.raycast(
            world, camera.position.x, camera.position.y, camera.position.z,
            camera.front.x, camera.front.y, camera.front.z, 5.0f
        )
        
        if (ray != null) {
            // Find placing spot based on targeted face
            val placeX = ray.bx + when (ray.face) {
                2 -> -1
                3 -> 1
                else -> 0
            }
            val placeY = ray.by + when (ray.face) {
                1 -> -1
                0 -> 1
                else -> 0
            }
            val placeZ = ray.bz + when (ray.face) {
                4 -> -1
                5 -> 1
                else -> 0
            }
            
            val blockBox = PhysicalEngine.AABB(
                placeX.toFloat(), placeY.toFloat(), placeZ.toFloat(),
                placeX.toFloat() + 1f, placeY.toFloat() + 1f, placeZ.toFloat() + 1f
            )
            val playerBox = PhysicalEngine.getEntityAABB(camera.position.x, camera.position.y, camera.position.z, 0.6f, 1.8f)
            
            if (noclip || !playerBox.intersects(blockBox)) {
                val blockToPlace = inventory.getSelectedBlock()
                if (!BlockRegistry.isItem(blockToPlace)) {
                    world.setBlock(placeX, placeY, placeZ, blockToPlace)
                    NetworkManager.sendBlockChange(placeX, placeY, placeZ, blockToPlace)
                }
            }
        }
    }
}
