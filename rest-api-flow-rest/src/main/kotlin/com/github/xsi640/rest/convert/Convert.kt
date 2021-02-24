package com.github.xsi640.rest.convert

import kotlin.reflect.KClass

interface Convert<T> {
    fun convert(t: T): String

    val type: KClass<*>
}