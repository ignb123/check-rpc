package io.check.rpc.reflect.javassist;

import io.check.rpc.reflect.api.ReflectInvoker;
import io.check.rpc.spi.annotation.SPIClass;
import javassist.util.proxy.ProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

@SPIClass
public class JavassistReflectInvoker implements ReflectInvoker {

    private final Logger logger = LoggerFactory.getLogger(JavassistReflectInvoker.class);

    /**
     * 使用javassist反射类型调用方法。
     *
     * @param serviceBean 服务 bean 对象，即将被调用方法所在的对象。
     * @param serviceClass 服务 bean 对象的类，用于指定生成代理对象的超类。
     * @param methodName 要调用的方法名。
     * @param parameterTypes 调用方法时的参数类型数组。
     * @param parameters 调用方法时的参数值数组。
     * @return 调用方法后的返回值，返回类型为 Object，具体类型依赖于被调用方法的返回类型。
     * @throws Throwable 如果方法调用过程中发生异常，则抛出。
     */
    @Override
    public Object invokeMethod(Object serviceBean, Class<?> serviceClass, String methodName, Class<?>[] parameterTypes, Object[] parameters) throws Throwable {
        logger.info("use javassist reflect type invoke method...");

        // 创建代理工厂，设置超类为传入的服务类
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setSuperclass(serviceClass);

        // 生成代理类
        Class<?> childClass = proxyFactory.createClass();

        // 获取方法并设置为可访问
        Method method = childClass.getMethod(methodName, parameterTypes);
        method.setAccessible(true);

        // 实例化代理类并调用方法，返回结果
        return method.invoke(childClass.newInstance(), parameters);
    }
}
