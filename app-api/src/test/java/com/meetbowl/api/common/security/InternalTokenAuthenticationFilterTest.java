package com.meetbowl.api.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.meetbowl.api.common.ApiHeaders;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.AuthenticatedUserRole;

class InternalTokenAuthenticationFilterTest {

    private static final String INTERNAL_TOKEN = "internal-token-value-with-at-least-32-bytes";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validInternalTokenSetsSystemAuthenticatedUser() throws Exception {
        InternalTokenAuthenticationFilter filter =
                new InternalTokenAuthenticationFilter(
                        INTERNAL_TOKEN, new ApiAuthenticationEntryPoint());
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/internal/mails/send");
        request.addHeader(ApiHeaders.INTERNAL_TOKEN, INTERNAL_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (servletRequest, servletResponse) -> {
                    AuthenticatedUser user =
                            assertInstanceOf(
                                    AuthenticatedUser.class,
                                    SecurityContextHolder.getContext()
                                            .getAuthentication()
                                            .getDetails());
                    assertEquals(AuthenticatedUserRole.SYSTEM, user.role());
                    assertEquals(
                            "ROLE_SYSTEM",
                            SecurityContextHolder.getContext()
                                    .getAuthentication()
                                    .getAuthorities()
                                    .iterator()
                                    .next()
                                    .getAuthority());
                });
    }

    @Test
    void invalidInternalTokenReturnsUnauthorizedWithoutContinuingChain() throws Exception {
        InternalTokenAuthenticationFilter filter =
                new InternalTokenAuthenticationFilter(
                        INTERNAL_TOKEN, new ApiAuthenticationEntryPoint());
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/internal/mails/send");
        request.addHeader(ApiHeaders.INTERNAL_TOKEN, "invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (servletRequest, servletResponse) -> {
                    throw new AssertionError("Filter chain must not continue.");
                });

        assertEquals(401, response.getStatus());
    }
}
