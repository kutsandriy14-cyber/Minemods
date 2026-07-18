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
                                onPlayClick = { worldName, mode, ip -> 
                                    navController.navigate("game/$worldName?mode=$mode&ip=$ip") 
                                },
                                onSettingsClick = { navController.navigate("settings") },
                                onExitClick = { finish() }
                            )
                        }
                        composable(
                            route = "game/{worldName}?mode={mode}&ip={ip}",
                            arguments = listOf(
                                navArgument("worldName") { type = NavType.StringType },
                                navArgument("mode") { type = NavType.StringType; defaultValue = "single" },
                                navArgument("ip") { type = NavType.StringType; defaultValue = "" }
                            )
                        ) { backStackEntry ->
                            val worldName = backStackEntry.arguments?.getString("worldName") ?: "default"
                            val mode = backStackEntry.arguments?.getString("mode") ?: "single"
                            val ip = backStackEntry.arguments?.getString("ip") ?: ""
                            GameScreen(
                                worldName = worldName,
                                mode = mode,
                                ip = ip,
                                onSettingsClick = { navController.navigate("settings") },
                                onBackClick = { navController.navigate("menu") { popUpTo("menu") { inclusive = true } } }
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
    CREATE_WORLD,
    MULTIPLAYER,
    HOST_SELECT,
    JOIN_INPUT
}

@Composable
fun MainMenu(
    onPlayClick: (worldName: String, mode: String, ip: String) -> Unit,
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit
) {
    val context = LocalContext.current
    var menuState by remember { mutableStateOf(MenuState.MAIN) }
    var worldsList by remember { mutableStateOf(emptyList<String>()) }
    
    // Refresh worlds list when entering WORLD_SELECT or HOST_SELECT state
    LaunchedEffect(menuState) {
        if (menuState == MenuState.WORLD_SELECT || menuState == MenuState.HOST_SELECT) {
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
                        text = "Voxel Sandbox Engine v1.2 (Multiplayer)",
                        color = Color(0xFF8FBDD3),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
                    )

                    // Retro Style Buttons
                    RetroMenuButton(text = "PLAY OFFLINE") {
                        menuState = MenuState.WORLD_SELECT
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    RetroMenuButton(text = "MULTIPLAYER") {
                        menuState = MenuState.MULTIPLAYER
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
                                            .clickable { onPlayClick(worldName, "single", "") }
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
                                onPlayClick(name, "single", "")
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
            
            MenuState.MULTIPLAYER -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "LAN MULTIPLAYER",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFB300),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Play together on the same Wi-Fi network",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Version selection panel
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .padding(bottom = 24.dp)
                            .width(320.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "YOUR GAME VERSION",
                                color = Color(0xFFFFB300),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            var currentVer by remember { mutableStateOf(com.example.engine.NetworkManager.gameVersion) }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("1.2", "1.2.3", "1.3").forEach { ver ->
                                    val isSelected = currentVer == ver
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, if (isSelected) Color(0xFFFFB300) else Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .background(if (isSelected) Color(0xFFFFB300).copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable { 
                                                currentVer = ver
                                                com.example.engine.NetworkManager.gameVersion = ver
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(ver, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = currentVer,
                                onValueChange = { 
                                    currentVer = it
                                    com.example.engine.NetworkManager.gameVersion = it
                                },
                                label = { Text("Custom Version", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFFB300),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            )
                        }
                    }

                    RetroMenuButton(text = "HOST LAN GAME") {
                        menuState = MenuState.HOST_SELECT
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    RetroMenuButton(text = "JOIN LAN GAME") {
                        menuState = MenuState.JOIN_INPUT
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    RetroMenuButton(text = "BACK") {
                        menuState = MenuState.MAIN
                    }
                }
            }

            MenuState.HOST_SELECT -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(450.dp)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "HOST MULTIPLAYER WORLD",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFB300),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

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
                                    text = "No worlds saved yet.\nCreate one in singleplayer first!",
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
                                            .clickable { onPlayClick(worldName, "host", "") }
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
                                                    text = "Co-Op Host Server",
                                                    color = Color.Green.copy(alpha = 0.7f),
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    RetroMenuButton(text = "BACK") {
                        menuState = MenuState.MULTIPLAYER
                    }
                }
            }

            MenuState.JOIN_INPUT -> {
                var ipInput by remember { mutableStateOf("") }
                
                DisposableEffect(Unit) {
                    com.example.engine.NetworkManager.startUdpDiscovery()
                    onDispose {
                        com.example.engine.NetworkManager.stopUdpDiscovery()
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .width(420.dp)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "JOIN LAN SERVER",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFB300),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Enter host IP or select a discovered game below:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        label = { Text("Host IP Address", fontFamily = FontFamily.Monospace) },
                        placeholder = { Text("e.g. 192.168.1.100", fontFamily = FontFamily.Monospace) },
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

                    Text(
                        text = "DISCOVERED LAN SERVERS",
                        color = Color(0xFFFFB300),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                    )

                    val discovered = com.example.engine.NetworkManager.discoveredServers

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    ) {
                        if (discovered.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Searching for LAN servers...\n(Make sure Host is on the same Wi-Fi)",
                                    color = Color.LightGray.copy(alpha = 0.6f),
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(discovered) { server ->
                                    val versionMatches = server.version == com.example.engine.NetworkManager.gameVersion
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (versionMatches) Color.White.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.05f)
                                        ),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                ipInput = server.ip
                                            }
                                            .border(1.dp, if (versionMatches) Color.Green.copy(alpha = 0.3f) else Color.Red.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = server.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "IP: ${server.ip}",
                                                    color = Color.LightGray,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "v${server.version}",
                                                    color = if (versionMatches) Color.Green else Color.Red,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = if (versionMatches) "COMPATIBLE" else "MISMATCH",
                                                    color = if (versionMatches) Color.Green.copy(alpha = 0.7f) else Color.Red.copy(alpha = 0.7f),
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val ip = ipInput.trim()
                                if (ip.isNotEmpty()) {
                                    onPlayClick("multiplayer_joined", "client", ip)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("CONNECT", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { menuState = MenuState.MULTIPLAYER },
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
