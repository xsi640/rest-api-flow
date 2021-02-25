package com.github.xsi640.rest

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.xsi640.rest.requesthandler.RequestHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import kotlin.reflect.KClass

interface RestExecutor {
    fun <T> exec(handler: RequestHandler, args: Array<Any>): T
}

@Component
class SimpleRestExecutor : RestExecutor {

    @Autowired
    private lateinit var restTemplateFactory: RestTemplateFactory

    @Autowired
    private lateinit var mapper: ObjectMapper

    override fun <T> exec(handler: RequestHandler, args: Array<Any>): T {
        val restTemplate = restTemplateFactory.create(handler.name)
        val headers = handler.extractHeaders(args)
        val cookies = handler.extractCookie(args)
        val pathParameters = handler.extractPathVariables(args)
        val urlParameters = handler.extractUrlParameters(args)
        val fromParameters = handler.extractForm(args)
        val body = handler.extractBody(args)
        if (cookies.isNotEmpty()) {
            headers[HttpHeaders.SET_COOKIE] = cookies.map { cookie ->
                "${cookie.name}=${cookie.value}"
            }
        }
        if (handler.mediaType != null) {
            headers.contentType = handler.mediaType
        }
        var url = handler.getUrl()
        if (urlParameters.isNotEmpty()) {
            val builder = UriComponentsBuilder.fromHttpUrl(url)
            urlParameters.forEach { (k, v) ->
                builder.queryParam(k, v)
            }
            url = builder.toUriString()
        }
        val builder = RequestEntity
            .method(handler.method, url, urlParameters)
            .headers(headers)
        var entity = if (body != null) {
            builder.body(body)
        } else {
            builder.body(fromParameters)
        }
        val response = restTemplate.exchange(entity, String::class.java)
        return if (handler.responseType.javaType != null) {
            toObject(response.body!!, handler.responseType.javaType!!) as T
        } else {
            if (handler.responseType.returnClass != null) {
                toObject(response.body!!, handler.responseType.returnClass!!) as T
            } else {
                response.body as T
            }
        }
    }

    fun toObject(value: String, clazz: KClass<*>): Any {
        return if (String::class.java == clazz) {
            value
        } else if (Boolean::class.java == clazz) {
            java.lang.Boolean.parseBoolean(value)
        } else if (Byte::class.java == clazz) {
            value.toByte()
        } else if (Short::class.java == clazz) {
            value.toShort()
        } else if (Int::class.java == clazz) {
            value.toInt()
        } else if (Long::class.java == clazz) {
            value.toLong()
        } else if (Float::class.java == clazz) {
            value.toFloat()
        } else if (Double::class.java == clazz) {
            value.toDouble()
        } else {
            mapper.readValue(value, clazz.java)
        }
    }

    fun toObject(value: String, type: JavaType): Any {
        return mapper.readValue(value, type)
    }
}