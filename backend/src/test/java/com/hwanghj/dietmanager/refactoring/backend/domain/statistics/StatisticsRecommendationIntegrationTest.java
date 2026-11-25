package com.hwanghj.dietmanager.refactoring.backend.domain.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.ActivityLevel;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.NutritionPreference;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.SettingHistory;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.repository.NutritionPreferenceRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.repository.SettingHistoryRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.GoalType;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.repository.UserRepository;
import com.hwanghj.dietmanager.refactoring.backend.support.MutableClock;

import jakarta.persistence.EntityManagerFactory;
import lombok.AllArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 기간 통계의 날짜별 권장 칼로리가 단일 날짜 권장량 API와 같고, 조회 횟수가 기간 길이와 관계없는지 검증한다. */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(StatisticsRecommendationIntegrationTest.ClockConfig.class)
class StatisticsRecommendationIntegrationTest {
    // 오늘 = 2026-07-01. 조회 기간은 모두 오늘 이전이다.
    private static final Instant TODAY = Instant.parse("2026-07-01T03:00:00Z");

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
    NutritionPreferenceRepository preferences;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.set(TODAY);
    }

    @Test
    void dailyRecommendationMatchesRecommendationApiAcrossHistoryChanges() throws Exception {
        var login = login(register("1995-01-01"));
        // 01-05 활동량 미선택 → 01-08 활동량 선택 → 02-20 목표를 감량으로 변경
        histories.save(new SettingHistory(login.userId, LocalDate.of(2026, 1, 5), GoalType.MAINTAIN_WEIGHT, null));
        histories.save(new SettingHistory(login.userId, LocalDate.of(2026, 1, 8), GoalType.MAINTAIN_WEIGHT, ActivityLevel.MODERATE));
        histories.save(new SettingHistory(login.userId, LocalDate.of(2026, 2, 20), GoalType.LOSE_WEIGHT, ActivityLevel.MODERATE));
        addMeasurement(login.auth, "2026-01-10", 70);
        addMeasurement(login.auth, "2026-02-10", 72);

        JsonNode days = statistics(login.auth, "2026-01-01", "2026-02-28");
        assertSameAsRecommendationApi(login.auth, days);

        // 모든 이력보다 이전(가장 오래된 이력 = 활동량 미선택), 활동량 미선택, 신체 기록 없음은 null
        assertThat(recommended(days, "2026-01-01")).isNull();
        assertThat(recommended(days, "2026-01-06")).isNull();
        assertThat(recommended(days, "2026-01-09")).isNull();
        assertThat(recommended(days, "2026-01-10")).isNotNull();
        // 신체 기록이 바뀐 날부터 값이 바뀌고, 목표 변경 전후는 300kcal 차이
        assertThat(recommended(days, "2026-02-10")).isNotEqualByComparingTo(recommended(days, "2026-02-09"));
        assertThat(recommended(days, "2026-02-19").subtract(recommended(days, "2026-02-20"))).isEqualByComparingTo("300");
    }

    @Test
    void underNineteenDatesHaveNoRecommendation() throws Exception {
        var login = login(register("2007-01-15"));
        addMeasurement(login.auth, "2025-12-01", 65);
        // 이력 없이 현재 활동량만 있는 경우(현재 설정으로 계산)
        preferences.save(new NutritionPreference(login.userId, ActivityLevel.LIGHT));

        JsonNode days = statistics(login.auth, "2026-01-01", "2026-01-31");
        assertSameAsRecommendationApi(login.auth, days);
        assertThat(recommended(days, "2026-01-14")).isNull();
        assertThat(recommended(days, "2026-01-15")).isNotNull();
    }

    @Test
    void noActivityMeansNoRecommendation() throws Exception {
        var login = login(register("1995-01-01"));
        addMeasurement(login.auth, "2026-01-01", 70);

        JsonNode days = statistics(login.auth, "2026-01-01", "2026-01-07");
        assertSameAsRecommendationApi(login.auth, days);
        for (JsonNode day : days)
            assertThat(day.get("recommendedCalories").isNull()).isTrue();
    }

    @Test
    void queryCountDoesNotDependOnRangeLength() throws Exception {
        var login = login(register("1995-01-01"));
        histories.save(new SettingHistory(login.userId, LocalDate.of(2025, 6, 1), GoalType.MAINTAIN_WEIGHT, ActivityLevel.MODERATE));
        histories.save(new SettingHistory(login.userId, LocalDate.of(2025, 12, 1), GoalType.LOSE_WEIGHT, ActivityLevel.ACTIVE));
        addMeasurement(login.auth, "2025-04-01", 70);
        addMeasurement(login.auth, "2025-10-01", 71);
        addMeasurement(login.auth, "2026-02-01", 72);

        long week = statementCount(login.auth, "2026-02-23", "2026-03-01");
        long year = statementCount(login.auth, "2025-03-01", "2026-03-01");

        System.out.println("통계 조회 SQL 실행 수: 7일=" + week + ", 366일=" + year);
        assertThat(year).isEqualTo(week);
    }

    private long statementCount(String auth, String start, String end) throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        statistics(auth, start, end);
        return statistics.getPrepareStatementCount();
    }

    /** 통계의 모든 날짜를 권장량 API로 하나씩 다시 조회해 칼로리가 같은지 비교한다. */
    private void assertSameAsRecommendationApi(String auth, JsonNode days) throws Exception {
        for (JsonNode day : days) {
            String date = day.get("date").asText();
            String body = mvc.perform(get("/api/nutrition/recommendation").header("Authorization", auth).param("date", date))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode recommendation = mapper.readTree(body);
            BigDecimal expected = recommendation.get("available").asBoolean() ? decimal(recommendation.get("calories")) : null;
            assertThat(decimal(day.get("recommendedCalories"))).as(date).isEqualTo(expected);
        }
    }

    private BigDecimal recommended(JsonNode days, String date) {
        for (JsonNode day : days) {
            if (day.get("date").asText().equals(date))
                return decimal(day.get("recommendedCalories"));
        }
        throw new AssertionError("기간에 없는 날짜: " + date);
    }

    private BigDecimal decimal(JsonNode node) {
        return node == null || node.isNull() ? null : node.decimalValue();
    }

    private JsonNode statistics(String auth, String start, String end) throws Exception {
        String body = mvc.perform(get("/api/statistics").header("Authorization", auth).param("start", start).param("end", end))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("days");
    }

    private void addMeasurement(String auth, String date, double weight) throws Exception {
        mvc.perform(post("/api/users/body-measurements").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"measuredDate\":\"" + date + "\",\"height\":175,\"weight\":" + weight + "}"))
                .andExpect(status().isOk());
    }

    @AllArgsConstructor
    private static class LoginResult {
        private final String auth;
        private final Long userId;
    }

    private String register(String birthDate) throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"Password123!","userName":"테스트 사용자",
                         "gender":"MALE","birthDate":"%s","goalType":"MAINTAIN_WEIGHT"}
                        """.formatted(email, birthDate)))
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
