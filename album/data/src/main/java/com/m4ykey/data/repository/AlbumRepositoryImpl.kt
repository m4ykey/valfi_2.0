package com.m4ykey.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.m4ykey.core.Constants.PAGE_SIZE
import com.m4ykey.core.network.Resource
import com.m4ykey.core.network.safeApiCall
import com.m4ykey.data.domain.model.album.AlbumDetail
import com.m4ykey.data.domain.model.album.AlbumItem
import com.m4ykey.data.domain.model.track.TrackItem
import com.m4ykey.data.domain.repository.AlbumRepository
import com.m4ykey.data.mapper.toAlbumDetail
import com.m4ykey.data.remote.api.AlbumApi
import com.m4ykey.data.remote.interceptor.SpotifyTokenProvider
import com.m4ykey.data.remote.paging.NewReleasePagingSource
import com.m4ykey.data.remote.paging.SearchAlbumPagingSource
import com.m4ykey.data.remote.paging.TrackListPagingSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AlbumRepositoryImpl @Inject constructor(
    private val api: AlbumApi,
    private val interceptor: SpotifyTokenProvider
) : AlbumRepository {

    private val pagingConfig = PagingConfig(
        pageSize = PAGE_SIZE,
        enablePlaceholders = false
    )

    override fun getNewReleases(): Flow<PagingData<AlbumItem>> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = {
                NewReleasePagingSource(
                    api = api,
                    token = interceptor
                )
            }
        ).flow
    }

    override fun searchAlbums(query: String): Flow<PagingData<AlbumItem>> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = {
                SearchAlbumPagingSource(
                    api = api,
                    token = interceptor,
                    query = query
                )
            }
        ).flow
    }

    override suspend fun getAlbumById(id: String): Flow<Resource<AlbumDetail>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall {
            api.getAlbumById(
                token = "Bearer ${interceptor.getAccessToken()}",
                id = id
            ).toAlbumDetail()
        })
    }

    override fun getAlbumTracks(id: String): Flow<PagingData<TrackItem>> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = {
                TrackListPagingSource(
                    id = id,
                    interceptor = interceptor,
                    api = api
                )
            }
        ).flow
    }
}