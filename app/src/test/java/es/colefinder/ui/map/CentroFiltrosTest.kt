package es.colefinder.ui.map

import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CentroFiltrosTest {

    @Test
    fun clasificarTipoCentro_ceipJuanXxiii_esPrimaria() {
        // Given
        val nombre = "CEIP Juan XXIII"
        // When
        val result = clasificarTipoCentro(nombre, tipo = "", descripcionEntidad = null)
        // Then
        assertEquals(TipoCentroClasificado.PRIMARIA, result)
    }

    @Test
    fun clasificarTipoCentro_iesRamonYCajal_esSecundaria() {
        // Given
        val nombre = "IES Ramón y Cajal"
        // When
        val result = clasificarTipoCentro(nombre, tipo = "", descripcionEntidad = null)
        // Then
        assertEquals(TipoCentroClasificado.SECUNDARIA, result)
    }

    @Test
    fun clasificarTipoCentro_cifpMantenimiento_esFp() {
        // Given
        val nombre = "CIFP Mantenimiento"
        // When
        val result = clasificarTipoCentro(nombre, tipo = "", descripcionEntidad = null)
        // Then
        assertEquals(TipoCentroClasificado.FP, result)
    }

    @Test
    fun clasificarTipoCentro_cepaAdultosSur_esAdultos() {
        // Given
        val nombre = "CEPA Adultos Sur"
        // When
        val result = clasificarTipoCentro(nombre, tipo = "", descripcionEntidad = null)
        // Then
        assertEquals(TipoCentroClasificado.ADULTOS, result)
    }

    @Test
    fun clasificarTipoCentro_ceeCentroEspecial_esEspecial() {
        // Given
        val nombre = "CEE Centro Especial"
        // When
        val result = clasificarTipoCentro(nombre, tipo = "", descripcionEntidad = null)
        // Then
        assertEquals(TipoCentroClasificado.ESPECIAL, result)
    }

    @Test
    fun clasificarTipoCentro_colegioPrivadoAbc_esOtros() {
        // Given
        val nombre = "Colegio Privado ABC"
        // When
        val result = clasificarTipoCentro(nombre, tipo = "", descripcionEntidad = null)
        // Then
        assertEquals(TipoCentroClasificado.OTROS, result)
    }

    @Test
    fun clasificarTipoCentro_tipoCentroNormalizadoPrimaria_tienePrioridadSobreNombreIes() {
        // Given
        val nombre = "IES Nombre Engañoso"
        // When
        val result = clasificarTipoCentro(
            nombre,
            tipo = "",
            descripcionEntidad = null,
            tipoCentroNormalizado = "PRIMARIA"
        )
        // Then
        assertEquals(TipoCentroClasificado.PRIMARIA, result)
    }

    @Test
    fun clasificarTipoCentro_descripcionSecundariaYBachillerato_esSecundaria() {
        // Given
        val descripcion = "Centro de Educación Secundaria y Bachillerato"
        // When
        val result = clasificarTipoCentro(
            nombre = "Centro genérico",
            tipo = "",
            descripcionEntidad = descripcion
        )
        // Then
        assertEquals(TipoCentroClasificado.SECUNDARIA, result)
    }

    @Test
    fun clasificarTipoCentro_descripcionConEmail_seIgnoraYclasificaPorNombre() {
        // Given
        val descripcion = "contacto@centro.edu (más de diez chars)"
        val nombre = "IES Ignora Email"
        // When
        val result = clasificarTipoCentro(nombre, tipo = "", descripcionEntidad = descripcion)
        // Then
        assertEquals(TipoCentroClasificado.SECUNDARIA, result)
    }

    @Test
    fun toggleTitularidad_pulsarTodos_desdeCualquierEstado_soloTodos() {
        // Given
        val current = setOf(TitularidadFiltro.PUBLICO, TitularidadFiltro.CONCERTADO)
        // When
        val result = toggleTitularidad(current, TitularidadFiltro.TODOS)
        // Then
        assertEquals(setOf(TitularidadFiltro.TODOS), result)
    }

    @Test
    fun toggleTitularidad_conTodos_pulsarPublico_soloPublico() {
        // Given
        val current = setOf(TitularidadFiltro.TODOS)
        // When
        val result = toggleTitularidad(current, TitularidadFiltro.PUBLICO)
        // Then
        assertEquals(setOf(TitularidadFiltro.PUBLICO), result)
    }

    @Test
    fun toggleTitularidad_conPublico_pulsarPublico_vuelveATodos() {
        // Given
        val current = setOf(TitularidadFiltro.PUBLICO)
        // When
        val result = toggleTitularidad(current, TitularidadFiltro.PUBLICO)
        // Then
        assertEquals(setOf(TitularidadFiltro.TODOS), result)
    }

    @Test
    fun toggleTitularidad_conPublico_pulsarConcertado_publicoYConcertado() {
        // Given
        val current = setOf(TitularidadFiltro.PUBLICO)
        // When
        val result = toggleTitularidad(current, TitularidadFiltro.CONCERTADO)
        // Then
        assertEquals(setOf(TitularidadFiltro.PUBLICO, TitularidadFiltro.CONCERTADO), result)
    }

    @Test
    fun toggleTipoCentro_pulsarTodos_desdeCualquierEstado_soloTodos() {
        // Given
        val current = setOf(TipoCentroFiltro.PRIMARIA, TipoCentroFiltro.SECUNDARIA)
        // When
        val result = toggleTipoCentro(current, TipoCentroFiltro.TODOS)
        // Then
        assertEquals(setOf(TipoCentroFiltro.TODOS), result)
    }

    @Test
    fun toggleTipoCentro_conTodos_pulsarPrimaria_soloPrimaria() {
        // Given
        val current = setOf(TipoCentroFiltro.TODOS)
        // When
        val result = toggleTipoCentro(current, TipoCentroFiltro.PRIMARIA)
        // Then
        assertEquals(setOf(TipoCentroFiltro.PRIMARIA), result)
    }

    @Test
    fun toggleTipoCentro_conPrimaria_pulsarPrimaria_vuelveATodos() {
        // Given
        val current = setOf(TipoCentroFiltro.PRIMARIA)
        // When
        val result = toggleTipoCentro(current, TipoCentroFiltro.PRIMARIA)
        // Then
        assertEquals(setOf(TipoCentroFiltro.TODOS), result)
    }

    @Test
    fun toggleTipoCentro_conPrimaria_pulsarSecundaria_primariaYSecundaria() {
        // Given
        val current = setOf(TipoCentroFiltro.PRIMARIA)
        // When
        val result = toggleTipoCentro(current, TipoCentroFiltro.SECUNDARIA)
        // Then
        assertEquals(setOf(TipoCentroFiltro.PRIMARIA, TipoCentroFiltro.SECUNDARIA), result)
    }

    @Test
    fun matchesTitularidadNormalizadaFiltros_publicoConFiltroPublico_true() {
        // Given
        val filtros = setOf(TitularidadFiltro.PUBLICO)
        // When
        val result = matchesTitularidadNormalizadaFiltros("PUBLICO", tipoRaw = "", filtros)
        // Then
        assertTrue(result)
    }

    @Test
    fun matchesTitularidadNormalizadaFiltros_privadoConFiltroPublico_false() {
        // Given
        val filtros = setOf(TitularidadFiltro.PUBLICO)
        // When
        val result = matchesTitularidadNormalizadaFiltros("PRIVADO", tipoRaw = "", filtros)
        // Then
        assertFalse(result)
    }

    @Test
    fun matchesTitularidadNormalizadaFiltros_nullNorm_conTipoPublico_fallback_true() {
        // Given
        val filtros = setOf(TitularidadFiltro.PUBLICO)
        // When
        val result = matchesTitularidadNormalizadaFiltros(null, tipoRaw = "Público", filtros)
        // Then
        assertTrue(result)
    }

    @Test
    fun matchesTitularidadNormalizadaFiltros_filtrosTodos_siempreTrue() {
        // Given
        val filtros = setOf(TitularidadFiltro.TODOS)
        // When
        val r1 = matchesTitularidadNormalizadaFiltros("PRIVADO", tipoRaw = "", filtros)
        val r2 = matchesTitularidadNormalizadaFiltros(null, tipoRaw = "cualquier cosa", filtros)
        // Then
        assertTrue(r1)
        assertTrue(r2)
    }

    @Test
    fun tipoCentroClasificado_matchesFiltros_todos_true() {
        // Given
        val filtros = setOf(TipoCentroFiltro.TODOS)
        // When
        val result = TipoCentroClasificado.PRIMARIA.matchesFiltros(filtros)
        // Then
        assertTrue(result)
    }

    @Test
    fun tipoCentroClasificado_matchesFiltros_soloSecundaria_falseParaPrimaria() {
        // Given
        val filtros = setOf(TipoCentroFiltro.SECUNDARIA)
        // When
        val result = TipoCentroClasificado.PRIMARIA.matchesFiltros(filtros)
        // Then
        assertFalse(result)
    }

    @Test
    fun tipoCentroClasificado_matchesFiltros_primariaYSecundaria_trueParaPrimaria() {
        // Given
        val filtros = setOf(TipoCentroFiltro.PRIMARIA, TipoCentroFiltro.SECUNDARIA)
        // When
        val result = TipoCentroClasificado.PRIMARIA.matchesFiltros(filtros)
        // Then
        assertTrue(result)
    }

    @Test
    fun titularidadFiltroSet_toRpcArray_todos_esNull() {
        // Given
        val set = setOf(TitularidadFiltro.TODOS)
        // When
        val result = set.toRpcArray()
        // Then
        assertNull(result)
    }

    @Test
    fun titularidadFiltroSet_toRpcArray_publicoYConcertado_jsonArrayExacto() {
        // Given
        val set = setOf(TitularidadFiltro.PUBLICO, TitularidadFiltro.CONCERTADO)
        // When
        val result = set.toRpcArray()
        // Then
        assertNotNull(result)
        val strings = result!!.map { it.jsonPrimitive.content }
        assertEquals(listOf("PUBLICO", "CONCERTADO"), strings)
    }
}
