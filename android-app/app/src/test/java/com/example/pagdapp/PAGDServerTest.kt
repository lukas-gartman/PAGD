package com.example.pagdapp

import com.example.pagdapp.models.dbModels.JwtToken
import com.example.pagdapp.repositories.pagdServer.PAGDServer
import com.example.pagdapp.repositories.pagdServer.ServerApi
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class PAGDServerTest {



    @io.mockk.impl.annotations.MockK
    private lateinit var serverApi: ServerApi

    @io.mockk.impl.annotations.MockK
    private lateinit var mockResponse: retrofit2.Response<JwtToken>

    @Before
    fun setUp()= MockKAnnotations.init(this)


    @Test
    fun testGetToken_successfulResponse() = runTest {
        val expectedToken = "your_token"
        val tokenResponse = JwtToken(expectedToken)

        coEvery { serverApi.register() } returns mockResponse
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body() } returns tokenResponse

        val actualToken = PAGDServer.getToken()

        assertEquals(expectedToken, actualToken)
    }

    @Test
    fun testGetToken_unsuccessfulResponse() = runTest {
        coEvery { serverApi.register() } returns mockResponse
        every { mockResponse.isSuccessful } returns false

        val actualToken = PAGDServer.getToken()

        assertNull(actualToken)
    }

    @Test
    fun testGetToken_exceptionThrown() = runTest {
        coEvery { serverApi.register() } throws RuntimeException("Error")

        val actualToken = PAGDServer.getToken()

        assertNull(actualToken)
    }

}