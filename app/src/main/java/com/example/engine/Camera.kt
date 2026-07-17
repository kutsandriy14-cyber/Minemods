package com.example.engine

import android.opengl.Matrix
import kotlin.math.cos
import kotlin.math.sin

class Camera {
    val position = Vector3f(0f, 32f, 0f)
    var yaw: Float = 0f
    var pitch: Float = 0f
    
    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    val vpMatrix = FloatArray(16)
    
    val front = Vector3f(0f, 0f, -1f)
    val right = Vector3f(1f, 0f, 0f)
    val up = Vector3f(0f, 1f, 0f)
    
    fun updateVectors() {
        val radYaw = Math.toRadians(yaw.toDouble())
        val radPitch = Math.toRadians(pitch.toDouble())
        
        front.x = (cos(radYaw) * cos(radPitch)).toFloat()
        front.y = sin(radPitch).toFloat()
        front.z = (sin(radYaw) * cos(radPitch)).toFloat()
        front.normalize()
        
        right.set(front.cross(Vector3f(0f, 1f, 0f)))
        right.normalize()
        
        up.set(right.cross(front))
        up.normalize()
    }
    
    fun updateViewMatrix() {
        updateVectors()
        val centerX = position.x + front.x
        val centerY = position.y + front.y
        val centerZ = position.z + front.z
        
        Matrix.setLookAtM(viewMatrix, 0,
            position.x, position.y, position.z,
            centerX, centerY, centerZ,
            up.x, up.y, up.z
        )
        
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }
    
    fun setProjection(width: Int, height: Int, fov: Float = 70f) {
        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, fov, ratio, 0.1f, 1000f)
    }
}
