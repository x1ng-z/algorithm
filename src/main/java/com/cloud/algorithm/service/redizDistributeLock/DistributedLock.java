package com.cloud.algorithm.service.redizDistributeLock;

import java.util.concurrent.TimeUnit;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/5 13:37
 */
public interface DistributedLock {


    boolean tryLock(String key,long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException;

    /**
     * Acquires the lock with defined <code>leaseTime</code>.
     * Waits if necessary until lock became available.
     *
     * Lock will be released automatically after defined <code>leaseTime</code> interval.
     *
     * @param leaseTime the maximum time to hold the lock after it's acquisition,
     *        if it hasn't already been released by invoking <code>unlock</code>.
     *        If leaseTime is -1, hold the lock until explicitly unlocked.
     * @param unit the time unit
     *
     */
    void lock(String key,long leaseTime, TimeUnit unit);

    void lock(String key);

    /**
     * Unlocks the lock independently of its state
     *
     * @return <code>true</code> if lock existed and now unlocked
     *          otherwise <code>false</code>
     */
    boolean forceUnlock(String key);

    void Unlock(String key);
}
