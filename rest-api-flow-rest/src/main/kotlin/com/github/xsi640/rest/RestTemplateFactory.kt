package com.github.xsi640.rest

import com.github.xsi640.rest.config.RestRepositoryConfigFactory
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.LaxRedirectStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.concurrent.ConcurrentHashMap

interface RestTemplateFactory {
    fun create(name: String): RestTemplate
}

@Component
class RestTemplateFactoryImpl : RestTemplateFactory {
    @Autowired
    private lateinit var restTemplateLoggingRequestInterceptor: RestTemplateLoggingRequestInterceptor

    @Autowired
    private lateinit var restRepositoryConfigFactory: RestRepositoryConfigFactory

    companion object {
        private val restTemplateMap = ConcurrentHashMap<String, RestTemplate>()
    }

    override fun create(name: String): RestTemplate {
        restTemplateMap.computeIfAbsent(name) {
            build(it)
        }
        return restTemplateMap[name]!!
    }

    fun build(name: String): RestTemplate {
        val config = restRepositoryConfigFactory.query(name)
        val restTemplate = RestTemplate()
        val interceptors = mutableListOf<ClientHttpRequestInterceptor>()
        if (config.trace) {
            interceptors.add(restTemplateLoggingRequestInterceptor)
        }
        if (config.interceptors.value.isNotEmpty()) {
            interceptors.addAll(config.interceptors.value)
        }
        restTemplate.interceptors = interceptors
        val httpRequestFactory = HttpComponentsClientHttpRequestFactory()
        httpRequestFactory.setConnectTimeout(config.connectTimeout)
        httpRequestFactory.setReadTimeout(config.readTimeout)
        if (config.followRedirects) {
            val httpClient = HttpClientBuilder.create()
                .setRedirectStrategy(LaxRedirectStrategy()).build()
            httpRequestFactory.httpClient = httpClient
        }
        val factory = BufferingClientHttpRequestFactory(httpRequestFactory)
        restTemplate.requestFactory = factory
        return restTemplate
    }
}