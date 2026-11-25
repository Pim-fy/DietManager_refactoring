package com.hwanghj.dietmanager.refactoring.backend.features;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.domain.food.dto.FoodNutrition;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.exception.FoodSelectionException;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.service.FoodNutritionService;
import com.hwanghj.dietmanager.refactoring.backend.domain.intake.exception.IntakeException;
import com.hwanghj.dietmanager.refactoring.backend.domain.nutrition.entity.ActivityLevel;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FeatureIntegrationTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @MockitoBean
    FoodNutritionService source;

    private String login(String birth, String goal) throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"Password123!","userName":"기능 테스트","gender":"MALE","birthDate":"%s","goalType":"%s"}
                """.formatted(email, birth, goal))).andExpect(status().isCreated());
        var response = mvc
                .perform(
                        post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readTree(response).path("accessToken").asText();
    }

    private long measurement(String auth, String date, int weight) throws Exception {
        var response = mvc.perform(post("/api/users/body-measurements").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"measuredDate\":\"" + date + "\",\"height\":170,\"weight\":" + weight + "}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).path("measurementId").asLong();
    }

    private String food() {
        String code = UUID.randomUUID().toString();
        when(source.selectedFood(code)).thenReturn(FoodNutrition.builder().foodCode(code).foodName("테스트 식품").makerName("").servingSize("100g")
                .baseAmount(new BigDecimal("100")).baseUnit("g").calories(new BigDecimal("200")).carbohydrate(new BigDecimal("30"))
                .protein(new BigDecimal("10")).sourceUpdatedDate("2026-01-01").build());
        return code;
    }

    /** 같은 요청을 동시에 보내고 응답 상태 코드를 모은다. */
    private List<Integer> concurrently(int count, IntFunction<RequestBuilder> request) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        try {
            var ready = new CountDownLatch(count);
            var go = new CountDownLatch(1);
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int index = i;
                Callable<Integer> task = () -> {
                    ready.countDown();
                    go.await();
                    return mvc.perform(request.apply(index)).andReturn().getResponse().getStatus();
                };
                results.add(pool.submit(task));
            }
            ready.await();
            go.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results)
                statuses.add(result.get());
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }

    // 다른 스레드의 요청이 가입한 사용자를 볼 수 있게 테스트 트랜잭션 없이 실행한다(MySQL 테스트는 클래스 전체가 트랜잭션).
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentFirstSavesOfSameUserDoNotFail() throws Exception {
        String auth = login("1995-01-01", "LOSE_WEIGHT");
        var activities = ActivityLevel.values();
        // 활동량 처음 저장(nutrition_preferences)과 오늘 설정 이력(setting_histories)
        assertThat(concurrently(8, i -> i % 2 == 0
                ? put("/api/nutrition/preferences").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityLevel\":\"" + activities[i % activities.length].name() + "\"}")
                : patch("/api/users/profile").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gender\":\"MALE\",\"birthDate\":\"1995-01-01\",\"goalType\":\"" + (i % 4 == 1 ? "GAIN_WEIGHT" : "MAINTAIN_WEIGHT")
                                + "\"}")))
                .containsOnly(200);
        // 같은 식품의 즐겨찾기와 섭취 기록(saved_foods 처음 저장)
        String favorite = food();
        assertThat(concurrently(8, i -> put("/api/foods/favorites").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"foodCode\":\"" + favorite + "\",\"favorite\":true}"))).containsOnly(200);
        String eaten = food();
        assertThat(concurrently(8, i -> post("/api/intakes").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"foodCode\":\"" + eaten + "\",\"eatenDate\":\"2026-01-01\",\"mealType\":\"LUNCH\",\"amount\":100}")))
                .containsOnly(201);
        mvc.perform(get("/api/foods/favorites").header("Authorization", auth)).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void approvedFormulaActivityAndGoalAreApplied() throws Exception {
        String auth = login("1995-01-01", "LOSE_WEIGHT");
        measurement(auth, "2026-01-01", 70);
        mvc.perform(get("/api/nutrition/recommendation").header("Authorization", auth).param("date", "2026-01-01"))
                .andExpect(jsonPath("$.available").value(false));
        mvc.perform(put("/api/nutrition/preferences").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"activityLevel\":\"MODERATE\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/nutrition/recommendation").header("Authorization", auth).param("date", "2026-01-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.restingCalories").value(1612.5)).andExpect(jsonPath("$.maintenanceCalories").value(2499.38))
                .andExpect(jsonPath("$.calories").value(2199.38)).andExpect(jsonPath("$.carbohydrate").value(274.92))
                .andExpect(jsonPath("$.protein").value(109.97)).andExpect(jsonPath("$.fat").value(73.31));
        mvc.perform(get("/api/nutrition/recommendation").header("Authorization", auth).param("date", "2025-12-31"))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void minorsAndMissingMeasurementsDoNotGetTargets() throws Exception {
        String auth = login("2015-01-01", "MAINTAIN_WEIGHT");
        mvc.perform(put("/api/nutrition/preferences").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"activityLevel\":\"LIGHT\"}"));
        mvc.perform(get("/api/nutrition/recommendation").header("Authorization", auth).param("date", "2026-01-01"))
                .andExpect(jsonPath("$.available").value(false));
        measurement(auth, "2026-01-01", 50);
        mvc.perform(get("/api/nutrition/recommendation").header("Authorization", auth).param("date", "2026-01-01"))
                .andExpect(jsonPath("$.available").value(false)).andExpect(jsonPath("$.calories").isEmpty());
    }

    @Test
    void femaleGainAndEveryActivityFactorAreApplied() throws Exception {
        String auth = login("1995-01-01", "GAIN_WEIGHT");
        measurement(auth, "2026-01-01", 70);
        mvc.perform(patch("/api/users/profile").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"gender\":\"FEMALE\",\"birthDate\":\"1995-01-01\",\"goalType\":\"GAIN_WEIGHT\"}")).andExpect(status().isOk());
        for (var activity : ActivityLevel.values()) {
            mvc.perform(put("/api/nutrition/preferences").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"activityLevel\":\"" + activity.name() + "\"}"))
                    .andExpect(status().isOk());
            double target = BigDecimal.valueOf(1446.5 * activity.factor + 300).setScale(2, RoundingMode.HALF_UP).doubleValue();
            mvc.perform(get("/api/nutrition/recommendation").header("Authorization", auth).param("date", "2026-01-01"))
                    .andExpect(jsonPath("$.restingCalories").value(1446.5)).andExpect(jsonPath("$.calories").value(target));
        }
    }

    @Test
    void futurePlansCannotBeConsumedAndInvalidAmountsAreRejected() throws Exception {
        String auth = login("1995-01-01", "MAINTAIN_WEIGHT"), code = food();
        String body = "{\"foodCode\":\"" + code + "\",\"plannedDate\":\"" + LocalDate.now().plusDays(1) + "\",\"mealType\":\"LUNCH\",\"amount\":100}";
        var response = mvc.perform(post("/api/meal-plans").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(response).path("id").asLong();
        mvc.perform(post("/api/meal-plans/" + id + "/consume").header("Authorization", auth)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEAL_PLAN_CONFLICT"));
        mvc.perform(post("/api/meal-plans").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("\"amount\":100", "\"amount\":-1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void measurementEditsDeletesRestoreAndOwnershipWork() throws Exception {
        String auth = login("1995-01-01", "MAINTAIN_WEIGHT"), other = login("1995-01-01", "MAINTAIN_WEIGHT");
        long old = measurement(auth, "2026-01-01", 70), latest = measurement(auth, "2026-01-02", 69);
        mvc.perform(patch("/api/users/body-measurements/" + latest).header("Authorization", other).contentType(MediaType.APPLICATION_JSON)
                .content("{\"measuredDate\":\"2026-01-02\",\"height\":170,\"weight\":68}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/users/body-measurements/" + latest).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"measuredDate\":\"2026-01-02\",\"height\":170,\"weight\":68}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.weight").value(68));
        mvc.perform(delete("/api/users/body-measurements/" + latest).header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(get("/api/users/body-measurements/latest").header("Authorization", auth)).andExpect(jsonPath("$.measurementId").value(old));
        mvc.perform(get("/api/users/body-measurements/deleted").header("Authorization", auth)).andExpect(jsonPath("$[0].measurementId").value(latest));
        mvc.perform(post("/api/users/body-measurements/" + latest + "/restore").header("Authorization", other)).andExpect(status().isNotFound());
        mvc.perform(post("/api/users/body-measurements/" + latest + "/restore").header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(get("/api/users/body-measurements/latest").header("Authorization", auth)).andExpect(jsonPath("$.weight").value(68));
    }

    @Test
    void favoritesArePrivateAndRemainUsableWithoutSearchCache() throws Exception {
        String auth = login("1995-01-01", "MAINTAIN_WEIGHT"), other = login("1995-01-01", "MAINTAIN_WEIGHT"), code = food();
        mvc.perform(put("/api/foods/favorites").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"foodCode\":\"" + code + "\",\"favorite\":true}"))
                .andExpect(status().isOk());
        when(source.selectedFood(code)).thenThrow(new FoodSelectionException("캐시 만료"));
        mvc.perform(get("/api/foods/favorites").header("Authorization", other)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/intakes").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"foodCode\":\"" + code + "\",\"eatenDate\":\"2026-01-01\",\"mealType\":\"LUNCH\",\"amount\":150}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.calories").value(300));
        mvc.perform(get("/api/foods/recent").header("Authorization", auth)).andExpect(jsonPath("$[0].food.foodCode").value(code));
        mvc.perform(get("/api/foods/recent").header("Authorization", other)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(put("/api/foods/favorites").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"foodCode\":\"" + code + "\",\"favorite\":false}"));
        mvc.perform(get("/api/foods/favorites").header("Authorization", auth)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void plansAreSeparateConvertOnceAndProtectOtherUsers() throws Exception {
        String auth = login("1995-01-01", "MAINTAIN_WEIGHT"), other = login("1995-01-01", "MAINTAIN_WEIGHT"), code = food();
        var response = mvc.perform(post("/api/meal-plans").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"foodCode\":\"" + code + "\",\"plannedDate\":\"2026-01-01\",\"mealType\":\"LUNCH\",\"amount\":150}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(response).path("id").asLong();
        mvc.perform(get("/api/intakes").header("Authorization", auth).param("date", "2026-01-01")).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(post("/api/meal-plans/" + id + "/consume").header("Authorization", other)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/meal-plans/" + id).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"foodCode\":\"" + code + "\",\"plannedDate\":\"2026-01-01\",\"mealType\":\"DINNER\",\"amount\":100}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nutrition.calories").value(200));
        mvc.perform(delete("/api/meal-plans/" + id).header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(post("/api/meal-plans/" + id + "/consume").header("Authorization", auth)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEAL_PLAN_CONFLICT"));
        mvc.perform(post("/api/meal-plans/" + id + "/restore").header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(post("/api/meal-plans/" + id + "/consume").header("Authorization", auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.intakeId").isNumber());
        mvc.perform(post("/api/meal-plans/" + id + "/consume").header("Authorization", auth)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEAL_PLAN_CONFLICT"));
        mvc.perform(get("/api/intakes").header("Authorization", auth).param("date", "2026-01-01")).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.calories.value").value(200));
    }

    @Test
    void statisticsPreserveMissingNutrientsAndRestrictRanges() throws Exception {
        String auth = login("1995-01-01", "MAINTAIN_WEIGHT"), code = food();
        measurement(auth, "2026-01-01", 70);
        mvc.perform(post("/api/intakes").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"foodCode\":\"" + code + "\",\"eatenDate\":\"2026-01-01\",\"mealType\":\"LUNCH\",\"amount\":100}"));
        mvc.perform(get("/api/statistics").header("Authorization", auth).param("start", "2026-01-01").param("end", "2026-01-02"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.days.length()").value(2)).andExpect(jsonPath("$.days[0].calories.value").value(200))
                .andExpect(jsonPath("$.days[0].fat.missingCount").value(1)).andExpect(jsonPath("$.days[0].weight").value(70))
                .andExpect(jsonPath("$.days[1].records").value(0)).andExpect(jsonPath("$.days[1].weight").isEmpty());
        mvc.perform(get("/api/statistics").header("Authorization", auth).param("start", "2026-01-02").param("end", "2026-01-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/statistics").header("Authorization", auth).param("start", "2020-01-01").param("end", "2026-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allNewEndpointsRequireAuthentication() throws Exception {
        for (String path : new String[] { "/api/nutrition/preferences", "/api/nutrition/recommendation?date=2026-01-01",
                "/api/statistics?start=2026-01-01&end=2026-01-02", "/api/foods/favorites", "/api/foods/recent", "/api/meal-plans?date=2026-01-01",
                "/api/users/body-measurements/deleted" })
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
}
