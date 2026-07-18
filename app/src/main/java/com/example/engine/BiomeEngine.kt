package com.example.engine

import com.example.game.BlockRegistry
import com.example.world.SimplexNoise

object BiomeEngine {
    // Biome IDs
    const val BIOME_PLAINS: Byte = 0
    const val BIOME_DESERT: Byte = 1
    const val BIOME_FOREST: Byte = 2
    const val BIOME_MOUNTAINS: Byte = 3
    const val BIOME_SNOWY_TUNDRA: Byte = 4
    const val BIOME_VOLCANIC_WASTELAND: Byte = 5
    const val BIOME_JUNGLE: Byte = 6

    fun getBiomeType(x: Int, z: Int, noise: SimplexNoise): Byte {
        val nx = x.toFloat() * 0.003f
        val nz = z.toFloat() * 0.003f
        
        // Use multi-octave biome selector noise
        val selector = noise.noise2D(nx * 0.4f + 1200f, nz * 0.4f + 1200f)
        val moisture = noise.noise2D(nx * 0.9f + 600f, nz * 0.9f + 600f)
        val temperature = noise.noise2D(nx * 0.7f + 3000f, nz * 0.7f + 3000f)

        return when {
            temperature < -0.4f -> BIOME_SNOWY_TUNDRA
            temperature > 0.5f && moisture < -0.3f -> BIOME_DESERT
            temperature > 0.4f && moisture > 0.4f -> BIOME_JUNGLE
            temperature > 0.3f && moisture < -0.4f -> BIOME_VOLCANIC_WASTELAND
            selector > 0.35f -> BIOME_MOUNTAINS
            moisture > 0.12f -> BIOME_FOREST
            else -> BIOME_PLAINS
        }
    }

    fun getTerrainHeight(x: Int, z: Int, noise: SimplexNoise): Int {
        val nx = x.toFloat() * 0.004f
        val nz = z.toFloat() * 0.004f
        
        val biome = getBiomeType(x, z, noise)
        
        return when (biome) {
            BIOME_SNOWY_TUNDRA -> {
                val base = 74f
                val hills = noise.noise2D(nx * 1.5f, nz * 1.5f) * 6f
                (base + hills).toInt().coerceIn(65, 95)
            }
            BIOME_VOLCANIC_WASTELAND -> {
                val base = 80f
                val ridges = Math.abs(noise.noise2D(nx * 2.0f, nz * 2.0f)) * 35f
                val craters = if (noise.noise2D(nx * 0.8f + 50f, nz * 0.8f + 50f) > 0.6f) -25f else 0f
                (base + ridges + craters).toInt().coerceIn(50, 140)
            }
            BIOME_JUNGLE -> {
                val base = 79f
                val hills = noise.noise2D(nx * 2.5f, nz * 2.5f) * 20f
                val detail = noise.noise2D(nx * 8f, nz * 8f) * 4f
                (base + hills + detail).toInt().coerceIn(55, 120)
            }
            BIOME_MOUNTAINS -> {
                val base = 88f
                val peaks = noise.noise2D(nx * 2.8f, nz * 2.8f) * 48f
                val detail = noise.noise2D(nx * 9f, nz * 9f) * 8f
                (base + peaks + detail).toInt().coerceIn(30, 185)
            }
            BIOME_DESERT -> {
                val base = 73f
                val dunes = noise.noise2D(nx * 2.2f, nz * 2.2f) * 8f
                (base + dunes).toInt().coerceIn(60, 95)
            }
            BIOME_FOREST -> {
                val base = 77f
                val hills = noise.noise2D(nx * 1.2f, nz * 1.2f) * 10f
                val detail = noise.noise2D(nx * 6f, nz * 6f) * 2f
                (base + hills + detail).toInt().coerceIn(55, 110)
            }
            else -> { // BIOME_PLAINS
                val base = 76f
                val hills = noise.noise2D(nx * 1.0f, nz * 1.0f) * 8f
                val detail = noise.noise2D(nx * 5f, nz * 5f) * 2f
                (base + hills + detail).toInt().coerceIn(50, 105)
            }
        }
    }

    fun getSurfaceBlockForBiome(biome: Byte, height: Int): Byte {
        return when (biome) {
            BIOME_DESERT -> BlockRegistry.SAND
            BIOME_SNOWY_TUNDRA -> BlockRegistry.SNOW
            BIOME_VOLCANIC_WASTELAND -> BlockRegistry.BASALT
            BIOME_MOUNTAINS -> {
                if (height > 125) BlockRegistry.SNOW
                else if (height > 105) BlockRegistry.STONE
                else BlockRegistry.GRASS
            }
            BIOME_JUNGLE -> BlockRegistry.GRASS
            else -> BlockRegistry.GRASS
        }
    }

    fun getUndergroundBlockForBiome(biome: Byte): Byte {
        return when (biome) {
            BIOME_VOLCANIC_WASTELAND -> BlockRegistry.STONE
            BIOME_SNOWY_TUNDRA -> BlockRegistry.DIRT
            BIOME_DESERT -> BlockRegistry.SAND
            else -> BlockRegistry.DIRT
        }
    }
}
