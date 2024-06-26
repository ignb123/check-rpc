package io.check.rpc.serialization.hessian2;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import io.check.rpc.common.exception.SerializerException;
import io.check.rpc.serialization.api.Serialization;
import io.check.rpc.spi.annotation.SPIClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@SPIClass
public class Hessian2Serialization implements Serialization {

    private final Logger logger = LoggerFactory.getLogger(Hessian2Serialization.class);
    @Override
    public <T> byte[] serialize(T obj) {
        logger.info("execute hessian2 serialize...");
        if (obj == null){
            throw new SerializerException("serialize object is null");
        }
        byte[] result = new byte[0];
        // 初始化 ByteArrayOutputStream 和 Hessian2Output
        ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
        Hessian2Output hessian2Output=new Hessian2Output(byteArrayOutputStream);

        try {
            // 开始一条消息，准备写入对象
            hessian2Output.startMessage();
            // 写入对象
            hessian2Output.writeObject(obj);
            // 刷新输出流，确保所有数据都被写入
            hessian2Output.flush();
            // 完成消息，标记序列化结束
            hessian2Output.completeMessage();
            // 将字节流转换为字节数组
            result = byteArrayOutputStream.toByteArray();
        }catch (IOException e){
            // 抛出序列化异常
            throw new SerializerException(e.getMessage(), e);
        }finally {
            // 尝试关闭所有资源，处理异常
            try {
                if(hessian2Output != null){
                    hessian2Output.close();
                }
                byteArrayOutputStream.close();
            } catch (IOException e) {
                throw new SerializerException(e.getMessage(), e);
            }
        }

        return result;
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> cls) {
        logger.info("execute hessian2 deserialize...");

        if (data == null){
            throw new SerializerException("deserialize data is null");
        }
        T obj = null;
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Hessian2Input hessian2Input = new Hessian2Input(byteArrayInputStream);

        try {
            hessian2Input.startMessage();
            obj = (T) hessian2Input.readObject();
            hessian2Input.completeMessage();
        }catch (IOException e){
            throw new SerializerException(e.getMessage(), e);
        }finally {
            try {
                if(hessian2Input != null){
                    hessian2Input.close();
                    byteArrayInputStream.close();
                }
            } catch (IOException e) {
                throw new SerializerException(e.getMessage(), e);
            }
        }

        return obj;
    }
}
