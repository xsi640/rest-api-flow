package com.github.xsi640.rest.convert

import kotlin.reflect.KClass

interface Convert<T> {
    fun to(t: T): String?

    val type: KClass<*>
}