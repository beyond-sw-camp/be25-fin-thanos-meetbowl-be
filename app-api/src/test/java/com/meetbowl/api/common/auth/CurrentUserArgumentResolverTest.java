package com.meetbowl.api.common.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.GlobalExceptionHandler;
import com.meetbowl.api.config.WebMvcConfig;
import com.meetbowl.application.auth.AccessTokenValidationService;
import com.meetbowl.common.response.ApiResponse;

@WebMvcTest(controllers = CurrentUserSampleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CurrentUserArgumentResolver.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class CurrentUserArgumentResolverTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;

    @Test
    void injectsAuthenticatedUserFromRequestAttribute() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(userId, organizationId, AuthenticatedUserRole.USER, "홍길동");

        mockMvc.perform(
                        get("/sample/current-user")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.organizationId").value(organizationId.toString()))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.displayName").value("홍길동"));
    }

    @Test
    void missingRequiredUserReturnsUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/sample/current-user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").value("인증이 필요합니다."));
    }

    @Test
    void optionalCurrentUserCanBeNull() throws Exception {
        mockMvc.perform(get("/sample/optional-current-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.authenticated").value(false));
    }
}

@RestController
class CurrentUserSampleController {

    @GetMapping("/sample/current-user")
    ApiResponse<CurrentUserResponse> currentUser(@CurrentUser AuthenticatedUser user) {
        return ApiResponse.ok(CurrentUserResponse.from(user));
    }

    @GetMapping("/sample/optional-current-user")
    ApiResponse<OptionalCurrentUserResponse> optionalCurrentUser(
            @CurrentUser(required = false) AuthenticatedUser user) {
        return ApiResponse.ok(new OptionalCurrentUserResponse(user != null));
    }
}

record CurrentUserResponse(
        UUID userId, UUID organizationId, AuthenticatedUserRole role, String displayName) {

    static CurrentUserResponse from(AuthenticatedUser user) {
        return new CurrentUserResponse(
                user.userId(), user.organizationId(), user.role(), user.displayName());
    }
}

record OptionalCurrentUserResponse(boolean authenticated) {}
