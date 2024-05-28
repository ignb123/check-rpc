package io.check.rpc.fusing.percent;

import io.check.rpc.constants.RpcConstants;
import io.check.rpc.fusing.base.AbstractFusingInvoker;
import io.check.rpc.spi.annotation.SPIClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SPIClass
public class PercentFusingInvoker extends AbstractFusingInvoker {
    private final Logger logger = LoggerFactory.getLogger(PercentFusingInvoker.class);

    @Override
    public boolean invokeFusingStrategy() {
        boolean result = false;
        switch (fusingStatus.get()){
            //关闭状态
            case RpcConstants.FUSING_STATUS_CLOSED:
                result =  this.invokeClosedFusingStrategy();
                break;
            //半开启状态
            case RpcConstants.FUSING_STATUS_HALF_OPEN:
                result = this.invokeHalfOpenFusingStrategy();
                break;
            //开启状态
            case RpcConstants.FUSING_STATUS_OPEN:
                result =  this.invokeOpenFusingStrategy();
                break;
            default:
                result = this.invokeClosedFusingStrategy();
                break;
        }
        logger.info("execute percent fusing strategy, current fusing status is {}", fusingStatus.get());
        return result;
    }

    /**
     * 处理开启状态的逻辑
     */
    private boolean invokeOpenFusingStrategy() {
        //获取当前时间
        long currentTimeStamp = System.currentTimeMillis();
        //超过一个指定的时间范围，则将状态设置为半开启状态
        if(currentTimeStamp - lastTimeStamp >= milliSeconds){
            fusingStatus.set(RpcConstants.FUSING_STATUS_HALF_OPEN);
            lastTimeStamp = currentTimeStamp;
            this.resetCount();
            return false;
        }
        return true;
    }

    /**
     * 处理半开启状态的逻辑
     */
    private boolean invokeHalfOpenFusingStrategy() {
        //获取当前时间
        long currentTimeStamp = System.currentTimeMillis();
        //服务已经恢复
        if(currentFailureCounter.get() <= 0){
            lastTimeStamp = currentTimeStamp;
            fusingStatus.set(RpcConstants.FUSING_STATUS_CLOSED);
            this.resetCount();
            return false;
        }
        //服务未恢复
        lastTimeStamp = currentTimeStamp;
        fusingStatus.set(RpcConstants.FUSING_STATUS_OPEN);
        return true;
    }

    /**
     * 处理关闭状态的逻辑
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
        //如果当前错误百分比大于或等于配置的百分比
        if(this.getCurrentPercent() >= totalFailure){
            lastTimeStamp = currentTimeStamp;
            fusingStatus.set(RpcConstants.FUSING_STATUS_OPEN);
            return true;
        }
        return false;
    }

    /**
     * 计算当前错误百分比
     */
    private double getCurrentPercent(){
        if (currentCounter.get() <= 0) return 0;
        return (double) currentFailureCounter.get() / currentCounter.get() * 100;
    }
}
