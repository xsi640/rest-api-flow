package com.github.xsi640.rest.advice

interface ResponseAdvice<T : Any> {
    fun afterResponse(t: T): T

    fun priority(): Int
}