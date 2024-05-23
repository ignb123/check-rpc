package io.check.rpc.loadbalancer.random.weight;

import io.check.rpc.loadbalancer.api.ServiceLoadBalancer;
import io.check.rpc.spi.annotation.SPIClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

@SPIClass
public class RandomWeightServiceLoadBalancer<T> implements ServiceLoadBalancer<T>  {

    private final Logger logger = LoggerFactory.getLogger(RandomWeightServiceLoadBalancer.class);

    /**
     * 基于加权随机算法的负载均衡选择方法。
     * 该方法通过给定的服务器列表和哈希码，选择一个服务器进行负载。
     *
     * @param servers 服务器列表，类型为泛型T的列表。该列表不应为null且不能为空。
     * @param hashCode 一个整数哈希码，用于计算选择服务器的权重。
     * @return 返回从服务器列表中根据加权随机算法选择出的一个服务器实例。如果输入列表为空或null，则返回null。
     */
    @Override
    public T select(List<T> servers, int hashCode) {
        logger.info("基于加权随机算法的负载均衡策略...");
        // 检查服务器列表是否为空，防止除零错误和空指针异常
        if (servers == null || servers.isEmpty()){
            return null;
        }
        // 将hashCode取绝对值，确保计算结果为正数
        hashCode = Math.abs(hashCode);
        // 根据hashCode计算选择服务器的权重范围
        int count = hashCode % servers.size();
        // 如果权重范围小于等于1，则将权重范围设置为服务器列表的大小，确保能选择到服务器
        if (count <= 1){
            count = servers.size();
        }
        // 创建随机数生成器
        Random random = new Random();
        // 生成一个在权重范围内的随机数，作为选择的服务器索引
        int index = random.nextInt(count);
        // 返回被选中的服务器
        return servers.get(index);
    }

}
