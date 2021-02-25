package com.github.xsi640.rest.config

import com.github.xsi640.rest.advice.ResponseAdvice
import com.github.xsi640.rest.convert.Convert
import org.springframework.cglib.beans.BeanCopier
import org.springframework.http.client.ClientHttpRequestInterceptor
import kotlin.reflect.KClass

class RestRepositoryConfig {
    var name = DEFAULT_NAME
    var interceptors = lazy { mutableListOf<ClientHttpRequestInterceptor>() }
    var trace = true
    var connectTimeout = 30000
    var readTimeout = 120000
    var followRedirects = true
    var host = ""
    var converters = lazy { mutableListOf<KClass<Convert<*>>>() }
    var superClass: KClass<*> = Unit::class
    var superField = ""
    var advices = lazy { mutableListOf<KClass<ResponseAdvice<*>>>() }

    fun copyTo(): RestRepositoryConfig {
        val result = RestRepositoryConfig()
        copier.copy(this, result, null)
        return result
    }

    companion object {
        const val DEFAULT_NAME = "default_rest_repository"
        private val copier = BeanCopier.create(
            RestRepositoryConfig::class.java,
            RestRepositoryConfig::class.java, false
        )
    }
}