package com.popov.egeanswers

import org.junit.Test
import java.util.*

class IncrementGradeUnitTest {
    @Test
    fun incrementGradeTest() {
        assert(incrementGrade(1423699200000L, 2015, Calendar.JANUARY) == 0) // 12 Feb 2015
        assert(incrementGrade(1433116800000L, 2015, Calendar.JANUARY) == 1) // 01 Jun 2015
        assert(incrementGrade(1433116800000L, 2015, Calendar.JUNE) == 0) // 01 Jun 2015

        assert(incrementGrade(1423699200000L, 2016, Calendar.JANUARY) == 1) // 12 Feb 2015
        assert(incrementGrade(1433116800000L, 2016, Calendar.JANUARY) == 0) // 01 Jun 2015
        assert(incrementGrade(1433116800000L, 2016, Calendar.JUNE) == 1) // 01 Jun 2015
        assert(incrementGrade(1423699200000L, 2016, Calendar.JUNE) == 2) // 12 Feb 2015

        assert(incrementGrade(1423699200000L, 2018, Calendar.JANUARY) == 3) // 12 Feb 2015
        assert(incrementGrade(1433116800000L, 2018, Calendar.JANUARY) == 2) // 01 Jun 2015
        assert(incrementGrade(1433116800000L, 2018, Calendar.JUNE) == 3) // 01 Jun 2015
        assert(incrementGrade(1423699200000L, 2018, Calendar.JUNE) == 4) // 12 Feb 2015
    }

    private fun incrementGrade(userClassSetDate: Long, currentYear: Int, currentMonth: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = userClassSetDate
        val year = calendar[Calendar.YEAR]
        val isBeforeSwitchingMonth = calendar[Calendar.MONTH] < Calendar.JUNE
        val isBeforeCurrentSwitchingMonth = currentMonth < Calendar.JUNE
        val pastYears = currentYear - year
        return if (pastYears == 0)
            if (isBeforeSwitchingMonth) if (!isBeforeCurrentSwitchingMonth) 1 else 0
            else if (isBeforeCurrentSwitchingMonth) 1 else 0
        else if (pastYears > 0)
            if (isBeforeSwitchingMonth)
                if (isBeforeCurrentSwitchingMonth) pastYears
                else pastYears + 1
            else
                if (isBeforeCurrentSwitchingMonth) pastYears - 1
                else pastYears
        else throw IllegalStateException("Time traveller detected!")
    }
}
