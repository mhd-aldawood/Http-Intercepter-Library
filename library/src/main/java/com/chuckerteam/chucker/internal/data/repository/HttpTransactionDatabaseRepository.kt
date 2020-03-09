package com.chuckerteam.chucker.internal.data.repository

import androidx.lifecycle.LiveData
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.data.entity.HttpTransactionTuple
import com.chuckerteam.chucker.internal.data.room.ChuckerDatabase
import com.chuckerteam.chucker.internal.support.distinctUntilChanged
import kotlinx.coroutines.*

internal class HttpTransactionDatabaseRepository(private val database: ChuckerDatabase) : HttpTransactionRepository {

    private val backgroundScope = CoroutineScope(Dispatchers.IO)

    private val transactionDao get() = database.transactionDao()

    override fun getFilteredTransactionTuples(code: String, path: String): LiveData<List<HttpTransactionTuple>> {
        val pathQuery = if (path.isNotEmpty()) "%$path%" else "%"
        return transactionDao.getFilteredTuples("$code%", pathQuery)
    }

    override fun getTransaction(transactionId: Long): LiveData<HttpTransaction?> {
        return transactionDao.getById(transactionId)
            .distinctUntilChanged { old, new -> old?.hasTheSameContent(new) != false }
    }

    override fun getSortedTransactionTuples(): LiveData<List<HttpTransactionTuple>> {
        return transactionDao.getSortedTuples()
    }

    override fun deleteAllTransactions() {
        backgroundScope.launch { transactionDao.deleteAll() }
    }

    override fun insertTransaction(transaction: HttpTransaction) {
       backgroundScope.launch {
           val id = transactionDao.insert(transaction)
           transaction.id = id ?: 0
       }
    }

    override fun updateTransaction(transaction: HttpTransaction): Int {
        return transactionDao.update(transaction)
    }

    override fun deleteOldTransactions(threshold: Long) {
         backgroundScope.launch { transactionDao.deleteBefore(threshold) }
    }
}
