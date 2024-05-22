package io.check.rpc.test.consumer.handler;

import io.check.rpc.consumer.common.RpcConsumer;
import io.check.rpc.consumer.common.context.RpcContext;
import io.check.rpc.consumer.common.future.RPCFuture;
import io.check.rpc.protocol.RpcProtocol;
import io.check.rpc.protocol.header.RpcHeaderFactory;
import io.check.rpc.protocol.request.RpcRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcConsumerHandlerTest {

    private final static Logger logger = LoggerFactory.getLogger(RpcConsumerHandlerTest.class);
    public static void main(String[] args) throws Exception {
        RpcConsumer consumer = RpcConsumer.getInstance();
        consumer.sendRequest(getRpcRequestProtocol());
        RPCFuture future = RpcContext.getContext().getRPCFuture();
        logger.info("无需获取返回的结果数据");
        consumer.close();
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
        request.setOneway(true);
        protocol.setBody(request);
        return protocol;
    }

}