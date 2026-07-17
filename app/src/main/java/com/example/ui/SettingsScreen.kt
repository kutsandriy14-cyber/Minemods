package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    
    var fpsLimit by remember { mutableStateOf(preferences.getInt("fpsLimit", 60).toFloat()) }
    var renderDistance by remember { mutableStateOf(preferences.getFloat("renderDistance", 8f)) }
    var textureQuality by remember { mutableStateOf(preferences.getString("textureQuality", "HIGH")) }
    var cameraSensitivity by remember { mutableStateOf(preferences.getFloat("sensitivity", 1.0f)) }
    
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1F3C54), // Deep night sky
            Color(0xFF0F1E2A), // Dark blue void
            Color(0xFF2B1C12)  // Earth brown bottom
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(550.dp)
                .padding(vertical = 16.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Text(
                text = "GAME SETTINGS",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFFFB300),
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Settings Container Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // FPS Limit Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "FPS LIMIT:",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Text(
                                "${fpsLimit.toInt()} FPS",
                                color = Color(0xFFFFB300),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                        Slider(
                            value = fpsLimit,
                            onValueChange = { 
                                fpsLimit = it
                                preferences.edit().putInt("fpsLimit", it.toInt()).apply()
                            },
                            valueRange = 30f..120f,
                            steps = 2,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFB300),
                                activeTrackColor = Color(0xFFFFB300),
                                inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Render Distance Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "RENDER DISTANCE:",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Text(
                                "${renderDistance.toInt()} Chunks",
                                color = Color(0xFFFFB300),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                        Slider(
                            value = renderDistance,
                            onValueChange = { 
                                renderDistance = it
                                preferences.edit().putFloat("renderDistance", it).apply()
                            },
                            valueRange = 4f..512f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFB300),
                                activeTrackColor = Color(0xFFFFB300),
                                inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Sensitivity Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "CAMERA SENSITIVITY:",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Text(
                                String.format("%.1fx", cameraSensitivity),
                                color = Color(0xFFFFB300),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                        Slider(
                            value = cameraSensitivity,
                            onValueChange = { 
                                cameraSensitivity = it
                                preferences.edit().putFloat("sensitivity", it).apply()
                            },
                            valueRange = 0.2f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFB300),
                                activeTrackColor = Color(0xFFFFB300),
                                inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Texture Quality Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TEXTURE QUALITY:",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("LOW", "MEDIUM", "HIGH").forEach { quality ->
                                val isSelected = textureQuality == quality
                                Button(
                                    onClick = {
                                        textureQuality = quality
                                        preferences.edit().putString("textureQuality", quality).apply()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFFFFB300) else Color(0xFF3C3C3C)
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = quality,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save / Back Button
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C4C4C)),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
                modifier = Modifier
                    .width(200.dp)
                    .height(44.dp)
            ) {
                Text(
                    text = "SAVE & BACK",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}
