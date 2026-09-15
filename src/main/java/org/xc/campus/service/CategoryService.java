package org.xc.campus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.xc.campus.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    /** 分类列表（后续会加 Redis 缓存） */
    List<Category> listAll();
}
