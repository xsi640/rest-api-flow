package com.github.xsi640.rest

import com.github.xsi640.rest.advice.ResponseAdvice
import com.github.xsi640.rest.config.RestRepositoryConfigFactory
import com.github.xsi640.rest.requesthandler.RequestHandler
import com.github.xsi640.rest.requesthandler.RequestHandlerBuilder
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class RestInvokerProxy(
    val objectType: KClass<*>,
    val restExecutor: RestExecutor,
    val requestHandlerBuilder: RequestHandlerBuilder,
    val repositoryConfigFactory: RestRepositoryConfigFactory
) : InvocationHandler {

    private var repository: RestRepository = objectType.findAnnotation()!!

    private var advices = mutableListOf<ResponseAdvice<*>>()

    init {
        repository.advice.forEach { advice ->
            advices.add(advice.value.objectInstance!!)
        }
        advices.sortBy { it.priority() }
    }

    override fun invoke(proxy: Any, method: Method, args: Array<Any>): Any {
        val requestHandler = requestHandlerBuilder.build(objectType, repository, method)
        var result = restExecutor.exec<Any>(requestHandler, args)
        if (advices.isNotEmpty()) {
            advices.forEach { adv ->
                result = adv.afterResponse(result as Nothing)
            }
        }
        return extractedReturn(requestHandler, result)
    }

    private fun extractedReturn(handler: RequestHandler, result: Any): Any {
        return if (handler.responseType.field != null) {
            handler.responseType.field!!.get(result)
        } else {
            result
        }
    }
}
