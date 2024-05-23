package io.check.rpc.test.spi.service;

import io.check.rpc.spi.annotation.SPI;

@SPI("spiService")
public interface SPIService {
    String hello(String name);
}
