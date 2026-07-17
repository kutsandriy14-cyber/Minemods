package com.example.world

import kotlin.math.floor

object SimplexNoise {
    private val grad3 = arrayOf(
        intArrayOf(1,1,0), intArrayOf(-1,1,0), intArrayOf(1,-1,0), intArrayOf(-1,-1,0),
        intArrayOf(1,0,1), intArrayOf(-1,0,1), intArrayOf(1,0,-1), intArrayOf(-1,0,-1),
        intArrayOf(0,1,1), intArrayOf(0,-1,1), intArrayOf(0,1,-1), intArrayOf(0,-1,-1)
    )

    private val p = IntArray(256)
    private val perm = IntArray(512)
    private val permMod12 = IntArray(512)

    init {
        // Initialize with default permutation
        val source = intArrayOf(
            151,160,137,91,90,15,
            131,13,201,95,96,53,194,233, 7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
            190, 6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,
            88,237,149,56,87,174,20,125,136,171,168, 68,175,74,165,71,134,139,48,27,166,
            77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
            102,143,54, 65,25,63,161, 1,216,80,73,209,76,132,187,208, 89,18,169,200,196,
            135,130,116,188,159,86,164,100,109,198,173,186, 3,64,52,217,226,250,124,123,
            5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,150,111,253,149,72,130,
            24,184,79,139,48,27,166,77,146,158,231,83,111,229,122,60,211,133,230,220,105,
            92,41,55,46,245,40,244,102,143,54,65,25,63,161,1,216,80,73,209,76,132,187,
            208,89,18,169,200,196,135,130,116,188,159,86,164,100,109,198,173,186,3,64,
            52,217,226,250,124,123,5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,
            150,110,84,189,241,210,124,123,5,202,38,147,118,126,255,82,85,212,207,206,59
        )
        setSeed(12345L, source)
    }

    fun setSeed(seed: Long, source: IntArray? = null) {
        val base = source ?: IntArray(256) { it }
        val random = java.util.Random(seed)
        if (source == null) {
            // Shuffle
            for (i in 255 downTo 1) {
                val j = random.nextInt(i + 1)
                val temp = base[i]
                base[i] = base[j]
                base[j] = temp
            }
        }
        for (i in 0..255) {
            p[i] = base[i]
            perm[i] = p[i]
            perm[256 + i] = p[i]
            permMod12[i] = (p[i] % 12)
            permMod12[256 + i] = (p[i] % 12)
        }
    }

    private const val F2 = 0.5f * (3.16227766f - 1.0f) // (sqrt(3.0)-1.0)/2.0
    private const val G2 = (3.0f - 3.16227766f) / 6.0f // (3.0-sqrt(3.0))/6.0
    private const val F3 = 1.0f / 3.0f
    private const val G3 = 1.0f / 6.0f

    fun noise2D(xin: Float, yin: Float): Float {
        var n0 = 0f; var n1 = 0f; var n2 = 0f
        val s = (xin + yin) * 0.366025403f // F2 constant
        val i = floor((xin + s).toDouble()).toInt()
        val j = floor((yin + s).toDouble()).toInt()
        val t = (i + j) * 0.211324865f // G2 constant
        val X0 = i - t
        val Y0 = j - t
        val x0 = xin - X0
        val y0 = yin - Y0

        var i1 = 0; var j1 = 0
        if (x0 > y0) { i1 = 1; j1 = 0 } else { i1 = 0; j1 = 1 }

        val x1 = x0 - i1 + 0.211324865f
        val y1 = y0 - j1 + 0.211324865f
        val x2 = x0 - 1.0f + 2.0f * 0.211324865f
        val y2 = y0 - 1.0f + 2.0f * 0.211324865f

        val ii = i and 255
        val jj = j and 255
        val gi0 = permMod12[ii + perm[jj]]
        val gi1 = permMod12[ii + i1 + perm[jj + j1]]
        val gi2 = permMod12[ii + 1 + perm[jj + 1]]

        var t0 = 0.5f - x0 * x0 - y0 * y0
        if (t0 >= 0) {
            t0 *= t0
            n0 = t0 * t0 * (grad3[gi0][0] * x0 + grad3[gi0][1] * y0)
        }
        var t1 = 0.5f - x1 * x1 - y1 * y1
        if (t1 >= 0) {
            t1 *= t1
            n1 = t1 * t1 * (grad3[gi1][0] * x1 + grad3[gi1][1] * y1)
        }
        var t2 = 0.5f - x2 * x2 - y2 * y2
        if (t2 >= 0) {
            t2 *= t2
            n2 = t2 * t2 * (grad3[gi2][0] * x2 + grad3[gi2][1] * y2)
        }
        return 70.0f * (n0 + n1 + n2)
    }

    fun noise3D(xin: Float, yin: Float, zin: Float): Float {
        var n0 = 0f; var n1 = 0f; var n2 = 0f; var n3 = 0f
        val s = (xin + yin + zin) * F3
        val i = floor((xin + s).toDouble()).toInt()
        val j = floor((yin + s).toDouble()).toInt()
        val k = floor((zin + s).toDouble()).toInt()
        val t = (i + j + k) * G3
        val X0 = i - t
        val Y0 = j - t
        val Z0 = k - t
        val x0 = xin - X0
        val y0 = yin - Y0
        val z0 = zin - Z0

        var i1 = 0; var j1 = 0; var k1 = 0
        var i2 = 0; var j2 = 0; var k2 = 0
        if (x0 >= y0) {
            if (y0 >= z0) { i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0 }
            else if (x0 >= z0) { i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1 }
            else { i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1 }
        } else {
            if (y0 < z0) { i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1 }
            else if (x0 < z0) { i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1 }
            else { i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0 }
        }

        val x1 = x0 - i1 + G3
        val y1 = y0 - j1 + G3
        val z1 = z0 - k1 + G3
        val x2 = x0 - i2 + 2.0f * G3
        val y2 = y0 - j2 + 2.0f * G3
        val z2 = z0 - k2 + 2.0f * G3
        val x3 = x0 - 1.0f + 3.0f * G3
        val y3 = y0 - 1.0f + 3.0f * G3
        val z3 = z0 - 1.0f + 3.0f * G3

        val ii = i and 255
        val jj = j and 255
        val kk = k and 255
        val gi0 = permMod12[ii + perm[jj + perm[kk]]]
        val gi1 = permMod12[ii + i1 + perm[jj + j1 + perm[kk + k1]]]
        val gi2 = permMod12[ii + i2 + perm[jj + j2 + perm[kk + k2]]]
        val gi3 = permMod12[ii + 1 + perm[jj + 1 + perm[kk + 1]]]

        var t0 = 0.6f - x0 * x0 - y0 * y0 - z0 * z0
        if (t0 >= 0) {
            t0 *= t0
            n0 = t0 * t0 * (grad3[gi0][0] * x0 + grad3[gi0][1] * y0 + grad3[gi0][2] * z0)
        }
        var t1 = 0.6f - x1 * x1 - y1 * y1 - z1 * z1
        if (t1 >= 0) {
            t1 *= t1
            n1 = t1 * t1 * (grad3[gi1][0] * x1 + grad3[gi1][1] * y1 + grad3[gi1][2] * z1)
        }
        var t2 = 0.6f - x2 * x2 - y2 * y2 - z2 * z2
        if (t2 >= 0) {
            t2 *= t2
            n2 = t2 * t2 * (grad3[gi2][0] * x2 + grad3[gi2][1] * y2 + grad3[gi2][2] * z2)
        }
        var t3 = 0.6f - x3 * x3 - y3 * y3 - z3 * z3
        if (t3 >= 0) {
            t3 *= t3
            n3 = t3 * t3 * (grad3[gi3][0] * x3 + grad3[gi3][1] * y3 + grad3[gi3][2] * z3)
        }
        return 32.0f * (n0 + n1 + n2 + n3)
    }
}
