package com.github.xsi640.rest.convert

import java.text.SimpleDateFormat
import java.util.*

class DateConverter : Convert<Date> {
    override fun convert(t: Date): String? {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return sdf.format(t)
    }

    override val type = Date::class
}