package com.example.engine

import com.example.game.BlockRegistry
import com.example.game.Player

object EffectsEngine {
    enum class EffectType {
        REGENERATION,
        SPEED,
        JUMP_BOOST,
        ABSORPTION
    }

    data class ActiveEffect(
        val type: EffectType,
        var durationSeconds: Float,
        val amplifier: Int
    )

    val activeEffects = mutableListOf<ActiveEffect>()

    fun applyEffect(type: EffectType, durationSeconds: Float, amplifier: Int, player: Player) {
        val existing = activeEffects.find { it.type == type }
        if (existing != null) {
            existing.durationSeconds = Math.max(existing.durationSeconds, durationSeconds)
        } else {
            activeEffects.add(ActiveEffect(type, durationSeconds, amplifier))
        }

        // Apply instant changes
        if (type == EffectType.ABSORPTION) {
            player.goldHearts = 4 * (amplifier + 1)
        }
    }

    fun updateEffects(dt: Float, player: Player) {
        val iterator = activeEffects.iterator()
        while (iterator.hasNext()) {
            val effect = iterator.next()
            effect.durationSeconds -= dt
            
            // Handle ticking behaviors
            when (effect.type) {
                EffectType.REGENERATION -> {
                    // Regenerate half a heart every 3 seconds per amplifier level
                    if (Math.random() < 0.3 * dt * (effect.amplifier + 1)) {
                        if (player.health < player.maxHealth) {
                            player.health = Math.min(player.health + 1, player.maxHealth)
                        }
                    }
                }
                else -> {}
            }

            if (effect.durationSeconds <= 0f) {
                iterator.remove()
            }
        }
    }

    fun consumeItem(item: Byte, player: Player) {
        when (item) {
            BlockRegistry.GOLDEN_APPLE -> {
                player.health = Math.min(player.health + 4, player.maxHealth)
                applyEffect(EffectType.ABSORPTION, 120f, 0, player)
                applyEffect(EffectType.REGENERATION, 15f, 1, player)
                // Spawn golden healing particles around player
                com.example.engine.ParticleEngine.spawnHealParticles(player.camera.position.x, player.camera.position.y, player.camera.position.z)
            }
            BlockRegistry.POTION_HEALING -> {
                player.health = Math.min(player.health + 8, player.maxHealth)
                applyEffect(EffectType.SPEED, 45f, 0, player)
                // Spawn magical potion aura sparkles
                com.example.engine.ParticleEngine.spawnHealParticles(player.camera.position.x, player.camera.position.y, player.camera.position.z)
            }
        }
    }

    fun getSpeedMultiplier(): Float {
        val speedEffect = activeEffects.find { it.type == EffectType.SPEED }
        return if (speedEffect != null) 1.5f + (0.25f * speedEffect.amplifier) else 1.0f
    }

    fun getJumpMultiplier(): Float {
        val jumpEffect = activeEffects.find { it.type == EffectType.JUMP_BOOST }
        return if (jumpEffect != null) 1.3f + (0.15f * jumpEffect.amplifier) else 1.0f
    }
}
