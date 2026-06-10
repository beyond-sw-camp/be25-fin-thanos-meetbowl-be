package com.meetbowl.api.sampletask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.meetbowl.api.common.GlobalExceptionHandler;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.AuthenticatedUserAttributes;
import com.meetbowl.api.common.auth.AuthenticatedUserRole;
import com.meetbowl.api.common.auth.CurrentUserArgumentResolver;
import com.meetbowl.api.config.WebMvcConfig;
import com.meetbowl.application.auth.AccessTokenValidationService;
import com.meetbowl.application.sampletask.CreateSampleTaskCommand;
import com.meetbowl.application.sampletask.CreateSampleTaskUseCase;
import com.meetbowl.application.sampletask.SampleTaskResult;

@ActiveProfiles("sample")
@WebMvcTest(controllers = SampleTaskController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    CurrentUserArgumentResolver.class,
    GlobalExceptionHandler.class,
    SampleTaskControllerTest.TestUseCaseConfig.class,
    WebMvcConfig.class
})
class SampleTaskControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;

    @Autowired private CreateSampleTaskUseCase createSampleTaskUseCase;

    @Test
    void createSampleTask() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(
                        ownerUserId, organizationId, AuthenticatedUserRole.USER, "홍길동");

        mockMvc.perform(
                        post("/api/v1/sample-tasks")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER, authenticatedUser)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "title": "샘플 작업"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.data.sampleTaskId")
                                .value(TestCreateSampleTaskUseCase.SAMPLE_TASK_ID.toString()))
                .andExpect(jsonPath("$.data.ownerUserId").value(ownerUserId.toString()))
                .andExpect(jsonPath("$.data.title").value("샘플 작업"))
                .andExpect(jsonPath("$.data.createdAt").value("2099-01-01T01:00:00Z"));

        CreateSampleTaskCommand command =
                ((TestCreateSampleTaskUseCase) createSampleTaskUseCase).lastCommand;
        assertEquals(ownerUserId, command.ownerUserId());
        assertEquals("샘플 작업", command.title());
    }

    @TestConfiguration
    static class TestUseCaseConfig {

        @Bean
        CreateSampleTaskUseCase createSampleTaskUseCase() {
            return new TestCreateSampleTaskUseCase();
        }
    }

    private static class TestCreateSampleTaskUseCase extends CreateSampleTaskUseCase {

        private static final UUID SAMPLE_TASK_ID =
                UUID.fromString("00000000-0000-0000-0000-000000000001");
        private static final Instant CREATED_AT = Instant.parse("2099-01-01T01:00:00Z");

        private CreateSampleTaskCommand lastCommand;

        TestCreateSampleTaskUseCase() {
            super(sampleTask -> sampleTask, Clock.fixed(CREATED_AT, ZoneOffset.UTC));
        }

        @Override
        public SampleTaskResult execute(CreateSampleTaskCommand command) {
            this.lastCommand = command;
            return new SampleTaskResult(
                    SAMPLE_TASK_ID, command.ownerUserId(), command.title(), CREATED_AT);
        }
    }
}
