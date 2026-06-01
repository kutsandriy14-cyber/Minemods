package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.minecraft.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    
    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Full Edge to Edge landscape support!
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainAppContent(gameViewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: GameViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Collect Screen lifecycle States
    val currentScreen by viewModel.currentScreen.collectAsState()
    val worlds by viewModel.worlds.collectAsState()
    val mods by viewModel.mods.collectAsState()
    val activeWorld by viewModel.activeWorld.collectAsState()
    val editingMod by viewModel.editingMod.collectAsState()

    // Intercept back button when not in the Launcher
    BackHandler(enabled = currentScreen != Screen.Launcher) {
        if (currentScreen == Screen.Game) {
            viewModel.closeContainer(context)
        }
        viewModel.navigateTo(Screen.Launcher)
    }

    // Load data from files at boot!
    LaunchedEffect(Unit) {
        viewModel.loadLauncherData(context)
    }

    // Material 3 Screen Navigator router
    when (currentScreen) {
        is Screen.Launcher -> {
            LauncherScreen(
                viewModel = viewModel,
                onNavigateToWorldSelect = { viewModel.navigateTo(Screen.WorldSelect) },
                onNavigateToModManager = { viewModel.navigateTo(Screen.ModManager) },
                mods = mods
            )
        }
        is Screen.WorldSelect -> {
            WorldSelectScreen(
                viewModel = viewModel,
                worlds = worlds,
                onBackToLauncher = { viewModel.navigateTo(Screen.Launcher) }
            )
        }
        is Screen.ModManager -> {
            ModManagerScreen(
                viewModel = viewModel,
                mods = mods,
                activeMod = editingMod,
                onBackToLauncher = { viewModel.navigateTo(Screen.Launcher) }
            )
        }
        is Screen.Game -> {
            activeWorld?.let { world ->
                GameScreen(
                    viewModel = viewModel,
                    world = world,
                    onQuitToMenu = { viewModel.navigateTo(Screen.Launcher) }
                )
            }
        }
    }
}
