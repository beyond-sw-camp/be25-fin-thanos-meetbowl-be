package com.meetbowl.api.common;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SampleController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void businessExceptionReturnsCommonErrorResponse() throws Exception {
        mockMvc.perform(get("/sample/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MEETING_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("회의를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.error.details").isArray())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void validationExceptionReturnsFieldDetails() throws Exception {
        mockMvc.perform(post("/sample/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.message").value("요청 값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.error.details[0].field").value("name"))
                .andExpect(jsonPath("$.error.details[0].reason").value("이름은 필수입니다."));
    }

    @Test
    void invalidRequestReturnsCommonInvalidRequest() throws Exception {
        mockMvc.perform(get("/sample/type-mismatch")
                        .param("id", "invalid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("잘못된 요청입니다."));
    }

}

@RestController
class SampleController {

    @GetMapping("/sample/business")
    ApiResponse<Void> business() {
        throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
    }

    @PostMapping("/sample/validation")
    ApiResponse<Void> validation(@Valid @RequestBody SampleRequest request) {
        return ApiResponse.ok();
    }

    @GetMapping("/sample/type-mismatch")
    ApiResponse<Void> typeMismatch(@RequestParam UUID id) {
        return ApiResponse.ok();
    }
}

record SampleRequest(
        @NotBlank(message = "이름은 필수입니다.")
        String name
) {
}
