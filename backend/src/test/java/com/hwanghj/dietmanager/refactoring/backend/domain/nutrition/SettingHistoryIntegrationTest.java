package com.hwanghj.dietmanager.refactoring.backend.domain.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.ActivityLevel;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.SettingHistory;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.repository.SettingHistoryRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.GoalType;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.repository.UserRepository;
import com.hwanghj.dietmanager.refactoring.backend.support.MutableClock;

import lombok.AllArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/** 과거 날짜의 권장량이 당시 목표·활동량으로 계산되는지 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SettingHistoryIntegrationTest {
    // 오늘 = 2026-03-01. 가입 시점을 과거로 만들 때는 시계를 돌려서 가입한다(생성일은 수정할 수 없는 감사 컬럼).
    private static final Instant TODAY = Instant.parse("2026-03-01T03:00:00Z");

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(TODAY, ZoneId.systemDefault());
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    UserRepository users;

    @Autowired
    SettingHistoryRepository histories;

    @Autowired
    MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.set(TODAY);
    }

    @Test
    void pastDatesUseTheSettingsInEffectOnThatDate() throws Exception {
        var login = registerAndLogin();
        addMeasurement(login.auth, "2025-12-01");
        // 생일(1월 1일)을 넘지 않는 2026년 날짜끼리 비교해 나이 차이를 없앤다.
        histories.save(new SettingHistory(login.userId, LocalDate.of(2026, 2, 1), GoalType.MAINTAIN_WEIGHT, ActivityLevel.MODERATE));
        histories.save(new SettingHistory(login.userId, LocalDate.of(2026, 3, 1), GoalType.LOSE_WEIGHT, ActivityLevel.MODERATE));

        double maintain = calories(login.auth, "2026-02-15");
        double lose = calories(login.auth, "2026-03-15");
        double beforeHistory = calories(login.auth, "2026-01-15");

        assertThat(maintain - lose).isEqualTo(300.0);
        // 모든 이력보다 이전 날짜는 가장 오래된 이력(유지)으로 계산한다.
        assertThat(beforeHistory).isEqualTo(maintain);
    }

    @Test
    void firstChangeKeepsThePreviousSettingFromProfileCreation() throws Exception {
        // 2026-01-01에 가입하고, 오늘(2026-03-01) 처음 활동량을 고른다.
        clock.set(Instant.parse("2026-01-01T03:00:00Z"));
        String email = register();
        clock.set(TODAY);
        var login = login(email);
        addMeasurement(login.auth, "2025-12-01");

        mvc.perform(put("/api/nutrition/preferences").header("Authorization", login.auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"activityLevel\":\"MODERATE\"}"))
                .andExpect(status().isOk());

        LocalDate today = LocalDate.now(clock);
        var baseline = histories.findByUserIdAndEffectiveDate(login.userId, LocalDate.of(2026, 1, 1)).orElseThrow();
        var todays = histories.findByUserIdAndEffectiveDate(login.userId, today).orElseThrow();
        assertThat(baseline.getActivityLevel()).isNull();
        assertThat(todays.getActivityLevel()).isEqualTo(ActivityLevel.MODERATE);

        // 활동량을 고르기 전 날짜는 활동량 미선택으로 계산할 수 없다.
        mvc.perform(get("/api/nutrition/recommendation").header("Authorization", login.auth).param("date", "2026-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.reason").value("활동량을 먼저 선택해주세요."));
        mvc.perform(get("/api/nutrition/recommendation").header("Authorization", login.auth).param("date", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));

        // 같은 날 목표를 바꾸면 오늘 이력 한 줄만 바뀐다.
        mvc.perform(patch("/api/users/profile").header("Authorization", login.auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"gender\":\"MALE\",\"birthDate\":\"1995-01-01\",\"goalType\":\"GAIN_WEIGHT\"}"))
                .andExpect(status().isOk());
        var updated = histories.findByUserIdAndEffectiveDate(login.userId, today).orElseThrow();
        assertThat(updated.getGoalType()).isEqualTo(GoalType.GAIN_WEIGHT);
        assertThat(updated.getActivityLevel()).isEqualTo(ActivityLevel.MODERATE);
        assertThat(histories.findByUserIdAndEffectiveDate(login.userId, LocalDate.of(2026, 1, 1)).orElseThrow().getGoalType())
                .isEqualTo(GoalType.MAINTAIN_WEIGHT);
    }

    @Test
    void unchangedSettingsDoNotCreateHistory() throws Exception {
        var login = registerAndLogin();
        mvc.perform(patch("/api/users/profile").header("Authorization", login.auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"gender\":\"MALE\",\"birthDate\":\"1995-01-01\",\"goalType\":\"MAINTAIN_WEIGHT\"}"))
                .andExpect(status().isOk());
        assertThat(histories.existsByUserId(login.userId)).isFalse();
    }

    private double calories(String auth, String date) throws Exception {
        String body = mvc.perform(get("/api/nutrition/recommendation").header("Authorization", auth).param("date", date))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("calories").asDouble();
    }

    private void addMeasurement(String auth, String date) throws Exception {
        mvc.perform(post("/api/users/body-measurements").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"measuredDate\":\"" + date + "\",\"height\":175,\"weight\":70}"))
                .andExpect(status().isOk());
    }

    @AllArgsConstructor
    private static class LoginResult {
        private final String auth;
        private final Long userId;
    }

    private LoginResult registerAndLogin() throws Exception {
        return login(register());
    }

    private String register() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"Password123!","userName":"테스트 사용자",
                         "gender":"MALE","birthDate":"1995-01-01","goalType":"MAINTAIN_WEIGHT"}
                        """.formatted(email)))
                .andExpect(status().isCreated());
        return email;
    }

    private LoginResult login(String email) throws Exception {
        String response = mvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"Password123!"}
                        """.formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Long userId = users.findByEmail(email).orElseThrow().getId();
        return new LoginResult("Bearer " + mapper.readTree(response).get("accessToken").asText(), userId);
    }

}
