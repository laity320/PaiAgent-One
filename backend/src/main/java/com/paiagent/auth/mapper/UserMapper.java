package com.paiagent.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.paiagent.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
