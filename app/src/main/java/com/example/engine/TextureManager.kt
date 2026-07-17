package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils

object TextureManager {
    var atlasTextureId: Int = 0
        private set
        
    var atlasBitmap: Bitmap? = null
        private set
        
    fun init(context: Context) {
        val textureObjectIds = IntArray(1)
        GLES30.glGenTextures(1, textureObjectIds, 0)
        
        if (textureObjectIds[0] == 0) {
            return
        }
        
        // Generate placeholder atlas (256x256, 16x16 tiles = 16x16 pixels per tile)
        val bitmap = createPlaceholderAtlas()
        atlasBitmap = bitmap
        
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureObjectIds[0])
        
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        
        // bitmap.recycle() // Keep for UI
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        
        atlasTextureId = textureObjectIds[0]
    }
    
    private fun createPlaceholderAtlas(): Bitmap {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()
        
        // Helper to draw a single pixel at (x, y) with a specific color
        fun drawPixel(x: Float, y: Float, color: Int) {
            paint.color = color
            canvas.drawRect(x, y, x + 1f, y + 1f, paint)
        }

        // 1. Grass top (0, 0)
        for (py in 0..15) {
            for (px in 0..15) {
                val noise = ((px * 17 + py * 31) % 5)
                val color = when (noise) {
                    0 -> android.graphics.Color.rgb(75, 150, 75)
                    1 -> android.graphics.Color.rgb(95, 185, 95)
                    2 -> android.graphics.Color.rgb(60, 135, 60)
                    3 -> android.graphics.Color.rgb(105, 200, 105)
                    else -> android.graphics.Color.rgb(85, 170, 85)
                }
                drawPixel(px.toFloat(), py.toFloat(), color)
            }
        }
        
        // 2. Dirt (1, 0) - offset x = 16
        for (py in 0..15) {
            for (px in 0..15) {
                val noise = ((px * 23 + py * 13) % 5)
                val color = when (noise) {
                    0 -> android.graphics.Color.rgb(114, 76, 47)
                    1 -> android.graphics.Color.rgb(144, 106, 77)
                    2 -> android.graphics.Color.rgb(94, 61, 35)
                    3 -> android.graphics.Color.rgb(124, 86, 57)
                    else -> android.graphics.Color.rgb(134, 96, 67)
                }
                drawPixel(16f + px, py.toFloat(), color)
            }
        }
        
        // 3. Grass side (2, 0) - offset x = 32
        for (py in 0..15) {
            for (px in 0..15) {
                val isGrass = py < 4 || (py == 4 && px % 2 == 0) || (py == 5 && px % 4 == 1)
                val color = if (isGrass) {
                    val noise = ((px * 17 + py * 31) % 5)
                    when (noise) {
                        0 -> android.graphics.Color.rgb(75, 150, 75)
                        1 -> android.graphics.Color.rgb(95, 185, 95)
                        2 -> android.graphics.Color.rgb(60, 135, 60)
                        3 -> android.graphics.Color.rgb(105, 200, 105)
                        else -> android.graphics.Color.rgb(85, 170, 85)
                    }
                } else {
                    val noise = ((px * 23 + py * 13) % 5)
                    when (noise) {
                        0 -> android.graphics.Color.rgb(114, 76, 47)
                        1 -> android.graphics.Color.rgb(144, 106, 77)
                        2 -> android.graphics.Color.rgb(94, 61, 35)
                        3 -> android.graphics.Color.rgb(124, 86, 57)
                        else -> android.graphics.Color.rgb(134, 96, 67)
                    }
                }
                drawPixel(32f + px, py.toFloat(), color)
            }
        }
        
        // 4. Stone (3, 0) - offset x = 48
        for (py in 0..15) {
            for (px in 0..15) {
                val noise = ((px * 19 + py * 29) % 6)
                val isCrack = (px + py == 8 || px + py == 16 || px - py == 4)
                val isHighlight = (px + py == 5 || px == 2 || py == 2)
                val color = when {
                    isCrack -> android.graphics.Color.rgb(90, 90, 90)
                    isHighlight -> android.graphics.Color.rgb(150, 150, 150)
                    noise == 0 -> android.graphics.Color.rgb(110, 110, 110)
                    noise == 1 -> android.graphics.Color.rgb(135, 135, 135)
                    else -> android.graphics.Color.rgb(125, 125, 125)
                }
                drawPixel(48f + px, py.toFloat(), color)
            }
        }
        
        // 5. Sand (4, 0) - offset x = 64
        for (py in 0..15) {
            for (px in 0..15) {
                val ripple = (px + py / 2) % 4
                val color = when (ripple) {
                    0 -> android.graphics.Color.rgb(209, 199, 149)
                    1 -> android.graphics.Color.rgb(229, 219, 169)
                    else -> android.graphics.Color.rgb(219, 209, 159)
                }
                drawPixel(64f + px, py.toFloat(), color)
            }
        }
        
        // Wood Top/Bottom (5, 1) - offset x=80, y=16
        for (py in 0..15) {
            for (px in 0..15) {
                val ring = Math.max(Math.abs(px - 7.5), Math.abs(py - 7.5)).toInt()
                val color = if (ring % 2 == 0) android.graphics.Color.rgb(130, 95, 60) else android.graphics.Color.rgb(100, 70, 45)
                drawPixel(80f + px, 16f + py, color)
            }
        }
        
        // Wood Side (4, 1) - offset x=64, y=16
        for (py in 0..15) {
            for (px in 0..15) {
                val grain = (px / 2) % 2
                val color = if (grain == 0) android.graphics.Color.rgb(100, 70, 45) else android.graphics.Color.rgb(80, 50, 30)
                drawPixel(64f + px, 16f + py, color)
            }
        }

        // Leaves (4, 3) - offset x=64, y=48
        for (py in 0..15) {
            for (px in 0..15) {
                val color = if ((px + py) % 3 == 0) android.graphics.Color.argb(200, 34, 139, 34) else android.graphics.Color.argb(220, 20, 100, 20)
                drawPixel(64f + px, 48f + py, color)
            }
        }

        // Planks (4, 0 is Sand, wait I used Sand for planks in BlockRegistry. Let's put Planks at 5, 0)
        // I will change BlockRegistry later. Let's put Planks at (5, 0) - offset x=80, y=0
        for (py in 0..15) {
            for (px in 0..15) {
                val line = (py % 4 == 3 || px % 8 == (py/4)*4)
                val color = if (line) android.graphics.Color.rgb(120, 90, 60) else android.graphics.Color.rgb(170, 130, 90)
                drawPixel(80f + px, py.toFloat(), color)
            }
        }

        // Glass (1, 3) - offset x=16, y=48
        for (py in 0..15) {
            for (px in 0..15) {
                val edge = (px == 0 || px == 15 || py == 0 || py == 15)
                val color = if (edge) android.graphics.Color.argb(200, 200, 220, 255) else android.graphics.Color.argb(40, 200, 220, 255)
                drawPixel(16f + px, 48f + py, color)
            }
        }
        
        // Cobblestone (0, 1) - offset x=0, y=16
        for (py in 0..15) {
            for (px in 0..15) {
                val color = if ((px+py)%5 == 0 || (px-py)%4 == 0) android.graphics.Color.rgb(60, 60, 60) else android.graphics.Color.rgb(100, 100, 100)
                drawPixel(px.toFloat(), 16f + py, color)
            }
        }

        // Water (13, 12) - offset x=208, y=192
        for (py in 0..15) {
            for (px in 0..15) {
                val color = android.graphics.Color.argb(180, 50, 100, 255)
                drawPixel(208f + px, 192f + py, color)
            }
        }
        
        // Lava (13, 14) - offset x=208, y=224
        for (py in 0..15) {
            for (px in 0..15) {
                val color = if ((px*py)%3 == 0) android.graphics.Color.rgb(255, 100, 0) else android.graphics.Color.rgb(255, 50, 0)
                drawPixel(208f + px, 224f + py, color)
            }
        }
        
        return bitmap
    }
}
