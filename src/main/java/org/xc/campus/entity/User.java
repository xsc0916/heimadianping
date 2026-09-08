package org.xc.campus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表实体，对应 schema.sql 的 user 表。
 * @TableName 告诉 MyBatis-Plus 这个类对应哪张表，之后的增删改查一行 SQL 都不用写。
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO) // 主键自增（配合表里的 AUTO_INCREMENT）
    private Long id;

    private String phone;

    private String nickName;

    private String icon;

    /** 0普通用户 1管理员 */
    private Integer role;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
