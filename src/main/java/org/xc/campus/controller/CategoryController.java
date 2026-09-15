package org.xc.campus.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xc.campus.entity.Category;
import org.xc.campus.service.CategoryService;
import org.xc.campus.utils.Result;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** 5.3.1 分类列表 */
    @GetMapping("/list")
    public Result<List<Category>> list() {
        return Result.ok(categoryService.listAll());
    }
}
