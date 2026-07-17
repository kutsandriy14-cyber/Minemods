package com.example.ui

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.example.game.Player
import com.example.world.World
import com.example.engine.VoxelRenderer

class GameSurfaceView(context: Context, val world: World, val player: Player) : GLSurfaceView(context) {
    
    private val renderer: VoxelRenderer
    
    private var lastX = 0f
    private var lastY = 0f
    
    init {
        setEGLContextClientVersion(3)
        renderer = VoxelRenderer(context, world, player.camera)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
    
    private var isDraggingCamera = false
    
    override fun onTouchEvent(e: MotionEvent): Boolean {
        // Only look around if touching the right half of the screen
        val x = e.x
        val y = e.y
        
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (x > width / 2f) {
                    isDraggingCamera = true
                    lastX = x
                    lastY = y
                } else {
                    isDraggingCamera = false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingCamera) {
                    val dx = x - lastX
                    val dy = y - lastY
                    
                    player.camera.yaw += dx * 0.25f
                    player.camera.pitch -= dy * 0.25f
                    
                    if (player.camera.pitch > 89.0f) player.camera.pitch = 89.0f
                    if (player.camera.pitch < -89.0f) player.camera.pitch = -89.0f
                    
                    lastX = x
                    lastY = y
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDraggingCamera = false
            }
        }
        return true
    }
}
