package com.example.game

import com.example.engine.MobAIEngine
import com.example.engine.Vector3f
import com.example.world.World

enum class MobType {
    PIG, ZOMBIE
}

class Mob(val type: MobType, val world: World, startX: Float, startY: Float, startZ: Float) {
    val position = Vector3f(startX, startY, startZ)
    val velocity = Vector3f(0f, 0f, 0f)
    var health = if (type == MobType.PIG) 10 else 20
    var isDead = false

    var targetDir = Vector3f((Math.random() - 0.5).toFloat(), 0f, (Math.random() - 0.5).toFloat()).apply { normalize() }
    var timeSinceDirChange = 0f
    var attackCooldown = 0f

    fun update(dt: Float, player: Player) {
        MobAIEngine.updateMobAI(this, dt, player, world)
    }
}

