package com.chuckerteam.chucker.api

import com.chuckerteam.chucker.ChuckerInterceptorDelegate
import com.chuckerteam.chucker.getResourceFile
import com.chuckerteam.chucker.readByteStringBody
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.ByteString
import okio.GzipSink
import org.junit.Rule
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ChuckerInterceptorTest {
    enum class ClientFactory {
        REGULAR {
            override fun create(interceptor: Interceptor): OkHttpClient {
                return OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build()
            }
        },
        NETWORK {
            override fun create(interceptor: Interceptor): OkHttpClient {
                return OkHttpClient.Builder()
                    .addNetworkInterceptor(interceptor)
                    .build()
            }
        };

        abstract fun create(interceptor: Interceptor): OkHttpClient
    }

    @get:Rule val server = MockWebServer()
    private val serverUrl = server.url("/") // Starts server implicitly

    private val chucker = ChuckerInterceptorDelegate()

    @ParameterizedTest
    @EnumSource(value = ClientFactory::class)
    fun imageResponse_isAvailableToChucker(factory: ClientFactory) {
        val image = getResourceFile("sample_image.png")
        server.enqueue(MockResponse().addHeader("Content-Type:image/jpeg").setBody(image))
        val request = Request.Builder().url(serverUrl).build()
        val expectedBody = image.snapshot()

        val client = factory.create(chucker)
        client.newCall(request).execute()

        assertEquals(expectedBody, ByteString.of(*chucker.expectTransaction().responseImageData!!))
    }

    @ParameterizedTest
    @EnumSource(value = ClientFactory::class)
    fun imageResponse_isAvailableToTheEndConsumer(factory: ClientFactory) {
        val image = getResourceFile("sample_image.png")
        server.enqueue(MockResponse().addHeader("Content-Type:image/jpeg").setBody(image))
        val request = Request.Builder().url(serverUrl).build()
        val expectedBody = image.snapshot()

        val client = factory.create(chucker)
        val responseBody = client.newCall(request).execute().readByteStringBody()!!

        assertEquals(expectedBody, responseBody)
    }

    @ParameterizedTest
    @EnumSource(value = ClientFactory::class)
    fun gzippedBody_isGunzippedForChucker(factory: ClientFactory) {
        val bytes = Buffer().apply { writeUtf8("Hello, world!") }
        val gzippedBytes = Buffer().apply {
            GzipSink(this).use { sink -> sink.write(bytes, bytes.size()) }
        }
        server.enqueue(MockResponse().addHeader("Content-Encoding: gzip").setBody(gzippedBytes))
        val request = Request.Builder().url(serverUrl).build()

        val client = factory.create(chucker)
        client.newCall(request).execute()

        val transaction = chucker.expectTransaction()
        assertTrue(transaction.isResponseBodyPlainText)
        assertEquals("Hello, world!", transaction.responseBody)
    }

    @ParameterizedTest
    @EnumSource(value = ClientFactory::class)
    fun gzippedBody_isGunzippedForTheEndConsumer(factory: ClientFactory) {
        val bytes = Buffer().apply { writeUtf8("Hello, world!") }
        val gzippedBytes = Buffer().apply {
            GzipSink(this).use { sink -> sink.write(bytes, bytes.size()) }
        }
        server.enqueue(MockResponse().addHeader("Content-Encoding: gzip").setBody(gzippedBytes))
        val request = Request.Builder().url(serverUrl).build()

        val client = factory.create(chucker)
        val responseBody = client.newCall(request).execute().readByteStringBody()!!

        assertEquals("Hello, world!", responseBody.utf8())
    }

    @ParameterizedTest
    @EnumSource(value = ClientFactory::class)
    fun regularBody_isAvailableForChucker(factory: ClientFactory) {
        val body = Buffer().apply { writeUtf8("Hello, world!") }
        server.enqueue(MockResponse().setBody(body))
        val request = Request.Builder().url(serverUrl).build()

        val client = factory.create(chucker)
        client.newCall(request).execute()

        val transaction = chucker.expectTransaction()
        assertTrue(transaction.isResponseBodyPlainText)
        assertEquals("Hello, world!", transaction.responseBody)
    }

    @ParameterizedTest
    @EnumSource(value = ClientFactory::class)
    fun regularBody_isAvailableForTheEndConsumer(factory: ClientFactory) {
        val body = Buffer().apply { writeUtf8("Hello, world!") }
        server.enqueue(MockResponse().setBody(body))
        val request = Request.Builder().url(serverUrl).build()

        val client = factory.create(chucker)
        val responseBody = client.newCall(request).execute().readByteStringBody()!!

        assertEquals("Hello, world!", responseBody.utf8())
    }
}
