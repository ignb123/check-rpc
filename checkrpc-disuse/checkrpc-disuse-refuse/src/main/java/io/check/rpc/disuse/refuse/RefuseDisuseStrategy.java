package io.check.rpc.disuse.refuse;

import io.check.rpc.common.exception.RefuseException;
import io.check.rpc.disuse.api.DisuseStrategy;
import io.check.rpc.disuse.api.connection.ConnectionInfo;
import io.check.rpc.spi.annotation.SPIClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@SPIClass
public class RefuseDisuseStrategy implements DisuseStrategy {
    private final Logger logger = LoggerFactory.getLogger(RefuseDisuseStrategy.class);

    @Override
    public ConnectionInfo selectConnection(List<ConnectionInfo> connectionList) {
        logger.info("execute refuse disuse strategy...");
        throw new RefuseException("refuse new connection...");
    }
}
