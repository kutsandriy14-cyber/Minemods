package com.example.engine

import android.util.Log
import com.example.world.World
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

object NetworkManager {
    private const val TAG = "NetworkManager"
    private const val PORT = 12345

    var isHost = false
    var isClient = false
    var myClientId: String = "Player_" + (1000..9999).random()
    
    // Remote players coordinates
    val remotePlayers = ConcurrentHashMap<String, Vector3f>()
    
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var serverJob: Job? = null
    private var clientJob: Job? = null
    
    // Keep list of client handlers on server
    private val clientHandlers = ConcurrentHashMap<String, ClientHandler>()
    
    // Callback to update the world on block changes
    private var onBlockChangeCallback: ((Int, Int, Int, Byte) -> Unit)? = null
    
    fun setOnBlockChangeCallback(callback: (Int, Int, Int, Byte) -> Unit) {
        onBlockChangeCallback = callback
    }

    fun startHost(world: World, onReady: () -> Unit) {
        Log.d(TAG, "Starting host server on port $PORT...")
        isHost = true
        isClient = false
        remotePlayers.clear()
        clientHandlers.clear()
        
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(PORT)
                onReady()
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    val handler = ClientHandler(socket, world)
                    CoroutineScope(Dispatchers.IO).launch {
                        handler.run()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error: ${e.message}")
            }
        }
    }

    fun startClient(hostIp: String, world: World, onConnectSuccess: () -> Unit, onConnectFailed: () -> Unit) {
        Log.d(TAG, "Connecting to host $hostIp:$PORT...")
        isHost = false
        isClient = true
        remotePlayers.clear()
        
        clientJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = Socket(hostIp, PORT)
                clientSocket = socket
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                
                // 1. Handshake: Send our client ID
                writer.println("HANDSHAKE $myClientId")
                
                // 2. Read Seed & Spawn from Host
                val seedLine = reader.readLine() ?: ""
                if (seedLine.startsWith("SEED ")) {
                    val seed = seedLine.substring(5).toLong()
                    world.seed = seed
                    world.generateInitialWorld()
                }
                
                val spawnLine = reader.readLine() ?: ""
                // Parse and apply modified blocks list
                val blocksCountLine = reader.readLine() ?: ""
                if (blocksCountLine.startsWith("BLOCKS_START ")) {
                    val count = blocksCountLine.substring(13).toInt()
                    for (i in 0 until count) {
                        val line = reader.readLine() ?: break
                        val parts = line.split(" ")
                        if (parts.size == 4) {
                            val bx = parts[0].toInt()
                            val by = parts[1].toInt()
                            val bz = parts[2].toInt()
                            val bType = parts[3].toByte()
                            world.setBlock(bx, by, bz, bType)
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    onConnectSuccess()
                }
                
                // 3. Continuously read messages
                while (isActive) {
                    val line = reader.readLine() ?: break
                    val parts = line.split(" ")
                    if (parts.isEmpty()) continue
                    
                    when (parts[0]) {
                        "PLAYER" -> {
                            if (parts.size >= 5) {
                                val cid = parts[1]
                                val px = parts[2].toFloat()
                                val py = parts[3].toFloat()
                                val pz = parts[4].toFloat()
                                remotePlayers[cid] = Vector3f(px, py, pz)
                            }
                        }
                        "BLOCK" -> {
                            if (parts.size >= 5) {
                                val bx = parts[1].toInt()
                                val by = parts[2].toInt()
                                val bz = parts[3].toInt()
                                val bType = parts[4].toByte()
                                world.setBlock(bx, by, bz, bType)
                                onBlockChangeCallback?.invoke(bx, by, bz, bType)
                            }
                        }
                        "DISCONNECT" -> {
                            if (parts.size >= 2) {
                                val cid = parts[1]
                                remotePlayers.remove(cid)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Client socket error: ${e.message}")
                withContext(Dispatchers.Main) {
                    onConnectFailed()
                }
            } finally {
                stop()
            }
        }
    }

    fun sendPlayerPosition(x: Float, y: Float, z: Float) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isHost) {
                    // Send our position to all clients
                    broadcast("PLAYER $myClientId $x $y $z", null)
                } else if (isClient) {
                    val socket = clientSocket
                    if (socket != null && !socket.isClosed) {
                        val writer = PrintWriter(socket.getOutputStream(), true)
                        writer.println("POS $x $y $z")
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun sendBlockChange(x: Int, y: Int, z: Int, type: Byte) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cmd = "BLOCK $x $y $z $type"
                if (isHost) {
                    broadcast(cmd, null)
                } else if (isClient) {
                    val socket = clientSocket
                    if (socket != null && !socket.isClosed) {
                        val writer = PrintWriter(socket.getOutputStream(), true)
                        writer.println(cmd)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun broadcast(message: String, excludeClientId: String?) {
        for ((cid, handler) in clientHandlers) {
            if (cid != excludeClientId) {
                handler.send(message)
            }
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping LAN network session...")
        isHost = false
        isClient = false
        remotePlayers.clear()
        
        serverJob?.cancel()
        serverJob = null
        clientJob?.cancel()
        clientJob = null
        
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverSocket = null
        
        try {
            clientSocket?.close()
        } catch (e: Exception) {}
        clientSocket = null
        
        for (handler in clientHandlers.values) {
            handler.close()
        }
        clientHandlers.clear()
    }

    private class ClientHandler(val socket: Socket, val world: World) {
        var clientId: String = ""
        private var writer: PrintWriter? = null
        private var reader: BufferedReader? = null
        private var running = true

        fun send(message: String) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    writer?.println(message)
                } catch (e: Exception) {}
            }
        }

        fun run() {
            try {
                writer = PrintWriter(socket.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                
                while (running) {
                    val line = reader?.readLine() ?: break
                    val parts = line.split(" ")
                    if (parts.isEmpty()) continue
                    
                    when (parts[0]) {
                        "HANDSHAKE" -> {
                            if (parts.size >= 2) {
                                clientId = parts[1]
                                clientHandlers[clientId] = this
                                
                                // Send Seed
                                writer?.println("SEED ${world.seed}")
                                writer?.println("SPAWN 0 40 0")
                                
                                // Send modified blocks count and details
                                val updates = world.blockUpdates
                                writer?.println("BLOCKS_START ${updates.size}")
                                for ((coords, type) in updates) {
                                    val sp = coords.split(",")
                                    if (sp.size == 3) {
                                        writer?.println("${sp[0]} ${sp[1]} ${sp[2]} $type")
                                    }
                                }
                            }
                        }
                        "POS" -> {
                            if (parts.size >= 4) {
                                val px = parts[1].toFloat()
                                val py = parts[2].toFloat()
                                val pz = parts[3].toFloat()
                                remotePlayers[clientId] = Vector3f(px, py, pz)
                                // Broadcast this player's position to all other clients
                                broadcast("PLAYER $clientId $px $py $pz", clientId)
                            }
                        }
                        "BLOCK" -> {
                            if (parts.size >= 5) {
                                val bx = parts[1].toInt()
                                val by = parts[2].toInt()
                                val bz = parts[3].toInt()
                                val bType = parts[4].toByte()
                                world.setBlock(bx, by, bz, bType)
                                onBlockChangeCallback?.invoke(bx, by, bz, bType)
                                // Broadcast to all other clients
                                broadcast("BLOCK $bx $by $bz $bType", clientId)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Client handler exception for $clientId: ${e.message}")
            } finally {
                close()
            }
        }

        fun close() {
            running = false
            if (clientId.isNotEmpty()) {
                clientHandlers.remove(clientId)
                remotePlayers.remove(clientId)
                broadcast("DISCONNECT $clientId", null)
            }
            try {
                socket.close()
            } catch (e: Exception) {}
        }
    }
}
