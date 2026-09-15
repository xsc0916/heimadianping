package org.xc.campus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.xc.campus.dto.PageVO;
import org.xc.campus.entity.Product;

public interface ProductService extends IService<Product> {

    /** 商品分页 + 关键词搜索（周边搜索与分页共用此逻辑） */
    PageVO<Product> pageQuery(Long categoryId, String keyword, int page, int size);

    /** 商品详情（朴素版：直查 DB，下半场改造为多级缓存版） */
    Product queryById(Long id);
}
