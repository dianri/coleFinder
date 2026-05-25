package es.colefinder.ui.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.colefinder.R
import es.colefinder.update.InAppUpdateUiState

/** Banner compacto para actualización flexible y reinicio tras descarga. */
@Composable
fun InAppUpdateBanner(
    state: InAppUpdateUiState,
    onUpdateClick: () -> Unit,
    onRestartClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible =
        (state.hayActualizacionDisponible && !state.actualizacionDisponibleDescartada) ||
            (state.descargaCompletadaPendienteReinicio && !state.reinicioBannerDescartado)
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    when {
                        state.descargaCompletadaPendienteReinicio -> {
                            Text(
                                text = stringResource(R.string.in_app_update_ready_title),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(R.string.in_app_update_ready_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        state.hayActualizacionDisponible -> {
                            Text(
                                text = stringResource(R.string.in_app_update_available_title),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(R.string.in_app_update_available_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when {
                        state.descargaCompletadaPendienteReinicio ->
                            TextButton(onClick = onRestartClick) {
                                Text(stringResource(R.string.in_app_update_apply_now))
                            }

                        state.hayActualizacionDisponible ->
                            TextButton(onClick = onUpdateClick) {
                                Text(stringResource(R.string.in_app_update_download))
                            }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.in_app_update_dismiss),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
