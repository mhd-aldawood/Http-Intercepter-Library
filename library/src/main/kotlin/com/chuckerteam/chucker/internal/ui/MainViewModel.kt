package com.chuckerteam.chucker.internal.ui

import androidx.lifecycle.*
import com.chuckerteam.chucker.internal.data.entity.Transaction
import com.chuckerteam.chucker.internal.data.repository.RepositoryProvider
import com.chuckerteam.chucker.internal.support.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal class MainViewModel : ViewModel() {

    private val searchQueryFlow = MutableSharedFlow<String>()

    val transactions = searchQueryFlow.flatMapLatest { searchQuery ->
        return@flatMapLatest when {
            searchQuery.isBlank() -> {
               RepositoryProvider.transaction().getSortedTransactions()
            }
            else -> {
                RepositoryProvider.transaction().getFilteredTransactions(searchQuery)
            }
        }
    }

    suspend fun getAllTransactions(): List<Transaction> =
        RepositoryProvider.transaction().getAllTransactions()

    fun updateItemsFilter(searchQuery: String) {
        viewModelScope.launch {
            searchQueryFlow.emit(searchQuery)
        }
    }

    fun clearTransactions() {
        viewModelScope.launch {
            RepositoryProvider.transaction().deleteAllTransactions()
        }
        NotificationHelper.clearBuffer()
    }
}
