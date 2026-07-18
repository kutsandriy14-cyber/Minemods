package com.example.engine

import android.opengl.GLES30
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PlayerMesh {
    private var vaoId = 0
    private var vboId = 0
    private var isInitialized = false
    
    fun init() {
        if (isInitialized) return
        val w = 0.3f // half-width
        val h = 0.9f // half-height
        val d = 0.3f // half-depth
        
        // Use the Gold block or Sand texture coordinate (baseU = 64/256, baseV = 0)
        val baseU = 64f / 256f
        val baseV = 0f
        
        // Each vertex: x, y, z, u, v, light, baseU, baseV
        val vertices = floatArrayOf(
            // Front Face
            -w, -h,  d,  0f, 1f, 1.0f, baseU, baseV,
             w, -h,  d,  1f, 1f, 1.0f, baseU, baseV,
             w,  h,  d,  1f, 0f, 1.0f, baseU, baseV,
            -w, -h,  d,  0f, 1f, 1.0f, baseU, baseV,
             w,  h,  d,  1f, 0f, 1.0f, baseU, baseV,
            -w,  h,  d,  0f, 0f, 1.0f, baseU, baseV,
            
            // Back Face
            -w, -h, -d,  1f, 1f, 0.6f, baseU, baseV,
            -w,  h, -d,  1f, 0f, 0.6f, baseU, baseV,
             w,  h, -d,  0f, 0f, 0.6f, baseU, baseV,
            -w, -h, -d,  1f, 1f, 0.6f, baseU, baseV,
             w,  h, -d,  0f, 0f, 0.6f, baseU, baseV,
             w, -h, -d,  0f, 1f, 0.6f, baseU, baseV,
             
            // Top Face
            -w,  h, -d,  0f, 0f, 1.0f, baseU, baseV,
            -w,  h,  d,  0f, 1f, 1.0f, baseU, baseV,
             w,  h,  d,  1f, 1f, 1.0f, baseU, baseV,
            -w,  h, -d,  0f, 0f, 1.0f, baseU, baseV,
             w,  h,  d,  1f, 1f, 1.0f, baseU, baseV,
             w,  h, -d,  1f, 0f, 1.0f, baseU, baseV,
             
            // Bottom Face
            -w, -h, -d,  0f, 0f, 0.4f, baseU, baseV,
             w, -h, -d,  1f, 0f, 0.4f, baseU, baseV,
             w, -h,  d,  1f, 1f, 0.4f, baseU, baseV,
            -w, -h, -d,  0f, 0f, 0.4f, baseU, baseV,
             w, -h,  d,  1f, 1f, 0.4f, baseU, baseV,
            -w, -h,  d,  0f, 1f, 0.4f, baseU, baseV,
            
            // Left Face
            -w, -h, -d,  0f, 1f, 0.8f, baseU, baseV,
            -w, -h,  d,  1f, 1f, 0.8f, baseU, baseV,
            -w,  h,  d,  1f, 0f, 0.8f, baseU, baseV,
            -w, -h, -d,  0f, 1f, 0.8f, baseU, baseV,
            -w,  h,  d,  1f, 0f, 0.8f, baseU, baseV,
            -w,  h, -d,  0f, 0f, 0.8f, baseU, baseV,
            
            // Right Face
             w, -h, -d,  1f, 1f, 0.8f, baseU, baseV,
             w,  h, -d,  1f, 0f, 0.8f, baseU, baseV,
             w,  h,  d,  0f, 0f, 0.8f, baseU, baseV,
             w, -h, -d,  1f, 1f, 0.8f, baseU, baseV,
             w,  h,  d,  0f, 0f, 0.8f, baseU, baseV,
             w, -h,  d,  0f, 1f, 0.8f, baseU, baseV
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
        
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 8 * 4, 0)
        GLES30.glEnableVertexAttribArray(0)
        
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 8 * 4, 3 * 4)
        GLES30.glEnableVertexAttribArray(1)
        
        GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, 8 * 4, 5 * 4)
        GLES30.glEnableVertexAttribArray(2)
        
        GLES30.glVertexAttribPointer(3, 2, GLES30.GL_FLOAT, false, 8 * 4, 6 * 4)
        GLES30.glEnableVertexAttribArray(3)
        
        GLES30.glBindVertexArray(0)
        isInitialized = true
    }
    
    fun draw(vpMatrix: FloatArray, px: Float, py: Float, pz: Float, scale: Float = 1.0f) {
        if (!isInitialized) return
        
        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)
        // Center characters nicely relative to eye-level position
        Matrix.translateM(modelMatrix, 0, px, py - (0.9f * scale), pz)
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale)
        
        val mvpMatrix = FloatArray(16)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        
        GLES30.glUniformMatrix4fv(ShaderManager.mvpMatrixHandle, 1, false, mvpMatrix, 0)
        
        GLES30.glBindVertexArray(vaoId)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36)
        GLES30.glBindVertexArray(0)
    }
}
