package io.check.rpc.provider.common.server.base.BaseServer;

import io.check.rpc.codec.RpcDecoder;
import io.check.rpc.codec.RpcEncoder;
import io.check.rpc.provider.common.handler.RpcProviderHandler;
import io.check.rpc.provider.common.server.api.Server.Server;
import io.check.rpc.registry.api.RegistryService;
import io.check.rpc.registry.api.config.RegistryConfig;
import io.check.rpc.registry.zookeeper.ZookeeperRegistryService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class BaseServer implements Server {
    private final Logger logger = LoggerFactory.getLogger(BaseServer.class);

    //主机域名或IP地址
    protected String host = "127.0.0.1";

    // 端口号
    protected int port = 27110;

    // 存储的是实体类
    protected Map<String,Object> handlerMap = new HashMap<>();

    // 注册中心
    protected RegistryService registryService;

    private String reflectType;

    public BaseServer(String serverAddress, String registryAddress, String registryType, String reflectType){
        if(!StringUtils.isEmpty(serverAddress)){
            String[] serverArray = serverAddress.split(":");
            this.host = serverArray[0];
            this.port = Integer.parseInt(serverArray[1]);
        }

        this.reflectType = reflectType;
        this.registryService = this.getRegistryService(registryAddress,registryType);
    }

    private RegistryService getRegistryService(String registryAddress, String registryType) {
        //TODO 后续扩展支持SPI
        RegistryService registryService = null;
        try {
            registryService = new ZookeeperRegistryService();
            registryService.init(new RegistryConfig(registryAddress, registryType));
        }catch (Exception e){
            logger.error("RPC Server init error", e);
        }
        return registryService;
    }

    @Override
    public void startNettyServer() {
        // 创建用于接受进来的连接的EventLoopGroup
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        // 创建用于处理连接的EventLoopGroup
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 初始化ServerBootstrap，配置服务器参数
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup,workerGroup)
                    // 指定使用的NIO通道类型
                    .channel(NioServerSocketChannel.class)
                    // 配置子通道的处理器，用于初始化每个新建的连接
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            // 配置ChannelPipeline，添加编解码器和自定义处理器
                            channel.pipeline()
                                    .addLast(new RpcDecoder())
                                    .addLast(new RpcEncoder())
                                    .addLast(new RpcProviderHandler(handlerMap,reflectType));
                        }
                    })
                    // 配置服务器端连接队列大小
                    .option(ChannelOption.SO_BACKLOG,128)
                    // 配置子通道保持活动状态
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            // 绑定端口并同步等待完成
            ChannelFuture future = bootstrap.bind(host, port).sync();
            // 记录服务器启动信息
            logger.info("Server started on {}:{}", host, port);
            // 同步等待通道关闭
            future.channel().closeFuture().sync();
        }catch (Exception e){
            // 记录启动过程中出现的异常
            logger.error("RPC Server start error", e);
        }finally {
            // 优雅关闭EventLoopGroup
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }

    }
}
