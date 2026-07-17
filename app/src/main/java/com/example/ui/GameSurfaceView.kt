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
    private var cameraPointerId = -1
    
    init {
        setEGLContextClientVersion(3)
        renderer = VoxelRenderer(context, world, player)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
    
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val action = e.actionMasked
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val sensitivity = prefs.getFloat("sensitivity", 1.0f)
        
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val actionIndex = e.actionIndex
                val pointerId = e.getPointerId(actionIndex)
                val x = e.getX(actionIndex)
                
                // If we are not currently tracking a look-around touch,
                // and this touch is on the right half of the screen, capture it!
                if (cameraPointerId == -1 && x > width / 2f) {
                    cameraPointerId = pointerId
                    lastX = x
                    lastY = e.getY(actionIndex)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (cameraPointerId != -1) {
                    val pointerIndex = e.findPointerIndex(cameraPointerId)
                    if (pointerIndex != -1) {
                        val x = e.getX(pointerIndex)
                        val y = e.getY(pointerIndex)
                        
                        val dx = x - lastX
                        val dy = y - lastY
                        
                        // Apply movement with sensitivity multiplier
                        player.camera.yaw += dx * 0.20f * sensitivity
                        player.camera.pitch -= dy * 0.20f * sensitivity
                        
                        if (player.camera.pitch > 89.0f) player.camera.pitch = 89.0f
                        if (player.camera.pitch < -89.0f) player.camera.pitch = -89.0f
                        
                        lastX = x
                        lastY = y
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val actionIndex = e.actionIndex
                val pointerId = e.getPointerId(actionIndex)
                if (pointerId == cameraPointerId) {
                    cameraPointerId = -1
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cameraPointerId = -1
            }
        }
        return true
    }
}
