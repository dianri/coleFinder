package es.colefinder.ui.map

import androidx.lifecycle.ViewModel
import app.cash.turbine.test
import com.google.android.gms.maps.model.LatLng
import es.colefinder.data.model.Colegio
import es.colefinder.data.model.ColegioSearchDto
import es.colefinder.data.repository.ColegioRepository
import es.colefinder.data.repository.UserPreferences
import es.colefinder.data.repository.UserPreferencesRepository
import es.colefinder.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Cancela [viewModelScope] (método [clear] interno en lifecycle 2.8: puede ir manglado como `clear$…`). */
    private fun ViewModel.cancelForTest() {
        var c: Class<*>? = javaClass
        while (c != null) {
            for (m in c.declaredMethods) {
                if (m.parameterCount != 0) continue
                val n = m.name
                if (n == "clear" || (n.startsWith("clear") && n.contains("$"))) {
                    m.isAccessible = true
                    m.invoke(this)
                    return
                }
            }
            c = c.superclass
        }
        throw IllegalStateException(
            "cancelForTest: no se encontró método clear/clear\$ en la jerarquía de ${javaClass.name}; " +
                "viewModelScope podría seguir activo."
        )
    }

    private companion object {
        const val DEFAULT_LAT = 40.4168
        const val DEFAULT_LON = -3.7038
    }

    private fun sampleColegio(id: Int = 7) = Colegio(
        id = id,
        nombre = "Centro Test",
        direccion = "Calle 1",
        latitud = 40.1,
        longitud = -3.1,
        tipo = "Público",
        localidad = "Madrid",
        telefono = null
    )

    /** Implementación mínima para forzar [Result.failure] (evita stubs MockK frágiles en suspend + defaults). */
    private class FakeColegioRepository(
        private val nearbyResult: Result<List<ColegioConDistancia>>
    ) : ColegioRepository {
        override suspend fun fetchNearbyColegios(
            lat: Double,
            lon: Double,
            limit: Int,
            titularidades: Set<TitularidadFiltro>,
            tipos: Set<TipoCentroFiltro>
        ): Result<List<ColegioConDistancia>> = nearbyResult

        override suspend fun searchColegiosByName(query: String, limit: Int): Result<List<ColegioSearchDto>> =
            Result.success(emptyList())
    }

    private fun sampleColegioConDistancia(id: Int = 7) = ColegioConDistancia(
        colegio = sampleColegio(id),
        distanciaMetros = 250.0,
        tipoCentroClasificado = TipoCentroClasificado.PRIMARIA,
        titularidadNormalizada = "PUBLICO"
    )

    private fun userPrefsWithFlow(
        flow: MutableStateFlow<UserPreferences> = MutableStateFlow(
            UserPreferences(hasDiscoveredLongPress = false, longPressHintCount = 0)
        )
    ): UserPreferencesRepository {
        val repo = mockk<UserPreferencesRepository>(relaxed = true)
        every { repo.userPreferencesFlow } returns flow
        coEvery { repo.updateHasDiscoveredLongPress(any()) } returns Unit
        coEvery { repo.incrementHintCount() } returns Unit
        return repo
    }

    @Test
    fun toggleFavorito_sinFavoritos_anadeId() = runTest(mainDispatcherRule.dispatcher) {
        // Given
        val colegioRepository = mockk<ColegioRepository>()
        coEvery {
            colegioRepository.fetchNearbyColegios(any(), any(), 50, any(), any())
        } returns Result.success(emptyList())
        coEvery { colegioRepository.searchColegiosByName(any(), any()) } returns Result.success(emptyList())
        val viewModel = MapViewModel(colegioRepository, userPrefsWithFlow())
        advanceUntilIdle()
        // When
        viewModel.state.test {
            assertEquals(emptySet<Int>(), awaitItem().favoritosIds)
            viewModel.toggleFavorito(42)
            // Then
            assertEquals(setOf(42), awaitItem().favoritosIds)
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.cancelForTest()
    }

    @Test
    fun toggleFavorito_conId42_quitaFavorito() = runTest(mainDispatcherRule.dispatcher) {
        // Given
        val colegioRepository = mockk<ColegioRepository>()
        coEvery {
            colegioRepository.fetchNearbyColegios(any(), any(), 50, any(), any())
        } returns Result.success(emptyList())
        coEvery { colegioRepository.searchColegiosByName(any(), any()) } returns Result.success(emptyList())
        val viewModel = MapViewModel(colegioRepository, userPrefsWithFlow())
        advanceUntilIdle()
        // When
        viewModel.state.test {
            assertEquals(emptySet<Int>(), awaitItem().favoritosIds)
            viewModel.toggleFavorito(42)
            assertEquals(setOf(42), awaitItem().favoritosIds)
            viewModel.toggleFavorito(42)
            // Then
            assertEquals(emptySet<Int>(), awaitItem().favoritosIds)
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.cancelForTest()
    }

    @Test
    fun toggleFiltroTitularidad_desdeTodos_aPublico_yLlamaFetch() = runTest(mainDispatcherRule.dispatcher) {
        // Given
        val colegioRepository = mockk<ColegioRepository>()
        coEvery {
            colegioRepository.fetchNearbyColegios(any(), any(), 50, any(), any())
        } returns Result.success(emptyList())
        coEvery { colegioRepository.searchColegiosByName(any(), any()) } returns Result.success(emptyList())
        val viewModel = MapViewModel(colegioRepository, userPrefsWithFlow())
        advanceUntilIdle()
        // When
        viewModel.state.test {
            assertEquals(setOf(TitularidadFiltro.TODOS), awaitItem().filtrosTitularidad)
            viewModel.toggleFiltroTitularidad(TitularidadFiltro.PUBLICO)
            // Then
            assertEquals(setOf(TitularidadFiltro.PUBLICO), awaitItem().filtrosTitularidad)
            cancelAndIgnoreRemainingEvents()
        }
        advanceUntilIdle()
        // Then
        coVerify(atLeast = 1) {
            colegioRepository.fetchNearbyColegios(
                DEFAULT_LAT,
                DEFAULT_LON,
                50,
                setOf(TitularidadFiltro.PUBLICO),
                setOf(TipoCentroFiltro.TODOS)
            )
        }
        viewModel.cancelForTest()
    }

    @Test
    fun toggleFiltroTipoCentro_desdeTodos_aPrimaria_yLlamaFetch() = runTest(mainDispatcherRule.dispatcher) {
        // Given
        val colegioRepository = mockk<ColegioRepository>()
        coEvery {
            colegioRepository.fetchNearbyColegios(any(), any(), 50, any(), any())
        } returns Result.success(emptyList())
        coEvery { colegioRepository.searchColegiosByName(any(), any()) } returns Result.success(emptyList())
        val viewModel = MapViewModel(colegioRepository, userPrefsWithFlow())
        advanceUntilIdle()
        // When
        viewModel.state.test {
            assertEquals(setOf(TipoCentroFiltro.TODOS), awaitItem().filtrosTipoCentro)
            viewModel.toggleFiltroTipoCentro(TipoCentroFiltro.PRIMARIA)
            // Then
            assertEquals(setOf(TipoCentroFiltro.PRIMARIA), awaitItem().filtrosTipoCentro)
            cancelAndIgnoreRemainingEvents()
        }
        advanceUntilIdle()
        // Then
        coVerify(atLeast = 1) {
            colegioRepository.fetchNearbyColegios(
                DEFAULT_LAT,
                DEFAULT_LON,
                50,
                setOf(TitularidadFiltro.TODOS),
                setOf(TipoCentroFiltro.PRIMARIA)
            )
        }
        viewModel.cancelForTest()
    }

    @Test
    fun clearError_trasFalloDeCarga_limpiaError() = runTest(mainDispatcherRule.dispatcher) {
        // Given
        val colegioRepository = FakeColegioRepository(Result.failure(Exception("Sin red")))
        val viewModel = MapViewModel(colegioRepository, userPrefsWithFlow())
        advanceUntilIdle()
        // When
        viewModel.loadNearbyColegios(1.0, 2.0)
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.error)
        viewModel.clearError()
        // Then
        viewModel.state.test {
            assertNull(awaitItem().error)
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.cancelForTest()
    }

    @Test
    fun onMapLongClick_actualizaReferencia_limpiaSeleccion_yFetch() = runTest(mainDispatcherRule.dispatcher) {
        // Given
        val colegioRepository = mockk<ColegioRepository>()
        coEvery {
            colegioRepository.fetchNearbyColegios(any(), any(), 50, any(), any())
        } returns Result.success(emptyList())
        coEvery { colegioRepository.searchColegiosByName(any(), any()) } returns Result.success(emptyList())
        val viewModel = MapViewModel(colegioRepository, userPrefsWithFlow())
        advanceUntilIdle()
        val click = LatLng(40.0, -3.0)
        // When
        viewModel.onMapLongClick(click)
        advanceUntilIdle()
        // Then
        viewModel.state.test {
            val s = awaitItem()
            assertEquals(click, s.puntoReferencia)
            assertNull(s.selectedColegioConDistancia)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(atLeast = 1) {
            colegioRepository.fetchNearbyColegios(
                40.0,
                -3.0,
                50,
                setOf(TitularidadFiltro.TODOS),
                setOf(TipoCentroFiltro.TODOS)
            )
        }
        viewModel.cancelForTest()
    }

    @Test
    fun clearSelectedColegio_trasClick_limpiaSeleccion() = runTest(mainDispatcherRule.dispatcher) {
        // Given
        val colegioRepository = mockk<ColegioRepository>()
        coEvery {
            colegioRepository.fetchNearbyColegios(any(), any(), 50, any(), any())
        } returns Result.success(emptyList())
        coEvery { colegioRepository.searchColegiosByName(any(), any()) } returns Result.success(emptyList())
        val viewModel = MapViewModel(colegioRepository, userPrefsWithFlow())
        advanceUntilIdle()
        val seleccion = sampleColegioConDistancia()
        try {
            // When / Then
            viewModel.state.test {
                awaitItem()
                viewModel.onColegioClick(seleccion)
                assertEquals(seleccion, awaitItem().selectedColegioConDistancia)
                viewModel.clearSelectedColegio()
                // Then
                assertNull(awaitItem().selectedColegioConDistancia)
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            viewModel.cancelForTest()
        }
    }

    @Test
    fun loadNearbyColegios_error_exponeMensaje_yDejaDeCargar() = runTest(mainDispatcherRule.dispatcher) {
        // Given
        val colegioRepository = FakeColegioRepository(Result.failure(Exception("Sin red")))
        val viewModel = MapViewModel(colegioRepository, userPrefsWithFlow())
        advanceUntilIdle()
        // When
        viewModel.loadNearbyColegios(10.0, 20.0)
        advanceUntilIdle()
        // Then
        viewModel.state.test {
            val s = awaitItem()
            assertNotNull(s.error)
            assertFalse(s.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.cancelForTest()
    }

    @Test
    fun loadNearbyColegios_exito_actualizaLista_yDejaDeCargar() = runTest(mainDispatcherRule.dispatcher) {
        // Given
        val lista = listOf(sampleColegioConDistancia())
        val colegioRepository = mockk<ColegioRepository>()
        coEvery {
            colegioRepository.fetchNearbyColegios(any(), any(), 50, any(), any())
        } returns Result.success(lista)
        coEvery { colegioRepository.searchColegiosByName(any(), any()) } returns Result.success(emptyList())
        val viewModel = MapViewModel(colegioRepository, userPrefsWithFlow())
        advanceUntilIdle()
        // When
        viewModel.loadNearbyColegios(0.0, 0.0)
        advanceUntilIdle()
        // Then
        viewModel.state.test {
            val s = awaitItem()
            assertEquals(lista, s.colegiosCercanos)
            assertFalse(s.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.cancelForTest()
    }

    @Test
    fun consumeFocusedRequest_dejaFocusedEnNone() = runTest(mainDispatcherRule.dispatcher) {
        // Given
        val colegioRepository = mockk<ColegioRepository>()
        coEvery {
            colegioRepository.fetchNearbyColegios(any(), any(), 50, any(), any())
        } returns Result.success(emptyList())
        coEvery { colegioRepository.searchColegiosByName(any(), any()) } returns Result.success(emptyList())
        val viewModel = MapViewModel(colegioRepository, userPrefsWithFlow())
        advanceUntilIdle()
        viewModel.onMapLongClick(LatLng(41.0, -4.0))
        advanceUntilIdle()
        // When
        viewModel.state.test {
            assertEquals(FocusedRequestType.POINT, awaitItem().focusedRequestType)
            viewModel.consumeFocusedRequest()
            // Then
            assertEquals(FocusedRequestType.NONE, awaitItem().focusedRequestType)
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.cancelForTest()
    }
}
