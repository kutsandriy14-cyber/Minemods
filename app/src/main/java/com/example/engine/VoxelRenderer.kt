package com.example.engine

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.world.World
import com.example.game.Player
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VoxelRenderer(private val context: Context, private val world: World, val player: Player) : GLSurfaceView.Renderer {

    val camera = player.camera
    private val playerMesh = PlayerMesh()
    
    private var lastFpsTime = System.nanoTime()
    private var fpsCount = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.5f, 0.8f, 1.0f, 1.0f) // Sky color
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        // Disable GLES face culling to avoid missing faces due to winding order in greedy mesher
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        
        ShaderManager.init()
        TextureManager.init(context)
        playerMesh.init()
        
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
        // Clean up any meshes that were unloaded on the background thread
        while (true) {
            val mesh = world.meshesToDestroy.poll() ?: break
            mesh.destroy()
        }

        if (world.isNight) {
            GLES30.glClearColor(0.04f, 0.05f, 0.1f, 1.0f) // Midnight/Space color
        } else {
            GLES30.glClearColor(0.5f, 0.8f, 1.0f, 1.0f) // Sunny sky color
        }
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        camera.updateViewMatrix()

        RenderChunkEngine.prepareRenderPass(ShaderManager.programId, TextureManager.atlasTextureId)

        // Draw chunks using RenderChunkEngine with frustum culling and OptimizationEngine integration
        RenderChunkEngine.drawChunks(
            world = world,
            vpMatrix = camera.vpMatrix,
            mvpMatrixHandle = ShaderManager.mvpMatrixHandle,
            cameraX = camera.position.x,
            cameraZ = camera.position.z,
            yawRad = camera.yaw * 0.017453292f, // convert to radians
            viewDistance = UIEngine.viewDistanceChunks
        )

        // Draw remote multiplayer participants
        for (playerPos in NetworkManager.remotePlayers.values) {
            playerMesh.draw(camera.vpMatrix, playerPos.x, playerPos.y, playerPos.z)
        }
        
        // Draw mobs
        for (mob in world.mobs) {
            val scale = if (mob.type == com.example.game.MobType.PIG) 0.5f else 1.0f
            playerMesh.draw(camera.vpMatrix, mob.position.x, mob.position.y, mob.position.z, scale)
        }

        // Draw 3D particle systems
        com.example.engine.ParticleEngine.draw(camera.vpMatrix)

        // Calculate Real Actual FPS from GPU rendering frequency
        fpsCount++
        val now = System.nanoTime()
        if (now - lastFpsTime >= 1_000_000_000L) {
            com.example.engine.GameEngine.currentFps = fpsCount
            fpsCount = 0
            lastFpsTime = now
        }
    }
}
