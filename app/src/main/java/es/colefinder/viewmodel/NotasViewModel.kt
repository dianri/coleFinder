package es.colefinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.colefinder.data.Nota
import es.colefinder.data.Supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotasViewModel : ViewModel() {

    private val _notas = MutableStateFlow<List<Nota>>(emptyList())
    val notas: StateFlow<List<Nota>> = _notas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchNotas()
    }

    fun fetchNotas() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = Supabase.client.postgrest["notas"]
                    .select()
                    .decodeList<Nota>()
                _notas.value = result
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = e.localizedMessage ?: "Error al obtener las notas"
            } finally {
                _isLoading.value = false
            }
        }
    }
}