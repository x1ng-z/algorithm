package com.cloud.algorithm.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/5 13:40
 */
@Configuration
@Data
@ConfigurationProperties(prefix = "redisdlock")
public class RedisDistributeLockConfig {

    private long waitTime = 20000;
    private long leaseTime = 60000;


}
