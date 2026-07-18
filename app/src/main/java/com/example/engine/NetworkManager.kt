package com.example.engine

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.example.world.World
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap

data class LanServer(
    val ip: String,
    val name: String,
    val version: String,
    val lastSeen: Long = System.currentTimeMillis()
)

object NetworkManager {
    private const val TAG = "NetworkManager"
    private const val PORT = 12345
    private const val UDP_PORT = 9998

    var gameVersion = "1.2"
    var isHost = false
    var isClient = false
    var myClientId: String = "Player_" + (1000..9999).random()
    var connectionError: String? = null
    
    // Remote players coordinates
    val remotePlayers = ConcurrentHashMap<String, Vector3f>()
    
    // Discovered local LAN servers
    val discoveredServers = mutableStateListOf<LanServer>()
    
    private var udpBeaconJob: Job? = null
    private var udpDiscoveryJob: Job? = null
    
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var serverJob: Job? = null
    private var clientJob: Job? = null
    
    // Keep list of client handlers on server
    internal val clientHandlers = ConcurrentHashMap<String, ClientHandler>()
    
    // Callback to update the world on block changes
    internal var onBlockChangeCallback: ((Int, Int, Int, Byte) -> Unit)? = null
    
    fun setOnBlockChangeCallback(callback: (Int, Int, Int, Byte) -> Unit) {
        onBlockChangeCallback = callback
    }

    fun startHost(world: World, worldName: String, onReady: () -> Unit) {
        Log.d(TAG, "Starting host server on port $PORT...")
        isHost = true
        isClient = false
        remotePlayers.clear()
        clientHandlers.clear()
        
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(PORT)
                onReady()
                // Start UDP beacon broadcasting
                startUdpBeacon(worldName)
                
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

    private fun startUdpBeacon(worldName: String) {
        udpBeaconJob?.cancel()
        udpBeaconJob = CoroutineScope(Dispatchers.IO).launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true
                val address = InetAddress.getByName("255.255.255.255")
                
                while (isActive && isHost) {
                    try {
                        val message = "VOXEL_LAN_BEACON;$worldName;$gameVersion"
                        val buffer = message.toByteArray()
                        val packet = DatagramPacket(buffer, buffer.size, address, UDP_PORT)
                        socket.send(packet)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending UDP beacon: ${e.message}")
                    }
                    delay(2000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "UDP Beacon socket error: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    fun startUdpDiscovery() {
        discoveredServers.clear()
        udpDiscoveryJob?.cancel()
        udpDiscoveryJob = CoroutineScope(Dispatchers.IO).launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(UDP_PORT)
                socket.soTimeout = 2500
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)
                
                while (isActive) {
                    try {
                        socket.receive(packet)
                        val data = String(packet.data, 0, packet.length)
                        if (data.startsWith("VOXEL_LAN_BEACON;")) {
                            val parts = data.split(";")
                            if (parts.size >= 3) {
                                val worldName = parts[1]
                                val version = parts[2]
                                val senderIp = packet.address.hostAddress ?: ""
                                
                                if (senderIp.isNotEmpty()) {
                                    val now = System.currentTimeMillis()
                                    val server = LanServer(senderIp, worldName, version, now)
                                    withContext(Dispatchers.Main) {
                                        discoveredServers.removeAll { it.ip == senderIp }
                                        discoveredServers.add(server)
                                    }
                                }
                            }
                        }
                    } catch (e: java.io.InterruptedIOException) {
                        // Check if inactive, clean old servers
                        val now = System.currentTimeMillis()
                        withContext(Dispatchers.Main) {
                            discoveredServers.removeAll { now - it.lastSeen > 8000 }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "UDP Discovery receive error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "UDP Discovery socket error: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    fun stopUdpDiscovery() {
        udpDiscoveryJob?.cancel()
        udpDiscoveryJob = null
        discoveredServers.clear()
    }

    fun startClient(hostIp: String, world: World, onConnectSuccess: () -> Unit, onConnectFailed: () -> Unit) {
        Log.d(TAG, "Connecting to host $hostIp:$PORT...")
        isHost = false
        isClient = true
        connectionError = null
        remotePlayers.clear()
        
        clientJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = Socket(hostIp, PORT)
                clientSocket = socket
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                
                // 1. Handshake: Send our client ID and chosen gameVersion
                writer.println("HANDSHAKE $myClientId $gameVersion")
                
                // 2. Read Seed or potential Error from Host
                val seedLine = reader.readLine() ?: ""
                if (seedLine.startsWith("ERROR ")) {
                    connectionError = seedLine.substring(6)
                    withContext(Dispatchers.Main) {
                        onConnectFailed()
                    }
                    socket.close()
                    return@launch
                }
                
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
                        "CHAT" -> {
                            if (parts.size >= 3) {
                                val sender = parts[1]
                                val msg = parts.drop(2).joinToString(" ")
                                withContext(Dispatchers.Main) {
                                    UIEngine.addChatMessage(sender, msg)
                                }
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

    fun sendChatMessage(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isHost) {
                    // Host local add on Main thread
                    withContext(Dispatchers.Main) {
                        UIEngine.addChatMessage(myClientId, text)
                    }
                    // Broadcast to all clients
                    broadcast("CHAT $myClientId $text", null)
                } else if (isClient) {
                    // Send to Host
                    val socket = clientSocket
                    if (socket != null && !socket.isClosed) {
                        val writer = PrintWriter(socket.getOutputStream(), true)
                        writer.println("CHAT $text")
                    }
                    // Add locally for client on Main thread
                    withContext(Dispatchers.Main) {
                        UIEngine.addChatMessage(myClientId, text)
                    }
                } else {
                    // Singleplayer local add on Main thread
                    withContext(Dispatchers.Main) {
                        UIEngine.addChatMessage("Player", text)
                    }
                }
            } catch (e: Exception) {
                // Ignore
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
        
        udpBeaconJob?.cancel()
        udpBeaconJob = null
        stopUdpDiscovery()
        
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
}
