package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import com.example.game.BlockRegistry

object RenderItemEngine {
    
    // Renders a beautiful 3D block preview onto a Compose canvas using native canvas isometric projection
    fun draw3DBlockItem(
        canvas: Canvas,
        atlas: Bitmap,
        bId: Byte,
        width: Float,
        height: Float
    ) {
        val paint = Paint().apply {
            isFilterBitmap = false
        }
        val (topX, topY) = BlockRegistry.getTextureUV(bId, 0)
        val (sideX, sideY) = BlockRegistry.getTextureUV(bId, 2)
        
        val srcTop = Rect(topX * 16, topY * 16, topX * 16 + 16, topY * 16 + 16)
        val srcSide = Rect(sideX * 16, sideY * 16, sideX * 16 + 16, sideY * 16 + 16)
        
        val blockSize = width * 0.43f
        val centerX = width / 2f
        val centerY = height / 2f
        
        // 1. Draw Top Face
        canvas.save()
        canvas.translate(centerX, centerY - blockSize * 0.5f)
        canvas.scale(1f, 0.5f)
        canvas.rotate(45f)
        canvas.drawBitmap(atlas, srcTop, Rect((-blockSize * 0.7f).toInt(), (-blockSize * 0.7f).toInt(), (blockSize * 0.7f).toInt(), (blockSize * 0.7f).toInt()), paint)
        canvas.restore()
        
        // Color Filters for shading to give depth
        val dimPaint = Paint().apply {
            isFilterBitmap = false
            colorFilter = PorterDuffColorFilter(
                android.graphics.Color.argb(90, 0, 0, 0),
                PorterDuff.Mode.SRC_ATOP
            )
        }
        val dimPaintDarker = Paint().apply {
            isFilterBitmap = false
            colorFilter = PorterDuffColorFilter(
                android.graphics.Color.argb(140, 0, 0, 0),
                PorterDuff.Mode.SRC_ATOP
            )
        }
        
        // 2. Draw Left Face
        canvas.save()
        canvas.translate(centerX - blockSize * 0.5f, centerY)
        canvas.skew(0f, 0.5f)
        canvas.drawBitmap(atlas, srcSide, Rect(0, 0, blockSize.toInt(), blockSize.toInt()), dimPaint)
        canvas.restore()
        
        // 3. Draw Right Face
        canvas.save()
        canvas.translate(centerX, centerY + blockSize * 0.5f)
        canvas.skew(0f, -0.5f)
        canvas.drawBitmap(atlas, srcSide, Rect(0, (-blockSize).toInt(), blockSize.toInt(), 0), dimPaintDarker)
        canvas.restore()
    }

    // Helper to draw standard flat items or plants (roses, dandelions, swords, pickaxes) in hotbar
    fun drawFlatItem(
        canvas: Canvas,
        atlas: Bitmap,
        itemId: Byte,
        width: Float,
        height: Float
    ) {
        val paint = Paint().apply {
            isFilterBitmap = false
        }
        val (tx, ty) = BlockRegistry.getTextureUV(itemId, 2)
        val src = Rect(tx * 16, ty * 16, tx * 16 + 16, ty * 16 + 16)
        val dst = Rect(0, 0, width.toInt(), height.toInt())
        canvas.drawBitmap(atlas, src, dst, paint)
    }
}
