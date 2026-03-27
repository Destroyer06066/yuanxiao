package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.platform.entity.School;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;
import java.util.UUID;

@Mapper
public interface SchoolRepository extends BaseMapper<School> {

    @Select("<script>SELECT * FROM school" +
            "<where>" +
            "<if test='keyword != null and keyword != &apos;&apos;'>" +
            " AND (school_name ILIKE '%' || #{keyword} || '%'" +
            " OR school_short_name ILIKE '%' || #{keyword} || '%')" +
            "</if>" +
            "<if test='province != null and province != &apos;&apos;'> AND province = #{province}</if>" +
            "<if test='schoolType != null and schoolType != &apos;&apos;'> AND school_type = #{schoolType}</if>" +
            "<if test='status != null and status != &apos;&apos;'> AND status = #{status}</if>" +
            "</where> ORDER BY created_at DESC</script>")
    IPage<School> pageQuery(Page<School> page,
                           @Param("keyword") String keyword,
                           @Param("province") String province,
                           @Param("schoolType") String schoolType,
                           @Param("status") String status);

    @Select("SELECT * FROM school WHERE school_id = #{schoolId}")
    Optional<School> findById(@Param("schoolId") UUID schoolId);

    @Select("SELECT EXISTS(SELECT 1 FROM school WHERE LOWER(school_name) = LOWER(#{name}))")
    boolean existsByName(@Param("name") String name);

    @Select("SELECT EXISTS(SELECT 1 FROM school WHERE LOWER(school_name) = LOWER(#{name}) AND school_id != #{excludeId})")
    boolean existsByNameExcluding(@Param("name") String name, @Param("excludeId") UUID excludeId);
}
