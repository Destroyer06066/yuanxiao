package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.Account;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends BaseMapper<Account> {

    /**
     * 通过 QueryWrapper 查询，保证 MyBatis-Plus 结果映射正确填充 accountId。
     */
    default Optional<Account> findByUsername(String username) {
        LambdaQueryWrapper<Account> q = new LambdaQueryWrapper<>();
        q.apply("LOWER(username) = LOWER({0})", username).last("LIMIT 1");
        return Optional.ofNullable(selectOne(q));
    }

    /**
     * 只更新登录相关字段，不依赖 entity 的 @TableId 填充。
     * 避免 findByUsername 返回的 accountId 为 null 时 updateById 失效。
     */
    @Update("UPDATE account SET failed_login_count = #{failedLoginCount}, " +
            "locked_until = #{lockedUntil}, last_login_at = #{lastLoginAt} " +
            "WHERE account_id = #{accountId}")
    int updateLoginFields(@Param("accountId") UUID accountId,
                           @Param("failedLoginCount") int failedLoginCount,
                           @Param("lockedUntil") Instant lockedUntil,
                           @Param("lastLoginAt") Instant lastLoginAt);

    default Optional<Account> findById(UUID accountId) {
        return Optional.ofNullable(selectById(accountId));
    }

    @Select("SELECT EXISTS(SELECT 1 FROM account WHERE LOWER(username) = LOWER(#{username}))")
    boolean existsByUsername(@Param("username") String username);

    @Select("SELECT EXISTS(SELECT 1 FROM account WHERE school_id = #{schoolId} AND role = 'SCHOOL_ADMIN' AND status != 'INACTIVE')")
    boolean existsSchoolAdmin(@Param("schoolId") UUID schoolId);

    @Select("SELECT EXISTS(SELECT 1 FROM account WHERE school_id = #{schoolId} AND status != 'INACTIVE')")
    int countActiveBySchool(@Param("schoolId") UUID schoolId);
}
