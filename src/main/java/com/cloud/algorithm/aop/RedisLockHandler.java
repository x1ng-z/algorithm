package com.cloud.algorithm.aop;

import com.cloud.algorithm.annotation.RedizDistributeLock;
import com.cloud.algorithm.service.KeyGenerant;
import com.cloud.algorithm.service.redizDistributeLock.DistributeLockImp;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/5 13:18
 */
@Component
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class RedisLockHandler implements KeyGenerant {

    @Autowired
    private DistributeLockImp distributeLockImp;


    @Pointcut("@annotation(com.cloud.algorithm.annotation.RedizDistributeLock) && args(modelId,..)")
    public void pointCut4RedisLock(Long modelId) {
    }


    @Around(value = "pointCut4RedisLock(modelId)", argNames = "joinPoint,modelId")
    public Object around(ProceedingJoinPoint joinPoint, Long modelId) throws Throwable {

        boolean isgetLock = false;
        try {
            log.debug("try to lock modelid=" + modelId);
            isgetLock = distributeLockImp.tryLock(generantAlgorithmLockRedisKey(modelId), 20000, 60000, TimeUnit.MILLISECONDS);

            if (isgetLock) {
                log.debug("get to lock modelid=" + modelId);
                return joinPoint.proceed(joinPoint.getArgs());
            } else {
                log.debug("failed to lock modelid=" + modelId);
                throw new RuntimeException("获取模型缓存锁失败");
            }

        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (isgetLock) {
                try {
                    distributeLockImp.Unlock(generantAlgorithmLockRedisKey(modelId));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

        }
        return null;
    }
}
