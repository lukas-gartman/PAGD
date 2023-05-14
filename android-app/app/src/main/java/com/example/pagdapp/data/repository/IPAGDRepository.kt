package com.example.pagdapp.data.repository

import com.example.pagdapp.data.model.dbModels.Gun
import com.example.pagdapp.data.model.dbModels.Gunshot
import com.example.pagdapp.data.model.dbModels.Report
import com.example.pagdapp.data.model.networkModels.GunshotNetworkModel
import com.example.pagdapp.data.model.networkModels.ReportNetworkModel
import com.example.pagdapp.utils.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface IPAGDRepository {

    suspend fun helloWorld(scope: CoroutineScope): Flow<NetworkResult<String>>

    suspend fun addGunToDB(gun: Gun): Flow<NetworkResult<String>>

    suspend fun getGun(gun_name: String?): Flow<NetworkResult<List<Gun>>>

    suspend fun addReport(report: Report): Flow<NetworkResult<String>>

    suspend fun getReport(
        id: Int?, time_from: Long?, time_to: Long?
    ): Flow<NetworkResult<List<ReportNetworkModel>>>

    suspend fun getGunshot(
        gunshotId: Int?,
        timeFrom: Long,
        timeTo: Long
    ): Flow<NetworkResult<List<Gunshot>>>

    suspend fun getLatestGunshot(): Flow<NetworkResult<Gunshot>>

    suspend fun getGunshotContinuously(
        scope: CoroutineScope,
        timeFrom: Long,
        timeTo: Long,
        delay: Long
    ): Flow<NetworkResult<List<Gunshot>>>

    suspend fun getGunshotSimple(
        timeFrom: Long,
        timeTo: Long
    ): NetworkResult<List<Gunshot>>

    suspend fun addGunshot(gunshot: GunshotNetworkModel): Flow<NetworkResult<String>>

    val getAllGuns: Flow<NetworkResult<List<Gun>>>

}