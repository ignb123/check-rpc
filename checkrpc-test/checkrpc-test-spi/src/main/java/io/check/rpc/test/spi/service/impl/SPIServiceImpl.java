package io.check.rpc.test.spi.service.impl;

import io.check.rpc.spi.annotation.SPIClass;
import io.check.rpc.test.spi.service.SPIService;

@SPIClass
public class SPIServiceImpl implements SPIService {
    @Override
    public String hello(String name) {
        return "hello " + name;
    }
}
