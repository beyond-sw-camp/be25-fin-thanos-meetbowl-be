package com.meetbowl.api.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.api.auth.dto.PasswordResetRequest;
import com.meetbowl.api.common.GlobalExceptionHandler;
import com.meetbowl.api.config.WebMvcConfig;
import com.meetbowl.application.auth.PasswordResetRequestUseCase;

@WebMvcTest(PasswordResetRequestController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class})
class PasswordResetRequestControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PasswordResetRequestUseCase passwordResetRequestUseCase;

    @Test
    void createSuccess() throws Exception {
        willDoNothing().given(passwordResetRequestUseCase).execute(any());

        mockMvc.perform(
                        post("/api/v1/password-reset-requests")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        new ObjectMapper()
                                                .writeValueAsString(
                                                        new PasswordResetRequest(
                                                                "user1",
                                                                "user1@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.message").value(PasswordResetRequestUseCase.ACCEPTED_MESSAGE));
    }
}
