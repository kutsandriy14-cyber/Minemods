package com.example.engine

import android.content.Context
import androidx.compose.runtime.*
import com.example.game.Player
import com.example.world.World
import com.example.world.WorldSaveManager
import com.example.game.BlockRegistry
import kotlinx.coroutines.*
import java.io.File

object GameEngine {
    var world by mutableStateOf<World?>(null)
    var player by mutableStateOf<Player?>(null)
    
    var moveForward by mutableStateOf(false)
    var moveBackward by mutableStateOf(false)
    var moveLeft by mutableStateOf(false)
    var moveRight by mutableStateOf(false)
    var moveUp by mutableStateOf(false)
    var moveDown by mutableStateOf(false)
    var jump by mutableStateOf(false)
    var isBreaking by mutableStateOf(false)
    
    var showAdminPanel by mutableStateOf(false)
    
    var posX by mutableStateOf(0f)
    var posY by mutableStateOf(0f)
    var posZ by mutableStateOf(0f)
    
    var currentFps by mutableStateOf(0)
    
    // Preferences values
    var fpsLimit by mutableStateOf(60)
    var renderDistance by mutableStateOf(8f)
    var textureQuality by mutableStateOf("HIGH")
    var cameraSensitivity by mutableStateOf(1.0f)
    
    var isConnecting by mutableStateOf(false)
    var connectionError by mutableStateOf<String?>(null)
    
    var triggerRecompose by mutableStateOf(0)
    var currentGameWorldName by mutableStateOf("World")

    fun initGame(context: Context, worldName: String, mode: String, ip: String) {
        currentGameWorldName = worldName
        val newWorld = World().apply {
            if (mode == "client") {
                seed = 123456L
            }
        }
        val newPlayer = Player(newWorld).apply {
            if (mode == "client") {
                camera.position.set(8.0f, 65f, 8.0f)
            } else {
                camera.position.set(0.5f, 80f, 0.5f)
            }
        }
        
        world = newWorld
        player = newPlayer
        isConnecting = (mode == "client")
        connectionError = null
        
        // Load preferences
        val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        fpsLimit = preferences.getInt("fpsLimit", 60)
        renderDistance = preferences.getFloat("renderDistance", 8f)
        textureQuality = preferences.getString("textureQuality", "HIGH") ?: "HIGH"
        cameraSensitivity = preferences.getFloat("sensitivity", 1.0f)
    }
    
    fun loadWorldData(context: Context, worldName: String, mode: String) {
        val w = world ?: return
        val p = player ?: return
        if (mode != "client") {
            val saveFile = File(WorldSaveManager.getWorldsDir(context), "$worldName/level.dat")
            if (!saveFile.exists()) {
                w.seed = worldName.hashCode().toLong()
                w.generateInitialWorld()
                val spawnY = w.getSpawnHeight(0, 0)
                p.camera.position.set(0.5f, spawnY, 0.5f)
            } else {
                WorldSaveManager.loadWorld(context, worldName, w, p)
            }
        }
    }

    fun startNetworking(mode: String, ip: String) {
        val w = world ?: return
        if (mode == "host") {
            NetworkManager.startHost(w, currentGameWorldName) {}
        } else if (mode == "client") {
            NetworkManager.startClient(
                hostIp = ip,
                world = w,
                onConnectSuccess = {
                    isConnecting = false
                },
                onConnectFailed = {
                    connectionError = NetworkManager.connectionError ?: "Failed to connect to Host at $ip.\nCheck IP address, Wi-Fi network, and make sure the Host has started the game."
                }
            )
        }

        NetworkManager.setOnBlockChangeCallback { bx, by, bz, type ->
            val cx = bx shr 4
            val cz = bz shr 4
            world?.chunks?.get(Pair(cx, cz))?.isModified = true
        }
    }

    fun updateGameTick(dt: Float) {
        val p = player ?: return
        val w = world ?: return
        
        val dirs = Vector3f(
            if(moveRight) 1f else if(moveLeft) -1f else 0f,
            if(moveUp) 1f else if(moveDown) -1f else 0f,
            if(moveForward) 1f else if(moveBackward) -1f else 0f
        )
        p.update(dt, dirs, jump, moveDown)
        p.updateBreaking(dt, isBreaking)
        
        // Update 3D particle systems
        com.example.engine.ParticleEngine.update(dt)
        
        // Update mobs
        w.mobs.removeIf { it.isDead }
        if (w.mobs.size < 5) {
            if (Math.random() < 0.01) {
                val spawnX = p.camera.position.x + (Math.random() - 0.5f) * 20f
                val spawnZ = p.camera.position.z + (Math.random() - 0.5f) * 20f
                val spawnY = w.getTerrainHeight(Math.floor(spawnX).toInt(), Math.floor(spawnZ).toInt()).toFloat() + 2f
                w.mobs.add(com.example.game.Mob(
                    if (Math.random() < 0.5) com.example.game.MobType.PIG else com.example.game.MobType.ZOMBIE,
                    w, spawnX.toFloat(), spawnY, spawnZ.toFloat()
                ))
            }
        }
        for (mob in w.mobs) {
            mob.update(dt, p)
        }
        
        posX = p.camera.position.x
        posY = p.camera.position.y
        posZ = p.camera.position.z
    }
    
    fun shutdown(context: Context, worldName: String) {
        val w = world ?: return
        val p = player ?: return
        com.example.engine.ChunkLoadEngine.stopEngine()
        WorldSaveManager.saveWorld(context, worldName, w, p)
        NetworkManager.stop()
    }
}
