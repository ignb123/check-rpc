package io.check.rpc.demo.spring.annotation.provider;

import io.check.rpc.demo.spring.annotation.provider.config.SpringAnnotationProviderConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class SpringAnnotationProviderStarter {
    public static void main(String[] args) {
        /**
         * 通过扫描指定类上的注解来配置应用程序上下文，
         * 从而初始化 Spring 应用程序上下文。
         */
        new AnnotationConfigApplicationContext(SpringAnnotationProviderConfig.class);
    }
}
