package org.xc.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.xc.campus.entity.User;

/**
 * 继承 BaseMapper<User> 后自动拥有：insert / selectById / update / delete 等单表操作。
 * 这就是选 MyBatis-Plus 的原因：80% 的单表 CRUD 零 SQL。
 */
public interface UserMapper extends BaseMapper<User> {
}
