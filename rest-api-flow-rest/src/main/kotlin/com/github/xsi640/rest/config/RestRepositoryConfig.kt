package com.github.xsi640.rest.config

import com.github.xsi640.rest.advice.ResponseAdvice
import com.github.xsi640.rest.convert.Convert
import org.springframework.cglib.beans.BeanCopier
import org.springframework.http.client.ClientHttpRequestInterceptor
import kotlin.reflect.KClass

class RestRepositoryConfig {
    val name = DEFAULT_NAME
    val interceptors = lazy { mutableListOf<ClientHttpRequestInterceptor>() }
    val trace = true
    val connectTimeout = 30000
    val readTimeout = 120000
    val followRedirects = true
    val host = ""
    val converters = lazy { mutableListOf<KClass<Convert<*>>>() }
    val superClass: KClass<*>? = null
    val superField = ""
    val advices = lazy { mutableListOf<KClass<ResponseAdvice<*>>>() }

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