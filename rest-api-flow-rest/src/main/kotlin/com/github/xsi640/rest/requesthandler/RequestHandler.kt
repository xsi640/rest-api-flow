package com.github.xsi640.rest.requesthandler

import com.fasterxml.jackson.databind.JavaType
import com.github.xsi640.rest.convert.Convert
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpCookie
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import java.io.File
import java.lang.reflect.Field
import java.net.URL
import kotlin.reflect.KClass

class ResponseType(
    val returnClass: KClass<*>,
    val javaType: JavaType,
    val field: Field
)

class RequestHandler {
    var name: String = ""
    var host: String = ""
    var path: String = ""
        private set
    var method: HttpMethod? = null
    var mediaType: MediaType? = null
    var parameters = mutableListOf<RequestParameter>()
        private set
    var userParameters = mutableListOf<UserRequestParameter>()
    var responseType: ResponseType? = null

    fun appendPath(path: String) {
        if (path.isEmpty())
            return
        if (path[0] != '/') {
            this.path = "${this.path}/$path"
        } else {
            this.path = "${this.path}$path"
        }
    }

    fun addUrlParameter(param: String) {
        if (param.isEmpty())
            return
        if (param.contains("=", true)) {
            val arr = param.split("\\=")
            if (arr.size == 2) {
                this.parameters.add(RequestParameter(arr[0], ParameterType.URL, null, arr[1]))
            }
        }
    }

    fun addFromParameter(param: String) {
        if (param.isEmpty())
            return

        if (param.contains("=", true)) {
            val arr = param.split("\\=")
            if (arr.size == 2) {
                this.parameters.add(RequestParameter(arr[0], ParameterType.FROM, null, arr[1]))
            }
        }
    }

    fun addFromParameter(key: String, value: String) {
        this.parameters.add(RequestParameter(key, ParameterType.FROM, null, value))
    }

    fun appendHeader(key: String, value: String) {
        this.parameters.add(RequestParameter(key, ParameterType.HEADER, null, value))
    }

    fun addHeader(header: String) {
        if (header.contains(":", true)) {
            val arr = header.split("\\:")
            if (arr.size == 2) {
                this.appendHeader(arr[0].trim(), arr[1].trim())
            }
        }
    }

    fun formatPath(path: String, arg: String) {
        this.path = this.path.replace(path, arg, false)
    }

    fun getUrl(): String {
        if (this.path.isEmpty()) {
            return this.host
        }
        if (this.host[this.host.length - 1] == '/') {
            this.host = this.host.substring(0, this.host.length - 1);
        }
        return this.host + this.path
    }

    fun addUserParameter(name: String, value: String?, type: ParameterType, required: Boolean, convert: Convert<*>?) {
        this.userParameters.add(UserRequestParameter(name, type, convert, value, required))
    }

    fun extractHeaders(args: Array<Any>): HttpHeaders {
        val headers = HttpHeaders()
        this.parameters.filter { it.type == ParameterType.HEADER }.forEach { p ->
            if (p.name.isNotEmpty())
                headers.add(p.name, p.toParameterValue().toString())
        }
        this.userParameters.forEachIndexed { i, p ->
            if (p.type == ParameterType.HEADER) {
                val v = args[i]
                if (v is HttpHeaders) {
                    headers.addAll(v)
                } else if (p.name.isNotEmpty()) {
                    if (v is List<*>) {
                        headers[p.name] = p.toParameterValues(v)
                    } else {
                        headers.add(p.name, p.toParameterValue(v))
                    }
                }
            }
        }
        return headers
    }

    fun extractCookie(args: Array<Any>) {
        val cookies = mutableListOf<HttpCookie>()
        this.parameters.filter { it.type == ParameterType.COOKIE }.forEach { p ->
            if (p.name.isNotEmpty())
                cookies.add(HttpCookie(p.name, p.toParameterValue().toString()))
        }
        this.userParameters.forEachIndexed { i, p ->
            if (p.type == ParameterType.COOKIE) {
                val v = args[i]
                if (v is HttpCookie) {
                    cookies.add(v)
                } else if (v is List<*>) {
                    cookies.addAll(v as Collection<HttpCookie>)
                } else if (p.name.isNotEmpty()) {
                    cookies.add(HttpCookie(p.name, p.toParameterValue(v)))
                }
            }
        }
    }

    fun extractUrlParameters(args: Array<Any>): LinkedMultiValueMap<String, String> {
        val result = LinkedMultiValueMap<String, String>()
        this.parameters.filter { it.type == ParameterType.URL }.forEach { p ->
            if (p.name.isNotEmpty())
                result.add(p.name, p.toParameterValue().toString())
        }
        this.userParameters.forEachIndexed { i, p ->
            if (p.type == ParameterType.URL) {
                val v = args[i]
                if (p.name.isNotEmpty()) {
                    if (v is List<*>) {
                        result[p.name] = p.toParameterValues(v)
                    } else {
                        result.add(p.name, p.toParameterValue(v))
                    }
                }
            }
        }
        return result
    }

    fun extractPathVariables(args: Array<Any>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        this.parameters.filter { it.type == ParameterType.PATH }.forEach { p ->
            if (p.name.isNotEmpty())
                result.put(p.name, p.toParameterValue().toString())
        }
        this.userParameters.forEachIndexed { i, p ->
            if (p.type == ParameterType.PATH) {
                val v = args[i]
                if (p.name.isNotEmpty()) {
                    result[p.name] = p.toParameterValue(v)
                }
            }
        }
        return result
    }

    fun extractForm(args: Array<Any>): LinkedMultiValueMap<String, Any> {
        val result = LinkedMultiValueMap<String, Any>()
        this.parameters.filter { it.type == ParameterType.FROM }.forEach { p ->
            if (p.name.isNotEmpty())
                result.add(p.name, p.toParameterValue())
        }
        this.userParameters.forEachIndexed { i, p ->
            if (p.type == ParameterType.FROM) {
                val v = args[i]
                if (v is List<*>) {
                    p.toParameterValues(v).forEach {
                        result.add(p.name, it)
                    }
                } else if (v is File) {
                    result.add(p.name, FileSystemResource(v))
                } else {
                    result.add(p.name, p.toParameterValue(v))
                }
            }
        }
        return result
    }

    fun extractBody(args: Array<Any>) {
    }
}