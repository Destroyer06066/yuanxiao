package com.campus.platform.entity.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 证书核验请求
 */
public record CertificateVerificationRequest(
    @NotBlank(message = "证书编号不能为空")
    String certificateNo,

    @NotBlank(message = "防伪验证码不能为空")
    String verifyCode
) {}
