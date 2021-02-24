package com.github.xsi640.rest

interface RestExecutor {
    fun <T> exec(handler: RequestHandler, args: Array<Any>): T
}