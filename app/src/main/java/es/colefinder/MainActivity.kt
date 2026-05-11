package es.colefinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import es.colefinder.ui.map.MapScreen
import es.colefinder.ui.theme.ColeFinderTheme
import es.colefinder.ui.update.InAppUpdateBanner
import es.colefinder.update.InAppUpdateManager
import es.colefinder.update.InAppUpdateUiState

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var inAppUpdateManager: InAppUpdateManager
    private val inAppUpdateState = mutableStateOf(InAppUpdateUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        inAppUpdateManager = InAppUpdateManager(this) { inAppUpdateState.value = it }

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
}