package com.chuckerteam.chucker.internal.data.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.data.entity.HttpTransactionTuple

@Dao
internal interface HttpTransactionDao {

    @Query(
        "SELECT id, requestDate, tookMs, protocol, method, host, " +
            "path, scheme, responseCode, requestPayloadSize, responsePayloadSize, error FROM " +
            "transactions ORDER BY requestDate DESC"
    )
    fun getSortedTuples(): LiveData<List<HttpTransactionTuple>>

    @RawQuery(observedEntities = [HttpTransaction::class])
    fun getFilteredTuples(
        query: SupportSQLiteQuery
    ): LiveData<List<HttpTransactionTuple>>

    @Insert
    suspend fun insert(transaction: HttpTransaction): Long?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(transaction: HttpTransaction): Int

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getById(id: Long): LiveData<HttpTransaction?>

    @Query("DELETE FROM transactions WHERE requestDate <= :threshold")
    suspend fun deleteBefore(threshold: Long)

    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<HttpTransaction>
}
