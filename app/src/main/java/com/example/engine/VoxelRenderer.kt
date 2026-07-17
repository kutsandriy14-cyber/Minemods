package com.example.engine

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.world.World
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VoxelRenderer(private val context: Context, private val world: World, val camera: Camera) : GLSurfaceView.Renderer {

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.5f, 0.8f, 1.0f, 1.0f) // Sky color
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        // Disable GLES face culling to avoid missing faces due to winding order in greedy mesher
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        
        ShaderManager.init()
        TextureManager.init(context)
        
        // Initial meshes
        for (chunk in world.chunks.values) {
            val meshData = GreedyMesher.generateMesh(chunk, world)
            chunk.mesh.updateMesh(meshData)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        camera.setProjection(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        camera.updateViewMatrix()

        GLES30.glUseProgram(ShaderManager.programId)
        
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, TextureManager.atlasTextureId)
        GLES30.glUniform1i(ShaderManager.textureHandle, 0)

        // Draw chunks
        for (chunk in world.chunks.values) {
            if (chunk.isModified) {
                val meshData = GreedyMesher.generateMesh(chunk, world)
                chunk.mesh.updateMesh(meshData)
                chunk.isModified = false
            }
            if (!chunk.mesh.isReady && chunk.mesh.vertexCount > 0) {
                chunk.mesh.buildGL()
            }
            
            // Frustum culling logic can be added here
            
            GLES30.glUniformMatrix4fv(ShaderManager.mvpMatrixHandle, 1, false, camera.vpMatrix, 0)
            chunk.mesh.draw()
        }
    }
}
