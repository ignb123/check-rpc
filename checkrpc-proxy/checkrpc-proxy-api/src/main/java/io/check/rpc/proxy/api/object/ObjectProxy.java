package io.check.rpc.proxy.api.object;

import io.check.rpc.cache.result.CacheResultKey;
import io.check.rpc.cache.result.CacheResultManager;
import io.check.rpc.common.exception.RpcException;
import io.check.rpc.common.utils.StringUtils;
import io.check.rpc.constants.RpcConstants;
import io.check.rpc.exception.processor.ExceptionPostProcessor;
import io.check.rpc.fusing.api.FusingInvoker;
import io.check.rpc.protocol.RpcProtocol;
import io.check.rpc.protocol.enumeration.RpcType;
import io.check.rpc.protocol.header.RpcHeaderFactory;
import io.check.rpc.protocol.request.RpcRequest;
import io.check.rpc.proxy.api.async.IAsyncObjectProxy;
import io.check.rpc.proxy.api.consumer.Consumer;
import io.check.rpc.proxy.api.future.RPCFuture;
import io.check.rpc.ratelimiter.api.RateLimiterInvoker;
import io.check.rpc.reflect.api.ReflectInvoker;
import io.check.rpc.registry.api.RegistryService;
import io.check.rpc.spi.loader.ExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;


/**
 * 用于Java动态代理的对象代理实现
 *
 * @param <T>
 */
public class ObjectProxy<T> implements IAsyncObjectProxy, InvocationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectProxy.class);

    /**
     * 接口的Class对象
     */
    private Class<T> clazz;

    /**
     * 服务版本号
     */
    private String serviceVersion;

    /**
     * 服务分组
     */
    private String serviceGroup;

    /**
     * 超时时间，默认15s
     */
    private long timeout = 15000;

    /**
     * 服务消费者
     */
    private Consumer consumer;

    /**
     * 序列化类型
     */
    private String serializationType;

    /**
     * 是否异步调用
     */
    private boolean async;

    /**
     * 是否单向调用
     */
    private boolean oneway;

    /**
     * 注册中心
     */
    private RegistryService registryService;

    /**
     * 是否开启结果缓存
     */
    private boolean enableResultCache;

    /**
     * 结果缓存管理器
     */
    private CacheResultManager<Object> cacheResultManager;

    /**
     * 反射调用实际方法的SPI接口
     */
    private ReflectInvoker reflectInvoker;

    /**
     * 容错处理的类Class对象
     */
    private Class<?> fallbackClass;

    /**
     * 限流规则SPI接口
     */
    private RateLimiterInvoker rateLimiterInvoker;

    /**
     * 是否开启限流
     */
    private boolean enableRateLimiter;

    /**
     * 当限流失败时的处理策略
     */
    private String rateLimiterFailStrategy;

    /**
     * 是否开启熔断
     */
    private boolean enableFusing;

    /**
     * 熔断SPI接口
     */
    private FusingInvoker fusingInvoker;

    /**
     * 异常处理后置处理器
     */
    private ExceptionPostProcessor exceptionPostProcessor;


    public ObjectProxy(Class<T> clazz) {
        this.clazz = clazz;
    }

    public ObjectProxy(Class<T> clazz, String serviceVersion, String serviceGroup, String serializationType,
                       long timeout, Consumer consumer, boolean async, boolean oneway,
                       RegistryService registryService, boolean enableResultCache, int resultCacheExpire,
                       String reflectType, String fallbackClassName, Class<?> fallbackClass, boolean enableRateLimiter,
                       String rateLimiterType, int permits, int milliSeconds, String rateLimiterFailStrategy,
                       boolean enableFusing, String fusingType, double totalFailure, int fusingMilliSeconds,
                       String exceptionPostProcessorType) {
        this.clazz = clazz;
        this.serviceVersion = serviceVersion;
        this.timeout = timeout;
        this.serviceGroup = serviceGroup;
        this.consumer = consumer;
        this.serializationType = serializationType;
        this.async = async;
        this.oneway = oneway;
        this.registryService = registryService;
        this.enableResultCache = enableResultCache;
        if (resultCacheExpire <= 0) {
            resultCacheExpire = RpcConstants.RPC_SCAN_RESULT_CACHE_EXPIRE;
        }
        this.cacheResultManager = CacheResultManager.getInstance(resultCacheExpire, enableResultCache);
        this.reflectInvoker = ExtensionLoader.getExtension(ReflectInvoker.class, reflectType);
        this.fallbackClass = this.getFallbackClass(fallbackClassName, fallbackClass);
        this.enableRateLimiter = enableRateLimiter;
        this.initRateLimiter(rateLimiterType, permits, milliSeconds);
        if (StringUtils.isEmpty(rateLimiterFailStrategy)){
            rateLimiterFailStrategy = RpcConstants.RATE_LIMILTER_FAIL_STRATEGY_DIRECT;
        }
        this.rateLimiterFailStrategy = rateLimiterFailStrategy;
        this.enableFusing = enableFusing;
        if (StringUtils.isEmpty(exceptionPostProcessorType)){
            exceptionPostProcessorType = RpcConstants.EXCEPTION_POST_PROCESSOR_PRINT;
        }
        this.exceptionPostProcessor = ExtensionLoader.getExtension(ExceptionPostProcessor.class, exceptionPostProcessorType);
        this.initFusing(fusingType, totalFailure, fusingMilliSeconds);

    }

    /**
     * 初始化熔断SPI接口
     */
    private void initFusing(String fusingType, double totalFailure, int fusingMilliSeconds) {
        if (enableFusing){
            fusingType = StringUtils.isEmpty(fusingType) ? RpcConstants.DEFAULT_FUSING_INVOKER : fusingType;
            this.fusingInvoker = ExtensionLoader.getExtension(FusingInvoker.class, fusingType);
            this.fusingInvoker.init(totalFailure, fusingMilliSeconds);
        }
    }

    /**
     * 初始化限流器
     */
    private void initRateLimiter(String rateLimiterType, int permits, int milliSeconds) {
        if (enableRateLimiter) {
            rateLimiterType = StringUtils.isEmpty(rateLimiterType) ? RpcConstants.DEFAULT_RATELIMITER_INVOKER : rateLimiterType;
            this.rateLimiterInvoker = ExtensionLoader.getExtension(RateLimiterInvoker.class, rateLimiterType);
            this.rateLimiterInvoker.init(permits, milliSeconds);
        }
    }

    /**
     * 获取容错处理的Class对象，这里，优先使用fallbackClass的值，
     * 如果fallbackClass为空，同时，fallbackClassName不为空，
     * 则使用fallbackClassName创建Class对象赋值给fallbackClass
     */
    private Class<?> getFallbackClass(String fallbackClassName, Class<?> fallbackClass) {
        if (this.isFallbackClassEmpty(fallbackClass)) {
            try {
                if (!StringUtils.isEmpty(fallbackClassName)) {
                    fallbackClass = Class.forName(fallbackClassName);
                }
            } catch (ClassNotFoundException e) {
                exceptionPostProcessor.postExceptionProcessor(e);
                LOGGER.error(e.getMessage());
            }
        }
        return fallbackClass;
    }

    /**
     * 判断fallbackClass对象是否为空
     */
    private boolean isFallbackClassEmpty(Class<?> fallbackClass) {
        return fallbackClass == null
                || fallbackClass == RpcConstants.DEFAULT_FALLBACK_CLASS
                || RpcConstants.DEFAULT_FALLBACK_CLASS.equals(fallbackClass);
    }

    /**
     * 直接调用服务容错类的方法获取结果数据
     */
    private Object getFallbackResult(Method method, Object[] args) {
        try {
            //fallbackClass不为空，则执行容错处理
            if (this.isFallbackClassEmpty(fallbackClass)){
                return null;
            }
            return reflectInvoker
                    .invokeMethod(fallbackClass.newInstance(), fallbackClass, method.getName(),
                            method.getParameterTypes(), args);
        } catch (Throwable ex) {
            exceptionPostProcessor.postExceptionProcessor(ex);
            LOGGER.error(ex.getMessage());
        }
        return null;
    }

    /**
     * 执行限流失败时的处理逻辑
     */
    private Object invokeFailRateLimiterMethod(Method method, Object[] args) throws Exception{
        LOGGER.info("execute {} fail rate limiter strategy...", rateLimiterFailStrategy);
        switch (rateLimiterFailStrategy){
            case RpcConstants.RATE_LIMILTER_FAIL_STRATEGY_EXCEPTION:
            case RpcConstants.RATE_LIMILTER_FAIL_STRATEGY_FALLBACK:
                return this.getFallbackResult(method, args);
            case RpcConstants.RATE_LIMILTER_FAIL_STRATEGY_DIRECT:
                return this.invokeSendRequestMethodWithFusing(method, args);
        }
        return this.invokeSendRequestMethodWithFusing(method, args);
    }

    /**
     * 以熔断方式请求数据
     */
    private Object invokeSendRequestMethodWithFusing(Method method, Object[] args) throws Exception{
        //开启了熔断
        if (enableFusing){
            return invokeFusingSendRequestMethod(method, args);
        }else {
            return invokeSendRequestMethod(method, args);
        }
    }


    /**
     * 熔断请求
     */
    private Object invokeFusingSendRequestMethod(Method method, Object[] args) throws Exception {
        //触发了熔断规则，直接返回降级处理业务
        if (fusingInvoker.invokeFusingStrategy()){
            return this.getFallbackResult(method, args);
        }
        //请求计数器加1
        fusingInvoker.incrementCount();
        Object result = null;
        try {
            result = invokeSendRequestMethod(method, args);
            fusingInvoker.markSuccess();
        }catch (Throwable e){
            exceptionPostProcessor.postExceptionProcessor(e);
            fusingInvoker.markFailed();
            throw new RpcException(e.getMessage());
        }
        return result;
    }


    /**
     * 真正调用服务提供者远程方法
     *
     * @param method
     * @param args
     * @return
     * @throws Exception
     */
    private Object invokeSendRequestMethod(Method method, Object[] args) throws Exception {
        RpcProtocol<RpcRequest> requestRpcProtocol = getSendRequest(method, args);
        RPCFuture rpcFuture = this.consumer.sendRequest(requestRpcProtocol, registryService);
        return rpcFuture == null ? null : timeout > 0 ? rpcFuture.get(timeout, TimeUnit.MILLISECONDS) : rpcFuture.get();
    }

    /**
     * 以限流方式发送请求
     */
    private Object invokeSendRequestMethodWithRateLimiter(Method method, Object[] args) throws Exception {
        Object result = null;
        if (enableRateLimiter) {
            if (rateLimiterInvoker.tryAcquire()) {
                try {
                    result = invokeSendRequestMethodWithFusing(method, args);
                } finally {
                    rateLimiterInvoker.release();
                }
            } else {
                result = this.invokeFailRateLimiterMethod(method, args);
            }
        } else {
            result = invokeSendRequestMethodWithFusing(method, args);
        }
        return result;
    }

    /**
     * 以容错方式真正发送请求
     */
    private Object invokeSendRequestMethodWithFallback(Method method, Object[] args) throws Exception {
        try {
            return invokeSendRequestMethodWithRateLimiter(method, args);
        }catch (Throwable e){
            exceptionPostProcessor.postExceptionProcessor(e);
            return getFallbackResult(method, args);
        }
    }

    /**
     * 结合缓存处理结果数据
     *
     * @param method
     * @param args
     * @return
     * @throws Exception
     */
    private Object invokeSendRequestMethodCache(Method method, Object[] args) throws Exception {
        //开启缓存，则处理缓存
        CacheResultKey cacheResultKey = new CacheResultKey(method.getDeclaringClass().getName(),
                method.getName(), method.getParameterTypes(), args, serviceVersion, serviceGroup);
        Object obj = this.cacheResultManager.get(cacheResultKey);
        if (obj == null) {
            obj = invokeSendRequestMethodWithFallback(method, args);
            if (obj != null) {
                cacheResultKey.setCacheTimeStamp(System.currentTimeMillis());
                this.cacheResultManager.put(cacheResultKey, obj);
            }
        }
        return obj;
    }

    /**
     * 封装请求协议对象
     */
    private RpcProtocol<RpcRequest> getSendRequest(Method method, Object[] args) {
        RpcProtocol<RpcRequest> requestRpcProtocol = new RpcProtocol<RpcRequest>();

        requestRpcProtocol.setHeader(RpcHeaderFactory.getRequestHeader(serializationType, RpcType.REQUEST.getType()));

        RpcRequest request = new RpcRequest();
        request.setVersion(this.serviceVersion);
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setGroup(this.serviceGroup);
        request.setParameters(args);
        request.setAsync(async);
        request.setOneway(oneway);
        requestRpcProtocol.setBody(request);

        // Debug
        LOGGER.debug(method.getDeclaringClass().getName());
        LOGGER.debug(method.getName());

        if (method.getParameterTypes() != null && method.getParameterTypes().length > 0) {
            for (int i = 0; i < method.getParameterTypes().length; ++i) {
                LOGGER.debug(method.getParameterTypes()[i].getName());
            }
        }

        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; ++i) {
                LOGGER.debug(args[i].toString());
            }
        }
        return requestRpcProtocol;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class == method.getDeclaringClass()) {
            String name = method.getName();
            // 根据方法名执行相应的逻辑
            if ("equals".equals(name)) {
                return proxy == args[0]; // 比较代理对象与参数是否相等
            } else if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy); // 返回代理对象的系统身份哈希码
            } else if ("toString".equals(name)) {
                // 返回代理对象的字符串表示
                return proxy.getClass().getName() + "@" +
                        Integer.toHexString(System.identityHashCode(proxy)) +
                        ", with InvocationHandler " + this;
            } else {
                // 如果方法名不是预期内的一个，抛出异常
                throw new IllegalStateException(String.valueOf(method));
            }
        }
        //开启缓存，直接调用方法请求服务提供者
        if (enableResultCache) return invokeSendRequestMethodCache(method, args);
        return invokeSendRequestMethodWithFallback(method, args);
    }

    @Override
    public RPCFuture call(String funcName, Object... args) {
        RpcProtocol<RpcRequest> request = getCallRequest(this.clazz.getName(), funcName, args);
        RPCFuture rpcFuture = null;
        try {
            rpcFuture = this.consumer.sendRequest(request, registryService);
        } catch (Exception e) {
            exceptionPostProcessor.postExceptionProcessor(e);
            LOGGER.error("async all throws exception:{}", e);
        }
        return rpcFuture;
    }

    private RpcProtocol<RpcRequest> getCallRequest(String className, String methodName, Object[] args) {
        RpcProtocol<RpcRequest> requestRpcProtocol = new RpcProtocol<RpcRequest>();
        requestRpcProtocol.setHeader(RpcHeaderFactory.getRequestHeader(serializationType));
        RpcRequest request = new RpcRequest();
        request.setClassName(className);
        request.setMethodName(methodName);
        request.setGroup(this.serviceGroup);
        request.setParameters(args);
        request.setVersion(this.serviceVersion);

        Class[] parameterTypes = new Class[args.length];
        // Get the right class type
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = getClassType(args[i]);
        }
        request.setParameterTypes(parameterTypes);
        requestRpcProtocol.setBody(request);

        LOGGER.debug(className);
        LOGGER.debug(methodName);
        for (int i = 0; i < parameterTypes.length; ++i) {
            LOGGER.debug(parameterTypes[i].getName());
        }
        for (int i = 0; i < args.length; ++i) {
            LOGGER.debug(args[i].toString());
        }

        return requestRpcProtocol;
    }

    private Class<?> getClassType(Object obj) {
        Class<?> classType = obj.getClass();
        String typeName = classType.getName();
        switch (typeName) {
            case "java.lang.Integer":
                return Integer.TYPE;
            case "java.lang.Long":
                return Long.TYPE;
            case "java.lang.Float":
                return Float.TYPE;
            case "java.lang.Double":
                return Double.TYPE;
            case "java.lang.Character":
                return Character.TYPE;
            case "java.lang.Boolean":
                return Boolean.TYPE;
            case "java.lang.Short":
                return Short.TYPE;
            case "java.lang.Byte":
                return Byte.TYPE;
        }
        return classType;
    }
}
