package com.github.xsi640.rest.requesthandler

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.xsi640.rest.RestRepository
import com.github.xsi640.rest.SuperClass
import com.github.xsi640.rest.config.RestRepositoryConfig
import com.github.xsi640.rest.config.RestRepositoryConfigFactory
import com.github.xsi640.rest.convert.Convert
import com.github.xsi640.rest.convert.ConverterFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.HttpCookie
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

interface RequestHandlerBuilder {
    fun build(clazz: KClass<*>, repository: RestRepository, method: Method): RequestHandler
}

@Component
class RequestHandlerBuilderImpl : RequestHandlerBuilder {

    companion object {
        private val map = ConcurrentHashMap<String, RequestHandler>()
    }

    private val VALUE_PATTERN = Pattern.compile("^@\\{([\\s\\S]+)}$", Pattern.CASE_INSENSITIVE);

    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var mapper: ObjectMapper

    @Autowired
    private lateinit var converterFactory: ConverterFactory

    @Autowired
    private lateinit var repositoryConfigFactory: RestRepositoryConfigFactory

    override fun build(clazz: KClass<*>, repository: RestRepository, method: Method): RequestHandler {
        val config = repositoryConfigFactory.query(repository.name).copyTo()
        fillRestRepository(config, repository)
        registerConverter(config.name, config.converters)
        val key = "${config.name}.${clazz.simpleName}.${method.name}"
        map.computeIfAbsent(key) {
            create(clazz, config, method)
        }
        return map[key]!!
    }

    private fun registerConverter(name: String, converters: Lazy<MutableList<KClass<Convert<*>>>>) {
        converters.value.forEach {
            converterFactory.register(name, it)
        }
    }

    private fun fillRestRepository(config: RestRepositoryConfig, repository: RestRepository) {
        config.name = repository.name
        if (repository.host.isNotEmpty())
            config.host = repository.host
        repository.converts.forEach {
            config.converters.value.add(it.value)
        }
        repository.advice.forEach {
            config.advices.value.add(it.value)
        }
        if (repository.responseClass.superClass != Unit::class) {
            config.superClass = repository.responseClass.superClass
        }
        if (repository.responseClass.superFiled.isNotEmpty())
            config.superField = repository.responseClass.superFiled
    }

    private fun create(clazz: KClass<*>, config: RestRepositoryConfig, method: Method): RequestHandler {
        val handler = RequestHandler()
        handler.name = config.name
        var host = config.host
        val matcher = VALUE_PATTERN.matcher(host)
        if (matcher.find()) {
            host = environment.getProperty(matcher.group(1))!!
        }
        handler.host = host
        val mapping = clazz.findAnnotation<RequestMapping>()
        extractedRequestMapping(mapping, handler)
        extractedMethod(method, handler)
        val responseType = ResponseType()
        handler.responseType = responseType
        responseType.returnClass = config.superClass
        val superClass = method.getAnnotation(SuperClass::class.java)
        if (config.superClass != Unit::class) {
            if (config.superField.isEmpty()) {
                throw IllegalArgumentException("The rest superClass is not null, but super field is empty.")
            } else {
                val field = config.superClass.java.getDeclaredField(config.superField)
                    ?: throw IllegalArgumentException("The rest superField not found.")
                field.isAccessible = true
                responseType.field = field
            }
            if (superClass != null) {
                val innerType = mapper.typeFactory.constructParametricType(method.returnType, superClass.value.java)
                val javaType = mapper.typeFactory.constructParametricType(responseType.returnClass.java, innerType)
                responseType.javaType = javaType
            } else {
                responseType.javaType =
                    mapper.typeFactory.constructParametricType(responseType.returnClass.java, method.returnType)
            }
        } else {
            if (superClass != null) {
                responseType.javaType =
                    mapper.typeFactory.constructParametricType(method.returnType, superClass.value.java)
            } else {
                responseType.returnClass = method.returnType.kotlin
            }
        }
        method.parameters.forEachIndexed { i, paramter ->
            if (paramter.type == HttpHeaders::class.java) {
                handler.addUserParameter("", null, ParameterType.HEADER, false, null)
            } else if (paramter.type == HttpCookie::class.java) {
                handler.addUserParameter("", null, ParameterType.COOKIE, false, null)
            } else {
                extractedParameter(config.name, paramter, handler)
            }
        }
        return handler;
    }

    private fun extractedParameter(name: String, parameter: Parameter, handler: RequestHandler) {
        val param = parameter.getAnnotation(RequestParam::class.java)
        val body = parameter.getAnnotation(RequestBody::class.java)
        val pathVariable = parameter.getAnnotation(PathVariable::class.java)
        val cookieValue = parameter.getAnnotation(CookieValue::class.java)
        val superClass = parameter.getAnnotation(SuperClass::class.java)
        var converter = converterFactory.get(name, parameter.type.kotlin)
        if (superClass != null) {
            converter = converterFactory.get(name, superClass.value)
        }
        if (param == null && body == null && pathVariable == null) {
            throw IllegalArgumentException("The method none rest repository annotation")
        }
        if (pathVariable != null) {
            val parameterName = if (pathVariable.name.isEmpty()) pathVariable.value else pathVariable.name
            handler.addUserParameter(parameterName, null, ParameterType.PATH, pathVariable.required, converter)
        } else if (body != null) {
            handler.addUserParameter("", null, ParameterType.BODY, body.required, converter)
        } else if (cookieValue != null) {
            val parameterName = if (cookieValue.name.isEmpty()) cookieValue.value else cookieValue.name
            handler.addUserParameter(parameterName, null, ParameterType.COOKIE, cookieValue.required, converter)
        } else {
            val parameterName = if (param.name.isEmpty()) param.value else param.name
            val defaultValue = if (param.defaultValue.isEmpty()) null else param.defaultValue
            if (handler.method == HttpMethod.POST || handler.method == HttpMethod.PUT) {
                handler.addUserParameter(parameterName, defaultValue, ParameterType.FROM, param.required, converter)
            } else {
                handler.addUserParameter(parameterName, defaultValue, ParameterType.URL, param.required, converter)
            }
        }
    }

    private fun extractedMethod(method: Method, handler: RequestHandler) {
        val getMapping = method.getAnnotation(GetMapping::class.java)
        extractedGetMapping(getMapping, handler)
        val putMapping = method.getAnnotation(PutMapping::class.java)
        extractedPutMapping(putMapping, handler)
        val postMapping = method.getAnnotation(PostMapping::class.java)
        extractedPostMapping(postMapping, handler)
        val deleteMapping = method.getAnnotation(DeleteMapping::class.java)
        extractedDeleteMapping(deleteMapping, handler)
        val requestMapping = method.getAnnotation(RequestMapping::class.java)
        extractedRequestMapping(requestMapping, handler)
    }

    private fun extractedGetMapping(mapping: GetMapping?, handler: RequestHandler) {
        if (mapping == null) return
        var paths: Array<String> = mapping.path
        if (paths.isEmpty()) paths = mapping.value
        if (paths.isNotEmpty()) handler.appendPath(paths[0])
        handler.method = toHttpMethod(RequestMethod.GET)
        if (mapping.params.isNotEmpty()) {
            mapping.params.forEach { param ->
                handler.addUrlParameter(param)
            }
        }
        if (mapping.headers.isNotEmpty()) {
            mapping.headers.forEach { header ->
                handler.addHeader(header)
            }
        }
        if (mapping.consumes.isNotEmpty())
            handler.mediaType = MediaType.valueOf(mapping.consumes[0])
    }

    private fun extractedPutMapping(mapping: PutMapping?, handler: RequestHandler) {
        if (mapping == null) return
        var paths: Array<String> = mapping.path
        if (paths.isEmpty()) paths = mapping.value
        if (paths.isNotEmpty()) handler.appendPath(paths[0])
        handler.method = toHttpMethod(RequestMethod.PUT)
        if (mapping.params.isNotEmpty()) {
            mapping.params.forEach { param ->
                handler.addFromParameter(param)
            }
        }
        if (mapping.headers.isNotEmpty()) {
            mapping.headers.forEach { header ->
                handler.addHeader(header)
            }
        }
        if (mapping.consumes.isNotEmpty())
            handler.mediaType = MediaType.valueOf(mapping.consumes[0])
    }

    private fun extractedPostMapping(mapping: PostMapping?, handler: RequestHandler) {
        if (mapping == null) return
        var paths: Array<String> = mapping.path
        if (paths.isEmpty()) paths = mapping.value
        if (paths.isNotEmpty()) handler.appendPath(paths[0])
        handler.method = toHttpMethod(RequestMethod.POST)
        if (mapping.params.isNotEmpty()) {
            mapping.params.forEach { param ->
                handler.addFromParameter(param)
            }
        }
        if (mapping.headers.isNotEmpty()) {
            mapping.headers.forEach { header ->
                handler.addHeader(header)
            }
        }
        if (mapping.consumes.isNotEmpty())
            handler.mediaType = MediaType.valueOf(mapping.consumes[0])
    }

    private fun extractedDeleteMapping(mapping: DeleteMapping?, handler: RequestHandler) {
        if (mapping == null) return
        var paths: Array<String> = mapping.path
        if (paths.isEmpty()) paths = mapping.value
        if (paths.isNotEmpty()) handler.appendPath(paths[0])
        handler.method = toHttpMethod(RequestMethod.DELETE)
        if (mapping.params.isNotEmpty()) {
            mapping.params.forEach { param ->
                handler.addUrlParameter(param)
            }
        }
        if (mapping.headers.isNotEmpty()) {
            mapping.headers.forEach { header ->
                handler.addHeader(header)
            }
        }
        if (mapping.consumes.isNotEmpty())
            handler.mediaType = MediaType.valueOf(mapping.consumes[0])
    }

    private fun extractedRequestMapping(mapping: RequestMapping?, handler: RequestHandler) {
        if (mapping == null)
            return
        var paths: Array<String> = mapping.path
        if (paths.isEmpty()) paths = mapping.value
        if (paths.isNotEmpty()) handler.appendPath(paths[0])
        handler.method = toHttpMethod(RequestMethod.GET)
        if (mapping.params.isNotEmpty()) {
            mapping.params.forEach { param ->
                handler.addUrlParameter(param)
            }
        }
        if (mapping.headers.isNotEmpty()) {
            mapping.headers.forEach { header ->
                handler.addHeader(header)
            }
        }
        if (mapping.consumes.isNotEmpty())
            handler.mediaType = MediaType.valueOf(mapping.consumes[0])
    }

    private fun toHttpMethod(method: RequestMethod): HttpMethod {
        return HttpMethod.valueOf(method.name)
    }
}
