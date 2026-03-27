package com.campus.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("supplement_round")
public class SupplementRound extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private UUID roundId;

    @TableField("round_number")
    private Integer roundNumber;

    @TableField("start_time")
    private Instant startTime;

    @TableField("end_time")
    private Instant endTime;

    private String status;

    private String remark;
}
