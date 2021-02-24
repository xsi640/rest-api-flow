package com.github.xsi640.rest.advice

interface ResponseAdvice<T> {
    fun afterResponse(t: T): T

    fun priority(): Int
}