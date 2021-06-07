package com.cloud.algorithm.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;


@Slf4j
@Configuration
public class RestTemplateConfig {

    @Bean("nocloud")
    public RestTemplate nocloudRestTemplate() {
        HttpComponentsClientHttpRequestFactory httpRequestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient());
        return new RestTemplate(httpRequestFactory);
    }

    @Bean("cloud")
    @LoadBalanced
    public RestTemplate cloudRestTemplate(){
        return new RestTemplate();
    }

    /**
     * http client 配置
     *
     * @return CloseableHttpClient
     */
    @Bean
    public CloseableHttpClient httpClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        // 设置整个连接池最大连接数 根据自己的场景决定
        connectionManager.setMaxTotal(100);
        // 路由是对maxTotal的细分
        connectionManager.setDefaultMaxPerRoute(50);

        RequestConfig requestConfig = RequestConfig.custom()
                // 服务器返回数据(response)的时间，超过该时间抛出read timeout
                .setSocketTimeout(40000)
                // 连接上服务器(握手成功)的时间，超出该时间抛出connect timeout
                .setConnectTimeout(10000)
                // 从连接池中获取连接的超时时间，超过该时间未拿到可用连接，会抛出org.apache.http.conn.ConnectionPoolTimeoutException
                .setConnectionRequestTimeout(5000)
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(retryHandler())
                .build();
    }

    /**
     * 重试机制
     *
     * @return HttpRequestRetryHandler
     */
    private HttpRequestRetryHandler retryHandler() {
        // 重试3次
        int retryTimes = 3;
        // 请求重试处理
        return (exception, executionCount, context) -> {

            if (executionCount >= retryTimes) {
                // 重试超过次数,放弃请求
                log.error("retry has more than {} time, give up request", retryTimes);
                return false;
            }
            if (exception instanceof NoHttpResponseException) {
                // 服务器没有响应,可能是服务器断开了连接,应该重试
                log.error("receive no response from server, retry");
                return true;
            }
            if (exception instanceof SSLHandshakeException) {
                // SSL握手异常
                log.error("SSL hand shake exception");
                return false;
            }
            if (exception instanceof InterruptedIOException) {
                // 超时是否重试
                return false;
            }
            if (exception instanceof UnknownHostException) {
                // 服务器不可达
                log.error("server host unknown");
                return true;
            }
            if (exception instanceof SSLException) {
                log.error("SSLException");
                return false;
            }

            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            // 如果请求不是关闭连接的请求
            return !(request instanceof HttpEntityEnclosingRequest);
        };
    }
}
