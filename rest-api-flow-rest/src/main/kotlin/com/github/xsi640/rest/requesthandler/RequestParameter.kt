package com.github.xsi640.rest.requesthandler

import com.github.xsi640.rest.convert.Convert
import org.springframework.core.env.MissingRequiredPropertiesException

abstract class BaseRequestParameter(
    val name: String,
    val type: ParameterType,
    val convert: Convert<*>?,
    val value: Any?
)

class RequestParameter(
    name: String,
    type: ParameterType,
    convert: Convert<*>?,
    value: Any?
) : BaseRequestParameter(name, type, convert, value) {

    fun toParameterValue(): Any? {
        if (value != null) {
            return if (convert == null) {
                value
            } else {
                convert.convert(value as Nothing)
            }
        }
        return null
    }
}

class UserRequestParameter(
    name: String,
    type: ParameterType,
    convert: Convert<*>?,
    value: Any?,
    val required: Boolean = false
) : BaseRequestParameter(name, type, convert, value) {
    fun toParameterValues(list: List<*>?): List<String> {
        if (required && value == null && list == null) {
            throw MissingRequiredPropertiesException()
        }
        val result = mutableListOf<String>()
        if (list == null) {
            result.addAll(value.toString().split("\\,"))
        } else {
            list.forEach {
                if (convert == null)
                    result.add(it.toString())
                else {
                    result.add(convert.convert(it as Nothing))
                }
            }
        }
        return result
    }

    fun toParameterValue(value: Any?): String {
        if (required && this.value == null && value == null)
            throw MissingRequiredPropertiesException()
        return if (value == null) {
            this.value.toString()
        } else {
            if (convert == null)
                return value.toString()
            else
                convert.convert(value as Nothing)
        }
    }
}

enum class ParameterType {
    BODY,
    FROM,
    PATH,
    URL,
    COOKIE,
    HEADER,
}