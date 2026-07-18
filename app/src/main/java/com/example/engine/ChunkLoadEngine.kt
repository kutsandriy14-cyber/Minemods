package com.example.engine

import com.example.world.World
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

object ChunkLoadEngine {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val workerJobs = mutableListOf<Job>()
    
    // Thread safe queues for background chunk requests
    private val chunkQueue = ConcurrentLinkedQueue<Pair<Int, Int>>()
    private val processingSet = java.util.concurrent.ConcurrentHashMap.newKeySet<Pair<Int, Int>>()

    fun startEngine(world: World, playerX: Float, playerZ: Float, radius: Int) {
        stopEngine()
        
        // Spawn 4 concurrent background chunk workers for multi-threaded chunk generation
        for (i in 0 until 4) {
            val job = scope.launch {
                while (isActive) {
                    val nextChunkCoords = chunkQueue.poll()
                    if (nextChunkCoords != null) {
                        processingSet.add(nextChunkCoords)
                        val cx = nextChunkCoords.first
                        val cz = nextChunkCoords.second
                        if (!world.chunks.containsKey(nextChunkCoords)) {
                            world.generateChunk(cx, cz)
                        }
                        processingSet.remove(nextChunkCoords)
                    } else {
                        delay(30) // Wait longer when queue is empty to conserve CPU
                    }
                    delay(5) // Allow other threads and UI thread to breathe
                }
            }
            workerJobs.add(job)
        }
    }

    fun stopEngine() {
        for (job in workerJobs) {
            job.cancel()
        }
        workerJobs.clear()
    }

    // Requests load/generation of a chunk surrounding the player
    fun queueChunkLoad(cx: Int, cz: Int, world: World) {
        val coords = Pair(cx, cz)
        if (!world.chunks.containsKey(coords) && !processingSet.contains(coords) && !chunkQueue.contains(coords)) {
            chunkQueue.add(coords)
        }
    }

    // Handles the active radius of chunks around the player, queueing needed ones and recycling distant ones
    fun updatePlayerLoadRadius(world: World, playerX: Float, playerZ: Float, radius: Int) {
        val playerCx = Math.floor(playerX.toDouble() / 16.0).toInt()
        val playerCz = Math.floor(playerZ.toDouble() / 16.0).toInt()
        
        // Optimize: Sort candidate chunk positions by proximity to player before queuing
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (cx in (playerCx - radius)..(playerCx + radius)) {
            for (cz in (playerCz - radius)..(playerCz + radius)) {
                candidates.add(Pair(cx, cz))
            }
        }
        
        // Closest chunks first (Euclidean distance sorting)
        candidates.sortBy { (cx, cz) ->
            val dx = cx - playerCx
            val dz = cz - playerCz
            dx * dx + dz * dz
        }
        
        // Queue chunk loads in prioritized order
        for (coords in candidates) {
            queueChunkLoad(coords.first, coords.second, world)
        }
        
        // Recycle chunks further than the unload threshold
        val unloadThreshold = radius + 2
        val iterator = world.chunks.keys.iterator()
        while (iterator.hasNext()) {
            val coords = iterator.next()
            val dx = Math.abs(coords.first - playerCx)
            val dz = Math.abs(coords.second - playerCz)
            if (dx > unloadThreshold || dz > unloadThreshold) {
                val removed = world.chunks[coords]
                if (removed != null) {
                    world.meshesToDestroy.add(removed.mesh)
                }
                iterator.remove()
            }
        }
    }
}
