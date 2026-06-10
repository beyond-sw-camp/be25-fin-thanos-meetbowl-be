package com.meetbowl.api.common.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.GlobalExceptionHandler;
import com.meetbowl.api.common.security.ApiAccessDeniedHandler;
import com.meetbowl.api.common.security.ApiAuthenticationEntryPoint;
import com.meetbowl.api.config.SecurityConfig;
import com.meetbowl.application.auth.AccessTokenValidationService;
import com.meetbowl.common.response.ApiResponse;

@WebMvcTest(controllers = RequireAdminSampleController.class)
@Import({
    ApiAccessDeniedHandler.class,
    ApiAuthenticationEntryPoint.class,
    GlobalExceptionHandler.class,
    JwtAuthenticatedUserConverter.class,
    SecurityConfig.class
})
class RequireAdminTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;

    @Test
    void adminCanAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/sample/admin-only").with(jwt().authorities(adminAuthority())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void userCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/sample/admin-only").with(jwt().authorities(userAuthority())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));
    }

    private List<GrantedAuthority> adminAuthority() {
        return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private List<GrantedAuthority> userAuthority() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}

@RestController
class RequireAdminSampleController {

    @RequireAdmin
    @GetMapping("/sample/admin-only")
    ApiResponse<Void> adminOnly() {
        return ApiResponse.ok();
    }
}
