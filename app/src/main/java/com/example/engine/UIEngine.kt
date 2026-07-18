package com.example.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object UIEngine {
    // UI HUD States
    var showChat by mutableStateOf(false)
    var chatInputText by mutableStateOf("")
    val chatMessages = mutableStateListOf<String>()
    
    // Settings state
    var viewDistanceChunks by mutableStateOf(4)
    var showFps by mutableStateOf(true)
    var showCoordinates by mutableStateOf(true)

    init {
        // Welcoming messages
        chatMessages.add("[System] Sandbox Engine Initialized!")
        chatMessages.add("[System] Welcome to Voxel Sandbox v3.0!")
        chatMessages.add("[System] Type /help to see list of commands.")
    }

    fun addSystemMessage(text: String) {
        chatMessages.add("[System] $text")
        if (chatMessages.size > 50) {
            chatMessages.removeAt(0)
        }
    }

    fun addChatMessage(sender: String, text: String) {
        chatMessages.add("<$sender> $text")
        if (chatMessages.size > 50) {
            chatMessages.removeAt(0)
        }
    }
}
