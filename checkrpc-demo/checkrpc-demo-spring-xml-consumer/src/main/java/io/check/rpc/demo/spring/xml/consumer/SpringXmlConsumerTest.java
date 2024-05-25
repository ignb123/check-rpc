package io.check.rpc.demo.spring.xml.consumer;

import io.check.rpc.consumer.RpcClient;
import io.check.rpc.demo.api.DemoService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
/**
 * 当SpringXmlConsumerTest类启动时，
 * Spring会读取classpath路径下的client-spring.xml文件创建IOC容器
 */
@ContextConfiguration(locations = "classpath:client-spring.xml")
public class SpringXmlConsumerTest {

    private static Logger logger = LoggerFactory.getLogger(SpringXmlConsumerTest.class);

    @Autowired
    private RpcClient rpcClient;

    @Test
    public void testInterfaceRpc() throws InterruptedException {
        DemoService demoService = rpcClient.create(DemoService.class);
        String result = demoService.hello("check");
        logger.info("返回的结果数据===>>> " + result);
        //rpcClient.shutdown();
        while (true){
            Thread.sleep(1000);
        }
    }

}
