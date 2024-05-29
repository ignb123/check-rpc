package io.check.rpc.exception.processor;

import io.check.rpc.constants.RpcConstants;
import io.check.rpc.spi.annotation.SPI;

/**
 * @author check
 * @version 1.0.0
 * @description 异常信息后置处理器
 */
@SPI(RpcConstants.EXCEPTION_POST_PROCESSOR_PRINT)
public interface ExceptionPostProcessor {
    /**
     * 处理异常信息，进行统计等
     */
    void postExceptionProcessor(Throwable e);

}
