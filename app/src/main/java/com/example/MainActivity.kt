package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.GameScreen
import com.example.ui.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.world.WorldSaveManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "menu") {
                        composable("menu") {
                            MainMenu(
                                onPlayClick = { worldName -> navController.navigate("game/$worldName") },
                                onSettingsClick = { navController.navigate("settings") },
                                onExitClick = { finish() }
                            )
                        }
                        composable(
                            route = "game/{worldName}",
                            arguments = listOf(navArgument("worldName") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val worldName = backStackEntry.arguments?.getString("worldName") ?: "default"
                            GameScreen(
                                worldName = worldName,
                                onSettingsClick = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}

enum class MenuState {
    MAIN,
    WORLD_SELECT,
    CREATE_WORLD
}

@Composable
fun MainMenu(
    onPlayClick: (worldName: String) -> Unit,
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit
) {
    val context = LocalContext.current
    var menuState by remember { mutableStateOf(MenuState.MAIN) }
    var worldsList by remember { mutableStateOf(emptyList<String>()) }
    
    // Refresh worlds list when entering WORLD_SELECT state
    LaunchedEffect(menuState) {
        if (menuState == MenuState.WORLD_SELECT) {
            worldsList = WorldSaveManager.getWorldList(context)
        }
    }

    // Beautiful sky-to-earth theme gradient
    val menuGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2C5E8A), // Sky blue
            Color(0xFF14304A), // Deep evening blue
            Color(0xFF3B2416), // Dark earth brown
            Color(0xFF26150B)  // Underground stone black
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(menuGradient),
        contentAlignment = Alignment.Center
    ) {
        // Decorative pixel grid/block shapes in background
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f))
                )
            }
        }

        when (menuState) {
            MenuState.MAIN -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    // Stylized blocky logo with elegant layered shadows
                    Box {
                        // Background shadow 2
                        Text(
                            text = "MINEMODS",
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF1C1108),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.offset(x = 6.dp, y = 6.dp)
                        )
                        // Background shadow 1
                        Text(
                            text = "MINEMODS",
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF8B5A2B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.offset(x = 3.dp, y = 3.dp)
                        )
                        // Foreground text
                        Text(
                            text = "MINEMODS",
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFFB300),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Text(
                        text = "Voxel Sandbox Engine v3.0",
                        color = Color(0xFF8FBDD3),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
                    )

                    // Retro Style Buttons
                    RetroMenuButton(text = "PLAY WORLD") {
                        menuState = MenuState.WORLD_SELECT
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    RetroMenuButton(text = "SETTINGS", onClick = onSettingsClick)
                    Spacer(modifier = Modifier.height(16.dp))
                    RetroMenuButton(text = "EXIT", onClick = onExitClick)
                }
            }
            
            MenuState.WORLD_SELECT -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(450.dp)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "SELECT WORLD",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFB300),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // World List Box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    ) {
                        if (worldsList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No worlds saved yet.\nCreate one below!",
                                    color = Color.LightGray,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(worldsList) { worldName ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onPlayClick(worldName) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = worldName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = "Survival Mode",
                                                    color = Color.Gray,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            
                                            IconButton(
                                                onClick = {
                                                    WorldSaveManager.deleteWorld(context, worldName)
                                                    worldsList = WorldSaveManager.getWorldList(context)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete World",
                                                    tint = Color.Red.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons at bottom
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { menuState = MenuState.CREATE_WORLD },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New World", tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("NEW WORLD", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { menuState = MenuState.MAIN },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("BACK", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            MenuState.CREATE_WORLD -> {
                var worldNameInput by remember { mutableStateOf("") }
                var seedInput by remember { mutableStateOf("") }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .width(400.dp)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "CREATE NEW WORLD",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFB300),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = worldNameInput,
                        onValueChange = { worldNameInput = it },
                        label = { Text("World Name", fontFamily = FontFamily.Monospace) },
                        placeholder = { Text("My Awesome World", fontFamily = FontFamily.Monospace) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFB300),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFFFFB300),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = seedInput,
                        onValueChange = { seedInput = it },
                        label = { Text("World Seed (Optional)", fontFamily = FontFamily.Monospace) },
                        placeholder = { Text("Random", fontFamily = FontFamily.Monospace) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFB300),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFFFFB300),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val name = worldNameInput.trim().ifEmpty { "World_${System.currentTimeMillis() % 10000}" }
                                onPlayClick(name)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("CREATE", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { menuState = MenuState.WORLD_SELECT },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("CANCEL", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RetroMenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C5C5C)),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
        modifier = Modifier
            .width(280.dp)
            .height(54.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}
