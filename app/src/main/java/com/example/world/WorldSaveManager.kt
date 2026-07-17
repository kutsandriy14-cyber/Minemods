package com.example.world

import android.content.Context
import android.util.Log
import com.example.engine.Chunk
import com.example.game.Player
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object WorldSaveManager {
    private const val TAG = "WorldSaveManager"
    private const val WORLDS_DIR = "worlds"

    fun getWorldsDir(context: Context): File {
        val dir = File(context.filesDir, WORLDS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getWorldList(context: Context): List<String> {
        val dir = getWorldsDir(context)
        return dir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
    }

    fun deleteWorld(context: Context, worldName: String): Boolean {
        val worldDir = File(getWorldsDir(context), worldName)
        return worldDir.deleteRecursively()
    }

    fun saveWorld(context: Context, worldName: String, world: World, player: Player): Boolean {
        val worldDir = File(getWorldsDir(context), worldName)
        if (!worldDir.exists()) worldDir.mkdirs()

        val saveFile = File(worldDir, "level.dat")
        try {
            FileOutputStream(saveFile).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    DataOutputStream(bos).use { dos ->
                        // 1. Write Header / Magic Number
                        dos.writeUTF("MINEMODS_WORLD_v1")
                        // 2. Write Seed
                        dos.writeLong(world.seed)
                        // 3. Write Player Position
                        dos.writeFloat(player.camera.position.x)
                        dos.writeFloat(player.camera.position.y)
                        dos.writeFloat(player.camera.position.z)

                        // 4. Write Chunks count
                        val activeChunks = world.chunks.values.toList()
                        dos.writeInt(activeChunks.size)

                        // 5. Write each chunk
                        for (chunk in activeChunks) {
                            dos.writeInt(chunk.chunkX)
                            dos.writeInt(chunk.chunkZ)
                            
                            // Compress chunk blocks using GZIP to make it tiny
                            val byteStream = ByteArrayOutputStream()
                            GZIPOutputStream(byteStream).use { gzip ->
                                gzip.write(chunk.blocks)
                            }
                            val compressedBytes = byteStream.toByteArray()
                            dos.writeInt(compressedBytes.size)
                            dos.write(compressedBytes)
                        }
                    }
                }
            }
            Log.d(TAG, "World '$worldName' successfully saved.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save world '$worldName'", e)
            return false
        }
    }

    fun loadWorld(context: Context, worldName: String, world: World, player: Player): Boolean {
        val worldDir = File(getWorldsDir(context), worldName)
        val saveFile = File(worldDir, "level.dat")
        if (!saveFile.exists()) {
            Log.w(TAG, "Save file level.dat does not exist for world '$worldName'")
            return false
        }

        try {
            FileInputStream(saveFile).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    DataInputStream(bis).use { dis ->
                        val version = dis.readUTF()
                        if (version != "MINEMODS_WORLD_v1") {
                            Log.w(TAG, "Unsupported world version: $version")
                            return false
                        }

                        world.seed = dis.readLong()
                        val px = dis.readFloat()
                        val py = dis.readFloat()
                        val pz = dis.readFloat()
                        player.camera.position.set(px, py, pz)

                        val chunkCount = dis.readInt()
                        world.chunks.clear()

                        for (i in 0 until chunkCount) {
                            val cx = dis.readInt()
                            val cz = dis.readInt()
                            val compressedSize = dis.readInt()
                            val compressedBytes = ByteArray(compressedSize)
                            dis.readFully(compressedBytes)

                            val chunk = Chunk(cx, cz)
                            ByteArrayInputStream(compressedBytes).use { bais ->
                                GZIPInputStream(bais).use { gzip ->
                                    var bytesRead = 0
                                    val buffer = chunk.blocks
                                    while (bytesRead < buffer.size) {
                                        val read = gzip.read(buffer, bytesRead, buffer.size - bytesRead)
                                        if (read == -1) break
                                        bytesRead += read
                                    }
                                }
                            }
                            chunk.isModified = true
                            world.chunks[Pair(cx, cz)] = chunk
                        }
                    }
                }
            }
            Log.d(TAG, "World '$worldName' successfully loaded.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load world '$worldName'", e)
            return false
        }
    }
}
