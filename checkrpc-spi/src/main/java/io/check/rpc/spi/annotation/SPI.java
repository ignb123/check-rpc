package io.check.rpc.spi.annotation;

import java.lang.annotation.*;

/**
 * @Documented 标记此注解应该被包含在Javadoc中。
 * @Retention 指定此注解的保留策略为RUNTIME，意味着它可以在运行时通过反射访问。
 * @Target 指定此注解可以应用于TYPE（类、接口、枚举）上。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SPI {

    String value() default "";
}
