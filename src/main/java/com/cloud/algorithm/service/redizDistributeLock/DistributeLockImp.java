package com.cloud.algorithm.service.redizDistributeLock;

import com.cloud.algorithm.config.RedisDistributeLockConfig;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/5 13:38
 */
@Service
@Slf4j
public class DistributeLockImp implements DistributedLock {



    @Autowired
    private RedissonClient redisson;


    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        RLock rLock = redisson.getLock(key);
        return rLock.tryLock(waitTime, leaseTime, unit);
    }

    @Override
    public void lock(String key, long leaseTime, TimeUnit unit) {
        RLock rLock = redisson.getLock(key);
        rLock.lock(leaseTime, unit);
    }

    @Override
    public void lock(String key) {
        RLock rLock = redisson.getLock(key);
        rLock.lock();
    }

    @Override
    public boolean forceUnlock(String key) {
        RLock rLock = redisson.getLock(key);
        rLock.forceUnlock();
        return false;
    }

    @Override
    public void Unlock(String key) {
        RLock rLock = redisson.getLock(key);
        if(rLock.isLocked()){
            rLock.unlock();
        }
    }


}
