package com.example.pagdapp

import com.example.pagdapp.data.model.dbModels.JwtToken
import com.example.pagdapp.data.remote.api.IPAGDApi
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class PAGDServiceTest {



    @io.mockk.impl.annotations.MockK
    private lateinit var IPagdApi: IPAGDApi

    @io.mockk.impl.annotations.MockK
    private lateinit var mockResponse: retrofit2.Response<JwtToken>

    @Before
    fun setUp()= MockKAnnotations.init(this)


    @Test
    fun testGetToken_successfulResponse() = runTest {
        val expectedToken = "your_token"
        val tokenResponse = JwtToken(expectedToken)

        coEvery { IPagdApi.register() } returns mockResponse
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body() } returns tokenResponse

        val actualToken = PAGDService.getToken()

        assertEquals(expectedToken, actualToken)
    }

    @Test
    fun testGetToken_unsuccessfulResponse() = runTest {
        coEvery { IPagdApi.register() } returns mockResponse
        every { mockResponse.isSuccessful } returns false

        val actualToken = PAGDService.getToken()

        assertNull(actualToken)
    }

    @Test
    fun testGetToken_exceptionThrown() = runTest {
        coEvery { IPagdApi.register() } throws RuntimeException("Error")

        val actualToken = PAGDService.getToken()

        assertNull(actualToken)
    }

}