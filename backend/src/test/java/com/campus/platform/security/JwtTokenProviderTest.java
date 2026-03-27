package com.campus.platform.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtTokenProvider 单元测试
 * 不依赖 Spring 容器，直接 new 实例后反射调用 @PostConstruct init()
 */
class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        provider = new JwtTokenProvider();

        // @Value 不会自动注入，需手动设置 accessTokenValiditySeconds，否则 token 立即过期
        Field validityField = JwtTokenProvider.class.getDeclaredField("accessTokenValiditySeconds");
        validityField.setAccessible(true);
        validityField.set(provider, 7200L);

        // 反射调用 @PostConstruct init()
        var initMethod = JwtTokenProvider.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(provider);
    }

    private AccountPrincipal makeAdmin() {
        return new AccountPrincipal(
                UUID.randomUUID(),
                "OP_ADMIN",
                null,
                "管理员",
                UUID.randomUUID().toString()
        );
    }

    private AccountPrincipal makeSchoolUser(UUID schoolId) {
        return new AccountPrincipal(
                UUID.randomUUID(),
                "SCHOOL_ADMIN",
                schoolId,
                "招生负责人",
                UUID.randomUUID().toString()
        );
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateTokenTests {

        @Test
        @DisplayName("生成非空 Token 字符串")
        void generateToken_returnsNonNullString() {
            String token = provider.generateToken(makeAdmin());
            assertNotNull(token);
            assertFalse(token.isBlank());
        }

        @Test
        @DisplayName("OP_ADMIN Token 不含 schoolId claim")
        void generateToken_opAdmin_noSchoolId() {
            AccountPrincipal admin = makeAdmin();
            String token = provider.generateToken(admin);

            AccountPrincipal parsed = provider.parseToken(token);
            assertNotNull(parsed);
            assertNull(parsed.getSchoolId());
        }

        @Test
        @DisplayName("SCHOOL_ADMIN Token 携带 schoolId claim")
        void generateToken_schoolAdmin_hasSchoolId() {
            UUID schoolId = UUID.randomUUID();
            AccountPrincipal user = makeSchoolUser(schoolId);
            String token = provider.generateToken(user);

            AccountPrincipal parsed = provider.parseToken(token);
            assertNotNull(parsed);
            assertEquals(schoolId, parsed.getSchoolId());
        }
    }

    @Nested
    @DisplayName("parseToken")
    class ParseTokenTests {

        @Test
        @DisplayName("解析合法 Token → 返回 Principal，sub/role/realName 一致")
        void parseToken_validToken_returnsPrincipal() {
            AccountPrincipal original = makeAdmin();
            String token = provider.generateToken(original);

            AccountPrincipal parsed = provider.parseToken(token);

            assertNotNull(parsed);
            assertEquals(original.getAccountId(), parsed.getAccountId());
            assertEquals(original.getRole(), parsed.getRole());
            assertEquals(original.getRealName(), parsed.getRealName());
        }

        @Test
        @DisplayName("解析伪造/损坏 Token → 返回 null")
        void parseToken_invalidToken_returnsNull() {
            AccountPrincipal result = provider.parseToken("not.a.real.token");
            assertNull(result);
        }

        @Test
        @DisplayName("解析空字符串 → 返回 null（不抛出异常）")
        void parseToken_emptyString_returnsNull() {
            assertDoesNotThrow(() -> {
                AccountPrincipal result = provider.parseToken("");
                assertNull(result);
            });
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateTokenTests {

        @Test
        @DisplayName("合法 Token → 返回 true")
        void validateToken_valid_returnsTrue() {
            String token = provider.generateToken(makeAdmin());
            assertTrue(provider.validateToken(token));
        }

        @Test
        @DisplayName("非法 Token → 返回 false")
        void validateToken_invalid_returnsFalse() {
            assertFalse(provider.validateToken("garbage-token"));
        }

        @Test
        @DisplayName("空字符串 Token → 返回 false（不抛出异常）")
        void validateToken_empty_returnsFalse() {
            assertDoesNotThrow(() -> assertFalse(provider.validateToken("")));
        }
    }
}
