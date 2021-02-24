package com.github.xsi640.rest

import com.github.xsi640.rest.config.RestRepositoryConfigFactory
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.reflect.KClass

class RestInvokerProxy(
    objectType: KClass<*>,
    restExecutor: RestExecutor,
    requestHandlerBuilder: RequestHandlerBuilder,
    repositoryConfigFactory: RestRepositoryConfigFactory
) : InvocationHandler {
    override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any {
        TODO("Not yet implemented")
    }
}
