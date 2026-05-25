package es.colefinder.update

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Actualización in-app flexible (Play Core).
 *
 * Prueba como tester interno/cerrado: publica en Play una versión con versionCode mayor que la
 * instalada, instala la build anterior desde Play (o internal sharing), abre la app; debe aparecer
 * el aviso. Tras aceptar la descarga en segundo plano, al terminar aparece "Reiniciar".
 * No funciona con APK sideload fuera de Play (appUpdateInfo suele fallar o no hay UPDATE_AVAILABLE).
 */
data class InAppUpdateUiState(
    /** Hay una versión más nueva en Play y el tipo FLEXIBLE está permitido. */
    val hayActualizacionDisponible: Boolean = false,
    /** Descarga flexible terminada; hay que llamar a [InAppUpdateManager.completeFlexibleUpdate]. */
    val descargaCompletadaPendienteReinicio: Boolean = false,
    /** El usuario cerró el aviso de actualización disponible en esta sesión. */
    val actualizacionDisponibleDescartada: Boolean = false,
    /** El usuario pospuso el reinicio tras descargar la actualización flexible. */
    val reinicioBannerDescartado: Boolean = false,
)

class InAppUpdateManager(
    private val activity: ComponentActivity,
    private val onUiState: (InAppUpdateUiState) -> Unit,
) {
    private val appUpdateManager = AppUpdateManagerFactory.create(activity)

    private var uiState = InAppUpdateUiState()
        set(value) {
            field = value
            onUiState(value)
        }

    private val installStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> { /* opcional: progreso */ }
            InstallStatus.DOWNLOADED ->
                uiState = uiState.copy(
                    descargaCompletadaPendienteReinicio = true,
                    hayActualizacionDisponible = false,
                    reinicioBannerDescartado = false,
                )
            InstallStatus.FAILED, InstallStatus.CANCELED ->
                Log.d(TAG, "install status=${state.installStatus()}")
            else -> Unit
        }
    }

    private val updateFlowLauncher: ActivityResultLauncher<androidx.activity.result.IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                Log.d(TAG, "Flujo de actualización cerrado o cancelado (resultCode=${result.resultCode})")
            }
        }

    init {
        registerInstallListener()
    }

    private fun registerInstallListener() {
        appUpdateManager.registerListener(installStateListener)
    }

    fun unregisterListener() {
        appUpdateManager.unregisterListener(installStateListener)
    }

    /**
     * Llamar desde [Activity.onResume] (cada vez que el usuario vuelve a la app o entra en primer plano):
     * relanza IMMEDIATE si quedó a medias, detecta descarga flexible completada o update disponible.
     */
    fun checkForUpdateOnResume() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                when {
                    info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                        resumeImmediateUpdateFlow(info)

                    info.installStatus() == InstallStatus.DOWNLOADED ->
                        uiState = uiState.copy(
                            descargaCompletadaPendienteReinicio = true,
                            hayActualizacionDisponible = false,
                        )

                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) &&
                        !uiState.actualizacionDisponibleDescartada ->
                        uiState = uiState.copy(hayActualizacionDisponible = true)

                    else ->
                        if (!uiState.descargaCompletadaPendienteReinicio) {
                            uiState = uiState.copy(hayActualizacionDisponible = false)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "No se pudo obtener AppUpdateInfo (normal fuera de Play o sin red)", e)
            }
    }

    private fun resumeImmediateUpdateFlow(info: AppUpdateInfo) {
        if (!info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) return
        try {
            appUpdateManager.startUpdateFlowForResult(
                info,
                updateFlowLauncher,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
            )
        } catch (e: Exception) {
            Log.w(TAG, "Reanudar flujo IMMEDIATE en onResume", e)
        }
    }

    /**
     * Flujo de actualización según config remota (Supabase).
     * Usar cuando VERSION_CODE es menor que min_version_code (IMMEDIATE o FLEXIBLE).
     */
    fun startConfiguredUpdateFlow(updateType: Int) {
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
                        updateFlowLauncher,
                        AppUpdateOptions.newBuilder(updateType).build(),
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "startUpdateFlowForResult (configured)", e)
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "appUpdateInfo en startConfiguredUpdateFlow", e)
            }
    }

    /** Inicia el flujo de UI de Play para descarga flexible (tras pulsar "Actualizar"). */
    fun startFlexibleUpdateFlow() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE ||
                    !info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    return@addOnSuccessListener
                }
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        updateFlowLauncher,
                        AppUpdateOptionsHolder.flexible(),
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "startUpdateFlowForResult", e)
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "appUpdateInfo antes de startUpdateFlow", e)
            }
    }

    /**
     * Reinicia la app con la nueva versión ya descargada (flexible).
     * Solo tiene efecto si el estado instalado es DOWNLOADED.
     */
    fun completeFlexibleUpdate() {
        appUpdateManager.completeUpdate()
    }

    fun dismissAvailableUpdateBanner() {
        uiState = uiState.copy(
            hayActualizacionDisponible = false,
            actualizacionDisponibleDescartada = true,
        )
    }

    fun dismissRestartBanner() {
        uiState = uiState.copy(
            descargaCompletadaPendienteReinicio = false,
            reinicioBannerDescartado = true,
        )
    }

    private object AppUpdateOptionsHolder {
        fun flexible() = com.google.android.play.core.appupdate.AppUpdateOptions
            .defaultOptions(AppUpdateType.FLEXIBLE)
    }

    companion object {
        private const val TAG = "InAppUpdateManager"
    }
}
