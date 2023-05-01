package com.example.pagdapp.data.repository

import com.example.pagdapp.data.model.dbModels.Gun
import com.example.pagdapp.data.model.dbModels.Gunshot
import com.example.pagdapp.data.model.dbModels.Report
import com.example.pagdapp.data.model.networkModels.GunNetworkModel
import com.example.pagdapp.data.model.networkModels.ReportNetworkModel
import com.example.pagdapp.data.remote.TokenProvider
import com.example.pagdapp.data.remote.api.IPAGDApi
import com.example.pagdapp.utils.Constants
import com.example.pagdapp.utils.NetworkCallHelper
import com.example.pagdapp.utils.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class PAGDRepository @Inject constructor(
    private val pagdApi: IPAGDApi,
    private val tokenProvider: TokenProvider
) : IPAGDRepository {


    private suspend fun checkToken() {
        tokenProvider.isTokenValid()
    }

    override suspend fun helloWorld(): Flow<NetworkResult<String>> {
        return NetworkCallHelper
            .apiCallContinuous(
                { pagdApi.helloWorld() },
                Constants.CONTINUOUS_NETWORK_CALL_MIDDLE
            ).flowOn(Dispatchers.IO)
    }


    override suspend fun addGunToDB(gun: Gun): Flow<NetworkResult<String>> {
        val gunCall = GunNetworkModel(gun.name, gun.type)
        return NetworkCallHelper.apiCall(
            apiCall = { pagdApi.addGunToDB(tokenProvider.getValidToken(), gunCall) },
            callBackError = { tokenProvider.getToken() }
        ).map { result ->
            when (result) {
                is NetworkResult.Success -> {
                    NetworkResult.Success(result.code ?: 200, result.data.toString())
                }
                is NetworkResult.Error -> {
                    NetworkResult.Error(
                        result.code ?: 0,
                        result.message ?: "",
                        result.data?.toString()
                    )
                }
                is NetworkResult.Loading -> {
                    NetworkResult.Loading()
                }
            }
        }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getGun(gun_name: String?): Flow<NetworkResult<List<Gun>>> {
        return NetworkCallHelper.apiCall(
            apiCall = { pagdApi.getGun(tokenProvider.getValidToken(), gun_name) },
            callBackError = { tokenProvider.getToken() },
            delay = 1000L
        ).flowOn(Dispatchers.IO)
    }


    override suspend fun addReport(report: Report): Flow<NetworkResult<String>> {
        return NetworkCallHelper.apiCall(
            apiCall = { pagdApi.addReport(tokenProvider.getValidToken(), report) },
            callBackError = { tokenProvider.getToken() }
        ).map { result ->
            when (result) {
                is NetworkResult.Success -> {
                    NetworkResult.Success(result.code ?: 200, result.data.toString())
                }
                is NetworkResult.Error -> {
                    NetworkResult.Error(
                        result.code ?: 0,
                        result.message ?: "",
                        result.data?.toString()
                    )
                }
                is NetworkResult.Loading -> {
                    NetworkResult.Loading()
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getReport(
        id: Int?,
        time_from: Long?,
        time_to: Long?
    ): Flow<NetworkResult<List<ReportNetworkModel>>> {
        return NetworkCallHelper.apiCall(
            apiCall = { pagdApi.getReport(tokenProvider.getValidToken(), id, time_from, time_to) },
            callBackError = { tokenProvider.getToken() },
            delay = 1000L

        ).flowOn(Dispatchers.IO)
            .catch { e -> e.printStackTrace() }
    }

    override suspend fun getGunshot(
        gunshotId: Int?,
        timeFrom: Long,
        timeTo: Long
    ): Flow<NetworkResult<List<Gunshot>>> = flow {
        val result = NetworkCallHelper.simpleApiCall {
            pagdApi.getGunshot(tokenProvider.getValidToken(), gunshotId, timeFrom, timeTo)
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

    override suspend fun getLatestGunshot(): Flow<NetworkResult<Gunshot>> = flow {
        val result =
            NetworkCallHelper.simpleApiCall { pagdApi.getLatestGunshot(tokenProvider.getValidToken()) }
        emit(result)
    }.flowOn(Dispatchers.IO)


    override val getAllGuns: Flow<NetworkResult<List<Gun>>> = flow {
        val result =
            NetworkCallHelper.simpleApiCall { pagdApi.getGun(tokenProvider.getValidToken(), null) }
        emit(result)
    }.flowOn(Dispatchers.IO)


}