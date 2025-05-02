package com.hmdp.voucher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.common.redislock.ILock;
import com.hmdp.common.redislock.impl.AdvancedRedisLock;
import com.hmdp.common.redislock.impl.SimpleRedisLock;
import com.hmdp.common.utils.RedisGlobalIdWorker;
import com.hmdp.common.utils.UserHolder;
import com.hmdp.model.dto.Result;
import com.hmdp.model.dto.UserDTO;
import com.hmdp.model.entity.SeckillVoucher;
import com.hmdp.model.entity.VoucherOrder;
import com.hmdp.voucher.mapper.VoucherOrderMapper;
import com.hmdp.voucher.mq.VoucherOrderProducer;
import com.hmdp.voucher.service.ISecKillVoucherService;
import com.hmdp.voucher.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.Block;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static com.hmdp.common.utils.RedisConstants.LOGIN_USER_KEY;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;

@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private ISecKillVoucherService secKillVoucherService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private VoucherOrderProducer voucherOrderProducer;

    // 业务实现方案一，使用java语言实现
    @Override
    public Result orderSecKillVoucher(Long voucherId, HttpServletRequest request) {
        // 查询优惠劵
        SeckillVoucher voucher = secKillVoucherService.getById(voucherId);
        // 判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) return Result.fail("秒杀尚未开始");
        // 判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) return Result.fail("秒杀已经结束");
        // 判断库存是否充足
        if (voucher.getStock() < 1) return Result.fail("库存不足");
        // 获取当前登录用户id
        Long userId = new UserHolder(stringRedisTemplate).getUser(request).getId();
        // 锁名字（加上优惠劵id，区分同一个用户下不同的优惠劵购买）
//        String lockName = "order" + userId + ":" + voucherId; // 自定义的锁
        String lockName = "Lock:order" + userId + ":" + voucherId;
        // 获取锁对象
//        ILock redisLock = new AdvancedRedisLock(stringRedisTemplate, lockName); // 自定义的锁
        RLock redisLock = redissonClient.getLock(lockName);
        // 获取锁（30秒后自动释放锁，redisson默认是30秒）
        boolean isLock = redisLock.tryLock();
        // 判断是否获取锁成功
        if (!isLock) return Result.fail("您已经购买过该类型优惠劵");
        // 必须先提交事务再释放锁
        try {
            // this调用方法会使事务失效，所以必须先获取当前对象的代理对象，注意，这里的当前对象是IVoucherOrderService接口
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            // 创建订单
            return proxy.createVoucherOrder(voucherId, userId);
        } finally {
            // 释放锁
            redisLock.unlock();
        }
    }

    // 创建订单（实现方案一）
    @Override
    @Transactional
    public Result createVoucherOrder(Long voucherId, Long userId) {
        // 查询订单（一人一单）
        VoucherOrder voucherOrder = this.getOne(new QueryWrapper<VoucherOrder>()
                .eq("user_id", userId)
                .eq("voucher_id", voucherId)
        );
        // 判断订单是否存在
        if (voucherOrder != null) return Result.fail("您已经购买过该类型优惠劵");
        // 库存充足，扣减库存
        boolean isUpdate = secKillVoucherService.updateStockWithOptimisticLock(voucherId, 0);
        // 判断是否更新成功
        if (!isUpdate) return Result.fail("库存异常");
        // 创建订单
        voucherOrder = new VoucherOrder();
        long orderId = new RedisGlobalIdWorker(stringRedisTemplate).nextId("order");
        voucherOrder.setId(orderId); // 订单id
        voucherOrder.setUserId(userId); // 用户id
        voucherOrder.setVoucherId(voucherId); // 代金券id
        voucherOrder.setStatus(1);
        // 保存到数据库
        this.save(voucherOrder);
        // 返回订单id
        return Result.ok(orderId);
    }

    @Override
    public void removeByStatus() {
        baseMapper.deleteByStatus();
    }



    /* TODO 从这里开始，以下部分是第二种业务实现方案，推荐使用，不过比较麻烦，方案一也够用 */

    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
        initFlowRules();
    }

    // 阻塞队列
    private BlockingQueue<VoucherOrder> ordersQueue = new ArrayBlockingQueue<>(1024 * 1024);

    // 线程池
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    private static final DefaultRedisScript<Long> SECKILL_ORDER_SCRIPT;

    static {
        SECKILL_ORDER_SCRIPT = new DefaultRedisScript<>();
        SECKILL_ORDER_SCRIPT.setLocation(new ClassPathResource("lua\\seckill_order.lua"));
        SECKILL_ORDER_SCRIPT.setResultType(Long.class);
    }

    // 代理对象
    private IVoucherOrderService proxy;

    // 业务实现方案二，配合lua脚本实现异步下单
    @Override
    @SentinelResource(value = "seckillVoucher",
            blockHandler = "handleBlockException",
            fallback = "handleFallback")
    public Result orderSecKillVoucherWithLua(Long voucherId, HttpServletRequest request) {
        // 获取当前登录用户id
        // Long userId = new UserHolder(stringRedisTemplate).getUser(request).getId();
        // 为方便测试 随机生成userId
        Random random = new Random();
        long userId = ThreadLocalRandom.current().nextLong(1000, 100000 + 1);
        log.info("用户id：{}", userId);
        // 执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_ORDER_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                String.valueOf(userId)
        );
        // 判断结果
        int resultValue = result.intValue();
        // 不为0则没有购买资格
        if (resultValue != 0) {
            return Result.fail(resultValue == 1 ? "库存不足" : "不能重复下单");
        }
        // 创建订单id
        long orderId = new RedisGlobalIdWorker(stringRedisTemplate).nextId("order");
        
        // 发送消息到RocketMQ
        voucherOrderProducer.sendOrderMessage(orderId, userId, voucherId);
        
        return Result.ok(orderId);
    }

    // 限流处理方法
    public Result handleBlockException(Long voucherId, HttpServletRequest request, BlockException ex) {
        log.error("购买优惠券被限流", ex);
        return Result.fail("系统繁忙，请稍后再试");
    }

    // 降级处理方法
    public Result handleFallback(Long voucherId, HttpServletRequest request, Throwable ex) {
        log.error("购买优惠券异常", ex);
        return Result.fail("系统异常，请稍后再试");
    }

    // 在构造方法或@PostConstruct方法中配置规则
    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();
        
        // QPS限流
        FlowRule rule1 = new FlowRule();
        rule1.setResource("seckillVoucher");
        rule1.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule1.setCount(200);
        
        // 并发线程数限流
        FlowRule rule2 = new FlowRule();
        rule2.setResource("seckillVoucher");
        rule2.setGrade(RuleConstant.FLOW_GRADE_THREAD);
        rule2.setCount(1000);
        
        rules.add(rule1);
        rules.add(rule2);
        FlowRuleManager.loadRules(rules);
    }

    private class VoucherOrderHandler implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    // 获取队列中的订单信息
                    VoucherOrder voucherOrder = ordersQueue.take();
                    // 创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (InterruptedException e) {
                    log.error("处理订单异常", e);
                }
            }
        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        // 获取用户id
        Long userId = voucherOrder.getUserId();
        // 获取优惠劵id
        Long voucherId = voucherOrder.getVoucherId();
        // 锁名字（加上优惠劵id，区分同一个用户下不同的优惠劵购买）
        String lockName = "Lock:order" + userId + ":" + voucherId;
        // 获取锁对象
        RLock redisLock = redissonClient.getLock(lockName);
        // 获取锁（30秒后自动释放锁，redisson默认是30秒）
        boolean isLock = redisLock.tryLock();
        // 判断是否获取锁成功
        if (!isLock) return;
        // 必须先提交事务再释放锁
        try {
            // 创建订单
            proxy.createVoucherOrderWithLua(voucherOrder);
        } finally {
            // 释放锁
            redisLock.unlock();
        }
    }

    // 创建订单
    @Override
    @Transactional
    public void createVoucherOrderWithLua(VoucherOrder voucherOrder) {
        // 查询订单（一人一单）
        VoucherOrder Order = this.getOne(new QueryWrapper<VoucherOrder>()
                .eq("user_id", voucherOrder.getUserId())
                .eq("voucher_id", voucherOrder.getVoucherId())
        );
        // 判断订单是否存在
        if (Order != null) return;
        // 库存充足，扣减库存
        boolean isUpdate = secKillVoucherService.updateStockWithOptimisticLock(voucherOrder.getVoucherId(), 0);
        // 判断是否更新成功
        if (!isUpdate) return;
        // 保存到数据库
        this.save(voucherOrder);
    }
}
