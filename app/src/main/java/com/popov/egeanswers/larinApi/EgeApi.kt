package com.popov.egeanswers.larinApi

class EgeApi : LarinApi() {
    private val varNumbers = mutableMapOf<Int, List<Int>>()

    override suspend fun getPdf(number: Int, year: Int): ByteArray {
        require(year >= EGE_START_YEAR)
        val response = okHttpClient
                .urlCall("$baseUrl/ege/$year/trvar$number.pdf")
                .execute("Can`t get pdf from $baseUrl") { response, errorMsg ->
                    Pair(response.body()?.bytes(), errorMsg)
                }
        val bytes = response.first
        if (bytes != null && bytes.isNotEmpty()) return bytes
        else throw Exception(response.second)
    }

    override suspend fun getLatestVarNumber(currentYear: Int): Int {
        require(currentYear >= EGE_START_YEAR)
        if (varNumbers.contains(currentYear)) return varNumbers.getValue(currentYear).first()
        val varNumbers = getVarNumbers(currentYear)
        return if (varNumbers.isNotEmpty()) varNumbers.max()!! else 0
    }

    /**
     * @return list of variant numbers in ascending order for a specified [year]
     */
    override suspend fun getVarNumbers(year: Int, useCache: Boolean): List<Int> {
        require(year >= EGE_START_YEAR)
        if (useCache && varNumbers.containsKey(year)) return varNumbers.getValue(year)
        val html = getHtml("$baseUrl/ege${year.toString().takeLast(2)}.html")
        val list = Regex("ege/$year/trvar.*.html")
                .findAll(html)
                .toList()
                .mapNotNull {
                    it.value
                            .substringAfterLast("ege/$year/trvar")
                            .substringBeforeLast(".html")
                            .toIntOrNull()
                }
        varNumbers[year] = list
        return list
    }

    suspend fun getPart2Answers(number: Int, year: Int): ByteArray {
        require(year >= EGE_START_YEAR)
        val url = if (number >= 285) "$baseUrl/ege/$year/trvar${number}_0.png"
        else "$baseUrl/ege/$year/trvar$number.png"
        var response = okHttpClient
                .urlCall(url)
                .execute("Can`t get part 2 answers from $baseUrl") { response, errorMsg ->
                    Triple(response.body()?.bytes(), response.code(), errorMsg)
                }
        // 404 because this var is the latest one, answer image is blank (290.png is used)
        if (response.second == 404) response = okHttpClient
                .urlCall("$baseUrl/ege/$year/trvar290.png")
                .execute("Can`t get part 2 answers from $baseUrl") { response, errorMsg ->
                    Triple(response.body()?.bytes(), response.code(), errorMsg)
                }
        val bytes = response.first
        if (response.second in 200..299 && bytes != null && bytes.isNotEmpty()) return bytes
        else throw Exception(response.third)
    }
}