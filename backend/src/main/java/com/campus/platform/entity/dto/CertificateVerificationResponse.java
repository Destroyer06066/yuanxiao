package com.campus.platform.entity.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 证书核验响应
 */
public record CertificateVerificationResponse(
    boolean valid,                    // 证书是否有效
    String testCenterCode,            // 考点代码
    String testDate,                  // 考试日期
    String testFormat,                // 考试形式
    String certificateNo,             // 证书编号
    String name,                      // 姓名
    String gender,                    // 性别
    String dateOfBirth,               // 出生日期
    String nationality,               // 国籍
    List<SubjectScore> subjectScores, // 各科成绩
    String verifyCode,                // 防伪验证码
    String issueDate,                 // 签发日期
    String issueOrganization,          // 签发机构
    String message                    // 核验消息
) {
    /**
     * 科目成绩
     */
    public record SubjectScore(
        String subject,   // 科目名称
        String language,  // 考试语言
        BigDecimal score  // 分数
    ) {}
}
