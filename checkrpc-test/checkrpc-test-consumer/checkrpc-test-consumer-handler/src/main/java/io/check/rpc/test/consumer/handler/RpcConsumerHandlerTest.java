package io.check.rpc.test.consumer.handler;

import io.check.rpc.common.exception.RegistryException;
import io.check.rpc.consumer.common.RpcConsumer;

import io.check.rpc.protocol.RpcProtocol;
import io.check.rpc.protocol.header.RpcHeaderFactory;
import io.check.rpc.protocol.request.RpcRequest;
import io.check.rpc.proxy.api.callback.AsyncRPCCallback;
import io.check.rpc.proxy.api.future.RPCFuture;
import io.check.rpc.registry.api.RegistryService;
import io.check.rpc.registry.api.config.RegistryConfig;
import io.check.rpc.registry.zookeeper.ZookeeperRegistryService;
import io.check.rpc.spi.loader.ExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class RpcConsumerHandlerTest {

    private final static Logger logger = LoggerFactory.getLogger(RpcConsumerHandlerTest.class);
    public static void main(String[] args) throws Exception {
        RpcConsumer consumer = RpcConsumer.getInstance();
        RPCFuture future = consumer.sendRequest(getRpcRequestProtocol(),getRegistryService("127.0.0.1:2181", "zookeeper","random"));
        future.addCallback(new AsyncRPCCallback() {
            @Override
            public void onSuccess(Object result) {
                logger.info("从服务消费者获取到的数据===>>>" + result);
            }

            @Override
            public void onException(Exception e) {
                logger.info("抛出了异常===>>>" + e);
            }
        });
        Thread.sleep(200);
        consumer.close();
    }
    private static RegistryService getRegistryService(String registryAddress, String registryType, String registryLoadBalanceType) {
        if (StringUtils.isEmpty(registryType)){
            throw new IllegalArgumentException("registry type is null");
        }

        RegistryService registryService = ExtensionLoader.getExtension(RegistryService.class, registryType);
        try {
            registryService.init(new RegistryConfig(registryAddress, registryType, registryLoadBalanceType));
        } catch (Exception e) {
            logger.error("RpcClient init registry service throws exception:{}", e);
            throw new RegistryException(e.getMessage(), e);
        }
        return registryService;
    }

    private static RpcProtocol<RpcRequest> getRpcRequestProtocol(){
        //模拟发送数据
        RpcProtocol<RpcRequest> protocol = new RpcProtocol<RpcRequest>();
        protocol.setHeader(RpcHeaderFactory.getRequestHeader("jdk"));
        RpcRequest request = new RpcRequest();
        request.setClassName("io.check.rpc.test.api.DemoService");
        request.setGroup("check");
        request.setMethodName("hello");
        request.setParameters(new Object[]{"check"});
        request.setParameterTypes(new Class[]{String.class});
        request.setVersion("1.0.0");
        request.setAsync(false);
        request.setOneway(false);
        protocol.setBody(request);
        return protocol;
    }

}
