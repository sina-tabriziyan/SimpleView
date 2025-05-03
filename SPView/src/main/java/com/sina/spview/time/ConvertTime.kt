/**
 * Created by ST on 1/22/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.time

import android.content.Context
import com.sina.simpleview.library.R
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Date

object ConvertTime {
    private val fileTimeConvertor = FileTimeConvertor()

    @JvmStatic
    fun getModifiedTime(
        fileTime: String?,
        shortTime: Boolean,
        timeZone:String?,
        dayLightRange:String?,
        calenderType:Int?,
    ): String {
        if (fileTime == "-1") return " "

        val arrDate = fileTimeConvertor.Picker(
            fileTime?.toLong()
                ?: convertToWindowsFileTime(millisToUnixTime(System.currentTimeMillis())),
            timeZone?.toLong() ?: 0L,
            dayLightRange,
            0
        )

        return if (shortTime) {
            "${arrDate[3]}:${arrDate[4]}"
        } else {
            when {
                calenderType == 0 && arrDate[0] == Calendar.getInstance()[Calendar.YEAR].toString() &&
                        (arrDate[1].toIntOrNull()
                            ?: 0) == Calendar.getInstance()[Calendar.MONTH] + 1 &&
                        arrDate[2] == Calendar.getInstance()[Calendar.DAY_OF_MONTH].toString() ->
                    "${arrDate[3]}:${arrDate[4]}"

                calenderType == 0 -> "${getMonth(arrDate[1].toIntOrNull() ?: 0)} ${arrDate[2]}"
                else -> "${arrDate[1]}/${arrDate[2]}"
            }
        }
    }

    private fun getMonth(month: Int): String = DateFormatSymbols().months[month - 1]

    fun getDate(context: Context, fileTime: String, language: String, timezone: Long): String {
        return if (language.equals("fa", ignoreCase = true) || language.equals(
                "Persian",
                ignoreCase = true
            )
        ) {
            // Use Persian Calendar
            val persianCalendar = PersianCalendar(context, fileTimeToNormalTime(fileTime, timezone))
            if (isToday(persianCalendar)) context.getString(R.string.txt_today)
            else StringBuilder()
                .append(ConvertNumber.convertEnToFa(persianCalendar.getPersianYear().toString()))
                .append("/")
                .append(persianCalendar.getPersianMonthName())
                .append("/")
                .append(ConvertNumber.convertEnToFa(persianCalendar.getPersianDay().toString()))
                .toString()
        } else {
            // Use Gregorian Calendar
            val calendar = getGregorianCalendar(fileTimeToNormalTime(fileTime))
            if (isToday(calendar)) context.getString(R.string.txt_today)
            else "${
                getMonthName(
                    context,
                    calendar.get(Calendar.MONTH)
                )
            } ${calendar.get(Calendar.DAY_OF_MONTH)}, ${calendar.get(Calendar.YEAR)}"
        }
    }


    private fun isToday(calendar: Calendar): Boolean {
        val todayCalendar = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == todayCalendar.get(Calendar.DAY_OF_MONTH)
    }

    private fun isToday(
        persianCalendar: PersianCalendar,
        context: Context,
        fileTime: Long
    ): Boolean {
        val todayPersianCalendar = PersianCalendar(context, fileTime)
        return persianCalendar.getPersianYear() == todayPersianCalendar.getPersianYear() &&
                persianCalendar.getPersianMonth() == todayPersianCalendar.getPersianMonth() &&
                persianCalendar.getPersianDay() == todayPersianCalendar.getPersianDay()
    }

    private fun fileTimeToNormalTime(fileTime: String, timezone: Long): Long {
        val fileTimeLong = fileTime.toLong()
        if (fileTimeLong < (10000000 * 11644473600L)) return 0
        return (((fileTimeLong / 10000000 - 11644473600L) + timezone / 10000000) * 1000)
    }

    private fun fileTimeToNormalTime(fileTime: String): Long {
        val fileTimeLong = fileTime.toLong()
        if (fileTimeLong < (10000000 * 11644473600L))
            return 0
        return ((fileTimeLong / 10000000 - 11644473600L) * 1000)
    }

    private fun getGregorianCalendar(timeMillis: Long): Calendar {
        val calendar = Calendar.getInstance()
        calendar.time = Date(timeMillis)
        return calendar
    }

    private fun getMonthName(context: Context, month: Int): String =
        context.resources.getStringArray(R.array.month_name)[month]
}
