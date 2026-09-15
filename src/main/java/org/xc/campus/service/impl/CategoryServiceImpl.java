package org.xc.campus.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.xc.campus.entity.Category;
import org.xc.campus.mapper.CategoryMapper;
import org.xc.campus.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public List<Category> listAll() {
        return lambdaQuery().orderByAsc(Category::getSort).list();
    }
}
