package io.check.rpc.disuse.lru;

import io.check.rpc.disuse.api.DisuseStrategy;
import io.check.rpc.disuse.api.connection.ConnectionInfo;
import io.check.rpc.spi.annotation.SPIClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SPIClass
public class LruDisuseStrategy implements DisuseStrategy {

    private final Logger logger = LoggerFactory.getLogger(LruDisuseStrategy.class);
    private final Comparator<ConnectionInfo> lastUseTimeComparator = (o1, o2) -> {
        return o1.getLastUseTime() - o2.getLastUseTime() > 0 ? 1 : -1;
    };
    @Override
    public ConnectionInfo selectConnection(List<ConnectionInfo> connectionList) {
        logger.info("execute lru disuse strategy...");
        if (connectionList.isEmpty()) return null;
        Collections.sort(connectionList, lastUseTimeComparator);
        return connectionList.get(0);
    }
}
