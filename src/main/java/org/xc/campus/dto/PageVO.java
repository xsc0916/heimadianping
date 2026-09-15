package org.xc.campus.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 通用分页返回壳：{ total, list }，对应文档 5.3.2 的响应结构
 */
@Data
@AllArgsConstructor
public class PageVO<T> {

    private Long total;
    private List<T> list;

    public static <T> PageVO<T> of(Page<T> page) {
        return new PageVO<>(page.getTotal(), page.getRecords());
    }
}
