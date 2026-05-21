package es.colefinder

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
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

    private val updateResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                Log.w(TAG, "El usuario canceló o falló la actualización")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        inAppUpdateManager = InAppUpdateManager(this) { inAppUpdateState.value = it }

        lifecycleScope.launch {
            checkForUpdates()
        }

        setContent {
            val state by inAppUpdateState
            ColeFinderTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        MapScreen()
                    }
                    InAppUpdateBanner(
                        state = state,
                        onUpdateClick = { inAppUpdateManager.startFlexibleUpdateFlow() },
                        onRestartClick = { inAppUpdateManager.completeFlexibleUpdate() },
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        inAppUpdateManager.checkForFlexibleUpdate()
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

            val updateType = if (config.updateType == "IMMEDIATE") {
                AppUpdateType.IMMEDIATE
            } else {
                AppUpdateType.FLEXIBLE
            }

            val appUpdateManager = AppUpdateManagerFactory.create(this)
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { info ->
                    if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE ||
                        !info.isUpdateTypeAllowed(updateType)
                    ) {
                        return@addOnSuccessListener
                    }
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            info,
                            updateResultLauncher,
                            AppUpdateOptions.newBuilder(updateType).build(),
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "startUpdateFlowForResult", e)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "appUpdateInfo en checkForUpdates", e)
                }
        } catch (e: Exception) {
            Log.w(TAG, "checkForUpdates", e)
        }
    }

    private companion object {
        private const val TAG = "InAppUpdate"
    }
}
