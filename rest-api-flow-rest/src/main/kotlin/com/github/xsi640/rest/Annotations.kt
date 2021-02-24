package com.github.xsi640.rest

import com.github.xsi640.rest.advice.ResponseAdvice
import com.github.xsi640.rest.convert.Convert
import org.springframework.context.annotation.Import
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import
annotation class EnableRestRepository(
    val basePackages: Array<String>
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RestRepository(
    val name: String = "",
    val host: String = "",
    val converts: Array<Converter> = [],
    val responseClass: ResponseClass = ResponseClass(),
    val advice: Array<Advice> = []
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SuperClass(
    val value: KClass<*>
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Converter(
    val value: KClass<Convert<*>>
)

@Retention(AnnotationRetention.RUNTIME)
annotation class ResponseClass(
    val superClass: KClass<*> = Unit::class,
    val superFiled: String = ""
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Advice(
    val value: KClass<ResponseAdvice<*>>
)