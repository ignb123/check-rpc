package io.check.rpc.serialization.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.check.rpc.common.exception.SerializerException;
import io.check.rpc.serialization.api.Serialization;
import io.check.rpc.spi.annotation.SPIClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;

@SPIClass
public class JsonSerialization implements Serialization {

    private final Logger logger = LoggerFactory.getLogger(JsonSerialization.class);

    private static ObjectMapper objMapper = new ObjectMapper();

    static {
        // 创建并配置 ObjectMapper，用于对象的序列化和反序列化
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss"); // 定义日期格式
        objMapper.setDateFormat(dateFormat); // 设置日期格式
        objMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL); // 只序列化非空字段
        objMapper.enable(SerializationFeature.INDENT_OUTPUT); // 启用输出格式缩进
        // 配置 JsonGenerator，不自动关闭目标和JSON内容
        objMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        objMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, false);
        // 禁用一些序列化特性，以优化性能和处理特定场景
        objMapper.disable(SerializationFeature.FLUSH_AFTER_WRITE_VALUE);
        objMapper.disable(SerializationFeature.CLOSE_CLOSEABLE);
        objMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // 配置 DeserializationFeature，禁用对未知属性的失败处理和允许空bean
        objMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objMapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true); // 忽略未定义的JSON属性

    }

    @Override
    public <T> byte[] serialize(T obj) {
        logger.info("execute json serialize...");
        if (obj == null){
            throw new SerializerException("serialize object is null");
        }
        byte[] bytes = new byte[0];
        try {
            bytes = objMapper.writeValueAsBytes(obj);
        }catch (JsonProcessingException e) {
            throw new SerializerException(e.getMessage(), e);
        }
        return bytes;
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> cls) {
        logger.info("execute json deserialize...");
        if (data == null){
            throw new SerializerException("deserialize data is null");
        }
        T obj = null;
        try {
            obj = objMapper.readValue(data,cls);
        }catch (IOException e) {
            throw new SerializerException(e.getMessage(), e);
        }
        return obj;
    }
}
