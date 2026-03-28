package com.campus.platform.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private AuthenticationFilter authenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    private AccountPrincipal makePrincipal() {
        return new AccountPrincipal(
                UUID.randomUUID(),
                "SCHOOL_ADMIN",
                UUID.randomUUID(),
                "测试用户",
                UUID.randomUUID().toString(),
                List.of("major:read", "quota:read")
        );
    }

    @Nested
    @DisplayName("doFilterInternal 过滤逻辑")
    class DoFilterInternalTests {

        @Test
        @DisplayName("合法 Token → SecurityContext 设置、chain 调用、最终清理")
        void validToken_setsContextAndCallsChainAndClears() throws Exception {
            AccountPrincipal principal = makePrincipal();
            String token = "valid-jwt-token";

            request.addHeader("Authorization", "Bearer " + token);
            when(jwtTokenProvider.parseToken(token)).thenReturn(principal);

            authenticationFilter.doFilterInternal(request, response, filterChain);

            // chain.doFilter 被调用（MockFilterChain 记录 request）
            assertNotNull(filterChain.getRequest());
            // SecurityContext 在 finally 中已清理
            assertNull(SecurityContext.get());
        }

        @Test
        @DisplayName("无效 Token → SecurityContext 未设置，chain 仍然调用")
        void invalidToken_contextNotSet_chainStillCalled() throws Exception {
            String token = "invalid-token";
            request.addHeader("Authorization", "Bearer " + token);
            when(jwtTokenProvider.parseToken(token)).thenReturn(null);

            authenticationFilter.doFilterInternal(request, response, filterChain);

            assertNotNull(filterChain.getRequest());
            assertNull(SecurityContext.get());
        }

        @Test
        @DisplayName("无 Authorization 头 → chain 调用，无错误")
        void noAuthHeader_chainCalled_noError() throws Exception {
            authenticationFilter.doFilterInternal(request, response, filterChain);

            assertNotNull(filterChain.getRequest());
            assertEquals(200, response.getStatus());
            assertNull(SecurityContext.get());
        }
    }

    @Nested
    @DisplayName("shouldNotFilter 路径排除")
    class ShouldNotFilterTests {

        @Test
        @DisplayName("/api/v1/auth/login → 返回 true（跳过过滤）")
        void loginPath_shouldNotFilter() {
            request.setRequestURI("/api/v1/auth/login");
            assertTrue(authenticationFilter.shouldNotFilter(request));
        }

        @Test
        @DisplayName("/swagger-ui/index.html → 返回 true（跳过过滤）")
        void swaggerPath_shouldNotFilter() {
            request.setRequestURI("/swagger-ui/index.html");
            assertTrue(authenticationFilter.shouldNotFilter(request));
        }

        @Test
        @DisplayName("/api/v1/students → 返回 false（需要过滤）")
        void apiPath_shouldFilter() {
            request.setRequestURI("/api/v1/students");
            assertFalse(authenticationFilter.shouldNotFilter(request));
        }
    }
}
