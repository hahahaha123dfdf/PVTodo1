package com.yg.pvtodo.papers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yg.pvtodo.llm.LlmConfigStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PapersViewModel(
    private val repo: PaperRepository,
    val llmStore: LlmConfigStore
) : ViewModel() {
    val papers: StateFlow<List<PaperEntity>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val llmConfig = llmStore.config

    fun toggleFav(p: PaperEntity) {
        viewModelScope.launch { repo.setFav(p.doi, !p.fav) }
    }

    init { refresh() }
    fun refresh() {
        if (_loading.value) return
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try { repo.refresh() } catch (e: Exception) { _error.value = e.message }
            _loading.value = false
        }
    }
}
