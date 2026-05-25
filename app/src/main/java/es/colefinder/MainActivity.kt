package es.colefinder

import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.install.model.AppUpdateType
import dagger.hilt.android.AndroidEntryPoint
import es.colefinder.data.repository.AppConfigRepository
import es.colefinder.ui.map.MapScreen
import es.colefinder.ui.theme.ColeFinderTheme
import es.colefinder.ui.update.InAppUpdateBanner
import es.colefinder.update.InAppUpdateManager
import es.colefinder.update.InAppUpdateUiState
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appConfigRepository: AppConfigRepository

    private lateinit var inAppUpdateManager: InAppUpdateManager
    private val inAppUpdateState = mutableStateOf(InAppUpdateUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        configureModernEdgeToEdge(window)
        super.onCreate(savedInstanceState)
        inAppUpdateManager = InAppUpdateManager(this) { inAppUpdateState.value = it }

        lifecycleScope.launch {
            checkForUpdates()
        }

        setContent {
            val state by inAppUpdateState
            ColeFinderTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.systemBars,
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            MapScreen(contentPadding = innerPadding)
                        }
                        InAppUpdateBanner(
                            state = state,
                            onUpdateClick = { inAppUpdateManager.startFlexibleUpdateFlow() },
                            onRestartClick = { inAppUpdateManager.completeFlexibleUpdate() },
                            onDismiss = {
                                if (state.descargaCompletadaPendienteReinicio) {
                                    inAppUpdateManager.dismissRestartBanner()
                                } else {
                                    inAppUpdateManager.dismissAvailableUpdateBanner()
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = innerPadding.calculateTopPadding()),
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        inAppUpdateManager.checkForUpdateOnResume()
    }

    override fun onDestroy() {
        if (::inAppUpdateManager.isInitialized) {
            inAppUpdateManager.unregisterListener()
        }
        super.onDestroy()
    }

    /**
     * Actualización forzada según [AppConfigRepository] (Supabase).
     * Convive con [InAppUpdateManager]: este flujo actúa si VERSION_CODE &lt; min_version_code.
     */
    private suspend fun checkForUpdates() {
        try {
            val config = appConfigRepository.getUpdateConfig()
            if (BuildConfig.VERSION_CODE >= config.minVersionCode) return

            val currentState = inAppUpdateState.value
            if (currentState.hayActualizacionDisponible ||
                currentState.descargaCompletadaPendienteReinicio
            ) return

            val updateType = if (config.updateType == "IMMEDIATE") {
                AppUpdateType.IMMEDIATE
            } else {
                AppUpdateType.FLEXIBLE
            }

            inAppUpdateManager.startConfiguredUpdateFlow(updateType)
        } catch (e: Exception) {
            Log.w(TAG, "checkForUpdates", e)
        }
    }

    private companion object {
        private const val TAG = "InAppUpdate"

        /**
         * Edge-to-edge sin APIs deprecadas en Android 15 (setStatusBarColor,
         * setNavigationBarColor, LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES).
         * Con targetSdk 35+ el sistema fuerza edge-to-edge en Android 15; esto cubre API 30–34.
         */
        private fun configureModernEdgeToEdge(window: Window) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
    }
}
