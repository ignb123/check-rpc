package io.check.rpc.annotation;

import io.check.rpc.constants.RpcConstants;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author check
 * @version 1.0.0
 * @description checkrpc服务消费者
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Autowired
public @interface RpcReference {

    /**
     * 版本号
     */
    String version() default "1.0.0";

    /**
     * 注册中心类型, 目前的类型包含：zookeeper、nacos、etcd、consul
     */
    String registryType() default "zookeeper";

    /**
     * 注册地址
     */
    String registryAddress() default "127.0.0.1:2181";

    /**
     * 负载均衡类型，默认基于ZK的一致性Hash
     */
    String loadBalanceType() default "zkconsistenthash";

    /**
     * 序列化类型，目前的类型包含：protostuff、kryo、json、jdk、hessian2、fst
     */
    String serializationType() default "protostuff";

    /**
     * 超时时间，默认5s
     */
    long timeout() default 5000;

    /**
     * 是否异步执行
     */
    boolean async() default false;

    /**
     * 是否单向调用
     */
    boolean oneway() default false;

    /**
     * 代理的类型，jdk：jdk代理， javassist: javassist代理, cglib: cglib代理
     */
    String proxy() default "jdk";

    /**
     * 服务分组，默认为空
     */
    String group() default "";

    /**
     * 心跳间隔时间，默认30秒
     */
    int heartbeatInterval() default 30000;

    /**
     * 扫描空闲连接间隔时间，默认60秒
     */
    int scanNotActiveChannelInterval() default 60000;

    /**
     * 重试间隔时间
     */
    int retryInterval() default 1000;

    /**
     * 重试间隔时间
     */
    int retryTimes() default 3;

    /**
     * 是否开启结果缓存
     */
    boolean enableResultCache() default false;

    /**
     * 缓存结果的时长，单位是毫秒
     */
    int resultCacheExpire() default RpcConstants.RPC_SCAN_RESULT_CACHE_EXPIRE;


    /**
     * 是否开启直连服务
     */
    boolean enableDirectServer() default false;

    /**
     * 直连服务的地址
     */
    String directServerUrl() default RpcConstants.RPC_COMMON_DEFAULT_DIRECT_SERVER;

    //是否开启延迟连接
    boolean enableDelayConnection() default false;

    int corePoolSize() default RpcConstants.DEFAULT_CORE_POOL_SIZE;

    int maximumPoolSize() default RpcConstants.DEFAULT_MAXI_NUM_POOL_SIZE;

    String flowType() default RpcConstants.FLOW_POST_PROCESSOR_PRINT;

    boolean enableBuffer() default false;

    int bufferSize() default RpcConstants.DEFAULT_BUFFER_SIZE;

    /**
     * 容错class
     */
    Class<?> fallbackClass() default void.class;

    /**
     * 容错class名称
     */
    String fallbackClassName() default RpcConstants.DEFAULT_FALLBACK_CLASS_NAME;

    /**
     * 反射类型
     */
    String reflectType() default RpcConstants.DEFAULT_REFLECT_TYPE;

    /**
     * 是否开启限流
     */
    boolean enableRateLimiter() default false;

    /**
     * 限流类型
     */
    String rateLimiterType() default RpcConstants.DEFAULT_RATELIMITER_INVOKER;

    /**
     * 在milliSeconds毫秒内最多能够通过的请求个数
     */
    int permits() default RpcConstants.DEFAULT_RATELIMITER_PERMITS;

    /**
     * 毫秒数
     */
    int milliSeconds() default RpcConstants.DEFAULT_RATELIMITER_MILLI_SECONDS;

    /**
     * 当限流失败时的处理策略
     */
    String rateLimiterFailStrategy() default RpcConstants.RATE_LIMILTER_FAIL_STRATEGY_DIRECT;

    /**
     * 是否开启熔断策略
     */
    boolean enableFusing() default false;

    /**
     * 熔断规则标识
     */
    String fusingType() default RpcConstants.DEFAULT_FUSING_INVOKER;

    /**
     * 在fusingMilliSeconds毫秒内触发熔断操作的上限值
     */
    double totalFailure() default RpcConstants.DEFAULT_FUSING_TOTAL_FAILURE;

    /**
     * 熔断的毫秒时长
     */
    int fusingMilliSeconds() default RpcConstants.DEFAULT_FUSING_MILLI_SECONDS;

    /**
     * 异常处理类型
     */
    String exceptionPostProcessorType() default RpcConstants.EXCEPTION_POST_PROCESSOR_PRINT;

}
