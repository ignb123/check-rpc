package io.check.rpc.disuse.api.connection;

import io.netty.channel.Channel;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author check
 * @version 1.0.0
 * @description 连接信息
 */
public class ConnectionInfo implements Serializable {

    private static final long serialVersionUID = -9165095996736033806L;
    private Channel channel;
    private long connectionTime;
    private long lastUseTime;
    private AtomicInteger useCount = new AtomicInteger(0);

    public ConnectionInfo() {
    }

    public ConnectionInfo(Channel channel) {
        this.channel = channel;
        long currentTimeStamp = System.currentTimeMillis();
        this.connectionTime = currentTimeStamp;
        this.lastUseTime = currentTimeStamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionInfo info = (ConnectionInfo) o;
        return Objects.equals(channel, info.channel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channel);
    }

    public int getUseCount() {
        return useCount.get();
    }

    public int incrementUseCount() {
        return this.useCount.incrementAndGet();
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public long getConnectionTime() {
        return connectionTime;
    }

    public void setConnectionTime(long connectionTime) {
        this.connectionTime = connectionTime;
    }

    public long getLastUseTime() {
        return lastUseTime;
    }

    public void setLastUseTime(long lastUseTime) {
        this.lastUseTime = lastUseTime;
    }

    public void setUseCount(AtomicInteger useCount) {
        this.useCount = useCount;
    }
}
