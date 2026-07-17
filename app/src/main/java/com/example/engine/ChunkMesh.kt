package com.example.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import android.opengl.GLES30

class ChunkMesh {
    var vaoId: Int = 0
    var vboId: Int = 0
    var vertexCount: Int = 0
    var isReady: Boolean = false
    
    private var tempBuffer: FloatBuffer? = null
    
    // Each vertex: x,y,z (3) + u,v (2) + light (1) + baseU,baseV (2) = 8 floats
    fun updateMesh(vertexData: FloatArray) {
        vertexCount = vertexData.size / 8
        if (vertexCount == 0) {
            isReady = true
            return
        }
        
        val bb = ByteBuffer.allocateDirect(vertexData.size * 4)
        bb.order(ByteOrder.nativeOrder())
        tempBuffer = bb.asFloatBuffer()
        tempBuffer?.put(vertexData)
        tempBuffer?.position(0)
    }
    
    fun buildGL() {
        val buffer = tempBuffer ?: return
        if (vertexCount == 0) {
            isReady = true
            tempBuffer = null
            return
        }
        
        if (vaoId == 0) {
            val buffers = IntArray(2)
            GLES30.glGenVertexArrays(1, buffers, 0)
            GLES30.glGenBuffers(1, buffers, 1)
            vaoId = buffers[0]
            vboId = buffers[1]
        }
        
        GLES30.glBindVertexArray(vaoId)
        
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, buffer.capacity() * 4, buffer, GLES30.GL_STATIC_DRAW)
        
        // Position
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 8 * 4, 0)
        GLES30.glEnableVertexAttribArray(0)
        
        // TexCoord
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 8 * 4, 3 * 4)
        GLES30.glEnableVertexAttribArray(1)
        
        // Light
        GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, 8 * 4, 5 * 4)
        GLES30.glEnableVertexAttribArray(2)
        
        // BaseUV
        GLES30.glVertexAttribPointer(3, 2, GLES30.GL_FLOAT, false, 8 * 4, 6 * 4)
        GLES30.glEnableVertexAttribArray(3)

        
        GLES30.glBindVertexArray(0)
        
        isReady = true
        tempBuffer = null
    }
    
    fun draw() {
        if (!isReady || vertexCount == 0 || vaoId == 0) return
        
        GLES30.glBindVertexArray(vaoId)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)
        GLES30.glBindVertexArray(0)
    }
    
    fun destroy() {
        if (vaoId != 0) {
            val buffers = intArrayOf(vaoId, vboId)
            GLES30.glDeleteVertexArrays(1, buffers, 0)
            GLES30.glDeleteBuffers(1, buffers, 1)
            vaoId = 0
            vboId = 0
        }
    }
}
