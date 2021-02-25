package com.github.xsi640.rest.convert

import com.github.xsi640.rest.config.RestRepositoryConfig
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

interface ConverterFactory {
    fun get(name: String, clazz: KClass<*>): Convert<*>?
    fun register(name: String, clazz: KClass<Convert<*>>)
}

@Component
class ConverterFactoryImpl : ConverterFactory {
    companion object {
        private val converterMap = ConcurrentHashMap<String, MutableMap<KClass<*>, Convert<*>>>()
    }

    override fun get(name: String, clazz: KClass<*>): Convert<*>? {
        val key = if (name.isEmpty()) {
            RestRepositoryConfig.DEFAULT_NAME
        } else {
            name
        }
        val map = converterMap[key]
        return if (map != null)
            map[clazz]
        else
            null
    }

    override fun register(name: String, clazz: KClass<Convert<*>>) {
        val map = converterMap.computeIfAbsent(name) { HashMap() }
        val converter = clazz.createInstance()
        map[converter.type] = converter
    }
}