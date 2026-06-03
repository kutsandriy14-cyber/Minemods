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
fun ItemIcon(color: Color, modifier: Modifier = Modifier, isTool: Boolean = false) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (isTool) {
            // Draw a stick (handle)
            drawLine(
                color = Color(0xFF5E3A1A),
                start = Offset(w * 0.2f, h * 0.8f),
                end = Offset(w * 0.6f, h * 0.4f),
                strokeWidth = w * 0.15f
            )
            // Draw tool head
            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.45f, h * 0.1f),
                size = Size(w * 0.45f, h * 0.4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
            )
            // Highlight
            drawRoundRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(w * 0.5f, h * 0.15f),
                size = Size(w * 0.35f, h * 0.1f)
            )
        } else {
            // Draw as a 3D isometric block
            // Top face
            val topColor = Color(
                color.red.coerceIn(0f, 1f) * 1.2f,
                color.green.coerceIn(0f, 1f) * 1.2f,
                color.blue.coerceIn(0f, 1f) * 1.2f
            ).coerceInColor()
            // Right face
            val rightColor = Color(
                color.red * 0.8f,
                color.green * 0.8f,
                color.blue * 0.8f
            ).coerceInColor()
            // Left face (base color)
            
            // Draw Left Face
            val leftPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.1f, h * 0.35f)
                lineTo(w * 0.5f, h * 0.55f)
                lineTo(w * 0.5f, h * 0.95f)
                lineTo(w * 0.1f, h * 0.75f)
                close()
            }
            drawPath(path = leftPath, color = color)
            
            // Draw Right Face
            val rightPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.5f, h * 0.55f)
                lineTo(w * 0.9f, h * 0.35f)
                lineTo(w * 0.9f, h * 0.75f)
                lineTo(w * 0.5f, h * 0.95f)
                close()
            }
            drawPath(path = rightPath, color = rightColor)
            
            // Draw Top Face
            val topPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.5f, h * 0.15f)
                lineTo(w * 0.9f, h * 0.35f)
                lineTo(w * 0.5f, h * 0.55f)
                lineTo(w * 0.1f, h * 0.35f)
                close()
            }
            drawPath(path = topPath, color = topColor)
            
            // Inner borders for pixel art feel
            drawPath(path = topPath, color = Color.Black.copy(alpha=0.3f), style = Stroke(width = w*0.05f))
            drawPath(path = leftPath, color = Color.Black.copy(alpha=0.3f), style = Stroke(width = w*0.05f))
            drawPath(path = rightPath, color = Color.Black.copy(alpha=0.3f), style = Stroke(width = w*0.05f))
        }
    }
}

fun Color.coerceInColor(): Color {
    return Color(
        red = this.red.coerceIn(0f, 1f),
        green = this.green.coerceIn(0f, 1f),
        blue = this.blue.coerceIn(0f, 1f),
        alpha = this.alpha.coerceIn(0f, 1f)
    )
}

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
