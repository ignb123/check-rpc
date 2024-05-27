package io.check.rpc.connection.manager;

import io.check.rpc.common.exception.RefuseException;
import io.check.rpc.constants.RpcConstants;
import io.check.rpc.disuse.api.DisuseStrategy;
import io.check.rpc.disuse.api.connection.ConnectionInfo;
import io.check.rpc.spi.loader.ExtensionLoader;
import io.netty.channel.Channel;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {

    /**
     * 存储连接的信息，Map对象的Key就是Channel的Id，Value就是封装后的ConnectionInfo对象
     */
    private volatile Map<String, ConnectionInfo> connectionMap = new ConcurrentHashMap<>();
    private final DisuseStrategy disuseStrategy;
    private final int maxConnections;
    private static volatile ConnectionManager instance;

    private ConnectionManager(int maxConnections, String disuseStrategyType){
        this.maxConnections = maxConnections <= 0 ? Integer.MAX_VALUE : maxConnections;
        disuseStrategyType = StringUtils.isEmpty(disuseStrategyType) ?
                RpcConstants.RPC_CONNECTION_DISUSE_STRATEGY_DEFAULT : disuseStrategyType;
        this.disuseStrategy = ExtensionLoader.getExtension(DisuseStrategy.class, disuseStrategyType);
    }

    public static ConnectionManager getInstance(int maxConnections, String disuseStrategyType){
        if (instance == null){
            synchronized (ConnectionManager.class){
                if (instance == null){
                    instance = new ConnectionManager(maxConnections, disuseStrategyType);
                }
            }
        }
        return instance;
    }

    public void add(Channel channel){
        ConnectionInfo info = new ConnectionInfo(channel);
        if (this.checkConnectionList(info)){
            connectionMap.put(getKey(channel), info);
        }
    }

    public void remove(Channel channel){
        connectionMap.remove(getKey(channel));
    }

    public void update(Channel channel){
        ConnectionInfo info = connectionMap.get(getKey(channel));
        // 更新最后使用时间
        info.setLastUseTime(System.currentTimeMillis());
        info.incrementUseCount();
        connectionMap.put(getKey(channel), info);
    }

    /**
     * 检查当前连接列表是否超过最大连接数，并根据策略移除过期连接。
     *
     * @param info 提供的连接信息，用于检查和可能的关闭操作。
     * @return true 如果连接列表未超过最大连接数或成功移除一个连接；否则返回false。
     */
    private boolean checkConnectionList(ConnectionInfo info) {
        // 从连接映射中收集所有当前活动的连接信息
        List<ConnectionInfo> connectionList = new ArrayList<>(connectionMap.values());
        // 如果当前连接数达到或超过最大连接数
        if (connectionList.size() >= maxConnections){
            try{
                // 根据移除策略选择一个要移除的连接
                ConnectionInfo cacheConnectionInfo = disuseStrategy.selectConnection(connectionList);
                if (cacheConnectionInfo != null){
                    // 关闭选定的连接并从映射中移除
                    cacheConnectionInfo.getChannel().close();
                    connectionMap.remove(getKey(cacheConnectionInfo.getChannel()));
                }
            }catch (RefuseException e){
                // 如果移除连接时拒绝执行，关闭提供的连接并返回false
                info.getChannel().close();
                return false;
            }
        }
        // 如果未超过最大连接数或成功处理移除，返回true
        return true;
    }

    /**
     * 获取给定 Channel 的 ID，并以长文本形式返回。
     *
     * @param channel 传入的 Channel 对象，不能为 null。
     * @return 返回该 Channel 的 ID，以长文本形式表示。
     */
    private String getKey(Channel channel){
        // 转换 Channel 的 ID 为长文本形式并返回
        return channel.id().asLongText();
    }
}
