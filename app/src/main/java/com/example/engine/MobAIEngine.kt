package com.example.engine

import com.example.game.BlockRegistry
import com.example.game.Mob
import com.example.game.MobType
import com.example.game.Player
import com.example.world.World

object MobAIEngine {
    fun updateMobAI(mob: Mob, dt: Float, player: Player, world: World) {
        if (mob.isDead) return
        
        val playerPos = player.camera.position

        mob.timeSinceDirChange += dt
        mob.attackCooldown -= dt
        
        // Custom Mob AI behavior
        if (mob.timeSinceDirChange > 2f) {
            mob.timeSinceDirChange = 0f
            if (mob.type == MobType.ZOMBIE) {
                // Zombie follows player if within 18 blocks
                val dx = playerPos.x - mob.position.x
                val dz = playerPos.z - mob.position.z
                val dist = Math.sqrt((dx * dx + dz * dz).toDouble()).toFloat()
                if (dist < 18f) {
                    mob.targetDir.set(dx / dist, 0f, dz / dist)
                } else {
                    mob.targetDir.set((Math.random() - 0.5).toFloat(), 0f, (Math.random() - 0.5).toFloat())
                    mob.targetDir.normalize()
                }
            } else {
                // Pig wandering randomly
                mob.targetDir.set((Math.random() - 0.5).toFloat(), 0f, (Math.random() - 0.5).toFloat())
                mob.targetDir.normalize()
            }
        }
        
        // Attacking behavior for hostile mobs
        if (mob.type == MobType.ZOMBIE && mob.attackCooldown <= 0f && player.gameMode == Player.GameMode.SURVIVAL) {
            val dx = playerPos.x - mob.position.x
            val dy = playerPos.y - mob.position.y
            val dz = playerPos.z - mob.position.z
            val dist = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
            if (dist < 2.0f) {
                // If player has gold hearts, consume those first, otherwise subtract regular health
                if (player.goldHearts > 0) {
                    player.goldHearts -= 1
                } else {
                    player.health -= 2
                }
                mob.attackCooldown = 1.2f // attack delay
            }
        }

        // Calculate horizontal movement
        val speed = if (mob.type == MobType.ZOMBIE) 1.6f else 1.1f
        mob.velocity.x = mob.targetDir.x * speed
        mob.velocity.z = mob.targetDir.z * speed
        
        // Gravity and floor collision check
        mob.velocity.y -= 18f * dt
        if (mob.velocity.y < -25f) mob.velocity.y = -25f
        
        // Use PhysicalEngine for reliable collision and auto-jumping
        val wasGrounded = PhysicalEngine.resolveCollision(
            world = world,
            pos = mob.position,
            vel = mob.velocity,
            dt = dt,
            w = 0.6f,
            h = 1.8f,
            noclip = false
        )
        
        // Mob Auto-jump when hitting an obstacle!
        if (!wasGrounded && Math.abs(mob.velocity.x) < 0.1f && Math.abs(mob.velocity.z) < 0.1f) {
            // Check if there is an obstacle in front and we can jump over it
            val frontX = mob.position.x + mob.targetDir.x * 0.5f
            val frontZ = mob.position.z + mob.targetDir.z * 0.5f
            val floorBlockY = Math.floor(mob.position.y.toDouble()).toInt() - 1
            val obstacleBlock = world.getBlock(Math.floor(frontX.toDouble()).toInt(), floorBlockY + 1, Math.floor(frontZ.toDouble()).toInt())
            if (BlockRegistry.isSolid(obstacleBlock)) {
                mob.velocity.y = 5.5f // Jump!
            }
        }
    }
}
