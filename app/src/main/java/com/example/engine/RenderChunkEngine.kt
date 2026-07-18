package com.example.engine

import android.opengl.GLES30
import com.example.world.World

object RenderChunkEngine {
    
    // Binds shaders, active texture slot, and loads basic projection uniform parameters
    fun prepareRenderPass(programId: Int, textureId: Int) {
        GLES30.glUseProgram(programId)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glUniform1i(ShaderManager.textureHandle, 0)
    }

    // Draws all visible loaded chunks in the world, running mesh generation on modified ones
    fun drawChunks(
        world: World,
        vpMatrix: FloatArray,
        mvpMatrixHandle: Int,
        cameraX: Float,
        cameraZ: Float,
        yawRad: Float,
        viewDistance: Int
    ) {
        for (chunk in world.chunks.values) {
            // Re-generate mesh if chunk was modified (blocks added/removed)
            if (chunk.isModified) {
                val meshData = GreedyMesher.generateMesh(chunk, world)
                chunk.mesh.updateMesh(meshData)
                chunk.isModified = false
            }
            
            // Build GL resources on GL thread if data is ready
            if (!chunk.mesh.isReady && chunk.mesh.vertexCount > 0) {
                chunk.mesh.buildGL()
            }
            
            // Frustum Culling via OptimizationEngine!
            val inFrustum = OptimizationEngine.isChunkInFrustum(
                cx = chunk.chunkX,
                cz = chunk.chunkZ,
                camX = cameraX,
                camZ = cameraZ,
                yawRad = yawRad,
                viewDistanceChunks = viewDistance
            )
            
            if (inFrustum && chunk.mesh.isReady) {
                GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, vpMatrix, 0)
                chunk.mesh.draw()
            }
        }
    }
}
