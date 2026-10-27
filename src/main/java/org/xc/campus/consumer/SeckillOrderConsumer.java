package org.xc.campus.consumer;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.xc.campus.constant.MqTopic;
import org.xc.campus.constant.RedisKeyConstant;
import org.xc.campus.dto.SeckillOrderMsg;
import org.xc.campus.dto.SeckillResultVO;
import org.xc.campus.entity.Order;
import org.xc.campus.entity.SeckillProduct;
import org.xc.campus.mapper.OrderMapper;
import org.xc.campus.mapper.SeckillProductMapper;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 秒杀下单消费者 —— 异步链路的终点：把 Kafka 里的"受理单"真正落成 DB 订单。
 * 接口侧毫秒级返回，落库速度由消费者控制 —— 这就是"削峰"：DB 按自己的节奏干活。
 *
 * 注意 yml 里 enable-auto-commit=false + ack-mode=manual_immediate：
 * 手动 ack 才能实现"业务成功才确认"，失败不 ack → Kafka 重投 → 配合幂等做成 at-least-once。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private final OrderMapper orderMapper;
    private final SeckillProductMapper seckillProductMapper;
    private final StringRedisTemplate redis;

    @KafkaListener(topics = MqTopic.SECKILL_ORDER)
    public void onMessage(String json, Acknowledgment ack) {
        SeckillOrderMsg msg = JSONUtil.toBean(json, SeckillOrderMsg.class);
        log.info("收到秒杀消息 orderNo={} userId={} seckillId={}",
                msg.getOrderNo(), msg.getUserId(), msg.getSeckillId());
        try {
            // ① 幂等：Kafka 是 at-least-once，同一条消息可能被投递多次（重试/重平衡）
            Order exist = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, msg.getOrderNo()));
            if (exist != null) {
                log.info("订单已存在，跳过（幂等）orderNo={}", msg.getOrderNo());
                ack.acknowledge();
                return;
            }

            // ② 防超卖第二道保险：DB 条件更新扣库存（where stock > 0 由行锁保证原子）
            int rows = seckillProductMapper.deductStock(msg.getSeckillId());
            if (rows == 0) {
                // 极端情况：Redis 说还有货，DB 已经空了 → 回补 Redis，标记失败
                rollbackRedis(msg);
                writeResult(msg, 2, null, "秒杀库存不足");
                ack.acknowledge();
                return;
            }

            // ③ 落库：金额以 DB 中的秒杀价为准（不信任消息里的任何价格快照）
            SeckillProduct sp = seckillProductMapper.selectById(msg.getSeckillId());
            Order order = new Order();
            order.setOrderNo(msg.getOrderNo());
            order.setUserId(msg.getUserId());
            order.setProductId(sp.getProductId());
            order.setSeckillFlag(1);
            order.setSeckillId(msg.getSeckillId());
            order.setCount(1);
            order.setAmount(sp.getSeckillPrice());
            order.setStatus(0);   // 待支付
            order.setVersion(0);
            orderMapper.insert(order);

            // ④ 写结果，供前端轮询（status=1 成功 + orderId，用户拿它去支付）
            writeResult(msg, 1, order.getId(), null);
            ack.acknowledge();
            log.info("秒杀单落库成功 orderNo={} orderId={}", msg.getOrderNo(), order.getId());

        } catch (DuplicateKeyException e) {
            // 唯一索引 (user_id, seckill_id) 兜底：并发/重复投递撞索引，说明已处理过
            log.warn("唯一索引拦截重复下单 orderNo={}", msg.getOrderNo());
            ack.acknowledge();
        } catch (Exception e) {
            // 不 ack：让 Kafka 重新投递（配合幂等，重试是安全的）
            log.error("秒杀消息处理失败，等待重试 orderNo={}", msg.getOrderNo(), e);
        }
    }

    /** DB 扣减失败 → 把 Redis 预扣的库存和买家资格还回去，保证账不失衡 */
    private void rollbackRedis(SeckillOrderMsg msg) {
        redis.opsForValue().increment(RedisKeyConstant.SECKILL_STOCK + msg.getSeckillId());
        redis.opsForSet().remove(RedisKeyConstant.SECKILL_BUYERS + msg.getSeckillId(),
                String.valueOf(msg.getUserId()));
    }

    /** 结果写 Redis，纯供轮询（TTL 10min 与下单接口保持一致） */
    private void writeResult(SeckillOrderMsg msg, int status, Long orderId, String failMsg) {
        SeckillResultVO vo = new SeckillResultVO(msg.getOrderNo(), msg.getUserId(), status, orderId, failMsg);
        redis.opsForValue().set(RedisKeyConstant.SECKILL_ORDER + msg.getOrderNo(),
                JSONUtil.toJsonStr(vo), Duration.ofMinutes(10));
    }
}
