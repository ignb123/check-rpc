package io.check.rpc.consumer.spring.context;


import org.springframework.context.ApplicationContext;

/**
 * @author check
 * @version 1.0.0
 * @description Spring上下文
 */
public class RpcConsumerSpringContext {

    /**
     * Spring ApplicationContext
     */
    private ApplicationContext context;

    private RpcConsumerSpringContext(){

    }

    private static class Holder{
        private static final RpcConsumerSpringContext INSTANCE = new RpcConsumerSpringContext();
    }

    public static RpcConsumerSpringContext getInstance(){
        return Holder.INSTANCE;
    }

    public ApplicationContext getContext() {
        return context;
    }

    public void setContext(ApplicationContext context) {
        this.context = context;
    }

}
