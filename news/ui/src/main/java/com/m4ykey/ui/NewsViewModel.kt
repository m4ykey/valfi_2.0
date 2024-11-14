package com.m4ykey.ui

import androidx.lifecycle.viewModelScope
import com.m4ykey.core.Constants.PAGE_SIZE
import com.m4ykey.core.views.BaseViewModel
import com.m4ykey.data.domain.model.Article
import com.m4ykey.data.domain.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository
) : BaseViewModel() {

    private var _news = MutableStateFlow<List<Article>>(emptyList())
    val news : StateFlow<List<Article>> get() = _news

    private var page = 1
    var isPaginationEnded = false

    fun getMusicNews(clearList : Boolean, sortBy : String) = viewModelScope.launch {
        if (_isLoading.value || isPaginationEnded) return@launch

        if (clearList) {
            _news.value = emptyList()
            page = 1
            isPaginationEnded = false
        }

        _isLoading.value = true
        _error.value = null
        try {
            repository.getMusicNews(page = page, pageSize = PAGE_SIZE, sortBy = sortBy)
                .collect { articles ->
                    if (articles.isNotEmpty()) {
                        _news.value += articles
                        page++
                    } else {
                        isPaginationEnded = true
                    }
                }
        } catch (e : Exception) {
            _error.value = e.message ?: "An unknown error occurred"
        } finally {
            _isLoading.value = false
        }
    }
}