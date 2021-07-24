package com.cloud.algorithm.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 14:51
 */
@ConfigurationProperties(prefix = "xalgorithmroute")
@Data
@Configuration
@RefreshScope
public class AlgorithmRouteConfig {
    private String url;


    private String python = "/customize";

    /**
     * F-PID运算接口
     */
    private String pid = "/pid";

    /**
     * DMC运算接口
     */
    private String dmc = "/dmc";




}
