package com.example.engine

import kotlin.math.sqrt

class Vector3f(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    fun set(x: Float, y: Float, z: Float) { this.x = x; this.y = y; this.z = z }
    fun set(other: Vector3f) { x = other.x; y = other.y; z = other.z }
    fun add(other: Vector3f) = Vector3f(x + other.x, y + other.y, z + other.z)
    fun sub(other: Vector3f) = Vector3f(x - other.x, y - other.y, z - other.z)
    fun mul(s: Float) = Vector3f(x * s, y * s, z * s)
    fun length() = sqrt(x * x + y * y + z * z)
    fun normalize(): Vector3f {
        val len = length()
        if (len > 0) { x /= len; y /= len; z /= len }
        return this
    }
    fun cross(other: Vector3f) = Vector3f(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )
    fun dot(other: Vector3f) = x * other.x + y * other.y + z * other.z
    operator fun plusAssign(other: Vector3f) { x += other.x; y += other.y; z += other.z }
    operator fun minusAssign(other: Vector3f) { x -= other.x; y -= other.y; z -= other.z }
    operator fun plus(other: Vector3f) = add(other)
    operator fun minus(other: Vector3f) = sub(other)
    operator fun times(s: Float) = mul(s)
    
    fun copy() = Vector3f(x, y, z)
}
