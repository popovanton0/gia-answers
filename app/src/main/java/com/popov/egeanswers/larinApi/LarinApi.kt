package com.popov.egeanswers.larinApi

import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

abstract class LarinApi {
    companion object {
        const val EGE_START_YEAR = 2016
        const val OGE_START_YEAR = 2015
        const val baseUrl = "http://alexlarin.net"
    }

    protected val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .build()

    abstract suspend fun getPdf(number: Int, year: Int): ByteArray
    abstract suspend fun getVarNumbers(year: Int, useCache: Boolean = true): List<Int>
    abstract suspend fun getLatestVarNumber(currentYear: Int): Int

    protected suspend fun <T> Call.execute(
            errorMsg: String = "Failed request",
            onSuccess: (response: Response, errorMsg: String) -> T
    ): T = suspendCoroutine { cont ->
        this.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) =
                    cont.resumeWithException(Exception(errorMsg, e))

            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use { cont.resume(onSuccess(response, errorMsg)) }
                } catch (e: Exception) {
                    cont.resumeWithException(Exception(errorMsg, e))
                }
            }
        })
    }

    protected suspend fun getHtml(url: String): String {
        val response = okHttpClient
                .urlCall(url)
                .execute("Can`t get HTML from $baseUrl") { response, errorMsg ->
                    Pair(response.body()?.string(), errorMsg)
                }
        if (!response.first.isNullOrBlank()) return response.first!!
        else throw Exception(response.second)
    }

    protected fun OkHttpClient.urlCall(url: String) = this.newCall(Request.Builder().url(url).build())
}