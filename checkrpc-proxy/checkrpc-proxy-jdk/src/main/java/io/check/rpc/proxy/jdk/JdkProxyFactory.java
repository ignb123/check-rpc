package io.check.rpc.proxy.jdk;

import io.check.rpc.proxy.api.BaseProxyFactory;
import io.check.rpc.proxy.api.ProxyFactory;

import java.lang.reflect.Proxy;

public class JdkProxyFactory<T> extends BaseProxyFactory<T> implements ProxyFactory {

    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
                new Class<?>[]{clazz},
                objectProxy);
    }
}
