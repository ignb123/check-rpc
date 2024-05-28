package io.check.rpc.fusing.counter;

import io.check.rpc.constants.RpcConstants;
import io.check.rpc.fusing.base.AbstractFusingInvoker;
import io.check.rpc.spi.annotation.SPIClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SPIClass
public class CounterFusingInvoker extends AbstractFusingInvoker {

    private final Logger logger = LoggerFactory.getLogger(CounterFusingInvoker.class);
    @Override
    public boolean invokeFusingStrategy() {
        boolean result = false;
        switch (fusingStatus.get()){
            //关闭状态
            case RpcConstants.FUSING_STATUS_CLOSED:
                result = this.invokeClosedFusingStrategy();
                break;
            //半开启状态
            case RpcConstants.FUSING_STATUS_HALF_OPEN:
                result = this.invokeHalfOpenFusingStrategy();
                break;
            //开启状态
            case RpcConstants.FUSING_STATUS_OPEN:
                result = this.invokeOpenFusingStrategy();
                break;
            default:
                result = this.invokeClosedFusingStrategy();
                break;
        }
        logger.info("execute counter fusing strategy, current fusing status is {}", fusingStatus.get());
        return result;
    }

    private boolean invokeOpenFusingStrategy() {
        //获取当前时间
        long currentTimeStamp = System.currentTimeMillis();
        if(currentTimeStamp - lastTimeStamp >= milliSeconds){
            fusingStatus.set(RpcConstants.FUSING_STATUS_HALF_OPEN);
            lastTimeStamp = currentTimeStamp;
            this.resetCount();
            return false;
        }
        return true;
    }

    /**
     * 处理半开启状态
     */
    private boolean invokeHalfOpenFusingStrategy() {
        //获取当前时间
        long currentTimeStamp = System.currentTimeMillis();
        //服务已经恢复
        if(currentFailureCounter.get() <= 0){
            fusingStatus.set(RpcConstants.FUSING_STATUS_CLOSED);
            lastTimeStamp = currentTimeStamp;
            this.resetCount();
            return false;
        }
        //服务未恢复
        fusingStatus.set(RpcConstants.FUSING_STATUS_OPEN);
        lastTimeStamp = currentTimeStamp;
        return true;
    }

    /**
     * 处理关闭状态逻辑
     */
    private boolean invokeClosedFusingStrategy() {
        //获取当前时间
        long currentTimeStamp = System.currentTimeMillis();
        //超过一个指定的时间范围
        if(currentTimeStamp - lastTimeStamp >= milliSeconds){
            lastTimeStamp = currentTimeStamp;
            this.resetCount();
            return false;
        }
        //超出配置的错误数量
        if(currentFailureCounter.get() >= totalFailure){
            lastTimeStamp = currentTimeStamp;
            fusingStatus.set(RpcConstants.FUSING_STATUS_OPEN);
            return true;
        }
        return false;
    }
}
