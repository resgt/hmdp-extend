package com.hmdp.common.redislock;

public interface ILock {

    /**
     * 尝试获取锁
     * @param timeoutSec 持有锁的过期时间，过期后自动释放锁
     * @return true 表示获取成功  false 表示获取失败
     */
    boolean tryGetLock(Long timeoutSec);

    /**
     * 释放锁
     */
    void unLock();

}
