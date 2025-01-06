package com.m4ykey.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.m4ykey.data.local.model.SearchResult
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchResultDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSearchResult(searchResult: SearchResult)

    @Query("DELETE FROM search_result_table")
    suspend fun deleteSearchResults()

    @Query("SELECT * FROM search_result_table ORDER BY id DESC")
    fun getSearchResult() : Flow<List<SearchResult>>

}