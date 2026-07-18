package com.example.engine

import android.opengl.GLES30
import android.opengl.Matrix
import com.example.game.BlockRegistry
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.CopyOnWriteArrayList

object ParticleEngine {
    
    data class Particle3D(
        var x: Float, var y: Float, var z: Float,
        var vx: Float, var vy: Float, var vz: Float,
        var size: Float,
        val blockType: Byte,
        var age: Float,
        val maxAge: Float
    )

    private val particles = CopyOnWriteArrayList<Particle3D>()
    
    private var vaoId = 0
    private var vboId = 0
    private var isInitialized = false

    fun spawnBlockBreakParticles(bx: Int, by: Int, bz: Int, blockType: Byte) {
        if (blockType == BlockRegistry.AIR) return
        
        val centerX = bx + 0.5f
        val centerY = by + 0.5f
        val centerZ = bz + 0.5f

        // Spawn 16 small particles at the block's center
        for (i in 0 until 16) {
            val px = centerX + (Math.random() - 0.5f).toFloat() * 0.5f
            val py = centerY + (Math.random() - 0.5f).toFloat() * 0.5f
            val pz = centerZ + (Math.random() - 0.5f).toFloat() * 0.5f

            val vx = (Math.random() - 0.5f).toFloat() * 2.5f
            val vy = (Math.random() * 3.0f + 1.0f).toFloat() // Burst upward
            val vz = (Math.random() - 0.5f).toFloat() * 2.5f

            val size = 0.06f + (Math.random() * 0.08f).toFloat()
            val maxAge = 0.5f + (Math.random() * 0.5f).toFloat()

            particles.add(Particle3D(px, py, pz, vx, vy, vz, size, blockType, 0f, maxAge))
        }
    }

    fun spawnJumpParticles(px: Float, py: Float, pz: Float) {
        // Dust splash at feet
        for (i in 0 until 10) {
            val rx = px + (Math.random() - 0.5f).toFloat() * 0.6f
            val ry = py - 1.55f
            val rz = pz + (Math.random() - 0.5f).toFloat() * 0.6f

            val vx = (Math.random() - 0.5f).toFloat() * 1.2f
            val vy = (Math.random() * 0.4f + 0.2f).toFloat()
            val vz = (Math.random() - 0.5f).toFloat() * 1.2f

            val size = 0.05f + (Math.random() * 0.04f).toFloat()
            val maxAge = 0.3f + (Math.random() * 0.3f).toFloat()

            particles.add(Particle3D(rx, ry, rz, vx, vy, vz, size, BlockRegistry.DIRT, 0f, maxAge))
        }
    }

    fun spawnHealParticles(px: Float, py: Float, pz: Float) {
        // Glowing star particles
        for (i in 0 until 25) {
            val rx = px + (Math.random() - 0.5f).toFloat() * 0.6f
            val ry = py + (Math.random() - 0.5f).toFloat() * 1.4f
            val rz = pz + (Math.random() - 0.5f).toFloat() * 0.6f

            val angle = Math.random() * 2.0 * Math.PI
            val speed = 0.8f + Math.random().toFloat() * 1.2f
            val vx = (Math.cos(angle) * speed).toFloat()
            val vy = (Math.random() * 1.5f - 0.3f).toFloat()
            val vz = (Math.sin(angle) * speed).toFloat()

            val size = 0.05f + (Math.random() * 0.05f).toFloat()
            val maxAge = 0.6f + (Math.random() * 0.4f).toFloat()

            particles.add(Particle3D(rx, ry, rz, vx, vy, vz, size, BlockRegistry.GOLD_ORE, 0f, maxAge))
        }
    }

    fun update(dt: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.age += dt
            if (p.age >= p.maxAge) {
                particles.remove(p)
                continue
            }

            // Gravity & Kinematics
            p.vy -= 9.8f * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt

            // Air friction
            p.vx *= (1f - 1.5f * dt).coerceIn(0f, 1f)
            p.vy *= (1f - 0.5f * dt).coerceIn(0f, 1f)
            p.vz *= (1f - 1.5f * dt).coerceIn(0f, 1f)
        }
    }

    private fun initMesh() {
        if (isInitialized) return
        
        val vertices = floatArrayOf(
            // Front Face (z = +0.5)
            -0.5f, -0.5f,  0.5f,  0.0f, 1.0f, 1.0f,
             0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 1.0f,
             0.5f,  0.5f,  0.5f,  1.0f, 0.0f, 1.0f,
            -0.5f, -0.5f,  0.5f,  0.0f, 1.0f, 1.0f,
             0.5f,  0.5f,  0.5f,  1.0f, 0.0f, 1.0f,
            -0.5f,  0.5f,  0.5f,  0.0f, 0.0f, 1.0f,
            
            // Back Face (z = -0.5)
            -0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 0.6f,
            -0.5f,  0.5f, -0.5f,  1.0f, 0.0f, 0.6f,
             0.5f,  0.5f, -0.5f,  0.0f, 0.0f, 0.6f,
            -0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 0.6f,
             0.5f,  0.5f, -0.5f,  0.0f, 0.0f, 0.6f,
             0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 0.6f,
             
            // Top Face (y = +0.5)
            -0.5f,  0.5f, -0.5f,  0.0f, 0.0f, 1.0f,
            -0.5f,  0.5f,  0.5f,  0.0f, 1.0f, 1.0f,
             0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 1.0f,
            -0.5f,  0.5f, -0.5f,  0.0f, 0.0f, 1.0f,
             0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 1.0f,
             0.5f,  0.5f, -0.5f,  1.0f, 0.0f, 1.0f,
             
            // Bottom Face (y = -0.5)
            -0.5f, -0.5f, -0.5f,  0.0f, 0.0f, 0.4f,
             0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 0.4f,
             0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 0.4f,
            -0.5f, -0.5f, -0.5f,  0.0f, 0.0f, 0.4f,
             0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 0.4f,
            -0.5f, -0.5f,  0.5f,  0.0f, 1.0f, 0.4f,
            
            // Left Face (x = -0.5)
            -0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 0.8f,
            -0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 0.8f,
            -0.5f,  0.5f,  0.5f,  1.0f, 0.0f, 0.8f,
            -0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 0.8f,
            -0.5f,  0.5f,  0.5f,  1.0f, 0.0f, 0.8f,
            -0.5f,  0.5f, -0.5f,  0.0f, 0.0f, 0.8f,
            
            // Right Face (x = +0.5)
             0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 0.8f,
             0.5f,  0.5f, -0.5f,  1.0f, 0.0f, 0.8f,
             0.5f,  0.5f,  0.5f,  0.0f, 0.0f, 0.8f,
             0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 0.8f,
             0.5f,  0.5f,  0.5f,  0.0f, 0.0f, 0.8f,
             0.5f, -0.5f,  0.5f,  0.0f, 1.0f, 0.8f
        )

        val bb = ByteBuffer.allocateDirect(vertices.size * 4)
        bb.order(ByteOrder.nativeOrder())
        val buffer = bb.asFloatBuffer()
        buffer.put(vertices)
        buffer.position(0)

        val buffers = IntArray(2)
        GLES30.glGenVertexArrays(1, buffers, 0)
        GLES30.glGenBuffers(1, buffers, 1)
        vaoId = buffers[0]
        vboId = buffers[1]

        GLES30.glBindVertexArray(vaoId)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, buffer.capacity() * 4, buffer, GLES30.GL_STATIC_DRAW)

        // Attribute 0: vec3 position
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 6 * 4, 0)
        GLES30.glEnableVertexAttribArray(0)

        // Attribute 1: vec2 texCoord
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 6 * 4, 3 * 4)
        GLES30.glEnableVertexAttribArray(1)

        // Attribute 2: float light
        GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, 6 * 4, 5 * 4)
        GLES30.glEnableVertexAttribArray(2)

        GLES30.glBindVertexArray(0)
        isInitialized = true
    }

    fun draw(vpMatrix: FloatArray) {
        if (particles.isEmpty()) return
        
        if (!isInitialized) {
            initMesh()
        }

        GLES30.glBindVertexArray(vaoId)
        
        // Disable attribute 3 so we can specify a static BaseUV coordinate for each particle
        GLES30.glDisableVertexAttribArray(3)

        val modelMatrix = FloatArray(16)
        val mvpMatrix = FloatArray(16)

        for (p in particles) {
            // Get texture UV for this block type
            val uv = BlockRegistry.getTextureUV(p.blockType, 0)
            val baseU = uv.first.toFloat() * (1f / 16f)
            val baseV = uv.second.toFloat() * (1f / 16f)
            
            // Set static constant BaseUV attribute for all vertices in this draw call
            GLES30.glVertexAttrib2f(3, baseU, baseV)

            // Setup model transformation matrix
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z)
            Matrix.scaleM(modelMatrix, 0, p.size, p.size, p.size)

            // Combine with VP matrix
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
            GLES30.glUniformMatrix4fv(ShaderManager.mvpMatrixHandle, 1, false, mvpMatrix, 0)

            // Draw particle cube
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36)
        }

        // Re-enable attribute 3 for standard chunk/player mesh rendering
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glBindVertexArray(0)
    }
}
