package com.example.engine

import android.opengl.GLES30
import android.util.Log

object ShaderManager {
    private const val TAG = "ShaderManager"

    private const val VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec2 a_TexCoord;
layout(location = 2) in float a_Light;
layout(location = 3) in vec2 a_BaseUV;

uniform mat4 u_MVPMatrix;

out vec2 v_TexCoord;
out float v_Light;
out vec2 v_BaseUV;

void main() {
    gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);
    v_TexCoord = a_TexCoord;
    v_Light = a_Light;
    v_BaseUV = a_BaseUV;
}
"""

    private const val FRAGMENT_SHADER = """#version 300 es
precision mediump float;

in vec2 v_TexCoord;
in float v_Light;
in vec2 v_BaseUV;

uniform sampler2D u_Texture;

out vec4 FragColor;

void main() {
    vec2 localUV = fract(v_TexCoord * 16.0) / 16.0;
    vec2 finalUV = v_BaseUV + localUV;
    vec4 texColor = texture(u_Texture, finalUV);
    if(texColor.a < 0.1) discard;
    FragColor = vec4(texColor.rgb * v_Light, texColor.a);
}
"""

    var programId: Int = 0
        private set
    
    var mvpMatrixHandle: Int = 0
        private set
        
    var textureHandle: Int = 0
        private set

    fun init() {
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)

        programId = GLES30.glCreateProgram().also {
            GLES30.glAttachShader(it, vertexShader)
            GLES30.glAttachShader(it, fragmentShader)
            GLES30.glLinkProgram(it)
            
            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(it, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                Log.e(TAG, "Error linking program: \${GLES30.glGetProgramInfoLog(it)}")
                GLES30.glDeleteProgram(it)
            }
        }
        
        mvpMatrixHandle = GLES30.glGetUniformLocation(programId, "u_MVPMatrix")
        textureHandle = GLES30.glGetUniformLocation(programId, "u_Texture")
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, shaderCode)
            GLES30.glCompileShader(shader)
            
            val compiled = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader \${type}:")
                Log.e(TAG, GLES30.glGetShaderInfoLog(shader))
                GLES30.glDeleteShader(shader)
            }
        }
    }
}
