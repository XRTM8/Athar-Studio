package app.xrtm8.athar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import app.xrtm8.athar.ui.AtharStudioScreen
import app.xrtm8.athar.ui.theme.AtharTheme
import app.xrtm8.athar.ui.theme.DarkBackground
import app.xrtm8.athar.updater.AtharUpdateManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var updateManager: AtharUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateManager = AtharUpdateManager(this)

        lifecycleScope.launch {
            updateManager.checkForUpdates(currentVersionCode = 1, showNotificationOnFound = true)
        }

        setContent {
            AtharTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    AtharStudioScreen(updateManager = updateManager)
                }
            }
        }
    }
}
