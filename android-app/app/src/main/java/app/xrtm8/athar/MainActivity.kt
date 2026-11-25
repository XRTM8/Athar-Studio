package app.xrtm8.athar

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import app.xrtm8.athar.typography.AtharFontManager
import app.xrtm8.athar.ui.AtharHomeScreen
import app.xrtm8.athar.ui.AtharStudioScreen
import app.xrtm8.athar.ui.AtharStudioViewModel
import app.xrtm8.athar.ui.theme.AmoledBlack
import app.xrtm8.athar.ui.theme.AmoledWhite
import app.xrtm8.athar.ui.theme.AtharTheme
import app.xrtm8.athar.ui.viewmodel.StudioUiEvent
import app.xrtm8.athar.updater.AtharUpdateManager
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch

enum class AtharScreen {
    HOME,
    STUDIO
}

class MainActivity : ComponentActivity() {

    private lateinit var updateManager: AtharUpdateManager
    private val studioViewModel: AtharStudioViewModel by viewModels()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Permission result handled gracefully without intrusive dialogs
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Enable Full-Screen Immersive Edge-to-Edge System Bar Integration
        enableEdgeToEdge()

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
                packageManager.getPackageInfo(packageName, 0).versionName ?: BuildConfig.VERSION_NAME
            } catch (e: Exception) {
                BuildConfig.VERSION_NAME
            }
            updateManager.checkForUpdates(currentVersionName = currentVersion, showNotificationOnFound = true)
        }

        // Preload authentic Arabic typefaces into memory
        AtharFontManager.preload(this)
        studioViewModel.initTheme(this)

        val initialProjectUri = if (intent?.action == Intent.ACTION_VIEW) intent.data else null

        if (initialProjectUri != null) {
            studioViewModel.openInitialProject(this, initialProjectUri)
        }

        setContent {
            LaunchedEffect(studioViewModel) {
                studioViewModel.uiEvents.collect { event ->
                    when (event) {
                        is StudioUiEvent.ShareFile -> {
                            val uri = FileProvider.getUriForFile(
                                this@MainActivity,
                                "${packageName}.fileprovider",
                                event.file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = event.mimeType
                                putExtra(Intent.EXTRA_STREAM, uri)
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            startActivity(Intent.createChooser(shareIntent, event.chooserTitle))
                        }
                    }
                }
            }

            val isAmoledWhite by studioViewModel.isAmoledWhite.collectAsState()

            // 2. Synchronize Status Bar & Navigation Bar light/dark appearance dynamically with AMOLED Theme
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as? Activity)?.window ?: return@SideEffect
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = isAmoledWhite
                    insetsController.isAppearanceLightNavigationBars = isAmoledWhite
                }
            }

            AtharTheme(isAmoledWhite = isAmoledWhite) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (isAmoledWhite) AmoledWhite else AmoledBlack
                    ) {
                        var currentScreen by androidx.compose.runtime.saveable.rememberSaveable {
                            mutableStateOf(
                                if (initialProjectUri != null) AtharScreen.STUDIO else AtharScreen.HOME
                            )
                        }

                        Crossfade(
                            targetState = currentScreen,
                            label = "ScreenCrossfade"
                        ) { screen ->
                            when (screen) {
                                AtharScreen.HOME -> {
                                    AtharHomeScreen(
                                        viewModel = studioViewModel,
                                        updateManager = updateManager,
                                        onNavigateToStudio = { currentScreen = AtharScreen.STUDIO }
                                    )
                                }
                                AtharScreen.STUDIO -> {
                                    AtharStudioScreen(
                                        updateManager = updateManager,
                                        viewModel = studioViewModel,
                                        onNavigateBackToHome = { currentScreen = AtharScreen.HOME }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
