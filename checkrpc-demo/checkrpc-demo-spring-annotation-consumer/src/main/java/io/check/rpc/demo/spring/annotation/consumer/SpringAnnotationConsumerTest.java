package io.check.rpc.demo.spring.annotation.consumer;

import io.check.rpc.demo.spring.annotation.consumer.config.SpringAnnotationConsumerConfig;
import io.check.rpc.demo.spring.annotation.consumer.service.ConsumerDemoService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SpringAnnotationConsumerTest {

    private static Logger logger = LoggerFactory.getLogger(SpringAnnotationConsumerTest.class);

    @Test
    public void testInterfaceRpc() {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(SpringAnnotationConsumerConfig.class);
        ConsumerDemoService consumerDemoService = context.getBean(ConsumerDemoService.class);
        String result = consumerDemoService.hello("check");
        logger.info("返回的结果数据===>>> " + result);
        while (true){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
