package com.example.engine

import com.example.game.BlockRegistry
import com.example.world.SimplexNoise

object WorldEngine {
    const val WATER_LEVEL = 70
    
    // Biome IDs
    const val BIOME_PLAINS: Byte = 0
    const val BIOME_DESERT: Byte = 1
    const val BIOME_FOREST: Byte = 2
    const val BIOME_MOUNTAINS: Byte = 3

    fun getTerrainHeight(x: Int, z: Int, noise: SimplexNoise): Int {
        val nx = x.toFloat() * 0.004f
        val nz = z.toFloat() * 0.004f
        
        val biomeSelector = noise.noise2D(nx * 0.5f + 1000f, nz * 0.5f + 1000f)
        
        // Base height and multipliers depending on selector (mountainous vs flat)
        return if (biomeSelector > 0.3f) {
            // Mountain Biome height scale
            val base = 85f
            val peaks = noise.noise2D(nx * 3.0f, nz * 3.0f) * 45f
            val detail = noise.noise2D(nx * 10.0f, nz * 10.0f) * 10f
            (base + peaks + detail).toInt().coerceIn(30, 180)
        } else if (biomeSelector < -0.4f) {
            // Desert Flat Biome
            val base = 73f
            val dunes = noise.noise2D(nx * 2.0f, nz * 2.0f) * 6f
            (base + dunes).toInt().coerceIn(60, 95)
        } else {
            // Plains/Forest gentle hills
            val base = 78f
            val hills = noise.noise2D(nx * 1.5f, nz * 1.5f) * 12f
            val detail = noise.noise2D(nx * 6.0f, nz * 6.0f) * 3f
            (base + hills + detail).toInt().coerceIn(50, 115)
        }
    }

    fun getBiomeType(x: Int, z: Int, noise: SimplexNoise): Byte {
        val nx = x.toFloat() * 0.004f
        val nz = z.toFloat() * 0.004f
        
        val biomeSelector = noise.noise2D(nx * 0.5f + 1000f, nz * 0.5f + 1000f)
        val moisture = noise.noise2D(nx * 0.8f + 500f, nz * 0.8f + 500f)

        return when {
            biomeSelector > 0.3f -> BIOME_MOUNTAINS
            biomeSelector < -0.4f -> BIOME_DESERT
            moisture > 0.15f -> BIOME_FOREST
            else -> BIOME_PLAINS
        }
    }

    fun getSurfaceBlockForBiome(biome: Byte, height: Int): Byte {
        return when (biome) {
            BIOME_DESERT -> BlockRegistry.SAND
            BIOME_MOUNTAINS -> {
                if (height > 125) BlockRegistry.STONE // Rocky snow peaks or bare stone peaks
                else BlockRegistry.GRASS
            }
            else -> BlockRegistry.GRASS
        }
    }

    fun isCave(x: Int, y: Int, z: Int, noise: SimplexNoise): Boolean {
        if (y > 130) return false // No caves in high peaks
        
        // 3D Cheese caves
        val cheese = noise.noise3D(x * 0.025f, y * 0.03f, z * 0.025f)
        if (cheese > 0.38f) return true
        
        // Spaghetti caves (winding tunnels)
        val spaghetti1 = noise.noise3D(x * 0.04f, y * 0.04f, z * 0.04f)
        val spaghetti2 = noise.noise3D(x * 0.04f + 200f, y * 0.04f + 200f, z * 0.04f + 200f)
        val isSpaghetti = Math.abs(spaghetti1) < 0.05f && Math.abs(spaghetti2) < 0.05f
        if (isSpaghetti) return true
        
        return false
    }
}
