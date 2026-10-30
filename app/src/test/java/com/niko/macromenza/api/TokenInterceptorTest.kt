package com.niko.macromenza.api

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TokenInterceptorTest {

    private lateinit var server: MockWebServer
    private val klijent = OkHttpClient.Builder().addInterceptor(TokenInterceptor()).build()

    @Before
    fun postavi() {
        server = MockWebServer().apply { start() }
        server.enqueue(MockResponse().setBody("{}"))
    }

    @After
    fun zatvori() {
        runBlocking { TokenStore.obrisi() }
        server.shutdown()
    }

    @Test
    fun zahtjevNosiAccessToken() {
        runBlocking { TokenStore.postavi("pristup-1", "osvjezi-1") }

        klijent.newCall(Request.Builder().url(server.url("/api/jela")).build()).execute().close()

        assertEquals("Bearer pristup-1", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun bezTokenaZahtjevIdeBezZaglavlja() {
        runBlocking { TokenStore.obrisi() }

        klijent.newCall(Request.Builder().url(server.url("/api/ping")).build()).execute().close()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }
}
