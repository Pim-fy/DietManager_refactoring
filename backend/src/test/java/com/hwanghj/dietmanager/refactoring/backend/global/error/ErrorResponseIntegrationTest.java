package com.hwanghj.dietmanager.refactoring.backend.global.error;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hwanghj.dietmanager.refactoring.backend.statistics.StatisticsService;

import tools.jackson.databind.ObjectMapper;

/** 오류 응답의 상태 코드·오류 코드·본문 형식을 API 계약으로 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorResponseIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean StatisticsService statistics;

    @Test
    void notFoundCodesMatchTheResource() throws Exception {
        String auth = registerAndLogin();
        mvc.perform(delete("/api/meal-plans/99999999").header("Authorization", auth))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("MEAL_PLAN_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("식단 계획을 찾을 수 없습니다."));
        mvc.perform(delete("/api/intakes/99999999").header("Authorization", auth))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("INTAKE_NOT_FOUND"));
        mvc.perform(put("/api/foods/favorites").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"foodCode\":\"NOT-SAVED\",\"favorite\":false}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("FOOD_NOT_FOUND"));
    }

    @Test
    void deletedMeasurementConflictHasMeasurementCode() throws Exception {
        String auth = registerAndLogin();
        String body = "{\"measuredDate\":\"2026-01-01\",\"height\":170,\"weight\":65}";
        String created = mvc.perform(post("/api/users/body-measurements").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(created).path("measurementId").asLong();
        mvc.perform(delete("/api/users/body-measurements/" + id).header("Authorization", auth))
            .andExpect(status().isNoContent());
        mvc.perform(patch("/api/users/body-measurements/" + id).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("BODY_MEASUREMENT_CONFLICT"));
    }

    @Test
    void refreshFailuresUseSessionCodes() throws Exception {
        mvc.perform(post("/api/users/refresh"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("SESSION_EXPIRED"));
        mvc.perform(post("/api/users/refresh").header("Sec-Fetch-Site", "cross-site"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(post("/api/users/logout").header("Origin", "http://evil.example"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void frameworkErrorsUseCommonShapeWithoutInternals() throws Exception {
        String auth = registerAndLogin();
        mvc.perform(get("/api/not-mapped").header("Authorization", auth))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.trace").doesNotExist());
        mvc.perform(delete("/api/users/account").header("Authorization", auth))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        mvc.perform(post("/api/intakes").header("Authorization", auth).contentType(MediaType.TEXT_PLAIN).content("x"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        mvc.perform(get("/api/users/account"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    void unexpectedErrorsHideDetails() throws Exception {
        when(statistics.range(any(), any(), any())).thenThrow(new IllegalStateException("내부 상세 정보"));
        mvc.perform(get("/api/statistics").header("Authorization", registerAndLogin())
                .param("start", "2026-01-01").param("end", "2026-01-02"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.message").value("요청을 처리하지 못했습니다. 잠시 후 다시 시도해주세요."));
    }

    @Test
    void validationErrorsListInvalidFields() throws Exception {
        String auth = registerAndLogin();
        mvc.perform(post("/api/users/body-measurements").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("measuredDate", "height", "weight")));
        mvc.perform(get("/api/foods").header("Authorization", auth).param("keyword", "밥").param("size", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.errors[0].field").value("size"));
        mvc.perform(get("/api/statistics").header("Authorization", auth).param("start", "2026-01-01"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("end"));
        mvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"a@b.c\",\"password\":\"x\"}"))
            .andExpect(jsonPath("$.errors").doesNotExist());
    }

    private String registerAndLogin() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"%s","password":"Password123!","userName":"테스트 사용자",
                 "gender":"MALE","birthDate":"1995-01-01","goalType":"MAINTAIN_WEIGHT"}
                """.formatted(email))).andExpect(status().isCreated());
        String response = mvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"%s","password":"Password123!"}
                """.formatted(email))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readTree(response).get("accessToken").asText();
    }
}
