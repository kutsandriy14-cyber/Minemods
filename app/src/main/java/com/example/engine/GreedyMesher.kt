package com.example.engine

import com.example.game.BlockRegistry
import com.example.world.World

object GreedyMesher {
    
    fun generateMesh(chunk: Chunk, world: World): FloatArray {
        val vertices = mutableListOf<Float>()
        
        val cx = chunk.chunkX * 16
        val cz = chunk.chunkZ * 16
        
        // 0: +y, 1: -y, 2: -x, 3: +x, 4: -z, 5: +z
        for (dir in 0..5) {
            val mask = IntArray(16 * 16 * 256)
            
            var u = 0; var v = 0; var d = 0
            when (dir) {
                0, 1 -> { u = 0; v = 2; d = 1 } // y-axis: u=x, v=z, d=y
                2, 3 -> { u = 2; v = 1; d = 0 } // x-axis: u=z, v=y, d=x
                4, 5 -> { u = 0; v = 1; d = 2 } // z-axis: u=x, v=y, d=z
            }
            
            val dimU = if (u == 0 || u == 2) 16 else 256
            val dimV = if (v == 1) 256 else 16
            val dimD = if (d == 1) 256 else 16
            
            val q = IntArray(3)
            val dirOffset = if (dir % 2 == 0) 1 else -1
            
            for (w in 0 until dimD) {
                // Compute mask for this slice
                var n = 0
                for (j in 0 until dimV) {
                    for (i in 0 until dimU) {
                        q[u] = i; q[v] = j; q[d] = w
                        val type = chunk.getBlock(q[0], q[1], q[2])
                        
                        val neighborType = if (w + dirOffset < 0 || w + dirOffset >= dimD) {
                            world.getBlock(cx + q[0] + if(d==0) dirOffset else 0,
                                           q[1] + if(d==1) dirOffset else 0,
                                           cz + q[2] + if(d==2) dirOffset else 0)
                        } else {
                            chunk.getBlock(q[0] + if(d==0) dirOffset else 0,
                                           q[1] + if(d==1) dirOffset else 0,
                                           q[2] + if(d==2) dirOffset else 0)
                        }
                        
                        if (type != BlockRegistry.AIR && neighborType == BlockRegistry.AIR) {
                            mask[n] = type.toInt()
                        } else {
                            mask[n] = 0
                        }
                        n++
                    }
                }
                
                // Greedy meshing
                n = 0
                for (j in 0 until dimV) {
                    var i = 0
                    while (i < dimU) {
                        val c = mask[n]
                        if (c != 0) {
                            // Compute width
                            var width = 1
                            while (i + width < dimU && mask[n + width] == c) {
                                width++
                            }
                            
                            // Compute height
                            var height = 1
                            var done = false
                            while (j + height < dimV) {
                                for (k in 0 until width) {
                                    if (mask[n + k + height * dimU] != c) {
                                        done = true
                                        break
                                    }
                                }
                                if (done) break
                                height++
                            }
                            
                            // Add quad
                            q[u] = i; q[v] = j; q[d] = w
                            val pos = floatArrayOf(q[0].toFloat(), q[1].toFloat(), q[2].toFloat())
                            
                            val du = FloatArray(3)
                            val dv = FloatArray(3)
                            du[u] = width.toFloat()
                            dv[v] = height.toFloat()
                            
                            val light = when (dir) {
                                0 -> 1.0f // top
                                1 -> 0.5f // bottom
                                2, 3 -> 0.8f // x sides
                                else -> 0.6f // z sides
                            }
                            
                            val uv = BlockRegistry.getTextureUV(c.toByte(), dir)
                            val texU = uv.first.toFloat() * (1f / 16f)
                            val texV = uv.second.toFloat() * (1f / 16f)
                            val duv = 1f / 16f
                            
                            // Depending on direction, adjust vertex ordering to have correct winding (CCW)
                            val p1 = floatArrayOf(pos[0] + if(dir%2!=0)0f else if(d==0)1f else 0f, 
                                                  pos[1] + if(dir%2!=0)0f else if(d==1)1f else 0f, 
                                                  pos[2] + if(dir%2!=0)0f else if(d==2)1f else 0f)
                            val p2 = floatArrayOf(p1[0] + du[0], p1[1] + du[1], p1[2] + du[2])
                            val p3 = floatArrayOf(p1[0] + du[0] + dv[0], p1[1] + du[1] + dv[1], p1[2] + du[2] + dv[2])
                            val p4 = floatArrayOf(p1[0] + dv[0], p1[1] + dv[1], p1[2] + dv[2])
                            
                            val wp1 = floatArrayOf(cx + p1[0], p1[1], cz + p1[2])
                            val wp2 = floatArrayOf(cx + p2[0], p2[1], cz + p2[2])
                            val wp3 = floatArrayOf(cx + p3[0], p3[1], cz + p3[2])
                            val wp4 = floatArrayOf(cx + p4[0], p4[1], cz + p4[2])
                            
                            // Texture coordinates mapping depends on orientation to allow wrapping
                            // For a simple atlas, we might repeat or just map to 1 tile.
                            // To tile correctly across combined blocks, we need to adjust uv:
                            val tw = width.toFloat() * duv
                            val th = height.toFloat() * duv
                            
                            if (dir % 2 != 0 || dir == 0) { // CCW adjustment based on face
                                addVertex(vertices, wp1, 0f, th, light, texU, texV)
                                addVertex(vertices, wp2, tw, th, light, texU, texV)
                                addVertex(vertices, wp3, tw, 0f, light, texU, texV)
                                
                                addVertex(vertices, wp1, 0f, th, light, texU, texV)
                                addVertex(vertices, wp3, tw, 0f, light, texU, texV)
                                addVertex(vertices, wp4, 0f, 0f, light, texU, texV)
                            } else {
                                addVertex(vertices, wp1, 0f, th, light, texU, texV)
                                addVertex(vertices, wp3, tw, 0f, light, texU, texV)
                                addVertex(vertices, wp2, tw, th, light, texU, texV)
                                
                                addVertex(vertices, wp1, 0f, th, light, texU, texV)
                                addVertex(vertices, wp4, 0f, 0f, light, texU, texV)
                                addVertex(vertices, wp3, tw, 0f, light, texU, texV)
                            }
                            
                            // Clear mask
                            for (l in 0 until height) {
                                for (k in 0 until width) {
                                    mask[n + k + l * dimU] = 0
                                }
                            }
                            
                            i += width
                            n += width
                        } else {
                            i++
                            n++
                        }
                    }
                }
            }
        }
        
        return vertices.toFloatArray()
    }
    
    private fun addVertex(vertices: MutableList<Float>, pos: FloatArray, u: Float, v: Float, light: Float, baseU: Float, baseV: Float) {
        vertices.add(pos[0])
        vertices.add(pos[1])
        vertices.add(pos[2])
        vertices.add(u)
        vertices.add(v)
        vertices.add(light)
        vertices.add(baseU)
        vertices.add(baseV)
    }
}
