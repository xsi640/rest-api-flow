package com.github.xsi640.rest

import org.springframework.beans.factory.BeanClassLoaderAware
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.BeanDefinitionHolder
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.annotation.AnnotationAttributes
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AnnotationTypeFilter

class RestRepositoryRegistrar : ImportBeanDefinitionRegistrar, BeanClassLoaderAware {

    private val classPathScanner: ClassPathScanner = ClassPathScanner(false)
    private var classLoader: ClassLoader? = null
    private var applicationContext: ApplicationContext? = null

    init {
        classPathScanner.addIncludeFilter(AnnotationTypeFilter(RestRepository::class.java))
    }

    override fun setBeanClassLoader(classLoader: ClassLoader) {
        this.classLoader = classLoader
    }

    override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
        val attributes =
            AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(EnableRestRepository::class.java.name))
        val scanBasePackages = attributes!!["basePackages"] as Array<String>
        scanBasePackages.forEach {
            registryRestRepository(it, registry)
        }
    }

    private fun registryRestRepository(basePackage: String, registry: BeanDefinitionRegistry) {
        val beanDefinitions = classPathScanner.findCandidateComponents(basePackage)
        beanDefinitions.forEach { beanDefinition ->
            val clazz = classLoader!!.loadClass(beanDefinition.beanClassName)
            val builder = BeanDefinitionBuilder.genericBeanDefinition(RestRepositoryBeanFactory::class.java)
            builder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE)
            builder.addPropertyValue("classLoader", classLoader!!)
            builder.addPropertyValue("objectType", clazz.kotlin)
            val bean = builder.beanDefinition
            val holder = BeanDefinitionHolder(bean, beanDefinition.beanClassName!!)
            BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry)
        }
    }
}

class ClassPathScanner(useDefaultFilters: Boolean) : ClassPathScanningCandidateComponentProvider(useDefaultFilters) {
    override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean {
        return beanDefinition.metadata.isIndependent
    }
}