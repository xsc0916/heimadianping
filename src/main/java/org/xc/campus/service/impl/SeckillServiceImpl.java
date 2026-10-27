package org.xc.campus.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.xc.campus.constant.MqTopic;
import org.xc.campus.constant.RedisKeyConstant;
import org.xc.campus.dto.*;
import org.xc.campus.entity.Product;
import org.xc.campus.entity.SeckillProduct;
import org.xc.campus.exception.BizException;
import org.xc.campus.mapper.ProductMapper;
import org.xc.campus.mapper.SeckillProductMapper;
import org.xc.campus.service.SeckillService;
import org.xc.campus.utils.RedisIdWorker;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl extends ServiceImpl<SeckillProductMapper, SeckillProduct> implements SeckillService {

    private final StringRedisTemplate redis;
    private final RedisIdWorker redisIdWorker;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ProductMapper productMapper;

    /** 秒杀 Lua 从 classpath 加载：脚本超过几行就该放文件统一管理（限流那种短的才适合内联） */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public List<SeckillListVO> listActive() {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillProduct> list = lambdaQuery()
                .gt(SeckillProduct::getEndTime, now)                       // 还没结束
                .lt(SeckillProduct::getBeginTime, now.plusDays(3))         // 且未来3天内已经开始或开始
                .orderByAsc(SeckillProduct::getBeginTime)
                .list();
        return list.stream().map(this::toListVO).toList();
    }

    private SeckillListVO toListVO(SeckillProduct sp) {
        SeckillListVO vo = new SeckillListVO();
        vo.setId(sp.getId());
        vo.setProductId(sp.getProductId());
        Product p = productMapper.selectById(sp.getProductId());
        if (p != null) {
            vo.setProductName(p.getName());
            vo.setCover(p.getCover());
        }
        vo.setSeckillPrice(sp.getSeckillPrice());
        vo.setStock(sp.getStock());
        vo.setBeginTime(sp.getBeginTime());
        vo.setEndTime(sp.getEndTime());
        vo.setStatus(sp.getBeginTime().isAfter(LocalDateTime.now()) ? 0 : 1);
        return vo;
    }

    @Override
    public SeckillDetailVO detail(Long seckillId) {
        SeckillProduct sp = getById(seckillId);
        if (sp == null || sp.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BizException(3001, "秒杀活动不存在或已结束");
        }
        // 随机码由预热任务生成；取不到说明活动还没预热（比如刚导入 DB 但应用没跑预热）
        String code = redis.opsForValue().get(RedisKeyConstant.SECKILL_CODE + seckillId);
        if (code == null) {
            throw new BizException(3001, "活动预热中，请稍后再试");
        }
        Product p = productMapper.selectById(sp.getProductId());
        SeckillDetailVO vo = new SeckillDetailVO();
        vo.setSeckillId(sp.getId());
        vo.setProductId(sp.getProductId());
        vo.setProductName(p != null ? p.getName() : "");
        vo.setSeckillPrice(sp.getSeckillPrice());
        vo.setStock(sp.getStock());
        vo.setCode(code);
        return vo;
    }

    @Override
    public SeckillOrderVO seckillOrder(Long userId, SeckillOrderDTO dto) {
        // ① 随机码校验：只有正常浏览详情页的用户才拿得到，直接构造 URL 的脚本被挡在门外
        String cachedCode = redis.opsForValue().get(RedisKeyConstant.SECKILL_CODE + dto.getSeckillId());
        if (cachedCode == null || !cachedCode.equals(dto.getCode())) {
            throw new BizException(3004, "非法秒杀请求");
        }
        // ② Redis+Lua 原子预扣：一人一单 + 库存判定一次完成，挡掉 99% 的无效流量
        Long result = redis.execute(SECKILL_SCRIPT,
                List.of(RedisKeyConstant.SECKILL_STOCK + dto.getSeckillId(),
                        RedisKeyConstant.SECKILL_BUYERS + dto.getSeckillId()),
                String.valueOf(userId));
        if (result == null) {
            throw new BizException(500, "系统繁忙");
        }
        switch (result.intValue()) {
            case 0 -> { }                                          // 预扣成功，继续受理
            case 1 -> throw new BizException(3003, "请勿重复下单"); // 一人一单
            case 2 -> throw new BizException(3002, "秒杀库存不足");
            default -> throw new BizException(3001, "秒杀活动未开始或已结束");
        }
        // ③ 生成全局唯一订单号（时间戳+Redis自增序列）
        String orderNo = String.valueOf(redisIdWorker.nextId("seckill"));
        // ④ 预写结果"排队中"：轮询接口纯读这个 key，绝不让轮询打到 DB
        redis.opsForValue().set(RedisKeyConstant.SECKILL_ORDER + orderNo,
                JSONUtil.toJsonStr(new SeckillResultVO(orderNo, userId, 0, null, null)),
                Duration.ofMinutes(10));
        // ⑤ 发 Kafka：key=seckillId → 同一活动的消息落同一分区（分区内有序），消费者异步落库
        SeckillOrderMsg msg = new SeckillOrderMsg(orderNo, userId, dto.getSeckillId());
        kafkaTemplate.send(MqTopic.SECKILL_ORDER, String.valueOf(dto.getSeckillId()), JSONUtil.toJsonStr(msg));
        log.info("秒杀受理成功 orderNo={} userId={} seckillId={}", orderNo, userId, dto.getSeckillId());
        // ⑥ 接口的使命到此为止：毫秒级返回排队号，落库交给消费者（削峰）
        return new SeckillOrderVO(orderNo, 0);
    }

    @Override
    public SeckillResultVO getResult(Long userId, String orderNo) {
        String json = redis.opsForValue().get(RedisKeyConstant.SECKILL_ORDER + orderNo);
        if (json == null) {
            throw new BizException(404, "排队结果不存在或已过期");
        }
        SeckillResultVO vo = JSONUtil.toBean(json, SeckillResultVO.class);
        // 只能查自己的单，防止遍历 orderNo 探测别人订单
        if (!userId.equals(vo.getUserId())) {
            throw new BizException(403, "无权查看该订单");
        }
        return vo;
    }
}
