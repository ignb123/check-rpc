package io.check.rpc.test.spi;

import io.check.rpc.spi.loader.ExtensionLoader;
import io.check.rpc.test.spi.service.SPIService;
import org.junit.Test;

public class SPITest {

    @Test
    public void testSpiLoader(){
        SPIService spiService = ExtensionLoader.getExtension(SPIService.class, "spiService");
        String result = spiService.hello("check");
        System.out.println(result);
    }
}
