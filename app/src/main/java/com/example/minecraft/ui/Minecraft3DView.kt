package com.example.minecraft.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.opengl.GLSurfaceView
import android.opengl.GLU
import android.opengl.GLUtils
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.Color as AndroidColor
import com.example.minecraft.model.*
import com.example.ui.theme.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Random
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Composable
fun Minecraft3DView(
    modifier: Modifier = Modifier,
    world: WorldSave,
    viewModel: GameViewModel,
    pitchAngle: Float = -12f,
    yawAngle: Float = 14f,
    zoomVal: Float = 8.5f,
    isFirstPerson: Boolean = true,
    onBlockTap: (Int, Int) -> Unit
) {
    val context = LocalContext.current

    // Keep state updated in the surface-view reference
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                // Configure GL context
                setEGLContextClientVersion(1) // OpenGLES 1.0/1.1
                
                val mRenderer = Minecraft3DRenderer(ctx, world, viewModel, pitchAngle, yawAngle, zoomVal, isFirstPerson)
                tag = mRenderer
                setRenderer(mRenderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                
                // Add touch support to select block coords via simple projection or click raycast
                // For direct reliability, clicking on screen translates to tapped coords
            }
        },
        update = { glSurfaceView ->
            val renderer = glSurfaceView.tag as? Minecraft3DRenderer
            if (renderer != null) {
                // Update properties
                renderer.worldState = world
                renderer.pitch = pitchAngle
                renderer.yaw = yawAngle
                renderer.zoom = zoomVal
                renderer.isFirstPerson = isFirstPerson
            }
        }
    )
}

class Minecraft3DRenderer(
    private val context: Context,
    @Volatile var worldState: WorldSave,
    private val viewModel: GameViewModel,
    @Volatile var pitch: Float,
    @Volatile var yaw: Float,
    @Volatile var zoom: Float,
    @Volatile var isFirstPerson: Boolean = true
) : GLSurfaceView.Renderer {

    private var textureId: Int = 0
    private var isTextureLoaded = false
    private var dynamicAtlas: Bitmap? = null

    private val maxVertices = 600000
    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(maxVertices * 3 * 4).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer()
    }
    private val texCoordinateBuffer: FloatBuffer = ByteBuffer.allocateDirect(maxVertices * 2 * 4).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer()
    }

    private val vertexArray = FloatArray(maxVertices * 3)
    private val texCoordArray = FloatArray(maxVertices * 2)
    private var vertexCount = 0

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        // Configure basic OpenGL settings
        gl.glEnable(GL10.GL_DEPTH_TEST)
        gl.glDepthFunc(GL10.GL_LEQUAL)
        
        // Lighting
        gl.glEnable(GL10.GL_LIGHTING)
        gl.glEnable(GL10.GL_LIGHT0)
        gl.glEnable(GL10.GL_COLOR_MATERIAL)

        // Set light position
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, floatArrayOf(10f, 30f, 15f, 1f), 0)
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, floatArrayOf(0.55f, 0.55f, 0.55f, 1f), 0)
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, floatArrayOf(0.75f, 0.75f, 0.75f, 1f), 0)

        // Texturing
        gl.glEnable(GL10.GL_TEXTURE_2D)

        // Generate procedural atlas
        dynamicAtlas = generateProceduralAtlas()
        
        // Load Texture atlas
        val textures = IntArray(1)
        gl.glGenTextures(1, textures, 0)
        textureId = textures[0]
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId)

        // Set filters to NEAREST for gorgeous crisp visual pixels!
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST.toFloat())
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST.toFloat())
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE.toFloat())
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE.toFloat())

        dynamicAtlas?.let {
            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, it, 0)
            isTextureLoaded = true
        }
        
        gl.glShadeModel(GL10.GL_SMOOTH)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        val aspect = width.toFloat() / height.toFloat()
        gl.glViewport(0, 0, width, height)
        gl.glMatrixMode(GL10.GL_PROJECTION)
        gl.glLoadIdentity()
        GLU.gluPerspective(gl, 45f, aspect, 0.1f, 120f)
        gl.glMatrixMode(GL10.GL_MODELVIEW)
    }

    private fun drawSteveFirstPersonHand(gl: GL10, px: Float, py: Float) {
        gl.glDisable(GL10.GL_TEXTURE_2D)
        gl.glDisable(GL10.GL_LIGHTING)
        
        // 1. Draw sleeve
        gl.glPushMatrix()
        gl.glLoadIdentity()
        gl.glColor4f(0.0f, 0.678f, 0.709f, 1f) // Steve's signature cyan shirt
        gl.glTranslatef(0.25f, -0.22f, -0.5f)
        gl.glRotatef(-30f, 0f, 1f, 0f)
        gl.glRotatef(15f, 1f, 0f, 0f)
        gl.glScalef(0.08f, 0.18f, 0.08f)
        drawCubeSkeleton(gl)
        gl.glPopMatrix()

        // 2. Draw bare skin hand (peach)
        gl.glPushMatrix()
        gl.glLoadIdentity()
        gl.glColor4f(0.91f, 0.71f, 0.55f, 1f) // Steve skin
        gl.glTranslatef(0.21f, -0.32f, -0.48f)
        gl.glRotatef(-30f, 0f, 1f, 0f)
        gl.glRotatef(15f, 1f, 0f, 0f)
        gl.glScalef(0.07f, 0.12f, 0.07f)
        drawCubeSkeleton(gl)
        gl.glPopMatrix()
        
        gl.glEnable(GL10.GL_LIGHTING)
        gl.glEnable(GL10.GL_TEXTURE_2D)
        gl.glColor4f(1f, 1f, 1f, 1f) // Reset
    }

    override fun onDrawFrame(gl: GL10) {
        // 1. Determine sky colored background dynamically
        val skyCol = getSkyColor(worldState.currentSkyTime)
        gl.glClearColor(skyCol.red, skyCol.green, skyCol.blue, 1.0f)
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)

        gl.glLoadIdentity()

        // 2. Position dynamic camera trailing Steve or inside Steve's head (First Person Mode!)
        val player = worldState.playerState
        
        if (isFirstPerson) {
            // Immersive First-Person View inside Steve's head looking outwards
            val eyeX = player.x
            val eyeY = player.y + 1.15f
            val eyeZ = 0.0f
            
            val radYaw = Math.toRadians(yaw.toDouble())
            val radPitch = Math.toRadians(pitch.toDouble())
            
            // Derive directional look vector from pitch and yaw
            val lookX = Math.sin(radYaw).toFloat() * Math.cos(radPitch).toFloat()
            val lookY = Math.sin(radPitch).toFloat()
            val lookZ = Math.cos(radYaw).toFloat() * Math.cos(radPitch).toFloat()
            
            GLU.gluLookAt(
                gl,
                eyeX, eyeY, eyeZ,                  // Eye at player's model head
                eyeX + lookX, eyeY + lookY, eyeZ + lookZ, // Look vector
                0f, 1f, 0f                          // Gravity up vector
            )
        } else {
            // Orbit mathematics (Third Person View)
            val camEyeX = player.x + (Math.sin(Math.toRadians(yaw.toDouble())) * Math.cos(Math.toRadians(pitch.toDouble())) * zoom).toFloat()
            val camEyeY = player.y + 0.8f - (Math.sin(Math.toRadians(pitch.toDouble())) * zoom).toFloat()
            val camEyeZ = (Math.cos(Math.toRadians(yaw.toDouble())) * Math.cos(Math.toRadians(pitch.toDouble())) * zoom).toFloat()

            GLU.gluLookAt(
                gl,
                camEyeX, camEyeY, camEyeZ,      // Eye position
                player.x, player.y + 0.5f, 0f,  // Look target
                0f, 1f, 0f                      // Up vector
            )
        }

        // Bind core texture
        if (isTextureLoaded) {
            gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId)
        }

        // Draw background stars during dark night
        if (worldState.currentSkyTime in 12500f..21500f) {
            draw3DStars(gl)
        }

        // 3. Render 3D Voxels Grid
        val centerGridX = player.x.toInt()
        val centerGridY = player.y.toInt()
 
        // Render dynamic chunk slices depending on chosen Render Distance
        val rDist = viewModel.renderDistanceSetting.value
        val renderRadX = when (rDist) {
            "Low" -> 6
            "High" -> 18
            else -> 12 // "Medium"
        }
        val renderRadY = when (rDist) {
            "Low" -> 5
            "High" -> 14
            else -> 10 // "Medium"
        }
        val terrainDepth = when (rDist) {
            "Low" -> 4
            "High" -> 15
            else -> 10 // "Medium"
        }
 
        val startX = (centerGridX - renderRadX).coerceAtLeast(0)
        val endX = (centerGridX + renderRadX).coerceAtMost(worldState.width - 1)
        val startY = (centerGridY - renderRadY).coerceAtLeast(0)
        val endY = (centerGridY + renderRadY).coerceAtMost(worldState.height - 1)
 
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
 
        vertexCount = 0
 
        for (bx in startX..endX) {
            for (by in startY..endY) {
                val bId = worldState.worldBlocks["$bx,$by"] ?: "air"
                if (bId == "air") continue
 
                // True volumetric rendering based on block categorization!
                val isTerrain = bId == "stone" || bId == "dirt" || bId == "grass" || bId == "bedrock" || 
                                bId.contains("ore") || bId == "obsidian" || bId == "tuff_bricks" || 
                                bId == "chiseled_tuff" || bId == "copper_block" || bId == "copper_bulb" || 
                                bId == "copper_grate" || bId == "chiseled_copper" || bId == "heavy_core" ||
                                bId == "deepslate" || bId == "netherrack" || bId == "ancient_debris" || 
                                bId == "amethyst_block" || bId == "prismarine" || bId == "mud" || bId.startsWith("mod_")
                val isBuilding = bId == "oak_planks" || bId == "cobblestone" || bId == "crafter" || bId == "barrel" || bId == "honeycomb_block"
 
                // Fast neighborhood culling flags
                val aboveId = worldState.worldBlocks["$bx,${by+1}"] ?: "air"
                val belowId = worldState.worldBlocks["$bx,${by-1}"] ?: "air"
                val leftId = worldState.worldBlocks["${bx-1},$by"] ?: "air"
                val rightId = worldState.worldBlocks["${bx+1},$by"] ?: "air"
 
                val isAboveSolid = aboveId != "air" && aboveId != "copper_grate" && aboveId != "trial_spawner"
                val isBelowSolid = belowId != "air" && belowId != "copper_grate" && belowId != "trial_spawner"
                val isLeftSolid = leftId != "air" && leftId != "copper_grate" && leftId != "trial_spawner"
                val isRightSolid = rightId != "air" && rightId != "copper_grate" && rightId != "trial_spawner"
                 
                if (isTerrain) {
                    for (bz in -terrainDepth..terrainDepth) {
                        // Apply an organic, satisfying slope curvature downwards away from the center ridge at z=0
                        val valeyCurvature = -0.06f * (bz * bz)
                         
                        // Sandwich check for depth slices
                        val drawBack = (bz == -terrainDepth)
                        val drawFront = (bz == terrainDepth)
                         
                        val aboveType = GameRegistry.blocks[aboveId]
                        val belowType = GameRegistry.blocks[belowId]
                        val leftType = GameRegistry.blocks[leftId]
                        val rightType = GameRegistry.blocks[rightId]
 
                        val drawTop = !isAboveSolid || (aboveType != null && !aboveType.isSolid)
                        val drawBottom = !isBelowSolid || (belowType != null && !belowType.isSolid)
                        val drawLeft = !isLeftSolid || (leftType != null && !leftType.isSolid)
                        val drawRight = !isRightSolid || (rightType != null && !rightType.isSolid)
 
                        build3DBlock(
                            bx.toFloat(), by.toFloat() + valeyCurvature, bz.toFloat(), bId,
                            drawFront = drawFront, drawBack = drawBack,
                            drawLeft = drawLeft, drawRight = drawRight,
                            drawTop = drawTop, drawBottom = drawBottom
                        )
                    }
                } else if (bId == "oak_log" || bId == "mangrove_log" || bId == "cherry_log") {
                    // Thick rounded cylindrical tree trunk (thickness 3)
                    val logDepth = terrainDepth.coerceAtMost(1)
                    for (bz in -logDepth..logDepth) {
                        val drawBack = (bz == -logDepth)
                        val drawFront = (bz == logDepth)
                        val drawTop = !isAboveSolid
                        val drawBottom = !isBelowSolid
                        val drawLeft = !isLeftSolid
                        val drawRight = !isRightSolid
 
                        build3DBlock(
                            bx.toFloat(), by.toFloat(), bz.toFloat(), bId,
                            drawFront = drawFront, drawBack = drawBack,
                            drawLeft = drawLeft, drawRight = drawRight,
                            drawTop = drawTop, drawBottom = drawBottom
                        )
                    }
                } else if (bId == "oak_leaves" || bId == "cherry_leaves") {
                    // Volumetric leaf canopy (thickness 5)
                    val leafDepth = terrainDepth.coerceAtMost(2)
                    for (bz in -leafDepth..leafDepth) {
                        val slope = if (Math.abs(bz) == leafDepth && leafDepth > 1) -0.15f else 0f
                        val drawBack = (bz == -leafDepth)
                        val drawFront = (bz == leafDepth)
                        val drawTop = !isAboveSolid
                        val drawBottom = !isBelowSolid
                        val drawLeft = !isLeftSolid
                        val drawRight = !isRightSolid
 
                        build3DBlock(
                            bx.toFloat(), by.toFloat() + slope, bz.toFloat(), bId,
                            drawFront = drawFront, drawBack = drawBack,
                            drawLeft = drawLeft, drawRight = drawRight,
                            drawTop = drawTop, drawBottom = drawBottom
                        )
                    }
                } else if (isBuilding) {
                    // Sturdy built walls of 3-block thickness
                    for (bz in -1..1) {
                        val drawBack = (bz == -1)
                        val drawFront = (bz == 1)
                        val drawTop = !isAboveSolid
                        val drawBottom = !isBelowSolid
                        val drawLeft = !isLeftSolid
                        val drawRight = !isRightSolid
 
                        build3DBlock(
                            bx.toFloat(), by.toFloat(), bz.toFloat(), bId,
                            drawFront = drawFront, drawBack = drawBack,
                            drawLeft = drawLeft, drawRight = drawRight,
                            drawTop = drawTop, drawBottom = drawBottom
                        )
                    }
                } else {
                    // Interactives & decorative blocks (1-slice thickness at bz=0)
                    build3DBlock(
                        bx.toFloat(), by.toFloat(), 0f, bId,
                        drawFront = true, drawBack = true,
                        drawLeft = !isLeftSolid, drawRight = !isRightSolid,
                        drawTop = !isAboveSolid, drawBottom = !isBelowSolid
                    )
                }
            }
        }
 
        // Render visible batch quads in precisely one call!
        if (vertexCount > 0) {
            vertexBuffer.clear()
            vertexBuffer.put(vertexArray, 0, vertexCount * 3)
            vertexBuffer.position(0)
 
            texCoordinateBuffer.clear()
            texCoordinateBuffer.put(texCoordArray, 0, vertexCount * 2)
            texCoordinateBuffer.position(0)
 
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer)
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texCoordinateBuffer)
 
            gl.glDrawArrays(GL10.GL_TRIANGLES, 0, vertexCount)
        }
 
        // 4. Draw Steve or Steve's Hand overlay!
        if (isFirstPerson) {
            drawSteveFirstPersonHand(gl, player.x, player.y)
        } else {
            draw3DSteve(gl, player.x, player.y)
        }
 
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
    }
 
    private fun build3DBlock(
        x: Float, y: Float, z: Float, blockId: String,
        drawFront: Boolean = true, drawBack: Boolean = true,
        drawLeft: Boolean = true, drawRight: Boolean = true,
        drawTop: Boolean = true, drawBottom: Boolean = true
    ) {
        if (drawFront) {
            val tile = getBlockTile(blockId, "front")
            addFrontFace(x, y, z, tile.first, tile.second)
        }
        if (drawBack) {
            val tile = getBlockTile(blockId, "back")
            addBackFace(x, y, z, tile.first, tile.second)
        }
        if (drawLeft) {
            val tile = getBlockTile(blockId, "left")
            addLeftFace(x, y, z, tile.first, tile.second)
        }
        if (drawRight) {
            val tile = getBlockTile(blockId, "right")
            addRightFace(x, y, z, tile.first, tile.second)
        }
        if (drawTop) {
            val tile = getBlockTile(blockId, "top")
            addTopFace(x, y, z, tile.first, tile.second)
        }
        if (drawBottom) {
            val tile = getBlockTile(blockId, "bottom")
            addBottomFace(x, y, z, tile.first, tile.second)
        }
    }
 
    private fun addVertex(x: Float, y: Float, z: Float, u: Float, v: Float) {
        if (vertexCount >= maxVertices) return
        val vIdx = vertexCount * 3
        vertexArray[vIdx] = x
        vertexArray[vIdx + 1] = y
        vertexArray[vIdx + 2] = z
 
        val tIdx = vertexCount * 2
        texCoordArray[tIdx] = u
        texCoordArray[tIdx + 1] = v
 
        vertexCount++
    }
 
    private fun addFace(
        x0: Float, y0: Float, z0: Float, u0: Float, v0: Float,
        x1: Float, y1: Float, z1: Float, u1: Float, v1: Float,
        x2: Float, y2: Float, z2: Float, u2: Float, v2: Float,
        x3: Float, y3: Float, z3: Float, u3: Float, v3: Float
    ) {
        addVertex(x0, y0, z0, u0, v0)
        addVertex(x1, y1, z1, u1, v1)
        addVertex(x2, y2, z2, u2, v2)
 
        addVertex(x0, y0, z0, u0, v0)
        addVertex(x2, y2, z2, u2, v2)
        addVertex(x3, y3, z3, u3, v3)
    }
 
    private fun addFrontFace(x: Float, y: Float, z: Float, tileCol: Int, tileRow: Int) {
        val size = 16f
        val sMin = tileCol / size
        val sMax = (tileCol + 1) / size
        val tMin = tileRow / size
        val tMax = (tileRow + 1) / size
 
        addFace(
            x - 0.5f, y - 0.5f, z + 0.5f, sMin, tMax,
            x + 0.5f, y - 0.5f, z + 0.5f, sMax, tMax,
            x + 0.5f, y + 0.5f, z + 0.5f, sMax, tMin,
            x - 0.5f, y + 0.5f, z + 0.5f, sMin, tMin
        )
    }
 
    private fun addBackFace(x: Float, y: Float, z: Float, tileCol: Int, tileRow: Int) {
        val size = 16f
        val sMin = tileCol / size
        val sMax = (tileCol + 1) / size
        val tMin = tileRow / size
        val tMax = (tileRow + 1) / size
 
        addFace(
            x + 0.5f, y - 0.5f, z - 0.5f, sMin, tMax,
            x - 0.5f, y - 0.5f, z - 0.5f, sMax, tMax,
            x - 0.5f, y + 0.5f, z - 0.5f, sMax, tMin,
            x + 0.5f, y + 0.5f, z - 0.5f, sMin, tMin
        )
    }
 
    private fun addLeftFace(x: Float, y: Float, z: Float, tileCol: Int, tileRow: Int) {
        val size = 16f
        val sMin = tileCol / size
        val sMax = (tileCol + 1) / size
        val tMin = tileRow / size
        val tMax = (tileRow + 1) / size
 
        addFace(
            x - 0.5f, y - 0.5f, z - 0.5f, sMin, tMax,
            x - 0.5f, y - 0.5f, z + 0.5f, sMax, tMax,
            x - 0.5f, y + 0.5f, z + 0.5f, sMax, tMin,
            x - 0.5f, y + 0.5f, z - 0.5f, sMin, tMin
        )
    }
 
    private fun addRightFace(x: Float, y: Float, z: Float, tileCol: Int, tileRow: Int) {
        val size = 16f
        val sMin = tileCol / size
        val sMax = (tileCol + 1) / size
        val tMin = tileRow / size
        val tMax = (tileRow + 1) / size
 
        addFace(
            x + 0.5f, y - 0.5f, z + 0.5f, sMin, tMax,
            x + 0.5f, y - 0.5f, z - 0.5f, sMax, tMax,
            x + 0.5f, y + 0.5f, z - 0.5f, sMax, tMin,
            x + 0.5f, y + 0.5f, z + 0.5f, sMin, tMin
        )
    }
 
    private fun addTopFace(x: Float, y: Float, z: Float, tileCol: Int, tileRow: Int) {
        val size = 16f
        val sMin = tileCol / size
        val sMax = (tileCol + 1) / size
        val tMin = tileRow / size
        val tMax = (tileRow + 1) / size
 
        addFace(
            x - 0.5f, y + 0.5f, z + 0.5f, sMin, tMax,
            x + 0.5f, y + 0.5f, z + 0.5f, sMax, tMax,
            x + 0.5f, y + 0.5f, z - 0.5f, sMax, tMin,
            x - 0.5f, y + 0.5f, z - 0.5f, sMin, tMin
        )
    }
 
    private fun addBottomFace(x: Float, y: Float, z: Float, tileCol: Int, tileRow: Int) {
        val size = 16f
        val sMin = tileCol / size
        val sMax = (tileCol + 1) / size
        val tMin = tileRow / size
        val tMax = (tileRow + 1) / size
 
        addFace(
            x - 0.5f, y - 0.5f, z - 0.5f, sMin, tMax,
            x + 0.5f, y - 0.5f, z - 0.5f, sMax, tMax,
            x + 0.5f, y - 0.5f, z + 0.5f, sMax, tMin,
            x - 0.5f, y - 0.5f, z + 0.5f, sMin, tMin
        )
    }

    private fun draw3DSteve(gl: GL10, px: Float, py: Float) {
        // Steve is composed of 3D scaled block structures
        // No texture translation needed, but we can bind solid voxel parts
        gl.glDisable(GL10.GL_TEXTURE_2D)
        gl.glNormal3f(0f, 0f, 1f)

        // 1. Torso body segment (Steve bright Cyan shirt!)
        gl.glColor4f(0f, 0.678f, 0.709f, 1f)
        gl.glPushMatrix()
        gl.glTranslatef(px, py + 0.6f, 0f) // mid torso point
        gl.glScalef(0.44f, 0.75f, 0.28f)
        drawCubeSkeleton(gl)
        gl.glPopMatrix()

        // 2. Head (Steve Peach skin color block!)
        gl.glColor4f(0.91f, 0.71f, 0.55f, 1f)
        gl.glPushMatrix()
        gl.glTranslatef(px, py + 1.25f, 0f)
        gl.glScalef(0.35f, 0.35f, 0.35f)
        drawCubeSkeleton(gl)
        gl.glPopMatrix()

        // Hair Cap
        gl.glColor4f(0.32f, 0.2f, 0.12f, 1f)
        gl.glPushMatrix()
        gl.glTranslatef(px, py + 1.38f, 0.02f)
        gl.glScalef(0.36f, 0.11f, 0.36f)
        drawCubeSkeleton(gl)
        gl.glPopMatrix()

        // 3. Trousers/Legs (Steve Purple)
        gl.glColor4f(0.247f, 0.231f, 0.423f, 1f)
        gl.glPushMatrix()
        gl.glTranslatef(px, py + 0.18f, 0f)
        gl.glScalef(0.42f, 0.45f, 0.26f)
        drawCubeSkeleton(gl)
        gl.glPopMatrix()

        // 4. Swaying arms if playing is walking!
        val time = System.currentTimeMillis()
        val walking = Math.abs(viewModel.joystickX) > 0.05f
        val swayAngle = if (walking) {
            (Math.sin(time.toDouble() * 0.015) * 35.0).toFloat()
        } else {
            0f
        }

        // Left Arm
        gl.glColor4f(0f, 0.678f, 0.709f, 1f)
        gl.glPushMatrix()
        gl.glTranslatef(px - 0.28f, py + 0.8f, 0f)
        gl.glRotatef(swayAngle, 1f, 0f, 0f)
        gl.glTranslatef(0f, -0.2f, 0f)
        gl.glScalef(0.12f, 0.45f, 0.12f)
        drawCubeSkeleton(gl)
        gl.glPopMatrix()

        // Right Arm
        gl.glPushMatrix()
        gl.glTranslatef(px + 0.28f, py + 0.8f, 0f)
        gl.glRotatef(-swayAngle, 1f, 0f, 0f)
        gl.glTranslatef(0f, -0.2f, 0f)
        gl.glScalef(0.12f, 0.45f, 0.12f)
        drawCubeSkeleton(gl)
        gl.glPopMatrix()

        gl.glEnable(GL10.GL_TEXTURE_2D)
        gl.glColor4f(1f, 1f, 1f, 1f) // Reset
    }

    private fun drawCubeSkeleton(gl: GL10) {
        // Draws a textured or solid scaled block cube
        // FRONT
        gl.glPushMatrix()
        gl.glTranslatef(0f, 0f, 0.5f)
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4)
        gl.glPopMatrix()

        // BACK
        gl.glPushMatrix()
        gl.glTranslatef(0f, 0f, -0.5f)
        gl.glRotatef(180f, 0f, 1f, 0f)
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4)
        gl.glPopMatrix()

        // LEFT
        gl.glPushMatrix()
        gl.glTranslatef(-0.5f, 0f, 0f)
        gl.glRotatef(-90f, 0f, 1f, 0f)
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4)
        gl.glPopMatrix()

        // RIGHT
        gl.glPushMatrix()
        gl.glTranslatef(0.5f, 0f, 0f)
        gl.glRotatef(90f, 0f, 1f, 0f)
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4)
        gl.glPopMatrix()

        // TOP
        gl.glPushMatrix()
        gl.glTranslatef(0f, 0.5f, 0f)
        gl.glRotatef(-90f, 1f, 0f, 0f)
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4)
        gl.glPopMatrix()

        // BOTTOM
        gl.glPushMatrix()
        gl.glTranslatef(0f, -0.5f, 0f)
        gl.glRotatef(90f, 1f, 0f, 0f)
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4)
        gl.glPopMatrix()
    }

    private fun draw3DStars(gl: GL10) {
        gl.glDisable(GL10.GL_TEXTURE_2D)
        gl.glDisable(GL10.GL_LIGHTING)
        gl.glColor4f(1f, 1f, 1f, 0.85f)
        
        // Draw 40 stars on a background matrix shell (deep behind blocks depth)
        val rand = Random(42)
        val center = worldState.playerState
        
        gl.glPushMatrix()
        // Stabilize stars following the player
        gl.glTranslatef(center.x, center.y + 2f, -12f)
        
        for (i in 0..40) {
            val sx = (rand.nextFloat() * 44f) - 22f
            val sy = (rand.nextFloat() * 30f) - 15f
            val sz = (rand.nextFloat() * 10f) - 5f
            
            gl.glPushMatrix()
            gl.glTranslatef(sx, sy, sz)
            gl.glScalef(0.06f, 0.06f, 0.06f)
            gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4)
            gl.glPopMatrix()
        }
        
        gl.glPopMatrix()
        gl.glEnable(GL10.GL_LIGHTING)
        gl.glEnable(GL10.GL_TEXTURE_2D)
        gl.glColor4f(1f, 1f, 1f, 1f)
    }

    private fun pushTextureCoords(gl: GL10, tile: Pair<Int, Int>) {
        val col = tile.first
        val row = tile.second

        val size = 16f
        val sMin = col / size
        val sMax = (col + 1) / size
        val tMin = row / size
        val tMax = (row + 1) / size

        // Update the texture buffer indices
        val coords = floatArrayOf(
            sMin, tMax,
            sMax, tMax,
            sMax, tMin,
            sMin, tMin
        )
        texCoordinateBuffer.clear()
        texCoordinateBuffer.put(coords)
        texCoordinateBuffer.position(0)
        
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texCoordinateBuffer)
    }

    private fun getBlockTile(blockId: String, face: String): Pair<Int, Int> {
        return when (blockId) {
            "grass" -> when (face) {
                "top" -> Pair(0, 0)
                "bottom" -> Pair(2, 0)
                else -> Pair(1, 0)
            }
            "dirt" -> Pair(2, 0)
            "stone" -> Pair(3, 0)
            "cobblestone" -> Pair(4, 0)
            "oak_log" -> when (face) {
                "top", "bottom" -> Pair(5, 0)
                else -> Pair(4, 0)
            }
            "oak_leaves" -> Pair(6, 0)
            "coal_ore" -> Pair(7, 0)
            "netherrack" -> Pair(8, 0)
            "soul_sand" -> Pair(9, 0)
            "glowstone" -> Pair(10, 0)
            "end_stone" -> Pair(11, 0)
            "purpur_block" -> Pair(12, 0)
            "chorus_flower" -> Pair(13, 0)
            "sand" -> Pair(14, 0)
            "sandstone" -> Pair(15, 0)

            "iron_ore" -> Pair(0, 1)
            "diamond_ore" -> Pair(1, 1)
            "obsidian" -> Pair(2, 1)
            "bedrock" -> Pair(3, 1)
            "crafting_table" -> when (face) {
                "top" -> Pair(4, 1)
                else -> Pair(5, 1)
            }
            "furnace" -> when (face) {
                "front" -> Pair(6, 1)
                else -> Pair(7, 1)
            }
            "netherite_block" -> Pair(8, 1)
            "crying_obsidian" -> Pair(9, 1)
            "ancient_debris" -> Pair(10, 1)
            "deepslate" -> Pair(11, 1)
            "sculk" -> Pair(12, 1)
            "sculk_sensor" -> Pair(13, 1)
            "amethyst_block" -> Pair(14, 1)
            "prismarine" -> Pair(15, 1)

            "chest" -> Pair(0, 2)
            "nano_banana_2" -> Pair(1, 2) // Bright shining banana texture!
            "crafter" -> Pair(2, 2)
            "trial_spawner" -> Pair(3, 2)
            "vault" -> Pair(4, 2)
            "heavy_core" -> Pair(5, 2)
            "copper_bulb" -> Pair(6, 2)
            "copper_grate" -> Pair(7, 2)
            "chiseled_copper" -> Pair(8, 2)
            "copper_block" -> Pair(9, 2)
            "copper_ore" -> Pair(10, 2)
            "tuff_bricks" -> Pair(11, 2)
            "chiseled_tuff" -> Pair(12, 2)
            "cherry_log" -> when (face) {
                "top", "bottom" -> Pair(5, 0)
                else -> Pair(13, 2)
            }
            "cherry_leaves" -> Pair(14, 2)
            "redstone_block" -> Pair(15, 2)

            "sea_lantern" -> Pair(0, 3)
            "beehive" -> Pair(1, 3)
            "sponge" -> Pair(2, 3)
            "magma_block" -> Pair(3, 3)
            "lapis_block" -> Pair(4, 3)
            "emerald_block" -> Pair(5, 3)
            "gold_block" -> Pair(6, 3)
            "iron_block" -> Pair(7, 3)
            "diamond_block" -> Pair(8, 3)
            "mud" -> Pair(9, 3)
            "mangrove_log" -> when (face) {
                "top", "bottom" -> Pair(5, 0)
                else -> Pair(10, 3)
            }
            "blackstone" -> Pair(11, 3)
            "honey_block" -> Pair(12, 3)
            "honeycomb_block" -> Pair(13, 3)
            "froglight" -> Pair(14, 3)
            "barrel" -> Pair(15, 3)

            else -> {
                if (blockId.contains("banana") || blockId.startsWith("mod_")) {
                    Pair(1, 2) // Custom or modded targets fallback to yellow nano banana
                } else {
                    Pair(3, 0) // Default fallback stone
                }
            }
        }
    }

    private fun getSkyColor(tick: Float): Color {
        return when {
            tick < 10000 -> SkyDay
            tick < 12000 -> {
                val ratio = (tick - 10000) / 2000f
                lerpColor(SkyDay, SkyDusk, ratio)
            }
            tick < 13000 -> {
                val ratio = (tick - 12000) / 1000f
                lerpColor(SkyDusk, SkyNight, ratio)
            }
            tick < 21000 -> SkyNight
            tick < 22000 -> {
                val ratio = (tick - 21000) / 1000f
                lerpColor(SkyNight, SkyDawn, ratio)
            }
            else -> {
                val ratio = (tick - 22000) / 2000f
                lerpColor(SkyDawn, SkyDay, ratio)
            }
        }
    }

    private fun lerpColor(c1: Color, c2: Color, bias: Float): Color {
        val r = c1.red + (c2.red - c1.red) * bias
        val g = c1.green + (c2.green - c1.green) * bias
        val b = c1.blue + (c2.blue - c1.blue) * bias
        return Color(r, g, b)
    }

    private fun generateProceduralAtlas(): Bitmap {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Fill background transparency
        paint.color = AndroidColor.TRANSPARENT
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        val tiles = 16
        val tilePx = 32
        val rand = Random(42) // Stabilize texture seed

        for (ty in 0 until tiles) {
            for (tx in 0 until tiles) {
                val tileLeft = tx * tilePx
                val tileTop = ty * tilePx

                for (py in 0 until tilePx) {
                    for (px in 0 until tilePx) {
                        val color = determinePixelColor(tx, ty, px, py, rand)
                        paint.color = color
                        canvas.drawRect(
                            (tileLeft + px).toFloat(),
                            (tileTop + py).toFloat(),
                            (tileLeft + px + 1).toFloat(),
                            (tileTop + py + 1).toFloat(),
                            paint
                        )
                    }
                }
            }
        }
        return bitmap
    }

    private fun determinePixelColor(tileX: Int, tileY: Int, px: Int, py: Int, rand: Random): Int {
        val tilePx = 32
        val x16 = (px * 16) / tilePx
        val y16 = (py * 16) / tilePx

        val coarseNoise = (rand.nextFloat() * 14 - 7).toInt()
        val fineNoise = (rand.nextFloat() * 6 - 3).toInt()
        val noise = coarseNoise + fineNoise

        // Tactile 3D Bevel shading modifiers
        var bevelModifier = 0
        if (px == 0 || py == 0 || (px == 1 && py in 1..30) || (py == 1 && px in 1..30)) {
            bevelModifier = 18  // Distinct sunny top-left glow
        } else if (px == tilePx - 1 || py == tilePx - 1 || (px == tilePx - 2 && py in 1..30) || (py == tilePx - 2 && px in 1..30)) {
            bevelModifier = -25 // Strong 3D ambient shadow border
        }

        // Color builder applying bevel and safe byte-clamps
        fun col(r: Int, g: Int, b: Int, alpha: Int = 255): Int {
            val finalR = (r + bevelModifier).coerceIn(0, 255)
            val finalG = (g + bevelModifier).coerceIn(0, 255)
            val finalB = (b + bevelModifier).coerceIn(0, 255)
            return AndroidColor.argb(alpha, finalR, finalG, finalB)
        }

        when (tileY) {
            0 -> when (tileX) {
                // TILE (0,0): Grass Top (rich meadow green with wildflowers!)
                0 -> {
                    val isFlower = (px in 6..8 && py in 6..8) || (px in 22..24 && py in 20..22)
                    if (isFlower) {
                        return if (rand.nextBoolean()) col(255, 235, 90) else col(245, 110, 165)
                    }
                    val grassFibre = (Math.sin(px * 0.7) * Math.cos(py * 0.7) * 16).toInt()
                    return col(75 + noise / 2 + grassFibre, 150 + noise + grassFibre, 42)
                }
                // TILE (1,0): Grass Side (hanging pasture turf)
                1 -> {
                    val grassHeight = 9 + (Math.sin(px.toDouble() * 0.7) * 4).toInt() + rand.nextInt(3)
                    if (py < grassHeight) {
                        val grassFibre = (Math.sin(px.toDouble() * 0.8) * 12).toInt()
                        return col(65 + noise / 2 + grassFibre, 140 + noise + grassFibre, 38)
                    } else {
                        return col(115 + noise, 78 + noise, 52)
                    }
                }
                // TILE (2,0): Soil/Dirt
                2 -> return col(118 + noise, 78 + noise, 52)
                // TILE (3,0): Stone (rugged structure granite slate)
                3 -> {
                    val isQuartz = (px * 3 + py * 5) % 19 == 0
                    if (isQuartz) return col(205 + noise, 205 + noise, 215)
                    return col(128 + noise * 2, 128 + noise * 2, 128 + noise * 2)
                }
                // TILE (4,0): Oak Log Side
                4 -> {
                    val isGroove = px % 6 == 0 || py % 8 == 0
                    val add = if (isGroove) -28 else 10
                    return col(90 + noise + add, 62 + noise + add, 42)
                }
                // TILE (5,0): Oak Log Top (wooden grain rings)
                5 -> {
                    val r = Math.sqrt(((px - 15.5) * (px - 15.5) + (py - 15.5) * (py - 15.5)))
                    val isRing = r.toInt() % 6 == 0 || r.toInt() % 6 == 1
                    return if (isRing) col(170 + noise, 120 + noise, 70) else col(218 + noise, 185 + noise, 120)
                }
                // TILE (6,0): Oak Leaves
                6 -> {
                    val isHole = (px + py) % 9 == 0 && (px * py) % 3 == 0
                    if (isHole) return AndroidColor.TRANSPARENT
                    val leafy = (Math.sin(px * 1.4) * Math.cos(py * 1.4) * 22).toInt()
                    return col(38 + leafy, 110 + noise + leafy, 28)
                }
                // TILE (7,0): Coal Ore
                7 -> {
                    val isCoal = Math.abs(px - 16) + Math.abs(py - 16) < 9 || (px % 12 == 1 && py % 10 == 1)
                    if (isCoal) return col(32 + noise, 32 + noise, 32 + noise)
                    return col(128 + noise * 2, 128 + noise * 2, 128 + noise * 2)
                }
                // TILE (8,0): Netherrack
                8 -> return col(120 + noise * 2, 28 + noise, 28 + noise)
                // TILE (9,0): Soul Sand
                9 -> {
                    val isFace = (px + py) % 4 == 0 && px in 5..27
                    return if (isFace) col(62 + noise, 42 + noise, 32) else col(88 + noise, 58 + noise, 45)
                }
                // TILE (10,0): Glowstone
                10 -> {
                    val grid = px % 8 == 0 || py % 8 == 0
                    return if (grid) col(220 + noise, 155 + noise, 65) else col(255 + noise, 235 + noise, 145)
                }
                // TILE (11,0): End Stone
                11 -> return col(218 + noise, 222 + noise, 168 + noise)
                // TILE (12,0): Purpur Block
                12 -> {
                    val grid = px % 8 == 0 || py % 8 == 0
                    return if (grid) col(138 + noise, 95 + noise, 138) else col(185 + noise, 132 + noise, 185)
                }
                // TILE (13,0): Chorus Flower
                13 -> return col(155 + noise, 92 + noise, 155 + noise)
                // TILE (14,0): Sand
                14 -> {
                    val wave = (Math.sin(px * 0.45) * 4).toInt()
                    return col(224 + noise + wave, 204 + noise + wave, 142 + noise)
                }
                // TILE (15,0): Sandstone
                15 -> {
                    val strata = py % 8 in 0..1
                    return if (strata) col(198 + noise, 162 + noise, 110) else col(220 + noise, 192 + noise, 135)
                }
            }

            1 -> when (tileX) {
                // TILE (0,1): Iron Ore
                0 -> {
                    val isIron = (px + py) % 6 == 0 && px in 3..28 && py in 3..28
                    if (isIron) return col(222 + noise, 142 + noise, 102)
                    return col(128 + noise * 2, 128 + noise * 2, 128 + noise * 2)
                }
                // TILE (1,1): Diamond Ore
                1 -> {
                    val isDia = (px + py) % 5 == 1 && px in 4..27 && py in 4..27
                    if (isDia) return col(72 + noise, 238 + noise, 248)
                    return col(128 + noise * 2, 128 + noise * 2, 128 + noise * 2)
                }
                // TILE (2,1): Obsidian
                2 -> return col(24 + noise / 2, 14 + noise / 3, 38 + noise / 2)
                // TILE (3,1): Bedrock
                3 -> {
                    val dense = (px * py) % 3 == 0
                    return if (dense) col(38 + noise, 38 + noise, 38 + noise) else col(88 + noise, 88 + noise, 88 + noise)
                }
                // TILE (4,1): Crafting Table Top
                4 -> {
                    val border = px < 3 || px > 28 || py < 3 || py > 28
                    return if (border) col(110 + noise, 78 + noise, 48) else col(180 + noise, 138 + noise, 82)
                }
                // TILE (5,1): Crafting Table Side
                5 -> return col(138 + noise, 98 + noise, 60)
                // TILE (6,1): Furnace Front (Combustible bright flame)
                6 -> {
                    val isOpening = px in 6..25 && py in 9..22
                    if (isOpening) {
                        return if (py > 15) col(255, 130 + noise * 2, 15) else col(245, 228, 38)
                    }
                    return col(98 + noise, 98 + noise, 98 + noise)
                }
                // TILE (7,1): Furnace Side
                7 -> return col(112 + noise, 112 + noise, 112 + noise)
                // TILE (8,1): Netherite Block
                8 -> return col(54 + noise, 52 + noise, 58 + noise)
                // TILE (9,1): Crying Obsidian
                9 -> {
                    val tears = px % 8 == 2 && py > 7
                    return if (tears) col(0, 222 + noise, 255) else col(25 + noise / 2, 14 + noise / 3, 42 + noise / 2)
                }
                // TILE (10,1): Ancient Debris
                10 -> return col(108 + noise, 88 + noise, 82)
                // TILE (11,1): Deepslate
                11 -> {
                    val compress = py % 4 == 0
                    return if (compress) col(52 + noise, 55 + noise, 60 + noise) else col(78 + noise, 78 + noise, 82 + noise)
                }
                // TILE (12,1): Sculk Catalyst
                12 -> {
                    val pulse = (px * py) % 7 == 1
                    return if (pulse) col(12 + noise, 222 + noise, 188) else col(15 + noise / 3, 26 + noise / 2, 35 + noise)
                }
                // TILE (13,1): Sculk Sensor
                13 -> return col(12 + noise, 62 + noise, 82 + noise)
                // TILE (14,1): Amethyst Block
                14 -> return col(168 + noise, 70 + noise, 235)
                // TILE (15,1): Prismarine
                15 -> return col(55 + noise, 105 + noise, 100 + noise)
            }

            2 -> when (tileX) {
                // TILE (0,2): Wooden Chest Side
                0 -> {
                    val isClasp = px in 14..17 && py in 11..15
                    if (isClasp) return col(225, 198, 40)
                    val border = px < 3 || px > 28 || py < 3 || py > 28
                    return if (border) col(82 + noise, 55 + noise, 28) else col(128 + noise, 88 + noise, 48)
                }
                // TILE (1,2): NANO BANANA 2
                1 -> {
                    val dx = px - 15.5
                    val dy = py - 15.5
                    val isBanana = (dy > -6 && dy < 6 && dx > -6 && dx < 6 && (dx * dx + dy * dy < 49 && dx * dx + dy * dy > 9))
                    if (isBanana) return col(255, 218, 0)
                    val isGrid = px % 8 == 0 || py % 8 == 0
                    return if (isGrid) col(0, 218, 255) else col(14, 20, 32)
                }
                // TILE (2,2): CRAFTER
                2 -> {
                    val mouth = px in 10..21 && py in 10..15
                    if (mouth) return col(212, 82, 22)
                    val conduit = px % 8 == 1 || py % 8 == 1
                    return if (conduit) col(122, 48, 32) else col(108 + noise, 108 + noise, 108 + noise)
                }
                // TILE (3,2): TRIAL SPAWNER
                3 -> {
                    val border = px < 3 || px > 28 || py < 3 || py > 28 || px % 8 == 0 || py % 8 == 0
                    val core = px in 12..19 && py in 12..19
                    if (core) return col(255, 142, 32)
                    if (border) return col(68 + noise, 58, 82 + noise)
                    return AndroidColor.TRANSPARENT
                }
                // TILE (4,2): VAULT
                4 -> {
                    val keyhole = px in 14..17 && py in 12..18
                    val frame = px in 8..23 && (py == 8 || py == 23)
                    if (keyhole) return col(28, 18, 18)
                    if (frame) return col(222, 185, 88)
                    return col(98 + noise, 88 + noise, 82 + noise)
                }
                // TILE (5,2): HEAVY CORE
                5 -> {
                    val rivet = (px % 8 == 0 && py % 8 == 0)
                    return if (rivet) col(32, 36, 42) else col(70 + noise, 78 + noise, 88 + noise)
                }
                // TILE (6,2): COPPER BULB
                6 -> {
                    val bulb = px in 11..20 && py in 11..20
                    if (bulb) return col(255, 215, 115)
                    return col(208 + noise, 118 + noise, 78 + noise)
                }
                // TILE (7,2): COPPER GRATE
                7 -> {
                    val lattice = (px + py) % 8 == 0 || (px - py) % 8 == 0
                    return if (lattice) col(198 + noise, 112 + noise, 78 + noise) else AndroidColor.TRANSPARENT
                }
                // TILE (8,2): CHISELED COPPER
                8 -> {
                    val dist = Math.max(Math.abs(px - 15.5), Math.abs(py - 15.5)).toInt()
                    val etch = dist == 6 || dist == 12
                    return if (etch) col(138 + noise, 68, 48) else col(215 + noise, 122 + noise, 80 + noise)
                }
                // TILE (9,2): COPPER BLOCK
                9 -> return col(208 + noise, 112 + noise, 72 + noise)
                // TILE (10,2): COPPER ORE
                10 -> {
                    val vein = (px + py) % 8 == 0 && px in 4..27 && py in 4..27
                    if (vein) return col(82 + noise, 178 + noise, 142)
                    return col(128 + noise * 2, 128 + noise * 2, 128 + noise * 2)
                }
                // TILE (11,2): TUFF BRICKS
                11 -> {
                    val grid = px % 16 == 0 || py % 16 == 0
                    return if (grid) col(68 + noise, 70 + noise, 72 + noise) else col(105 + noise, 105 + noise, 105 + noise)
                }
                // TILE (12,2): CHISELED TUFF
                12 -> {
                    val d = Math.sqrt(((px - 15.5) * (px - 15.5) + (py - 15.5) * (py - 15.5)))
                    val ring = d > 10.0 && d < 13.0 || d > 3.0 && d < 6.0
                    return if (ring) col(62 + noise, 65 + noise, 68 + noise) else col(98 + noise, 98 + noise, 98 + noise)
                }
                // TILE (13,2): Cherry Log Side
                13 -> return col(65 + noise, 48 + noise, 48)
                // TILE (14,2): Cherry Leaves
                14 -> {
                    val isHole = (px * py) % 9 == 0
                    if (isHole) return AndroidColor.TRANSPARENT
                    return col(255, 178 + noise / 2, 198 + noise)
                }
                // TILE (15,2): Redstone Block
                15 -> {
                    val core = px % 6 == 1 || py % 6 == 1
                    return if (core) col(255 + noise, 28 + noise, 58) else col(195 + noise, 12, 22)
                }
            }

            3 -> when (tileX) {
                // TILE (0,3): Sea Lantern
                0 -> {
                    val border = px < 3 || px > 28 || py < 3 || py > 28
                    return if (border) col(128 + noise, 192 + noise, 201) else col(212 + noise, 248 + noise, 248)
                }
                // TILE (1,3): Beehive
                1 -> {
                    val slot = py % 6 == 0
                    return if (slot) col(222 + noise, 132 + noise, 38) else col(252 + noise, 172 + noise, 58)
                }
                // TILE (2,3): Sponge
                2 -> {
                    val pore = (px * py) % 5 == 1
                    return if (pore) col(168 + noise, 162 + noise, 52) else col(218 + noise, 212 + noise, 82)
                }
                // TILE (3,3): Magma Block
                3 -> {
                    val activeCracks = (px + py) % 7 == 2 || (px * py) % 11 == 3
                    return if (activeCracks) col(255, 122 + noise * 2, 12) else col(62 + noise, 28 + noise, 22)
                }
                // TILE (4,3): Lapis Block
                4 -> {
                    val goldGleck = (px + py * 3) % 13 == 1
                    if (goldGleck) return col(218, 178, 48)
                    return col(22 + noise, 42 + noise, 138 + noise)
                }
                // TILE (5,3): Emerald Block
                5 -> {
                    val bevel = px < 3 || px > 28 || py < 3 || py > 28 || px == py
                    return if (bevel) col(0, 152 + noise, 52) else col(28 + noise, 225 + noise, 102)
                }
                // TILE (6,3): Gold Block
                6 -> return col(255 + noise, 222 + noise, 52)
                // TILE (7,3): Iron Block
                7 -> {
                    val plateBorder = px < 2 || px > 29 || py < 2 || py > 29 || px == 2 || py == 2
                    return if (plateBorder) col(178 + noise, 178 + noise, 178 + noise) else col(228 + noise, 228 + noise, 228 + noise)
                }
                // TILE (8,3): Diamond Block
                8 -> return col(102 + noise, 238 + noise, 248)
                // TILE (9,3): Mud Block
                9 -> return col(70 + noise, 58 + noise, 50 + noise)
                // TILE (10,3): Mangrove Log Side
                10 -> return col(95 + noise, 48 + noise, 38)
                // TILE (11,3): Blackstone
                11 -> return col(40 + noise, 38 + noise, 45 + noise)
                // TILE (12,3): Honey Block
                12 -> return col(255, 178 + noise / 2, 18, 180)
                // TILE (13,3): Honeycomb Block
                13 -> {
                    val hex = (px + py) % 6 == 0
                    return if (hex) col(232 + noise, 142 + noise, 22) else col(255 + noise, 198 + noise, 35)
                }
                // TILE (14,3): Froglight
                14 -> return col(255 + noise, 250 + noise, 212 + noise)
                // TILE (15,3): Barrel Side
                15 -> {
                    val hoop = py == 6 || py == 25
                    return if (hoop) col(78 + noise, 78 + noise, 78) else col(138 + noise, 98 + noise, 58)
                }
            }
        }

        // Catch-all fallback grey
        val fallbackGrey = (120 + noise).coerceIn(0, 255)
        return AndroidColor.rgb(fallbackGrey, fallbackGrey, fallbackGrey)
    }
}
