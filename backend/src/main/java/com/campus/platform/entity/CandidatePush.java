package com.campus.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.platform.handler.JsonMapTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("candidate_push")
public class CandidatePush extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID pushId;

    @TableField("school_id")
    private UUID schoolId;

    @TableField("major_id")
    private UUID majorId;

    @TableField("candidate_id")
    private String candidateId;

    @TableField("candidate_name")
    private String candidateName;

    private String nationality;

    @TableField("id_number")
    private String idNumber;

    private String email;

    @TableField("total_score")
    private BigDecimal totalScore;

    @TableField(value = "subject_scores", typeHandler = JsonMapTypeHandler.class)
    private Map<String, BigDecimal> subjectScores;

    private String intention;

    private String status;

    @TableField("admission_major_id")
    private UUID admissionMajorId;

    @TableField("admission_remark")
    private String admissionRemark;

    @TableField("condition_desc")
    private String conditionDesc;

    @TableField("condition_deadline")
    private Instant conditionDeadline;

    @TableField("push_round")
    private Integer pushRound;

    @TableField("pushed_at")
    private Instant pushedAt;

    @TableField("operated_at")
    private Instant operatedAt;

    @TableField("operator_id")
    private UUID operatorId;
}
