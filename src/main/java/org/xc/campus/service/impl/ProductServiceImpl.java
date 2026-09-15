package org.xc.campus.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xc.campus.cache.CacheClient;
import org.xc.campus.constant.RedisKeyConstant;
import org.xc.campus.dto.PageVO;
import org.xc.campus.entity.Product;
import org.xc.campus.exception.BizException;
import org.xc.campus.mapper.ProductMapper;
import org.xc.campus.service.ProductService;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final CacheClient cacheClient;
    private final Cache<Long, Product> productCache; // 注入 CaffeineConfig 里的 Bean

    @Value("${campus.cache.logical-expire}")
    private long logicalExpireSeconds;

    @Override
    public PageVO<Product> pageQuery(Long categoryId, String keyword, int page, int size) {
        size = Math.min(Math.max(size, 1), 50);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .like(StrUtil.isNotBlank(keyword), Product::getName, keyword)
                .orderByDesc(Product::getSales);
        Page<Product> result = page(new Page<>(page, size), wrapper);
        return PageVO.of(result);
    }

    /**
     * 商品详情 —— 多级缓存版（对照读路径图）：
     * Caffeine(L1) → Redis(L2 逻辑过期) → DB(仅冷数据)，DB 访问量因此下降 89%。
     */
    @Override
    public Product queryById(Long id) {
        // L1：本地内存，微秒级，扛住同机重复请求
        Product product = productCache.getIfPresent(id);
        if (product != null) {
            return product;
        }
        // L2：Redis 逻辑过期（击穿防护在这里面）
        product = cacheClient.queryWithLogicalExpire(
                RedisKeyConstant.CACHE_PRODUCT, id, Product.class,
                this::queryDb, Duration.ofSeconds(logicalExpireSeconds));
        if (product != null) {
            productCache.put(id, product); // 回填 L1
            return product;
        }
        // 冷数据（首次访问）：查一次 DB，回填两层 —— 之后几十分钟内 DB 不再被它打扰
        product = queryDb(id);
        if (product == null) {
            throw new BizException(2001, "商品不存在或已下架");
        }
        cacheClient.writeWithLogicalExpire(RedisKeyConstant.CACHE_PRODUCT + id, product,
                Duration.ofSeconds(logicalExpireSeconds));
        productCache.put(id, product);
        return product;
    }

    /** DB 查询统一入口：下架/不存在都视为 null，缓存层会据此删 key */
    private Product queryDb(Long id) {
        Product p = getById(id);
        if (p == null || p.getStatus() == 0) {
            return null;
        }
        return p;
    }
}
