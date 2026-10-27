package org.xc.campus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.xc.campus.dto.*;
import org.xc.campus.entity.SeckillProduct;

import java.util.List;

public interface SeckillService extends IService<SeckillProduct> {

    /** 秒杀场次列表：进行中 + 未来3天 */
    List<SeckillListVO> listActive();

    /** 秒杀详情 + 随机码（下单门票） */
    SeckillDetailVO detail(Long seckillId);

    /** 秒杀下单（异步版）：Redis 预扣 → 发 Kafka → 返回排队号 */
    SeckillOrderVO seckillOrder(Long userId, SeckillOrderDTO dto);

    /** 轮询下单结果（纯 Redis） */
    SeckillResultVO getResult(Long userId, String orderNo);
}
