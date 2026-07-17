package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils

object TextureManager {
    var atlasTextureId: Int = 0
        private set
        
    fun init(context: Context) {
        val textureObjectIds = IntArray(1)
        GLES30.glGenTextures(1, textureObjectIds, 0)
        
        if (textureObjectIds[0] == 0) {
            return
        }
        
        // Generate placeholder atlas (256x256, 16x16 tiles = 16x16 pixels per tile)
        val bitmap = createPlaceholderAtlas()
        
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureObjectIds[0])
        
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        
        bitmap.recycle()
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        
        atlasTextureId = textureObjectIds[0]
    }
    
    private fun createPlaceholderAtlas(): Bitmap {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()
        
        // Grass top (0, 0)
        paint.color = android.graphics.Color.rgb(85, 170, 85)
        canvas.drawRect(0f, 0f, 16f, 16f, paint)
        paint.color = android.graphics.Color.rgb(65, 150, 65)
        canvas.drawRect(2f, 2f, 6f, 6f, paint)
        
        // Dirt (1, 0)
        paint.color = android.graphics.Color.rgb(134, 96, 67)
        canvas.drawRect(16f, 0f, 32f, 16f, paint)
        paint.color = android.graphics.Color.rgb(104, 76, 47)
        canvas.drawRect(18f, 4f, 22f, 8f, paint)
        
        // Grass side (2, 0)
        paint.color = android.graphics.Color.rgb(134, 96, 67)
        canvas.drawRect(32f, 0f, 48f, 16f, paint)
        paint.color = android.graphics.Color.rgb(85, 170, 85)
        canvas.drawRect(32f, 0f, 48f, 4f, paint)
        
        // Stone (3, 0)
        paint.color = android.graphics.Color.rgb(128, 128, 128)
        canvas.drawRect(48f, 0f, 64f, 16f, paint)
        paint.color = android.graphics.Color.rgb(100, 100, 100)
        canvas.drawRect(50f, 50f, 60f, 60f, paint) // just noise
        
        // Sand (4, 0)
        paint.color = android.graphics.Color.rgb(219, 209, 159)
        canvas.drawRect(64f, 0f, 80f, 16f, paint)
        
        return bitmap
    }
}
