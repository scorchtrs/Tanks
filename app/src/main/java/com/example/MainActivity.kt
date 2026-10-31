package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.GameScreen
import com.example.ui.MainMenuScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameScreenState
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ScorchedTanksApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ScorchedTanksApp(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val screenState by viewModel.screenState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    if (screenState == GameScreenState.MENU) {
        MainMenuScreen(
            currentSettings = settings,
            onStartGame = { newSettings ->
                viewModel.updateSettings(newSettings)
                viewModel.startMatch()
            },
            onLoadCorrespondenceCode = { code ->
                viewModel.loadCorrespondenceTurn(code)
            },
            modifier = modifier
        )
    } else {
        BackHandler {
            viewModel.returnToMenu()
        }
        GameScreen(
            viewModel = viewModel,
            modifier = modifier
        )
    }
}
