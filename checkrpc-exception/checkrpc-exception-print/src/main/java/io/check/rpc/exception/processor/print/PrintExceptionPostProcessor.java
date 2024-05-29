package io.check.rpc.exception.processor.print;

import io.check.rpc.exception.processor.ExceptionPostProcessor;
import io.check.rpc.spi.annotation.SPIClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SPIClass
public class PrintExceptionPostProcessor implements ExceptionPostProcessor {

    private final Logger logger = LoggerFactory.getLogger(PrintExceptionPostProcessor.class);

    @Override
    public void postExceptionProcessor(Throwable e) {
        logger.info("程序抛出异常===>>>{}", e);
    }
}
