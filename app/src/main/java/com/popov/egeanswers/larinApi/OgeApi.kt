package com.popov.egeanswers.larinApi

import android.text.Html

class OgeApi : LarinApi() {
    private val varNumbers = mutableMapOf<Int, List<Int>>()

    override suspend fun getPdf(number: Int, ignored: Int): ByteArray {
        val response = okHttpClient
                .urlCall("$baseUrl/gia/trvar${number}_oge.pdf")
                .execute("Can`t get pdf from $baseUrl") { response, errorMsg ->
                    Pair(response.body()?.bytes(), errorMsg)
                }
        val bytes = response.first
        if (bytes != null && bytes.isNotEmpty()) return bytes
        else throw Exception(response.second)
    }

    override suspend fun getVarNumbers(year: Int, useCache: Boolean): List<Int> {
        require(year >= OGE_START_YEAR)
        if (useCache && varNumbers.containsKey(year)) return varNumbers.getValue(year)
        val html = getHtml("$baseUrl/ege${year.toString().takeLast(2)}.html")
        val list = Regex("gia/trvar.*_(oge|gia).html")
                .findAll(html)
                .toList()
                .mapNotNull {
                    it.value
                            .substringAfterLast("gia/trvar")
                            .substringBeforeLast("_oge.html")
                            .toIntOrNull()
                }
        varNumbers[year] = list
        return list
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
                .mapNotNull { it.groups[2]?.value }
                .map { Html.fromHtml(it).toString() }
    }

    override suspend fun getLatestVarNumber(currentYear: Int): Int {
        require(currentYear >= OGE_START_YEAR)
        val varNumbers = getVarNumbers(currentYear)
        return if (varNumbers.isNotEmpty()) varNumbers.max()!!
        else 0
    }

    suspend fun getYear(number: Int, currentYear: Int): Int {
        for (year in currentYear downTo OGE_START_YEAR) {
            val varNumbers = getVarNumbers(year)
            if (varNumbers.contains(number)) return year
        }
        return 0
    }
}