package io.check.rpc.disuse.api;

import io.check.rpc.constants.RpcConstants;
import io.check.rpc.disuse.api.connection.ConnectionInfo;
import io.check.rpc.spi.annotation.SPI;

import java.util.List;

/**
 * @author check
 * @version 1.0.0
 * @description 淘汰策略
 */
@SPI(RpcConstants.RPC_CONNECTION_DISUSE_STRATEGY_DEFAULT)
public interface DisuseStrategy {
    /**
     * 从连接列表中根据规则获取一个连接对象
     */
    ConnectionInfo selectConnection(List<ConnectionInfo> connectionList);
}
