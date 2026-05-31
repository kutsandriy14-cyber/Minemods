package com.example.minecraft.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Custom Minecraft Button with 3D retro borders.
 * Replicates the classic stone buttons of Minecraft Java Edition with precise highlight/shadow colors!
 */
@Composable
fun MinecraftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Minecraft Button Palette
    val baseColor = if (!enabled) Color(0xFF555555) else if (isPressed) Color(0xFF6A9246) else Color(0xFF8C8C8C)
    val topBorder = if (isPressed) Color(0xFF33551B) else Color(0xFFD2D2D2)
    val bottomBorder = if (isPressed) Color(0xFF86A070) else Color(0xFF565656)
    
    Box(
        modifier = modifier
            .testTag(testTag)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .background(baseColor)
            .border(width = 2.dp, color = topBorder, shape = RectangleShape)
            .padding(vertical = 10.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Overlay a dark shading on right and bottom borders
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            // Bottom shadow line
            drawRect(
                color = bottomBorder,
                topLeft = Offset(0f, h - 6f),
                size = Size(w, 6f)
            )
            // Right shadow line
            drawRect(
                color = bottomBorder,
                topLeft = Offset(w - 6f, 0f),
                size = Size(6f, h)
            )
        }
        
        Text(
            text = text.uppercase(),
            color = if (!enabled) Color(0xFFA0A0A0) else if (isPressed) Color.White else MinecraftTextYellow,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp)
        )
    }
}

/**
 * Beautiful full-screen Backdrop which creates a cross-hatch tiled soil/stone pattern,
 * perfectly echoing the Minecraft Title Screen background.
 */
@Composable
fun TiledDirtBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val tileSize = 48.dp.toPx()
        
        val cols = (width / tileSize).toInt() + 1
        val rows = (height / tileSize).toInt() + 1
        
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val px = c * tileSize
                val py = r * tileSize
                
                // Base brown soil color
                drawRect(
                    color = Color(0xFF4C3526),
                    topLeft = Offset(px, py),
                    size = Size(tileSize, tileSize)
                )
                
                // Distinct pixelated details in dirt block
                drawRect(
                    color = Color(0xFF3B271B),
                    topLeft = Offset(px + 4f, py + 4f),
                    size = Size(tileSize - 8f, tileSize - 8f)
                )

                // Render micro cross-hatch specks
                val seed = (c * 79 + r * 37)
                if (seed % 3 == 0) {
                    drawRect(
                        color = Color(0xFF2B1C12),
                        topLeft = Offset(px + 12f, py + 12f),
                        size = Size(8f, 8f)
                    )
                }
                if (seed % 5 == 0) {
                    drawRect(
                        color = Color(0xFF5F412E),
                        topLeft = Offset(px + 28f, py + 20f),
                        size = Size(10f, 10f)
                    )
                }
            }
        }
        // Top edge shadow overlays
        drawRect(
            color = Color(0x99000000),
            topLeft = Offset.Zero,
            size = Size(width, height)
        )
    }
}
