package com.github.xsi640.rest

import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors

@Component
class RestTemplateLoggingRequestInterceptor : ClientHttpRequestInterceptor {

    companion object {
        @JvmStatic
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        traceRequest(request, body)
        val response = execution.execute(request, body)
        traceResponse(response)
        return response
    }

    private fun traceRequest(request: HttpRequest, body: ByteArray) {
        log.info("-------------请求开始-------------")
        log.info("URI         : {}", request.uri)
        log.info("Method      : {}", request.method)
        log.info("Headers     : {}", request.headers)
        log.info("Request body: {}", String(body, StandardCharsets.UTF_8))
        log.info("-------------请求结束-------------")
    }

    @Throws(IOException::class)
    private fun traceResponse(response: ClientHttpResponse) {
        val isr = InputStreamReader(response.body, StandardCharsets.UTF_8)
        val body = BufferedReader(isr).lines().collect(Collectors.joining("\n"))
        log.info("-------------应答开始-------------")
        log.info("Status code  : {}", response.statusCode)
        log.info("Status text  : {}", response.statusText)
        log.info("Headers      : {}", response.headers)
        log.info("Response body: {}", body)
        log.info("-------------应答结束-------------")
    }
}
