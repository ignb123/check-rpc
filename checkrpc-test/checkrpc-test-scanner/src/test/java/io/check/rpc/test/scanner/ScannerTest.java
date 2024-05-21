package io.check.rpc.test.scanner;

import io.check.rpc.common.scanner.ClassScanner;
import io.check.rpc.common.scanner.reference.RpcReferenceScanner;
import io.check.rpc.common.scanner.server.RpcServiceScanner;
import org.junit.Test;

import java.util.List;

/**
 * 扫描io.check.rpc.test.scanner包下所有的类
 */
public class ScannerTest {

    /**
     * 扫描io.check.rpc.test.scanner包下所有的类
     */
    @Test
    public void testScannerClassNameList() throws Exception {
        List<String> classNameList = ClassScanner.getClassNameList("io.check.rpc.test.scanner");
        classNameList.forEach(System.out::println);
    }


    /**
     * 扫描io.binghe.rpc.test.scanner包下所有标注了@RpcService注解的类
     */
    @Test
    public void testScannerClassNameListByRpcService() throws Exception {
        RpcServiceScanner.
                doScannerWithRpcServiceAnnotationFilterAndRegistryService("io.check.rpc.test.scanner");
    }

    /**
     * 扫描io.binghe.rpc.test.scanner包下所有标注了@RpcReference注解的类
     */
    @Test
    public void testScannerClassNameListByRpcReference() throws Exception {
        RpcReferenceScanner.
                doScannerWithRpcReferenceAnnotationFilter("io.check.rpc.test.scanner");
    }
}
