package io.check.rpc.consumer.spring;

import io.check.rpc.annotation.RpcReference;
import io.check.rpc.constants.RpcConstants;
import io.check.rpc.consumer.spring.context.RpcConsumerSpringContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;


@Component
public class RpcConsumerPostProcessor implements ApplicationContextAware, BeanClassLoaderAware, BeanFactoryPostProcessor {

    private final Logger logger = LoggerFactory.getLogger(RpcConsumerPostProcessor.class);

    /**
     * 应用上下文，用于获取和管理Bean实例。
     */
    private ApplicationContext context;

    /**
     * 类加载器，用于加载类。
     */
    private ClassLoader classLoader;

    /**
     * 存储RPC引用的Bean定义的映射，键为Bean名称，值为Bean定义。
     * 使用LinkedHashMap保证插入顺序的遍历，这对于某些场景下的处理非常有用。
     */
    private final Map<String, BeanDefinition> rpcRefBeanDefinitions = new LinkedHashMap<>();

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * 在BeanFactory后处理期间解析RPC引用。
     * 遍历BeanFactory中所有bean的定义，对于有类名的bean，尝试解析它们的字段以查找RPC引用。
     * 发现RPC引用后，将其注册为一个bean定义，以便Spring容器能够实例化这些引用。
     *
     * @param beanFactory 允许配置的可列表bean工厂，用于访问bean定义和注册新bean定义。
     * @throws BeansException 如果处理过程中发生错误。
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 遍历所有bean定义，解析有RPC引用的类
        for (String beanDefinitionName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanDefinitionName);
            String beanClassName = beanDefinition.getBeanClassName();
            if (beanClassName != null) {
                Class<?> clazz = ClassUtils.resolveClassName(beanClassName, this.classLoader);
                ReflectionUtils.doWithFields(clazz, this::parseRpcReference);
            }
        }

        // 将解析出的RPC引用注册为bean定义
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        this.rpcRefBeanDefinitions.forEach((beanName, beanDefinition) -> {
            if (context.containsBean(beanName)) {
                throw new IllegalArgumentException("spring context already has a bean named " + beanName);
            }
            registry.registerBeanDefinition(beanName, rpcRefBeanDefinitions.get(beanName));
            logger.info("registered RpcReferenceBean {} success.", beanName);
        });
    }

    /**
     * 解析RPC引用注解。
     * 该方法用于处理字段上标注的RpcReference注解，根据注解配置创建一个RpcReferenceBean的BeanDefinition，
     * 并将其存储起来以便于后续的处理和使用。
     *
     * @param field 被注解标注的字段，该字段预期上有RpcReference注解。
     */
    private void parseRpcReference(Field field) {
        // 获取字段上的RpcReference注解
        RpcReference annotation = AnnotationUtils.getAnnotation(field, RpcReference.class);
        if (annotation != null) {
            // 使用BeanDefinitionBuilder创建一个通用的BeanDefinition实例
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RpcReferenceBean.class);
            // 设置初始化方法名
            builder.setInitMethodName(RpcConstants.INIT_METHOD_NAME);
            // 根据注解内容设置Bean的属性值
            // 这里设置了RpcReferenceBean的各种属性，它们都是根据注解中的相应字段获取的值
            builder.addPropertyValue("interfaceClass", field.getType());
            builder.addPropertyValue("version", annotation.version());
            builder.addPropertyValue("registryType", annotation.registryType());
            builder.addPropertyValue("registryAddress", annotation.registryAddress());
            builder.addPropertyValue("loadBalanceType", annotation.loadBalanceType());
            builder.addPropertyValue("serializationType", annotation.serializationType());
            builder.addPropertyValue("timeout", annotation.timeout());
            builder.addPropertyValue("async", annotation.async());
            builder.addPropertyValue("oneway", annotation.oneway());
            builder.addPropertyValue("proxy", annotation.proxy());
            builder.addPropertyValue("group", annotation.group());
            builder.addPropertyValue("scanNotActiveChannelInterval", annotation.scanNotActiveChannelInterval());
            builder.addPropertyValue("heartbeatInterval", annotation.heartbeatInterval());
            builder.addPropertyValue("retryInterval", annotation.retryInterval());
            builder.addPropertyValue("retryTimes", annotation.retryTimes());
            builder.addPropertyValue("enableResultCache", annotation.enableResultCache());
            builder.addPropertyValue("resultCacheExpire", annotation.resultCacheExpire());

            // 获取构建完成的BeanDefinition
            BeanDefinition beanDefinition = builder.getBeanDefinition();
            // 将BeanDefinition与字段名映射，存储起来
            rpcRefBeanDefinitions.put(field.getName(), beanDefinition);
        }
    }



    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
        RpcConsumerSpringContext.getInstance().setContext(applicationContext);
    }
}
