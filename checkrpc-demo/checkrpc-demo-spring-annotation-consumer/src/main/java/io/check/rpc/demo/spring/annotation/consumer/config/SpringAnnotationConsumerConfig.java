package io.check.rpc.demo.spring.annotation.consumer.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author check
 * @version 1.0.0
 * @description 服务消费者注解配置类
 */

@Configuration
@ComponentScan(value = {"io.check.rpc.*"})
public class SpringAnnotationConsumerConfig {
}
