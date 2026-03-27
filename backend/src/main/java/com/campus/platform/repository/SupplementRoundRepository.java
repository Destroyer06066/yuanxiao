package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.SupplementRound;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplementRoundRepository extends BaseMapper<SupplementRound> {

    Optional<SupplementRound> findById(@Param("roundId") UUID roundId);

    List<SupplementRound> findAll();

    Optional<SupplementRound> findActive();
}
