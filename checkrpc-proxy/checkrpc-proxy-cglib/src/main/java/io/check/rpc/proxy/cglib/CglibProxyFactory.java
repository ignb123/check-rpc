package io.check.rpc.proxy.cglib;


import io.check.rpc.proxy.api.BaseProxyFactory;
import io.check.rpc.proxy.api.ProxyFactory;
import io.check.rpc.spi.annotation.SPIClass;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

@SPIClass
public class CglibProxyFactory<T> extends BaseProxyFactory<T> implements ProxyFactory {

    private final Logger logger = LoggerFactory.getLogger(CglibProxyFactory.class);

    private final Enhancer enhancer = new Enhancer();

    /**
     * 基于CGLib库生成目标类的动态代理。
     *
     * @param clazz 需要生成代理的对象的类
     * @return 代理对象，其类型为参数clazz指定的类型
     * @param <T> 代理对象的类型
     */
    @Override
    public <T> T getProxy(Class<T> clazz) {
        logger.info("基于CGLib动态代理...");
        // 设置代理对象的父类为参数clazz指定的类
        enhancer.setInterfaces(new Class[]{clazz});
        // 设置代理对象的方法调用的回调处理
        enhancer.setCallback(new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                // 调用代理处理器对象的方法，实现对原始方法的拦截
                return objectProxy.invoke(o, method, objects);
            }
        });
        // 创建并返回代理对象
        return (T) enhancer.create();
    }
}
