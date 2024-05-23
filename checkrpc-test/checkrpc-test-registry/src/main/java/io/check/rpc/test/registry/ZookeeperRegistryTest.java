package io.check.rpc.test.registry;

import io.check.rpc.protocol.meta.ServiceMeta;
import io.check.rpc.registry.api.RegistryService;
import io.check.rpc.registry.api.config.RegistryConfig;
import io.check.rpc.registry.zookeeper.ZookeeperRegistryService;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class ZookeeperRegistryTest {

    private RegistryService registryService;

    private ServiceMeta serviceMeta;

    @Before
    public void init() throws Exception{
        RegistryConfig registryConfig = new RegistryConfig("127.0.0.1:2181", "zookeeper","random");
        this.registryService = new ZookeeperRegistryService();
        this.registryService.init(registryConfig);
        this.serviceMeta = new ServiceMeta(ZookeeperRegistryTest.class.getName(), "1.0.0", "check", "127.0.0.1", 8080);
    }

    @Test
    public void testRegister() throws Exception {
        this.registryService.register(serviceMeta);
    }


    @Test
    public void testUnRegister() throws Exception {
        this.registryService.unRegister(serviceMeta);
    }

    @Test
    public void testDiscovery() throws Exception {
        System.out.println(this.registryService.discovery(RegistryService.class.getName(), "check".hashCode()));
    }

    @Test
    public void testDestroy() throws IOException {
        this.registryService.destroy();
    }
}
