package app.xrtm8.mada

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import app.xrtm8.mada.ui.MadaStudioScreen
import app.xrtm8.mada.ui.theme.DarkBackground
import app.xrtm8.mada.ui.theme.MadaTheme
import app.xrtm8.mada.updater.MadaUpdateManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var updateManager: MadaUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateManager = MadaUpdateManager(this)

        // Non-intrusive / gentle update check in background upon launch
        lifecycleScope.launch {
            updateManager.checkForUpdates(currentVersionCode = 1, showNotificationOnFound = true)
        }

        setContent {
            MadaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    MadaStudioScreen(updateManager = updateManager)
                }
            }
        }
    }
}
