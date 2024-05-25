package io.check.rpc.demo.spring.annotation.consumer.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(value = {"io.check.rpc.*"})
public class SpringAnnotationConsumerConfig {
}
