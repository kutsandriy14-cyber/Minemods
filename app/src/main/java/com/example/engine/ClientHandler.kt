package com.example.engine

import android.util.Log
import com.example.world.World
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class ClientHandler(val socket: Socket, val world: World) {
    private val TAG = "ClientHandler"
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
                            val clientVer = if (parts.size >= 3) parts[2] else "1.0"
                            if (clientVer != NetworkManager.gameVersion) {
                                writer?.println("ERROR VERSION_MISMATCH Server is v${NetworkManager.gameVersion}, you have v$clientVer")
                                close()
                                return
                            }
                            clientId = parts[1]
                            NetworkManager.clientHandlers[clientId] = this
                            
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
                            NetworkManager.remotePlayers[clientId] = Vector3f(px, py, pz)
                            // Broadcast this player's position to all other clients
                            NetworkManager.broadcast("PLAYER $clientId $px $py $pz", clientId)
                        }
                    }
                    "BLOCK" -> {
                        if (parts.size >= 5) {
                            val bx = parts[1].toInt()
                            val by = parts[2].toInt()
                            val bz = parts[3].toInt()
                            val bType = parts[4].toByte()
                            world.setBlock(bx, by, bz, bType)
                            NetworkManager.onBlockChangeCallback?.invoke(bx, by, bz, bType)
                            // Broadcast to all other clients
                            NetworkManager.broadcast("BLOCK $bx $by $bz $bType", clientId)
                        }
                    }
                    "CHAT" -> {
                        if (parts.size >= 2) {
                            val msg = parts.drop(1).joinToString(" ")
                            // Add to host's local chat UI on Main thread
                            CoroutineScope(Dispatchers.Main).launch {
                                UIEngine.addChatMessage(clientId, msg)
                            }
                            // Broadcast this chat to all other connected clients
                            NetworkManager.broadcast("CHAT $clientId $msg", clientId)
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
            NetworkManager.clientHandlers.remove(clientId)
            NetworkManager.remotePlayers.remove(clientId)
            NetworkManager.broadcast("DISCONNECT $clientId", null)
        }
        try {
            socket.close()
        } catch (e: Exception) {}
    }
}
