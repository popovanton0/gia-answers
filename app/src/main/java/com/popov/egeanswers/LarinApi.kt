package com.popov.egeanswers

import android.text.Html
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class LarinApi {
    companion object {
        const val EGE_START_YEAR = 2016
        const val OGE_START_YEAR = 2015
    }

    private val baseUrl = "http://alexlarin.net"

    private val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .build()

    inner class EGE {
        suspend fun getPdf(number: Int, year: Int): ByteArray = suspendCoroutine { cont ->
            require(year >= EGE_START_YEAR)
            okHttpClient
                    .newCall(
                            Request.Builder()
                                    .url("$baseUrl/ege/$year/trvar$number.pdf")
                                    .build()
                    )
                    .enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            cont.resumeWithException(Exception("Can`t get pdf from $baseUrl", e))
                        }

                        override fun onResponse(call: Call, response: Response) {
                            try {
                                val bytes = response.body()?.bytes()
                                if (bytes != null && bytes.isNotEmpty()) cont.resume(bytes)
                                else cont.resumeWithException(Exception("Can`t get pdf from $baseUrl"))
                            } catch (e: Exception) {
                                cont.resumeWithException(Exception("Can`t get pdf from $baseUrl", e))
                            }
                        }
                    })
        }

        suspend fun getLatestVarNumber(currentYear: Int): Int {
            require(currentYear >= EGE_START_YEAR)
            val varNumbers = getVarNumbers(currentYear)
            return if (varNumbers.isNotEmpty()) varNumbers.max()!!
            else 0
        }

        suspend fun getVarNumbers(year: Int): List<Int> {
            require(year >= EGE_START_YEAR)
            val html = getHtml("$baseUrl/ege${year.toString().takeLast(2)}.html")
            return Regex("ege/$year/trvar.*.html")
                    .findAll(html)
                    .toList()
                    .map {
                        it.value
                                .substringAfterLast("ege/$year/trvar")
                                .substringBeforeLast(".html")
                                .toIntOrNull()
                    }.filterNotNull()
        }

        suspend fun getPart2Answers(number: Int, year: Int): ByteArray = suspendCoroutine { cont ->
            require(year >= EGE_START_YEAR)
            okHttpClient
                    .newCall(
                            Request.Builder()
                                    .url("$baseUrl/ege/$year/trvar$number.png")
                                    .build()
                    )
                    .enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            cont.resumeWithException(Exception("Can`t get part 2 answers from $baseUrl", e))
                        }

                        override fun onResponse(call: Call, response: Response) {
                            try {
                                val bytes = response.body()?.bytes()
                                if (bytes != null && bytes.isNotEmpty()) cont.resume(bytes)
                                else cont.resumeWithException(Exception())
                            } catch (e: Exception) {
                                cont.resumeWithException(Exception("Can`t get part 2 answers from $baseUrl", e))
                            }
                        }
                    })
        }
    }

    inner class OGE {
        suspend fun getPdf(number: Int): ByteArray = suspendCoroutine { cont ->
            okHttpClient
                    .newCall(
                            Request.Builder()
                                    .url("$baseUrl/gia/trvar${number}_oge.pdf")
                                    .build()
                    )
                    .enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            cont.resumeWithException(Exception("Can`t get pdf from $baseUrl", e))
                        }

                        override fun onResponse(call: Call, response: Response) {
                            try {
                                val bytes = response.body()?.bytes()
                                if (bytes != null && bytes.isNotEmpty()) cont.resume(bytes)
                                else cont.resumeWithException(Exception("Can`t get pdf from $baseUrl"))
                            } catch (e: Exception) {
                                cont.resumeWithException(Exception("Can`t get pdf from $baseUrl", e))
                            }
                        }
                    })
        }

        suspend fun getLatestVarNumber(currentYear: Int): Int {
            require(currentYear >= OGE_START_YEAR)
            val varNumbers = getVarNumbers(currentYear)
            return if (varNumbers.isNotEmpty()) varNumbers.max()!!
            else 0
        }

        suspend fun getVarNumbers(year: Int): List<Int> {
            require(year >= OGE_START_YEAR)
            val html = getHtml("$baseUrl/ege${year.toString().takeLast(2)}.html")
            return Regex("gia/trvar.*_(oge|gia).html")
                    .findAll(html)
                    .toList()
                    .map {
                        it.value
                                .substringAfterLast("gia/trvar")
                                .substringBeforeLast("_oge.html")
                                .toIntOrNull()
                    }.filterNotNull()
        }

        suspend fun getAnswers(number: Int): List<String> {
            val html = getHtml("$baseUrl/gia/trvar${number}_oge.html")
                    .substringAfter("<td valign=\"top\" style=\"height: 11.65pt; border-left: medium none; border-right: 1.0pt solid black; border-top: medium none; border-bottom: 1.0pt solid black; padding-left: 5.4pt; padding-right: 5.4pt; padding-top: 0cm; padding-bottom: 0cm\" align=\"center\" nowrap")
                    .let { "<td valign=\"top\" style=\"height: 11.65pt; border-left: medium none; border-right: 1.0pt solid black; border-top: medium none; border-bottom: 1.0pt solid black; padding-left: 5.4pt; padding-right: 5.4pt; padding-top: 0cm; padding-bottom: 0cm\" align=\"center\" nowrap$it" }
                    .replace("\n", "")
                    .replace("\t", "")
                    .replace(Regex("<span lang=\".{1,10}\">"), "")
                    .replace("</span>", "")
            return Regex("<td valign=\"top\" style=\"height: 11.65pt; border-left: medium none; border-right: 1.0pt solid black; border-top: medium none; border-bottom: 1.0pt solid black; padding-left: 5.4pt; padding-right: 5.4pt; padding-top: 0cm; padding-bottom: 0cm\" align=\"center\" nowrap( width=\"11\")?>(.{1,40})</td>")
                    .findAll(html)
                    .toList()
                    .map { it.groups[2]?.value }
                    .filterNotNull()
                    .map { Html.fromHtml(it).toString() }
        }

        suspend fun getYear(number: Int, currentYear: Int): Int {
            for (year in currentYear downTo OGE_START_YEAR) {
                val varNumbers = getVarNumbers(year)
                if (varNumbers.contains(number)) return year
            }
            return 0
        }
    }

    private suspend fun getHtml(url: String): String = suspendCoroutine { cont ->
        okHttpClient
                .newCall(
                        Request.Builder()
                                .url(url)
                                .build()
                )
                .enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        cont.resumeWithException(Exception(e))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            val resp = response.body()?.string()
                            if (resp != null) cont.resume(resp)
                            else cont.resumeWithException(Exception("Can`t get HTML from $baseUrl"))
                        } catch (e: Exception) {
                            cont.resumeWithException(Exception("Can`t get HTML from $baseUrl"))
                        }
                    }
                })
    }
}