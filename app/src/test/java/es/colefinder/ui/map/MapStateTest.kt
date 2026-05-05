package es.colefinder.ui.map

import es.colefinder.data.model.Colegio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapStateTest {

    private fun colegioBase(
        id: Int,
        tipo: String,
        titularidadNormalizada: String?,
        tipoCentroClasificado: TipoCentroClasificado
    ): ColegioConDistancia = ColegioConDistancia(
        colegio = Colegio(
            id = id,
            nombre = "Centro $id",
            direccion = "Calle $id",
            latitud = 40.0 + id * 0.01,
            longitud = -3.0,
            tipo = tipo,
            localidad = "Loc",
            telefono = null
        ),
        distanciaMetros = 100.0 * id,
        tipoCentroClasificado = tipoCentroClasificado,
        titularidadNormalizada = titularidadNormalizada
    )

    @Test
    fun colegiosConDistancia_filtrosTodos_devuelveTodos() {
        // Given
        val lista = listOf(
            colegioBase(1, "Público", "PUBLICO", TipoCentroClasificado.PRIMARIA),
            colegioBase(2, "Concertado", "CONCERTADO", TipoCentroClasificado.SECUNDARIA),
            colegioBase(3, "Privado", "PRIVADO", TipoCentroClasificado.FP)
        )
        val state = MapState(
            colegiosCercanos = lista,
            filtrosTitularidad = setOf(TitularidadFiltro.TODOS),
            filtrosTipoCentro = setOf(TipoCentroFiltro.TODOS)
        )
        // When
        val result = state.colegiosConDistancia
        // Then
        assertEquals(lista, result)
    }

    @Test
    fun colegiosConDistancia_filtroPublico_soloPublicoNormalizado() {
        // Given
        val publico = colegioBase(1, "Público", "PUBLICO", TipoCentroClasificado.PRIMARIA)
        val concertado = colegioBase(2, "Concertado", "CONCERTADO", TipoCentroClasificado.PRIMARIA)
        val privado = colegioBase(3, "Privado", "PRIVADO", TipoCentroClasificado.PRIMARIA)
        val lista = listOf(publico, concertado, privado)
        val state = MapState(
            colegiosCercanos = lista,
            filtrosTitularidad = setOf(TitularidadFiltro.PUBLICO),
            filtrosTipoCentro = setOf(TipoCentroFiltro.TODOS)
        )
        // When
        val result = state.colegiosConDistancia
        // Then
        assertEquals(listOf(publico), result)
    }

    @Test
    fun colegiosConDistancia_filtroPrimaria_soloClasificacionPrimaria() {
        // Given
        val primaria = colegioBase(1, "Público", "PUBLICO", TipoCentroClasificado.PRIMARIA)
        val secundaria = colegioBase(2, "Público", "PUBLICO", TipoCentroClasificado.SECUNDARIA)
        val lista = listOf(primaria, secundaria)
        val state = MapState(
            colegiosCercanos = lista,
            filtrosTitularidad = setOf(TitularidadFiltro.TODOS),
            filtrosTipoCentro = setOf(TipoCentroFiltro.PRIMARIA)
        )
        // When
        val result = state.colegiosConDistancia
        // Then
        assertEquals(listOf(primaria), result)
    }

    @Test
    fun colegiosConDistancia_publicoYSecundaria_soloAmbasCondiciones() {
        // Given
        val ambos = colegioBase(1, "Público", "PUBLICO", TipoCentroClasificado.SECUNDARIA)
        val publicoPrimaria = colegioBase(2, "Público", "PUBLICO", TipoCentroClasificado.PRIMARIA)
        val concertadoSecundaria = colegioBase(3, "Concertado", "CONCERTADO", TipoCentroClasificado.SECUNDARIA)
        val lista = listOf(ambos, publicoPrimaria, concertadoSecundaria)
        val state = MapState(
            colegiosCercanos = lista,
            filtrosTitularidad = setOf(TitularidadFiltro.PUBLICO),
            filtrosTipoCentro = setOf(TipoCentroFiltro.SECUNDARIA)
        )
        // When
        val result = state.colegiosConDistancia
        // Then
        assertEquals(listOf(ambos), result)
    }

    @Test
    fun colegiosConDistancia_listaVacia_devuelveVacia() {
        // Given
        val state = MapState(
            colegiosCercanos = emptyList(),
            filtrosTitularidad = setOf(TitularidadFiltro.PUBLICO),
            filtrosTipoCentro = setOf(TipoCentroFiltro.PRIMARIA)
        )
        // When
        val result = state.colegiosConDistancia
        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun colegiosConDistancia_titularidadNull_tipoPublico_fallbackIncluyeEnFiltroPublico() {
        // Given
        val item = colegioBase(
            id = 1,
            tipo = "Público",
            titularidadNormalizada = null,
            tipoCentroClasificado = TipoCentroClasificado.PRIMARIA
        )
        val state = MapState(
            colegiosCercanos = listOf(item),
            filtrosTitularidad = setOf(TitularidadFiltro.PUBLICO),
            filtrosTipoCentro = setOf(TipoCentroFiltro.TODOS)
        )
        // When
        val result = state.colegiosConDistancia
        // Then
        assertEquals(listOf(item), result)
    }
}
