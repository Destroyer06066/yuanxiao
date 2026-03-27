package com.campus.platform.controller;

import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.common.result.Result;
import com.campus.platform.security.AccountPrincipal;
import com.campus.platform.security.JwtTokenProvider;
import com.campus.platform.security.SecurityContext;
import com.campus.platform.service.AccountService;
import com.campus.platform.service.RedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "认证", description = "登录、登出、密码管理")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AccountService accountService;
    private final RedisService redisService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    // ========== 登录 ==========

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Valid LoginRequest req) {
        AccountPrincipal principal = accountService.login(req.getUsername(), req.getPassword(), "");

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", jwtTokenProvider.generateToken(principal));
        data.put("role", principal.getRole());
        if (principal.getSchoolId() != null) {
            data.put("schoolId", principal.getSchoolId().toString());
        }
        data.put("realName", principal.getRealName());
        data.put("requirePasswordChange", false);
        return Result.ok(data);
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        accountService.logout(SecurityContext.getAccountId());
        return Result.ok();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        AccountPrincipal p = SecurityContext.get();
        if (p == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Map<String, Object> meData = new HashMap<>();
        meData.put("accountId", p.getAccountId().toString());
        meData.put("role", p.getRole());
        if (p.getSchoolId() != null) {
            meData.put("schoolId", p.getSchoolId().toString());
        }
        meData.put("realName", p.getRealName());
        return Result.ok(meData);
    }

    @Operation(summary = "强制修改密码（首次登录）")
    @PutMapping("/password/force-change")
    public Result<Void> forceChangePassword(@RequestBody @Valid PasswordChangeRequest req) {
        // 此处简化处理，完整实现需要注入 AccountRepository
        return Result.ok();
    }

    @Operation(summary = "发送重置密码验证码")
    @PostMapping("/password/reset/send-code")
    public Result<Map<String, Object>> sendResetCode(@RequestBody @Valid SendCodeRequest req) {
        // 简化：生成验证码并存入 Redis
        String code = String.format("%06d", (int) (Math.random() * 1_000_000));
        redisService.saveSmsCode(req.getUsername(), code);
        // 实际应调用短信服务
        return Result.ok(Map.of("maskedPhone", "138****0000"));
    }

    @Operation(summary = "验证并重置密码")
    @PostMapping("/password/reset/confirm")
    public Result<Void> confirmReset(@RequestBody @Valid ResetConfirmRequest req) {
        String stored = redisService.getSmsCode(req.getUsername());
        if (stored == null) {
            throw new BusinessException(ErrorCode.SMS_CODE_EXPIRED, "验证码已过期");
        }
        if (!stored.equals(req.getCode())) {
            throw new BusinessException(ErrorCode.SMS_CODE_ERROR, "验证码错误");
        }
        redisService.deleteSmsCode(req.getUsername());
        // 更新密码逻辑...
        return Result.ok();
    }

    // ========== DTO ==========

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class PasswordChangeRequest {
        @NotBlank
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,20}$",
                message = "密码须包含大小写字母和数字，8-20位")
        private String newPassword;
    }

    @Data
    public static class SendCodeRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
    }

    @Data
    public static class ResetConfirmRequest {
        @NotBlank private String username;
        @NotBlank private String code;
        @NotBlank
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,20}$",
                message = "密码须包含大小写字母和数字，8-20位")
        private String newPassword;
    }
}
