package io.check.rpc.common.scanner.server;

import io.check.rpc.annotation.RpcService;
import io.check.rpc.common.scanner.ClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author check
 * @version 1.0.0
 * @description @RpcService注解扫描器
 */
public class RpcServiceScanner extends ClassScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServiceScanner.class);

    /**
     * 扫描指定包下的类，并筛选使用@RpcService注解标注的类
     */
    public static Map<String, Object> doScannerWithRpcServiceAnnotationFilterAndRegistryService(
            /*String host, int port, */ String scanPackage/*, RegistryService registryService*/) throws Exception{
        Map<String, Object> handlerMap = new HashMap<>();
        List<String> classNameList = getClassNameList(scanPackage);
        if (classNameList == null || classNameList.isEmpty()){
            return handlerMap;
        }
        classNameList.stream().forEach((className) -> {
            try {
                Class<?> clazz = Class.forName(className);
                RpcService rpcService = clazz.getAnnotation(RpcService.class);
                if (rpcService != null){
                    //优先使用interfaceClass, interfaceClass的name为空，再使用interfaceClassName
                    //TODO 后续逻辑向注册中心注册服务元数据，同时向handlerMap中记录标注了RpcService注解的类实例
                    //handlerMap中的Key先简单存储为serviceName+version+group，后续根据实际情况处理Key
                    String serviceName = geServiceName(rpcService);
                    String key = serviceName.concat(rpcService.version()).concat(rpcService.group());
                    handlerMap.put(key,clazz.newInstance());
                }
            } catch (Exception e) {
                LOGGER.error("scan classes throws exception: {}", e);
            }
        });
        return handlerMap;
    }

    private static String geServiceName(RpcService rpcService){
        return rpcService.interfaceClassName();
    }
}