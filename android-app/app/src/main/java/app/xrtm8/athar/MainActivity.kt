package app.xrtm8.athar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import app.xrtm8.athar.typography.AtharFontManager
import app.xrtm8.athar.ui.AtharStudioScreen
import app.xrtm8.athar.ui.theme.AmoledBlack
import app.xrtm8.athar.ui.theme.AtharTheme
import app.xrtm8.athar.updater.AtharUpdateManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var updateManager: AtharUpdateManager

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Permission result handled gracefully without intrusive dialogs
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateManager = AtharUpdateManager(this)

        // Request notification permission smoothly on Android 13+ (API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Silent non-blocking update check in background upon launch
        lifecycleScope.launch {
            val currentVersion = try {
                packageManager.getPackageInfo(packageName, 0).versionName ?: "1.1.0"
            } catch (e: Exception) {
                "1.1.0"
            }
            updateManager.checkForUpdates(currentVersionName = currentVersion, showNotificationOnFound = true)
        }

        // Preload authentic Arabic typefaces into memory
        AtharFontManager.preload(this)

        val initialProjectUri = if (intent?.action == Intent.ACTION_VIEW) intent.data else null

        setContent {
            AtharTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AmoledBlack
                ) {
                    AtharStudioScreen(
                        updateManager = updateManager,
                        initialProjectUri = initialProjectUri
                    )
                }
            }
        }
    }
}
