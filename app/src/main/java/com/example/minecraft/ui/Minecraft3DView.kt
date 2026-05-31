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

    // Vertices coordinates for a single face quad centered at 0,0,0
    private val faceVertices = floatArrayOf(
        -0.5f, -0.5f, 0f,
         0.5f, -0.5f, 0f,
         0.5f,  0.5f, 0f,
        -0.5f,  0.5f, 0f
    )

    private val vertexBuffer: FloatBuffer
    private val texCoordinateBuffer: FloatBuffer

    init {
        // Init direct buffers
        vertexBuffer = ByteBuffer.allocateDirect(faceVertices.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer()
        }.apply {
            put(faceVertices)
            position(0)
        }

        // Texture coords placeholder, updated on the fly for each specific tile
        texCoordinateBuffer = ByteBuffer.allocateDirect(8 * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer()
        }
    }

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

        // Render standard chunks width radius (e.g. 15 blocks horizontal, 11 blocks vertical)
        val renderRadX = 14
        val renderRadY = 11

        val startX = (centerGridX - renderRadX).coerceAtLeast(0)
        val endX = (centerGridX + renderRadX).coerceAtMost(worldState.width - 1)
        val startY = (centerGridY - renderRadY).coerceAtLeast(0)
        val endY = (centerGridY + renderRadY).coerceAtMost(worldState.height - 1)

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY)

        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer)

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
                    for (bz in -3..3) {
                        // Apply an organic, satisfying slope curvature downwards away from the center ridge at z=0
                        val valeyCurvature = -0.16f * (bz * bz)
                        
                        // Sandwich check for depth slices
                        val drawBack = (bz == -3)
                        val drawFront = (bz == 3)
                        
                        val aboveType = GameRegistry.blocks[aboveId]
                        val belowType = GameRegistry.blocks[belowId]
                        val leftType = GameRegistry.blocks[leftId]
                        val rightType = GameRegistry.blocks[rightId]

                        val drawTop = !isAboveSolid || (aboveType != null && !aboveType.isSolid)
                        val drawBottom = !isBelowSolid || (belowType != null && !belowType.isSolid)
                        val drawLeft = !isLeftSolid || (leftType != null && !leftType.isSolid)
                        val drawRight = !isRightSolid || (rightType != null && !rightType.isSolid)

                        draw3DBlock(
                            gl, bx.toFloat(), by.toFloat() + valeyCurvature, bz.toFloat(), bId,
                            drawFront = drawFront, drawBack = drawBack,
                            drawLeft = drawLeft, drawRight = drawRight,
                            drawTop = drawTop, drawBottom = drawBottom
                        )
                    }
                } else if (bId == "oak_log" || bId == "mangrove_log" || bId == "cherry_log") {
                    // Thick rounded cylindrical tree trunk (thickness 3)
                    for (bz in -1..1) {
                        val drawBack = (bz == -1)
                        val drawFront = (bz == 1)
                        val drawTop = !isAboveSolid
                        val drawBottom = !isBelowSolid
                        val drawLeft = !isLeftSolid
                        val drawRight = !isRightSolid

                        draw3DBlock(
                            gl, bx.toFloat(), by.toFloat(), bz.toFloat(), bId,
                            drawFront = drawFront, drawBack = drawBack,
                            drawLeft = drawLeft, drawRight = drawRight,
                            drawTop = drawTop, drawBottom = drawBottom
                        )
                    }
                } else if (bId == "oak_leaves" || bId == "cherry_leaves") {
                    // Volumetric leaf canopy (thickness 5)
                    for (bz in -2..2) {
                        val slope = if (Math.abs(bz) == 2) -0.15f else 0f
                        val drawBack = (bz == -2)
                        val drawFront = (bz == 2)
                        val drawTop = !isAboveSolid
                        val drawBottom = !isBelowSolid
                        val drawLeft = !isLeftSolid
                        val drawRight = !isRightSolid

                        draw3DBlock(
                            gl, bx.toFloat(), by.toFloat() + slope, bz.toFloat(), bId,
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

                        draw3DBlock(
                            gl, bx.toFloat(), by.toFloat(), bz.toFloat(), bId,
                            drawFront = drawFront, drawBack = drawBack,
                            drawLeft = drawLeft, drawRight = drawRight,
                            drawTop = drawTop, drawBottom = drawBottom
                        )
                    }
                } else {
                    // Interactives & decorative blocks (1-slice thickness at bz=0)
                    draw3DBlock(
                        gl, bx.toFloat(), by.toFloat(), 0f, bId,
                        drawFront = true, drawBack = true,
                        drawLeft = !isLeftSolid, drawRight = !isRightSolid,
                        drawTop = !isAboveSolid, drawBottom = !isBelowSolid
                    )
                }
            }
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

    private fun draw3DBlock(
        gl: GL10, x: Float, y: Float, z: Float, blockId: String,
        drawFront: Boolean = true, drawBack: Boolean = true,
        drawLeft: Boolean = true, drawRight: Boolean = true,
        drawTop: Boolean = true, drawBottom: Boolean = true
    ) {
        val bType = GameRegistry.blocks[blockId] ?: return

        // Top, Bottom, Left, Right, Front faces individually texturized
        // FRONT FACE
        if (drawFront) {
            pushTextureCoords(gl, getBlockTile(blockId, "front"))
            gl.glPushMatrix()
            gl.glTranslatef(x, y, z + 0.5f)
            gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4)
            gl.glPopMatrix()
        }

        // BACK FACE
        if (drawBack) {
            pushTextureCoords(gl, getBlockTile(blockId, "back"))
            gl.glPushMatrix()
            gl.glTranslatef(x, y, z - 0.5f)
            gl.glRotatef(180f, 0f, 1f, 0f)
            gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4)
            gl.glPopMatrix()
        }

        // LEFT FACE
        if (drawLeft) {
            pushTextureCoords(gl, getBlockTile(blockId, "left"))
            gl.glPushMatrix()
            gl.glTranslatef(x - 0.5f, y, z)
            gl.glRotatef(-90f, 0f, 1f, 0f)
            gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4)
            gl.glPopMatrix()
        }

        // RIGHT FACE
        if (drawRight) {
            pushTextureCoords(gl, getBlockTile(blockId, "right"))
            gl.glPushMatrix()
            gl.glTranslatef(x + 0.5f, y, z)
            gl.glRotatef(90f, 0f, 1f, 0f)
            gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4)
            gl.glPopMatrix()
        }

        // TOP FACE
        if (drawTop) {
            pushTextureCoords(gl, getBlockTile(blockId, "top"))
            gl.glPushMatrix()
            gl.glTranslatef(x, y + 0.5f, z)
            gl.glRotatef(-90f, 1f, 0f, 0f)
            gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4)
            gl.glPopMatrix()
        }

        // BOTTOM FACE
        if (drawBottom) {
            pushTextureCoords(gl, getBlockTile(blockId, "bottom"))
            gl.glPushMatrix()
            gl.glTranslatef(x, y - 0.5f, z)
            gl.glRotatef(90f, 1f, 0f, 0f)
            gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4)
            gl.glPopMatrix()
        }
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
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // 1. Fill entire base
        paint.color = AndroidColor.TRANSPARENT
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        val tiles = 16
        val tilePx = 16
        val rand = Random(42) // Stabilize noise

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
        // Match tile index to generate visual voxel graphics procedurally
        val noise = (rand.nextFloat() * 16 - 8).toInt()

        return when {
            // TILE (0,0): Grass Top (Rich leafy green)
            tileX == 0 && tileY == 0 -> {
                val baseG = 130 + noise
                val baseR = 70 + noise / 2
                val baseB = 40 + noise / 3
                AndroidColor.rgb(baseR.coerceIn(0, 255), baseG.coerceIn(0, 255), baseB.coerceIn(0, 255))
            }

            // TILE (1,0): Grass Side (Turf transition)
            tileX == 1 && tileY == 0 -> {
                if (py < 4 + rand.nextInt(3)) {
                    // Green turf fringe
                    val baseG = 120 + noise
                    val baseR = 65 + noise / 2
                    AndroidColor.rgb(baseR.coerceIn(0, 255), baseG.coerceIn(0, 255), 45)
                } else {
                    // Earth Brown base
                    val baseR = 110 + noise
                    val baseG = 75 + noise
                    AndroidColor.rgb(baseR.coerceIn(0, 255), baseG.coerceIn(0, 255), 50)
                }
            }

            // TILE (2,0): Dirt
            tileX == 2 && tileY == 0 -> {
                val baseR = 120 + noise
                val baseG = 80 + noise
                val baseB = 52 + noise
                AndroidColor.rgb(baseR.coerceIn(0, 255), baseG.coerceIn(0, 255), baseB.coerceIn(0, 255))
            }

            // TILE (3,0): Stone (Cold granite slate grey)
            tileX == 3 && tileY == 0 -> {
                val grey = 135 + noise * 2
                AndroidColor.rgb(grey.coerceIn(0, 255), grey.coerceIn(0, 255), grey.coerceIn(0, 255))
            }

            // TILE (4,0): Oak Log Side (Tree bark fibers)
            tileX == 4 && tileY == 0 -> {
                val isBarkStripe = px % 4 == 0 || py % 6 == 0
                val colorAdd = if (isBarkStripe) -20 else 10
                val baseR = 90 + noise + colorAdd
                val baseG = 65 + noise + colorAdd
                AndroidColor.rgb(baseR.coerceIn(0, 255), baseG.coerceIn(0, 255), 40)
            }

            // TILE (5,0): Oak Log Top (Circular rings)
            tileX == 5 && tileY == 0 -> {
                val dx = Math.abs(px - 7.5)
                val dy = Math.abs(py - 7.5)
                val dist = Math.sqrt(dx * dx + dy * dy)
                val isRing = dist.toInt() % 3 == 0
                if (isRing) {
                    AndroidColor.rgb(180 + noise, 130 + noise, 80)
                } else {
                    AndroidColor.rgb(225 + noise, 185 + noise, 120)
                }
            }

            // TILE (6,0): Oak Leaves
            tileX == 6 && tileY == 0 -> {
                // High frequency leafy clusters with transparency
                val isHole = (px + py) % 7 == 0 && (px * py) % 3 == 1
                if (isHole) {
                    AndroidColor.TRANSPARENT
                } else {
                    val green = 90 + noise * 4
                    AndroidColor.rgb(40, green.coerceIn(0, 255), 25)
                }
            }

            // TILE (7,0): Coal Ore
            tileX == 7 && tileY == 0 -> {
                val isCoalVein = (px in 3..7 && py in 3..6) || (px in 10..13 && py in 8..12)
                if (isCoalVein) {
                    AndroidColor.rgb(38, 38, 38)
                } else {
                    val grey = 135 + noise * 2
                    AndroidColor.rgb(grey.coerceIn(0, 255), grey.coerceIn(0, 255), grey.coerceIn(0, 255))
                }
            }

            // TILE (0,1): Iron Ore
            tileX == 0 && tileY == 1 -> {
                val isIronVein = (px + py) % 5 == 1 && px in 2..13 && py in 2..13
                if (isIronVein) {
                    AndroidColor.rgb(212, 142, 107) // Classic 1.21.1 Iron Peach-Rust color
                } else {
                    val grey = 135 + noise * 2
                    AndroidColor.rgb(grey.coerceIn(0, 255), grey.coerceIn(0, 255), grey.coerceIn(0, 255))
                }
            }

            // TILE (1,1): Diamond Ore (Luminous cyber blue)
            tileX == 1 && tileY == 1 -> {
                val isDiaVein = (px + py) % 4 == 0 && px in 3..12 && py in 3..12
                if (isDiaVein) {
                    AndroidColor.rgb(78, 222, 236)
                } else {
                    val grey = 135 + noise * 2
                    AndroidColor.rgb(grey.coerceIn(0, 255), grey.coerceIn(0, 255), grey.coerceIn(0, 255))
                }
            }

            // TILE (2,1): Obsidian (Deep dark runic purple)
            tileX == 2 && tileY == 1 -> {
                val purple = 24 + noise / 2
                val blueTone = 38 + noise
                AndroidColor.rgb(purple.coerceIn(10, 80), 12, blueTone.coerceIn(15, 110))
            }

            // TILE (3,1): Bedrock (Rugged matrix)
            tileX == 3 && tileY == 1 -> {
                val density = if ((px * py) % 2 == 0) 35 else 90
                val col = density + noise
                AndroidColor.rgb(col.coerceIn(0, 255), col.coerceIn(0, 255), col.coerceIn(0, 255))
            }

            // TILE (4,1): Crafting Table Top
            tileX == 4 && tileY == 1 -> {
                val isBorder = px < 2 || px > 13 || py < 2 || py > 13
                if (isBorder) {
                    AndroidColor.rgb(100 + noise, 75 + noise, 45)
                } else {
                    AndroidColor.rgb(160 + noise, 120 + noise, 75)
                }
            }

            // TILE (5,1): Crafting Table Side
            tileX == 5 && tileY == 1 -> {
                val baseR = 130 + noise
                val baseG = 95 + noise
                AndroidColor.rgb(baseR.coerceIn(0, 255), baseG.coerceIn(0, 255), 58)
            }

            // TILE (6,1): Furnace Front (Combustive blazing chamber)
            tileX == 6 && tileY == 1 -> {
                val isOpening = px in 3..12 && py in 4..11
                if (isOpening) {
                    // Dynamic fire gradient
                    if (py > 7) {
                        AndroidColor.rgb(255, 120 + noise * 2, 0) // Fire orange
                    } else {
                        AndroidColor.rgb(240, 230, 45) // Yellow
                    }
                } else {
                    val grey = 100 + noise
                    AndroidColor.rgb(grey.coerceIn(0, 255), grey.coerceIn(0, 255), grey.coerceIn(0, 255))
                }
            }

            // TILE (7,1): Furnace Side
            tileX == 7 && tileY == 1 -> {
                val grey = 110 + noise
                AndroidColor.rgb(grey.coerceIn(0, 255), grey.coerceIn(0, 255), grey.coerceIn(0, 255))
            }

            // TILE (0,2): Chest Side and details
            tileX == 0 && tileY == 2 -> {
                val isClasp = px in 7..8 && py in 5..7
                if (isClasp) {
                    AndroidColor.rgb(215, 190, 40) // Golden lock
                } else {
                    val baseR = 100 + noise
                    val baseG = 65 + noise
                    AndroidColor.rgb(baseR.coerceIn(0, 255), baseG.coerceIn(0, 255), 35)
                }
            }

            // TILE (1,2): NANO BANANA 2 (Gold-yellow pixel-perfect banana curve on cyber-blue mesh)
            tileX == 1 && tileY == 2 -> {
                // Determine banana curvature
                // A banana is roughly outline-shaped like a crescent shape: py in 4..12, px curved
                val isBanana = (py == 4 && px in 8..11) ||
                              (py == 5 && px in 6..10) ||
                              (py == 6 && px in 5..8) ||
                              (py == 7 && px in 4..7) ||
                              (py == 8 && px in 4..6) ||
                              (py == 9 && px in 3..6) ||
                              (py == 10 && px in 4..7) ||
                              (py == 11 && px in 5..9) ||
                              (py == 12 && px in 7..11)

                if (isBanana) {
                    // Golden cyber yellow
                    AndroidColor.rgb(255, 215, 0)
                } else {
                    // Cyber cyan network grid background
                    val isGrid = px % 4 == 0 || py % 4 == 0
                    if (isGrid) {
                        AndroidColor.rgb(0, 215, 255) // Cyber neon cyan
                    } else {
                        AndroidColor.rgb(10, 16, 28) // Deep digital navy
                    }
                }
            }

            // TILE (2,2): CRAFTER
            tileX == 2 && tileY == 2 -> {
                val isRedmouth = px in 5..10 && py in 5..8
                val isRedstoneRed = px % 5 == 1 || py % 5 == 1
                if (isRedmouth) {
                    AndroidColor.rgb(217, 74, 26) // Glowing redstone orange/red
                } else if (isRedstoneRed) {
                    AndroidColor.rgb(100, 40, 30) // Redstone conduit
                } else {
                    val base = 110 + noise
                    AndroidColor.rgb(base, base, base) // Raw smooth slab grey
                }
            }

            // TILE (3,2): TRIAL SPAWNER (Dark runic cage with blazing core!)
            tileX == 3 && tileY == 2 -> {
                val isCageBorder = px < 2 || px > 13 || py < 2 || py > 13 || px % 4 == 0 || py % 4 == 0
                val isBlazingCore = px in 6..9 && py in 6..9
                if (isBlazingCore) {
                    AndroidColor.rgb(255, 153, 51) // Blazing bright heat orange
                } else if (isCageBorder) {
                    AndroidColor.rgb(45 + noise/2, 35, 60 + noise/2) // Dark runic purple tuff cage
                } else {
                    AndroidColor.TRANSPARENT // Hollow mesh transparency!
                }
            }

            // TILE (4,2): VAULT (Brown armored containment box with golden lock and keyhole)
            tileX == 4 && tileY == 2 -> {
                val isKeyhole = px in 7..8 && py in 6..9
                val isGoldBorder = px in 4..11 && (py == 4 || py == 11)
                if (isKeyhole) {
                    AndroidColor.rgb(30, 20, 20) // Deep dark keyhole slit
                } else if (isGoldBorder) {
                    AndroidColor.rgb(218, 181, 83) // Golden frame rim
                } else {
                    val base = 90 + noise
                    val r = (base - 10).coerceIn(0, 255)
                    val g = (base - 15).coerceIn(0, 255)
                    AndroidColor.rgb(base, r, g) // Deep rugged clay brown vault alloy
                }
            }

            // TILE (5,2): HEAVY CORE (Industrial block)
            tileX == 5 && tileY == 2 -> {
                val isRivet = (px % 4 == 0 && py % 4 == 0)
                if (isRivet) {
                    AndroidColor.rgb(27, 30, 34) // Dark steel rivet heads
                } else {
                    val base = 65 + noise
                    val b = (base + 10).coerceIn(0, 255)
                    val g = (base + 5).coerceIn(0, 255)
                    AndroidColor.rgb(base, g, b) // Heavy blue-steel plates
                }
            }

            // TILE (6,2): COPPER BULB (Glowing copper orange light)
            tileX == 6 && tileY == 2 -> {
                val isBulb = px in 5..10 && py in 5..10
                if (isBulb) {
                    AndroidColor.rgb(255, 200, 110) // Super heated brilliant yellow bulb
                } else {
                    val baseR = 217 + noise
                    val baseG = 120 + noise / 2
                    AndroidColor.rgb(baseR.coerceIn(0, 255), baseG.coerceIn(0, 255), 82) // Bright trial copper
                }
            }

            // TILE (7,2): COPPER GRATE (Diagonal copper lattices with transparent holes)
            tileX == 7 && tileY == 2 -> {
                val isLattice = (px + py) % 4 == 0 || (px - py) % 4 == 0
                if (isLattice) {
                    val baseR = 204 + noise
                    val baseG = 112 + noise / 2
                    AndroidColor.rgb(baseR.coerceIn(0, 255), baseG.coerceIn(0, 255), 75)
                } else {
                    AndroidColor.TRANSPARENT // See through grate!
                }
            }

            // TILE (8,2): CHISELED COPPER (Concentric square patterns)
            tileX == 8 && tileY == 2 -> {
                val dist = Math.max(Math.abs(px - 7.5), Math.abs(py - 7.5)).toInt()
                val isEtch = dist == 3 || dist == 6
                if (isEtch) {
                    AndroidColor.rgb(130 + noise, 60, 40) // Dark copper oxidation grooves
                } else {
                    val baseR = 230 + noise
                    val baseG = 125 + noise / 2
                    AndroidColor.rgb(baseR.coerceIn(0, 255), baseG.coerceIn(0, 255), 83)
                }
            }

            // TILE (9,2): COPPER BLOCK (Solid clean copper bricks)
            tileX == 9 && tileY == 2 -> {
                val baseR = 211 + noise
                val baseG = 106 + noise / 2
                AndroidColor.rgb(baseR.coerceIn(0, 255), baseG.coerceIn(0, 255), 68)
            }

            // TILE (10,2): COPPER ORE (Oxidized turquoise-green veins)
            tileX == 10 && tileY == 2 -> {
                val isCopperVein = (px + py) % 5 == 0 && px in 2..13 && py in 2..13
                if (isCopperVein) {
                    AndroidColor.rgb((84 + noise).coerceIn(0, 255), (178 + noise).coerceIn(0, 255), (141 + noise).coerceIn(0, 255)) // Vibrant oxidized copper turquoise!
                } else {
                    val grey = 135 + noise * 2
                    AndroidColor.rgb(grey.coerceIn(0, 255), grey.coerceIn(0, 255), grey.coerceIn(0, 255))
                }
            }

            // TILE (11,2): TUFF BRICKS (Dark grey structural bricks)
            tileX == 11 && tileY == 2 -> {
                val isGroove = px % 8 == 0 || py % 8 == 0
                if (isGroove) {
                    AndroidColor.rgb((67 + noise).coerceIn(0, 255), (70 + noise).coerceIn(0, 255), (71 + noise).coerceIn(0, 255))
                } else {
                    val grey = 103 + noise
                    AndroidColor.rgb(grey.coerceIn(0, 255), grey.coerceIn(0, 255), grey.coerceIn(0, 255))
                }
            }

            // TILE (12,2): CHISELED TUFF (Ornate concentric circles in volcanic stone)
            tileX == 12 && tileY == 2 -> {
                val dx = Math.abs(px - 7.5)
                val dy = Math.abs(py - 7.5)
                val dist = Math.sqrt(dx * dx + dy * dy)
                val isCircle = dist > 4.5 && dist < 6.5 || dist > 1.5 && dist < 3.0
                if (isCircle) {
                    AndroidColor.rgb((56 + noise).coerceIn(0, 255), (59 + noise).coerceIn(0, 255), (60 + noise).coerceIn(0, 255))
                } else {
                    val grey = 95 + noise
                    AndroidColor.rgb(grey.coerceIn(0, 255), grey.coerceIn(0, 255), grey.coerceIn(0, 255))
                }
            }

            // Default Solid Voxel noise
            else -> {
                val base = 120 + noise
                AndroidColor.rgb(base.coerceIn(0,255), base.coerceIn(0,255), base.coerceIn(0,255))
            }
        }
    }
}
