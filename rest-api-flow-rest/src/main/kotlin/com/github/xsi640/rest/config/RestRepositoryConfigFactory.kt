package com.github.xsi640.rest.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

interface RestRepositoryConfigFactory {
    fun query(name: String): RestRepositoryConfig
}

@Component
class RestRepositoryConfigFactoryImpl : RestRepositoryConfigFactory {
    @Autowired
    private lateinit var configs: Map<String, RestRepositoryConfig>

    override fun query(name: String): RestRepositoryConfig {
        var config = configs[name]
        if (config == null)
            config = configs[RestRepositoryConfig.DEFAULT_NAME]
        return config!!
    }
}