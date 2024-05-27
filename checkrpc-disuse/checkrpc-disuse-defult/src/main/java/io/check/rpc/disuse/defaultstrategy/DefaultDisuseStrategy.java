package io.check.rpc.disuse.defaultstrategy;

import io.check.rpc.disuse.api.DisuseStrategy;
import io.check.rpc.disuse.api.connection.ConnectionInfo;
import io.check.rpc.spi.annotation.SPIClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@SPIClass
public class DefaultDisuseStrategy implements DisuseStrategy {

    private final Logger logger = LoggerFactory.getLogger(DefaultDisuseStrategy.class);

    @Override
    public ConnectionInfo selectConnection(List<ConnectionInfo> connectionList) {
        logger.info("execute default disuse strategy...");
        return connectionList.get(0);
    }
}
