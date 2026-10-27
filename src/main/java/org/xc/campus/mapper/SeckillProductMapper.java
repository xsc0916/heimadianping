package org.xc.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.xc.campus.entity.SeckillProduct;

public interface SeckillProductMapper extends BaseMapper<SeckillProduct> {

    /**
     * 防超卖第二道保险：DB 条件更新。
     * "stock = stock - 1 where stock > 0" 由 InnoDB 行锁保证原子，影响行数=0 即库存已空。
     * Redis 预扣挡掉 99% 流量，这里是最后一道兜底 —— 双保险才敢说不超卖。
     */
    @Update("update seckill_product set stock = stock - 1 where id = #{id} and stock > 0")
    int deductStock(@Param("id") Long id);

    /** 取消/关单时回补秒杀库存 */
    @Update("update seckill_product set stock = stock + 1 where id = #{id}")
    int restoreStock(@Param("id") Long id);
}
