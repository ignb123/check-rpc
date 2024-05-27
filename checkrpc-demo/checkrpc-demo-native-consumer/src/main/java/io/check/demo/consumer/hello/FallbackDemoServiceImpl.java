package io.check.demo.consumer.hello;

import io.check.rpc.demo.api.DemoService;

public class FallbackDemoServiceImpl implements DemoService {
    @Override
    public String hello(String name) {
        return "fallback hello " + name;
    }
}
