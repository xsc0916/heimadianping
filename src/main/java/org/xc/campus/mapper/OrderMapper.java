package org.xc.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;
import org.xc.campus.entity.Order;

import java.time.LocalDateTime;

public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 乐观锁支付：只有"待支付 + 版本号未变"才能改成已支付。
     * 影响行数=0 说明订单已被别人（超时关单任务）改过 → 状态竞争由 DB 的 CAS 语义裁决。
     */
    @Update("update orders set status = 1, pay_time = #{payTime}, version = version + 1 " +
            "where id = #{id} and status = 0 and version = #{version}")
    int payWithVersion(Long id, Integer version, LocalDateTime payTime);

    /** 乐观锁关单：仅待支付且已超时才关，影响行数=0 则说明已被支付/取消，跳过回补 */
    @Update("update orders set status = 5, version = version + 1 " +
            "where id = #{id} and status = 0 and version = #{version}")
    int closeWithVersion(Long id, Integer version);
}
