package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.Permission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionRepository extends BaseMapper<Permission> {
}
