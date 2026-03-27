package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.Role;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleRepository extends BaseMapper<Role> {
}
