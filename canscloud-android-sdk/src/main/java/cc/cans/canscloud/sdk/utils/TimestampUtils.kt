/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cc.cans.canscloud.sdk.utils

import android.content.Context
import cc.cans.canscloud.sdk.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class TimestampUtils {
    companion object {
        fun isToday(timestamp: Long, timestampInSecs: Boolean = true): Boolean {
            val cal = Calendar.getInstance()
            cal.timeInMillis = if (timestampInSecs) timestamp * 1000 else timestamp
            return isSameDay(cal, Calendar.getInstance())
        }

        fun isYesterday(timestamp: Long, timestampInSecs: Boolean = true): Boolean {
            val yesterday = Calendar.getInstance()
            yesterday.roll(Calendar.DAY_OF_MONTH, -1)
            val cal = Calendar.getInstance()
            cal.timeInMillis = if (timestampInSecs) timestamp * 1000 else timestamp
            return isSameDay(cal, yesterday)
        }

        fun isDay(timestamp: Long, daysAgo: Int, timestampInSecs: Boolean = true): Boolean {
            val referenceDay = Calendar.getInstance()
            referenceDay.roll(Calendar.DAY_OF_MONTH, -daysAgo)

            val cal = Calendar.getInstance()
            cal.timeInMillis = if (timestampInSecs) timestamp * 1000 else timestamp

            return isSameDay(cal, referenceDay)
        }

        fun isSameDay(timestamp1: Long, timestamp2: Long, timestampInSecs: Boolean = true): Boolean {
            val cal1 = Calendar.getInstance()
            cal1.timeInMillis = if (timestampInSecs) timestamp1 * 1000 else timestamp1
            val cal2 = Calendar.getInstance()
            cal2.timeInMillis = if (timestampInSecs) timestamp2 * 1000 else timestamp2
            return isSameDay(cal1, cal2)
        }

        fun isSameDay(
            cal1: Date,
            cal2: Date,
        ): Boolean {
            return isSameDay(cal1.time, cal2.time, false)
        }

        private fun isSameYear(timestamp: Long, timestampInSecs: Boolean = true): Boolean {
            val cal = Calendar.getInstance()
            cal.timeInMillis = if (timestampInSecs) timestamp * 1000 else timestamp
            return isSameYear(cal, Calendar.getInstance())
        }

        fun toString(
            timestamp: Long,
            onlyDate: Boolean = false,
            timestampInSecs: Boolean = true,
            shortDate: Boolean = true,
            hideYear: Boolean = true,
        ): String {
            val dateFormat = if (isToday(timestamp, timestampInSecs)) {
                DateFormat.getTimeInstance(DateFormat.SHORT)
            } else {
                if (onlyDate) {
                    DateFormat.getDateInstance(if (shortDate) DateFormat.SHORT else DateFormat.MEDIUM)
                } else {
                    DateFormat.getDateTimeInstance(if (shortDate) DateFormat.SHORT else DateFormat.MEDIUM, DateFormat.SHORT)
                }
            } as SimpleDateFormat

            if (hideYear || isSameYear(timestamp, timestampInSecs)) {
                // Remove the year part of the format
                dateFormat.applyPattern(
                    dateFormat.toPattern().replace("/?y+/?|,?\\s?y+\\s?".toRegex(), if (shortDate) "" else " "),
                )
            }

            val millis = if (timestampInSecs) timestamp * 1000 else timestamp
            return dateFormat.format(Date(millis))
        }

        private fun isSameDay(
            cal1: Calendar,
            cal2: Calendar,
        ): Boolean {
            return cal1[Calendar.ERA] == cal2[Calendar.ERA] &&
                cal1[Calendar.YEAR] == cal2[Calendar.YEAR] &&
                cal1[Calendar.DAY_OF_YEAR] == cal2[Calendar.DAY_OF_YEAR]
        }

        private fun isSameYear(
            cal1: Calendar,
            cal2: Calendar,
        ): Boolean {
            return cal1[Calendar.ERA] == cal2[Calendar.ERA] &&
                cal1[Calendar.YEAR] == cal2[Calendar.YEAR]
        }

        fun convertMillisToTime(timestampInMillis: Long): String {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestampInMillis * 1000
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            return dateFormat.format(cal.time)
        }

        fun formatDate(context: Context, date: Long): String {
            if (isToday(date)) {
                return context.getString(R.string.today)
            } else if (isYesterday(date)) {
                return context.getString(R.string.yesterday)
            } else if (isDay(date, 2)) {
                return calendarToDayOfWeek(date)
            } else if (isDay(date, 3)) {
                return calendarToDayOfWeek(date)
            } else if (isDay(date, 4)) {
                return calendarToDayOfWeek(date)
            } else if (isDay(date, 5)) {
                return calendarToDayOfWeek(date)
            } else if (isDay(date, 6)) {
                return calendarToDayOfWeek(date)
            }
            return toString(date, onlyDate = true, shortDate = false, hideYear = false)
        }

        fun calendarToDayOfWeek(timestamp: Long, timestampInSecs: Boolean = true): String {
            val cal = Calendar.getInstance()
            cal.timeInMillis = if (timestampInSecs) timestamp * 1000 else timestamp
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            return when (dayOfWeek) {
                Calendar.SUNDAY -> "Sunday"
                Calendar.MONDAY -> "Monday"
                Calendar.TUESDAY -> "Tuesday"
                Calendar.WEDNESDAY -> "Wednesday"
                Calendar.THURSDAY -> "Thursday"
                Calendar.FRIDAY -> "Friday"
                Calendar.SATURDAY -> "Saturday"
                else -> "Unknown"
            }
        }

        fun durationCallingTime(duration: Int): String {
            val hours = duration / 3600
            val minutes = (duration % 3600) / 60
            val remainingSeconds = duration % 60

            return if (hours != 0) {
                if (hours > 1) {
                    String.format("%2d hours %2d minutes", hours, minutes)
                } else {
                    String.format("%2d hour %2d minutes", hours, minutes)
                }
            } else if (minutes != 0) {
                String.format("%2d:%02d minutes", minutes, remainingSeconds)
            } else {
                String.format("%2d seconds", remainingSeconds)
            }
        }
    }
}
