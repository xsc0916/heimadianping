package org.xc.campus.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.xc.campus.annotation.RateLimit;
import org.xc.campus.dto.*;
import org.xc.campus.enums.LimitDimension;
import org.xc.campus.service.SeckillService;
import org.xc.campus.utils.Result;
import org.xc.campus.utils.UserHolder;

import java.util.List;

/**
 * 秒杀模块接口，对应文档 5.4。
 * list 公开；detail/order/result 需要登录（不在拦截器放行名单里，LoginInterceptor 兜底）
 */
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    /** 5.4.1 场次列表（公开） */
    @GetMapping("/list")
    public Result<List<SeckillListVO>> list() {
        return Result.ok(seckillService.listActive());
    }

    /** 5.4.2 详情+随机码 */
    @GetMapping("/{id}")
    public Result<SeckillDetailVO> detail(@PathVariable Long id) {
        return Result.ok(seckillService.detail(id));
    }

    /** 5.4.3 下单（异步）：文档典型配置——用户维度 10 秒最多 3 次，防脚本连点 */
    @RateLimit(limit = 3, windowSeconds = 10, dimension = LimitDimension.USER)
    @PostMapping("/order")
    public Result<SeckillOrderVO> order(@RequestBody @Valid SeckillOrderDTO dto) {
        return Result.ok(seckillService.seckillOrder(UserHolder.getUserId(), dto));
    }

    /** 5.4.4 轮询结果 */
    @GetMapping("/result/{orderNo}")
    public Result<SeckillResultVO> result(@PathVariable String orderNo) {
        return Result.ok(seckillService.getResult(UserHolder.getUserId(), orderNo));
    }
}
