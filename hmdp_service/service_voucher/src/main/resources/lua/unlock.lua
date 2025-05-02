-- 锁的key
local key = KEYS[1]
-- 线程标识
local threadFlag = ARGV[2]
-- 获取锁中的线程标识
local redisThreadFlag = redis.call('get', key)
-- 比较线程标识和锁中的线程标识是否一致
if (redisThreadFlag == threadFlag) then
    -- 一致则释放锁
    return redis.call('del', key)
end
return 0