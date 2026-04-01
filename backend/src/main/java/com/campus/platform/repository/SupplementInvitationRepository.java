package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.platform.entity.SupplementInvitation;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplementInvitationRepository extends BaseMapper<SupplementInvitation> {

    Optional<SupplementInvitation> findById(@Param("invitationId") UUID invitationId);

    List<SupplementInvitation> findByCandidateId(@Param("candidateId") String candidateId);

    List<SupplementInvitation> findByPushId(@Param("pushId") UUID pushId);

    List<SupplementInvitation> findBySchoolId(@Param("schoolId") UUID schoolId);

    List<SupplementInvitation> findByStatus(@Param("status") String status);

    List<SupplementInvitation> listBySchoolId(@Param("schoolId") UUID schoolId,
                                              @Param("status") String status,
                                              @Param("round") Integer round,
                                              @Param("candidateKeyword") String candidateKeyword);

    IPage<SupplementInvitation> pageQuery(Page<SupplementInvitation> page,
                                          @Param("schoolId") UUID schoolId,
                                          @Param("status") String status,
                                          @Param("round") Integer round,
                                          @Param("candidateKeyword") String candidateKeyword);

    List<SupplementInvitation> findExpiredInvitations(@Param("now") Instant now);

    Optional<SupplementInvitation> findLatestByCandidateId(@Param("candidateId") String candidateId);
}
