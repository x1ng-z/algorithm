package com.cloud.algorithm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/5 10:59
 */
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "mpc")
@Data
public class MpcConfig {

    private int NumPv=8;
    private int NumMv=8;
    private int MumFf=8;

}
