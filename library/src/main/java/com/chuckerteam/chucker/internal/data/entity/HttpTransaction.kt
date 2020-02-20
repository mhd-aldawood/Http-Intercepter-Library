@file:Suppress("TooManyFunctions")

package com.chuckerteam.chucker.internal.data.entity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.chuckerteam.chucker.internal.support.FormatUtils
import com.chuckerteam.chucker.internal.support.JsonConverter
import com.google.gson.reflect.TypeToken
import java.util.Date
import okhttp3.Headers
import okhttp3.HttpUrl

/**
 * Represent a full HTTP transaction (with Request and Response). Instances of this classes
 * should be populated as soon as the library receives data from OkHttp.
 */
@Entity(tableName = "transactions")
internal class HttpTransaction(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id")
    var id: Long = 0,
    @ColumnInfo(name = "requestDate") var requestDate: Long?,
    @ColumnInfo(name = "responseDate") var responseDate: Long?,
    @ColumnInfo(name = "tookMs") var tookMs: Long?,
    @ColumnInfo(name = "protocol") var protocol: String?,
    @ColumnInfo(name = "method") var method: String?,
    @ColumnInfo(name = "url") var url: String?,
    @ColumnInfo(name = "host") var host: String?,
    @ColumnInfo(name = "path") var path: String?,
    @ColumnInfo(name = "scheme") var scheme: String?,
    @ColumnInfo(name = "requestContentLength") var requestContentLength: Long?,
    @ColumnInfo(name = "requestContentType") var requestContentType: String?,
    @ColumnInfo(name = "requestHeaders") var requestHeaders: String?,
    @ColumnInfo(name = "requestBody") var requestBody: String?,
    @ColumnInfo(name = "isRequestBodyPlainText") var isRequestBodyPlainText: Boolean = true,
    @ColumnInfo(name = "responseCode") var responseCode: Int?,
    @ColumnInfo(name = "responseMessage") var responseMessage: String?,
    @ColumnInfo(name = "error") var error: String?,
    @ColumnInfo(name = "responseContentLength") var responseContentLength: Long?,
    @ColumnInfo(name = "responseContentType") var responseContentType: String?,
    @ColumnInfo(name = "responseHeaders") var responseHeaders: String?,
    @ColumnInfo(name = "responseBody") var responseBody: String?,
    @ColumnInfo(name = "isResponseBodyPlainText") var isResponseBodyPlainText: Boolean = true,
    @ColumnInfo(name = "responseImageData") var responseImageData: ByteArray?

) {

    @Ignore
    constructor() : this(
        requestDate = null,
        responseDate = null,
        tookMs = null,
        protocol = null,
        method = null,
        url = null,
        host = null,
        path = null,
        scheme = null,
        requestContentLength = null,
        requestContentType = null,
        requestHeaders = null,
        requestBody = null,
        responseCode = null,
        responseMessage = null,
        error = null,
        responseContentLength = null,
        responseContentType = null,
        responseHeaders = null,
        responseBody = null,
        responseImageData = null
    )

    enum class Status {
        Requested,
        Complete,
        Failed
    }

    val status: Status
        get() = when {
            error != null -> Status.Failed
            responseCode == null -> Status.Requested
            else -> Status.Complete
        }

    val requestDateString: String?
        get() = requestDate?.let { Date(it).toString() }

    val responseDateString: String?
        get() = responseDate?.let { Date(it).toString() }

    val durationString: String?
        get() = tookMs?.let { "$it ms" }

    val requestSizeString: String
        get() = formatBytes(requestContentLength ?: 0)

    val responseSizeString: String?
        get() = responseContentLength?.let { formatBytes(it) }

    val totalSizeString: String
        get() {
            val reqBytes = requestContentLength ?: 0
            val resBytes = responseContentLength ?: 0
            return formatBytes(reqBytes + resBytes)
        }

    val responseSummaryText: String?
        get() {
            return when (status) {
                Status.Failed -> error
                Status.Requested -> null
                else -> "$responseCode $responseMessage"
            }
        }

    val notificationText: String
        get() {
            return when (status) {
                Status.Failed -> " ! ! !  $method $path"
                Status.Requested -> " . . .  $method $path"
                else -> "$responseCode $method $path"
            }
        }

    val isSsl: Boolean
        get() = scheme.equals("https", ignoreCase = true)

    val responseImageBitmap: Bitmap?
        get() {
            return responseImageData?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }
        }

    fun setRequestHeaders(headers: Headers) {
        setRequestHeaders(toHttpHeaderList(headers))
    }

    fun setRequestHeaders(headers: List<HttpHeader>) {
        requestHeaders = JsonConverter.instance.toJson(headers)
    }

    fun getParsedRequestHeaders(): List<HttpHeader>? {
        return JsonConverter.instance.fromJson<List<HttpHeader>>(
            requestHeaders,
            object : TypeToken<List<HttpHeader>>() {
            }.type
        )
    }

    fun getParsedResponseHeaders(): List<HttpHeader>? {
        return JsonConverter.instance.fromJson<List<HttpHeader>>(
            responseHeaders,
            object : TypeToken<List<HttpHeader>>() {
            }.type
        )
    }

    fun getRequestHeadersString(withMarkup: Boolean): String {
        return FormatUtils.formatHeaders(getParsedRequestHeaders(), withMarkup)
    }

    fun setResponseHeaders(headers: Headers) {
        setResponseHeaders(toHttpHeaderList(headers))
    }

    fun setResponseHeaders(headers: List<HttpHeader>) {
        responseHeaders = JsonConverter.instance.toJson(headers)
    }

    fun getResponseHeadersString(withMarkup: Boolean): String {
        return FormatUtils.formatHeaders(getParsedResponseHeaders(), withMarkup)
    }

    private fun toHttpHeaderList(headers: Headers): List<HttpHeader> {
        val httpHeaders = ArrayList<HttpHeader>()
        for (i in 0 until headers.size()) {
            httpHeaders.add(HttpHeader(headers.name(i), headers.value(i)))
        }
        return httpHeaders
    }

    private fun formatBody(body: String, contentType: String?): String {
        return when {
            contentType != null && contentType.contains("json", ignoreCase = true) ->
                FormatUtils.formatJson(body)
            contentType != null && contentType.contains("xml", ignoreCase = true) ->
                FormatUtils.formatXml(body)
            else -> body
        }
    }

    private fun formatBytes(bytes: Long): String {
        return FormatUtils.formatByteCount(bytes, true)
    }

    fun getFormattedRequestBody(): String {
        return requestBody?.let { formatBody(it, requestContentType) } ?: ""
    }

    fun getFormattedResponseBody(): String {
        return responseBody?.let { formatBody(it, responseContentType) } ?: ""
    }

    fun populateUrl(url: HttpUrl): HttpTransaction {
        this.url = url.toString()
        host = url.host()
        path = ("/${url.pathSegments().joinToString("/")}${url.query()?.let { "?$it" } ?: ""}")
        scheme = url.scheme()
        return this
    }

    // Not relying on 'equals' because comparison be long due to request and response sizes
    // and it would be unwise to do this every time 'equals' is called.
    @Suppress("ComplexMethod")
    fun hasTheSameContent(other: HttpTransaction?): Boolean {
        if (this === other) return true
        if (other == null) return false

        if (id != other.id) return false
        if (requestDate != other.requestDate) return false
        if (responseDate != other.responseDate) return false
        if (tookMs != other.tookMs) return false
        if (protocol != other.protocol) return false
        if (method != other.method) return false
        if (url != other.url) return false
        if (host != other.host) return false
        if (path != other.path) return false
        if (scheme != other.scheme) return false
        if (requestContentLength != other.requestContentLength) return false
        if (requestContentType != other.requestContentType) return false
        if (requestHeaders != other.requestHeaders) return false
        if (requestBody != other.requestBody) return false
        if (isRequestBodyPlainText != other.isRequestBodyPlainText) return false
        if (responseCode != other.responseCode) return false
        if (responseMessage != other.responseMessage) return false
        if (error != other.error) return false
        if (responseContentLength != other.responseContentLength) return false
        if (responseContentType != other.responseContentType) return false
        if (responseHeaders != other.responseHeaders) return false
        if (responseBody != other.responseBody) return false
        if (isResponseBodyPlainText != other.isResponseBodyPlainText) return false
        val thisImageData = responseImageData
        if (thisImageData != null) {
            val otherImageData = other.responseImageData ?: return false
            if (!thisImageData.contentEquals(otherImageData)) return false
        } else if (other.responseImageData != null) return false

        return true
    }
}
