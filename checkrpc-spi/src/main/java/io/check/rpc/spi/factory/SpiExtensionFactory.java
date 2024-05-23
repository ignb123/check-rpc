package io.check.rpc.spi.factory;

import io.check.rpc.spi.annotation.SPI;
import io.check.rpc.spi.annotation.SPIClass;
import io.check.rpc.spi.loader.ExtensionLoader;

import java.util.Optional;

@SPIClass
public class SpiExtensionFactory implements ExtensionFactory{
    @Override
    public <T> T getExtension(final String key, final Class<T> clazz) {
        /**
         * 根据给定的类名，加载该类并返回其实例。该方法主要用于加载标注了SPI注解的接口的默认实现。
         *
         * @param clazz 待检查的类，可以为null。
         * @return 如果给定的类是一个标注了SPI注解的接口，则返回该接口的默认实现实例；否则返回null。
         */
        return Optional.ofNullable(clazz)
                .filter(Class::isInterface) // 过滤掉非接口类型的类
                .filter(cls -> cls.isAnnotationPresent(SPI.class)) // 过滤掉未标注SPI注解的接口
                .map(ExtensionLoader::getExtensionLoader) // 获取扩展加载器
                .map(ExtensionLoader::getDefaultSpiClassInstance) // 获取默认扩展实例
                .orElse(null); // 如果没有找到符合条件的接口，则返回null
    }
}
