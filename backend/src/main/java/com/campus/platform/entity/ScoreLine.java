package com.campus.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("score_line")
public class ScoreLine extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private UUID lineId;

    @TableField("school_id")
    private UUID schoolId;

    @TableField("major_id")
    private UUID majorId;

    private Integer year;

    private String subject;

    @TableField("min_score")
    private BigDecimal minScore;
}
