package io.check.rpc.serialization.protostuff;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import io.check.rpc.common.exception.SerializerException;
import io.check.rpc.serialization.api.Serialization;
import io.check.rpc.spi.annotation.SPIClass;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@SPIClass
public class ProtostuffSerialization implements Serialization {

    private final Logger logger = LoggerFactory.getLogger(ProtostuffSerialization.class);

    /**
     * 用于缓存Schema的映射，键为类类型，值为对应的Schema对象。
     * 使用ConcurrentHashMap保证线程安全，在多线程环境下可以安全地读写缓存。
     */
    private Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<>();

    /**
     * Objenesis实例，用于无参构造函数创建对象。
     * 设置为true表示启用延迟加载，可以减少在不需要时对类的加载。
     */
    private Objenesis objenesis = new ObjenesisStd(true);

    /**
     * 根据给定的类获取对应的Schema。
     * 此方法会首先尝试从缓存中获取Schema，如果缓存中不存在，则创建一个新的Schema并加入缓存。
     *
     * @param cls 要获取Schema的类对象
     * @return 对应类的Schema对象
     * @SuppressWarnings("unchecked") 用于忽略未经检查的转换警告
     */
    @SuppressWarnings("unchecked") // 忽略未经检查的转换警告
    private <T> Schema<T> getSchema(Class<T> cls) {
        // 尝试从缓存中获取Schema
        Schema<T> schema = (Schema<T>) cachedSchema.get(cls);
        if (schema == null) {
            // 如果缓存中不存在，则创建新Schema并加入缓存
            schema = RuntimeSchema.createFrom(cls);
            if (schema != null) {
                cachedSchema.put(cls, schema);
            }
        }
        return schema;
    }

    /**
     * 序列化（对象 -> 字节数组）
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> byte[] serialize(T obj) {
        logger.info("execute protostuff serialize...");
        if (obj == null){
            throw new SerializerException("serialize object is null");
        }
        // 获取对象的Class类型
        Class<T> cls = (Class<T>) obj.getClass();
        // 分配一个链式缓冲区用于序列化
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            // 获取对应类型的schema
            Schema<T> schema = getSchema(cls);
            // 使用Protostuff库将对象序列化为字节数组
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        }catch (Exception e) {
            // 抛出序列化异常，包含原始异常信息
            throw new SerializerException(e.getMessage(), e);
        }finally {
            // 清理缓冲区，释放资源
            buffer.clear();
        }

    }

    /**
     * 反序列化（字节数组 -> 对象）
     */
    @Override
    public <T> T deserialize(byte[] data, Class<T> cls) {
        logger.info("execute protostuff deserialize...");
        // 检查传入的二进制数据是否为null
        if (data == null){
            throw new SerializerException("deserialize data is null");
        }
        try {
            // 使用Objenesis实例化对象，避免调用构造函数
            T message = (T) objenesis.newInstance(cls);
            // 获取对应类型的Schema
            Schema<T> schema = getSchema(cls);
            // 使用Protostuff库将二进制数据合并到对象中
            ProtostuffIOUtil.mergeFrom(data, message, schema);
            return message;
        } catch (Exception e) {
            // 将内部异常封装并抛出为SerializerException
            throw new SerializerException(e.getMessage(), e);
        }
    }
}
