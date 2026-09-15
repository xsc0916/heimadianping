package org.xc.campus.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.xc.campus.dto.PageVO;
import org.xc.campus.entity.Product;
import org.xc.campus.service.ProductService;
import org.xc.campus.utils.Result;

/**
 * 商品模块接口，对应文档 5.3。公开接口（无需登录），已在 WebMvcConfig 放行 /api/product/**
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** 5.3.2 商品分页列表（keyword 非空即"周边搜索"） */
    @GetMapping("/page")
    public Result<PageVO<Product>> page(@RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return Result.ok(productService.pageQuery(categoryId, keyword, page, size));
    }

    /** 5.3.4 周边搜索独立入口，与分页共用逻辑（前端搜索页调用） */
    @GetMapping("/search")
    public Result<PageVO<Product>> search(@RequestParam String keyword,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return Result.ok(productService.pageQuery(null, keyword, page, size));
    }

    /** 5.3.3 商品详情 —— 多级缓存主战场，下半场改造 */
    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.ok(productService.queryById(id));
    }
}
