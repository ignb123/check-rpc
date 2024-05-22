
import io.check.rpc.protocol.RpcProtocol;
import io.check.rpc.protocol.header.RpcHeader;
import io.check.rpc.protocol.header.RpcHeaderFactory;
import io.check.rpc.protocol.request.RpcRequest;

/**
 * @author check
 * @version 1.0.0
 * @description
 */
public class Test {
    public static RpcProtocol<RpcRequest> getRpcProtocol(){
        RpcHeader header = RpcHeaderFactory.getRequestHeader("jdk");
        RpcRequest body = new RpcRequest();
        body.setOneway(false);
        body.setAsync(false);
        body.setClassName("io.check.rpc.demo.RpcProtocol");
        body.setMethodName("hello");
        body.setGroup("check");
        body.setParameters(new Object[]{"check"});
        body.setParameterTypes(new Class[]{String.class});
        body.setVersion("1.0.0");
        RpcProtocol<RpcRequest> protocol = new RpcProtocol<>();
        protocol.setBody(body);
        protocol.setHeader(header);
        return protocol;
    }
}
