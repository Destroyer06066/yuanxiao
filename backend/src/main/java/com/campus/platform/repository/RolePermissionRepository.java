package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface RolePermissionRepository extends BaseMapper<RolePermission> {

    @Delete("DELETE FROM role_permission WHERE role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") java.util.UUID roleId);
}
