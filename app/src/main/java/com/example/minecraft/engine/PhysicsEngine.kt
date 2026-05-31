package com.example.minecraft.engine

import com.example.minecraft.model.BlockType
import com.example.minecraft.model.GameRegistry
import com.example.minecraft.model.PlayerState
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

object PhysicsEngine {
    private const val GRAVITY = -0.015f
    private const val MAX_FALL_SPEED = -0.4f
    private const val JUMP_FORCE = 0.22f
    private const val PLAYER_WIDTH = 0.6f
    private const val PLAYER_HEIGHT = 1.8f
    private const val HORIZONTAL_SPEED = 0.08f

    /**
     * Updates player position and velocity for one game tick.
     * @return Damaged health if fall damage occurs, or 0.
     */
    fun updatePlayer(
        player: PlayerState,
        moveX: Float, // -1.0 for left, 1.0 for right, 0 for idle
        jumpPressed: Boolean,
        worldBlocks: Map<String, String>,
        worldWidth: Int,
        worldHeight: Int
    ): Float {
        var damageTaken = 0f

        // Apply Horizontal movement
        player.velocityX = moveX * HORIZONTAL_SPEED

        // Apply Gravity
        if (!player.isGrounded) {
            player.velocityY = (player.velocityY + GRAVITY).coerceAtLeast(MAX_FALL_SPEED)
        } else {
            if (player.velocityY < 0) {
                // Check fall speed for fall damage
                val fallVelocity = player.velocityY
                if (fallVelocity < -0.28f) {
                    val rawDmg = (abs(fallVelocity) * 45f - 8f).coerceIn(0f, 20f)
                    if (rawDmg > 1.0f) {
                        damageTaken = floor(rawDmg)
                    }
                }
                player.velocityY = 0f
            }
            
            // Jump trigger
            if (jumpPressed) {
                player.velocityY = JUMP_FORCE
                player.isGrounded = false
            }
        }

        // Try movement along X axis
        val originalX = player.x
        player.x += player.velocityX
        // Clamp to world bounds
        val maxX = worldWidth.toFloat() - PLAYER_WIDTH - 0.2f
        if (player.x < 0.2f) player.x = 0.2f
        if (player.x > maxX) player.x = maxX

        // Check if player intersects solid blocks along X
        if (checkCollision(player.x, player.y, worldBlocks)) {
            // Revert X movement if collided
            player.x = originalX
            player.velocityX = 0f
        }

        // Try movement along Y axis
        val originalY = player.y
        player.y += player.velocityY
        player.isGrounded = false

        // Check if player intersects solid blocks along Y
        if (checkCollision(player.x, player.y, worldBlocks)) {
            // If moving down, we hit the ground
            if (player.velocityY < 0) {
                // Snap player position to the top of the block below them
                player.y = ceil(player.y)
                player.isGrounded = true
            } else if (player.velocityY > 0) {
                // Hit head on ceiling!
                player.y = floor(player.y)
                player.velocityY = 0f
            }
        }

        // Secondary check: is there ground underneath?
        // Check if 0.05 units below player is solid. If not solid, player is no longer grounded.
        if (player.isGrounded && !checkCollision(player.x, player.y - 0.05f, worldBlocks)) {
            player.isGrounded = false
        }

        return damageTaken
    }

    /**
     * Checks if a player positioned at bounding box (x .. x+PLAYER_WIDTH, y .. y+PLAYER_HEIGHT)
     * overlaps any solid blocks.
     */
    fun checkCollision(px: Float, py: Float, worldBlocks: Map<String, String>): Boolean {
        val minBlockX = floor(px).toInt()
        val maxBlockX = floor(px + PLAYER_WIDTH).toInt()
        val minBlockY = floor(py).toInt()
        val maxBlockY = floor(py + PLAYER_HEIGHT).toInt()

        for (bx in minBlockX..maxBlockX) {
            for (by in minBlockY..maxBlockY) {
                val blockId = worldBlocks["$bx,$by"] ?: "air"
                val bType = GameRegistry.blocks[blockId]
                if (bType != null && bType.isSolid) {
                    // Check intersection bounding box
                    if (boxOverlap(
                            px, py, PLAYER_WIDTH, PLAYER_HEIGHT,
                            bx.toFloat(), by.toFloat(), 1.0f, 1.0f
                        )
                    ) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun boxOverlap(
        x1: Float, y1: Float, w1: Float, h1: Float,
        x2: Float, y2: Float, w2: Float, h2: Float
    ): Boolean {
        return x1 < x2 + w2 && x1 + w1 > x2 &&
                y1 < y2 + h2 && y1 + h1 > y2
    }
}
