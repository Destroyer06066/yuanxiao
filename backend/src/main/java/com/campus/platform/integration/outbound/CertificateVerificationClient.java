package com.campus.platform.integration.outbound;

import com.campus.platform.entity.dto.CertificateVerificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 证书核验客户端（出站调用报名平台）
 *
 * 对接报名平台API验证证书真伪。
 * 当前为示例实现，报名平台对接后替换 verify() 方法的真实调用。
 */
@Slf4j
@Service
public class CertificateVerificationClient {

    @Value("${integration.certificate-verification.base-url:}")
    private String baseUrl;

    /**
     * 核验证书
     *
     * @param certificateNo 证书编号
     * @param verifyCode    防伪验证码
     * @return 核验结果
     */
    public CertificateVerificationResponse verify(String certificateNo, String verifyCode) {
        log.info("[证书核验] 开始核验: certificateNo={}, verifyCode={}", certificateNo, verifyCode);

        // TODO: 报名平台对接后替换为真实API调用
        // 示例：证书编号以"CERT"开头视为有效证书
        CertificateVerificationResponse response = getMockResponse(certificateNo, verifyCode);

        log.info("[证书核验] 核验完成: valid={}, message={}", response.valid(), response.message());
        return response;
    }

    /**
     * 示例响应数据
     * 报名平台对接后删除此方法
     */
    private CertificateVerificationResponse getMockResponse(String certificateNo, String verifyCode) {
        if (certificateNo != null && certificateNo.toUpperCase().startsWith("CERT")) {
            return new CertificateVerificationResponse(
                true,
                "CHN0001",
                "2025-6-20",
                "机考/CBT",
                certificateNo,
                "MOHAMED HAFIZ IBRAHIM MOHAMED ELMOGTABA",
                "男/Male",
                "2010-6-20",
                "苏丹/Sudan",
                List.of(
                    new CertificateVerificationResponse.SubjectScore("理工中文/STEM Chinese", "中文/Chinese", new BigDecimal("75")),
                    new CertificateVerificationResponse.SubjectScore("数学/Mathematics", "英文/English", new BigDecimal("85")),
                    new CertificateVerificationResponse.SubjectScore("物理/Physics", "英文/English", new BigDecimal("70")),
                    new CertificateVerificationResponse.SubjectScore("化学/Chemistry", "中文/Chinese", new BigDecimal("66"))
                ),
                verifyCode,
                "2025-07-05",
                "国家留学基金管理委员会",
                "证书信息核验成功，证书真实有效"
            );
        }
        return new CertificateVerificationResponse(
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "未查询到该证书信息，请核对证书编号"
        );
    }
}
