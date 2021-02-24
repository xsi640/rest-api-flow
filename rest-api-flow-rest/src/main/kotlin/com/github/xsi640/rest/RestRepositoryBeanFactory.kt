package com.github.xsi640.rest

import com.github.xsi640.rest.config.RestRepositoryConfigFactory
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

class RestRepositoryBeanFactory : FactoryBean<Any> {

    private val classLoader: ClassLoader? = null
    private val objectType: KClass<*>? = null

    @Autowired
    private lateinit var restExecutor: RestExecutor

    @Autowired
    private lateinit var requestHandlerBuilder: RequestHandlerBuilder

    @Autowired
    private lateinit var repositoryConfigFactory: RestRepositoryConfigFactory

    override fun getObject(): Any? {
        return Proxy.newProxyInstance(
            classLoader,
            arrayOf(objectType!!.java),
            RestInvokerProxy(objectType, restExecutor, requestHandlerBuilder, repositoryConfigFactory)
        )
    }

    override fun getObjectType(): Class<*> {
        return this.objectType!!.java
    }
}