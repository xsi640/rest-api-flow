package com.github.xsi640.rest

import com.github.xsi640.rest.config.RestRepositoryConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@ComponentScan("com.github.xsi640.rest")
@Configuration
class RestAutoConfiguration {
    @Bean("restRepositoryBeanFactory")
    fun restRepositoryBeanFactory(): RestRepositoryBeanFactory? {
        return RestRepositoryBeanFactory()
    }

    @Bean
    fun restTemplate(): RestTemplate? {
        return RestTemplate()
    }

    @Bean(RestRepositoryConfig.DEFAULT_NAME)
    fun defaultRepositoryConfig(): RestRepositoryConfig? {
        return RestRepositoryConfig()
    }
}