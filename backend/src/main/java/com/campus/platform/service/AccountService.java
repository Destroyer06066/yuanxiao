package com.campus.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.entity.Account;
import com.campus.platform.entity.enums.AccountRole;
import com.campus.platform.entity.enums.SchoolStatus;
import com.campus.platform.repository.AccountRepository;
import com.campus.platform.repository.SchoolRepository;
import com.campus.platform.security.AccountPrincipal;
import com.campus.platform.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    private static final String SESSION_PREFIX = "session:";
    private static final Duration SESSION_TTL = Duration.ofHours(8);
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    // ========== 登录 ==========

    public AccountPrincipal login(String username, String password, String ip) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR, "用户名或密码错误"));

        // 安全检查：accountId 必须已填充
        if (account.getAccountId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "系统内部错误，请稍后重试");
        }
        UUID accountId = UUID.fromString(account.getAccountId());

        // 校验账号状态
        if ("INACTIVE".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "账号已停用");
        }

        if ("LOCKED".equals(account.getStatus()) && account.getLockedUntil() != null
                && account.getLockedUntil().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                    "账号已锁定，请 " + account.getLockedUntil() + " 后再试");
        }

        // 解锁过期锁定
        if ("LOCKED".equals(account.getStatus())) {
            accountRepository.updateLoginFields(accountId, 0, null, null);
        }

        // 院校状态校验
        if (!AccountRole.OP_ADMIN.name().equals(account.getRole())) {
            if (account.getSchoolId() != null) {
                Optional<?> school = schoolRepository.findById(account.getSchoolId());
                if (school.isPresent()) {
                    var s = school.get();
                    if ("INACTIVE".equals(SchoolStatus.INACTIVE.name())) {
                        throw new BusinessException(ErrorCode.SCHOOL_DISABLED, "所属院校已停用");
                    }
                }
            }
        }

        // 密码校验
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            int newCount = account.getFailedLoginCount() + 1;
            Instant lockedUntil = newCount >= 5 ? Instant.now().plusSeconds(1800) : null;
            accountRepository.updateLoginFields(accountId, newCount, lockedUntil, null);

            int remain = 5 - newCount;
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR,
                    remain > 0 ? "用户名或密码错误，剩余 " + remain + " 次尝试机会" : "账号已被锁定");
        }

        // 登录成功
        accountRepository.updateLoginFields(accountId, 0, null, Instant.now());

        // 生成 JWT
        AccountPrincipal principal = new AccountPrincipal(
                accountId,
                account.getRole(),
                account.getSchoolId(),
                account.getRealName(),
                UUID.randomUUID().toString()
        );

        // 写入 Redis Session
        redisService.createSession(accountId, principal.getJti());

        log.info("用户登录成功: username={}, role={}", username, account.getRole());
        return principal;
    }

    // ========== 登出 ==========

    public void logout(UUID accountId) {
        redisService.deleteSession(accountId);
    }

    // ========== 创建账号 ==========

    @Transactional
    public String createSchoolAdmin(UUID schoolId, String username, String realName, String phone, UUID operatorId) {
        // 校验每校唯一管理员
        if (accountRepository.existsSchoolAdmin(schoolId)) {
            throw new BusinessException(ErrorCode.SCHOOL_ADMIN_EXISTS, "该校已有管理员账号");
        }
        if (accountRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS, "用户名已存在");
        }

        String rawPassword = generatePassword(12);
        Account account = new Account();
        account.setAccountId(UUID.randomUUID().toString());
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setRole(AccountRole.SCHOOL_ADMIN.name());
        account.setSchoolId(schoolId);
        account.setRealName(realName);
        account.setPhone(phone);
        account.setStatus("ACTIVE");
        account.setMustChangePassword(true);
        account.setFailedLoginCount(0);
        account.setCreatedBy(operatorId);
        accountRepository.insert(account);

        // 将初始密码存入 Redis（TTL 10分钟）
        redisService.set("init_pwd:" + account.getAccountId(), rawPassword, Duration.ofMinutes(10));

        log.info("创建院校管理员: accountId={}, username={}", account.getAccountId(), username);
        return rawPassword;
    }

    @Transactional
    public String createStaff(UUID schoolId, String username, String realName, String phone,
                              String initialPassword, UUID operatorId) {
        if (accountRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS, "用户名已存在");
        }

        // 校验工作人员数量上限
        int count = accountRepository.countActiveBySchool(schoolId);
        if (count >= 50) { // 默认上限
            throw new BusinessException(ErrorCode.STAFF_QUOTA_EXCEEDED, "该校工作人员账号数量已达上限（50）");
        }

        String rawPassword = (initialPassword != null && !initialPassword.isBlank())
                ? initialPassword : generatePassword(12);

        Account account = new Account();
        account.setAccountId(UUID.randomUUID().toString());
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setRole(AccountRole.SCHOOL_STAFF.name());
        account.setSchoolId(schoolId);
        account.setRealName(realName);
        account.setPhone(phone);
        account.setStatus("ACTIVE");
        account.setMustChangePassword(true);
        account.setFailedLoginCount(0);
        account.setCreatedBy(operatorId);
        accountRepository.insert(account);

        log.info("创建工作人员: accountId={}, username={}", account.getAccountId(), username);
        return rawPassword;
    }

    // ========== 辅助方法 ==========

    private String generatePassword(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public IPage<Account> pageQuery(UUID schoolId, int page, int pageSize) {
        Page<Account> p = new Page<>(page, pageSize);
        // 简化实现
        return new Page<>();
    }
}
