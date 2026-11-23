package com.hwanghj.dietmanager.refactoring.backend.features;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import com.hwanghj.dietmanager.refactoring.backend.food.*;
import com.hwanghj.dietmanager.refactoring.backend.intake.IntakeException;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class FeatureIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean FoodNutritionService source;
    private String login(String birth,String goal) throws Exception {
        String email=UUID.randomUUID()+"@example.test";
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"%s","password":"Password123!","userName":"기능 테스트","gender":"MALE","birthDate":"%s","goalType":"%s"}
            """.formatted(email,birth,goal))).andExpect(status().isCreated());
        var response=mvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"Password123!\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer "+mapper.readTree(response).path("accessToken").asText();
    }
    private long measurement(String auth,String date,int weight) throws Exception {
        var response=mvc.perform(post("/api/users/body-measurements").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"measuredDate\":\""+date+"\",\"height\":170,\"weight\":"+weight+"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).path("measurementId").asLong();
    }
    private String food() {
        String code=UUID.randomUUID().toString();
        when(source.selectedFood(code)).thenReturn(new FoodNutrition(code,"테스트 식품","","100g",new BigDecimal("100"),"g",new BigDecimal("200"),new BigDecimal("30"),new BigDecimal("10"),null,"2026-01-01"));
        return code;
    }
    @Test void approvedFormulaActivityAndGoalAreApplied() throws Exception {
        String auth=login("1995-01-01","LOSE_WEIGHT"); measurement(auth,"2026-01-01",70);
        mvc.perform(get("/api/nutrition/recommendation").header("Authorization",auth).param("date","2026-01-01"))
            .andExpect(jsonPath("$.available").value(false));
        mvc.perform(put("/api/nutrition/preferences").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"activityLevel\":\"MODERATE\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/nutrition/recommendation").header("Authorization",auth).param("date","2026-01-01"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.restingCalories").value(1612.5)).andExpect(jsonPath("$.maintenanceCalories").value(2499.38))
            .andExpect(jsonPath("$.calories").value(2199.38)).andExpect(jsonPath("$.carbohydrate").value(274.92)).andExpect(jsonPath("$.protein").value(109.97)).andExpect(jsonPath("$.fat").value(73.31));
        mvc.perform(get("/api/nutrition/recommendation").header("Authorization",auth).param("date","2025-12-31"))
            .andExpect(jsonPath("$.available").value(false));
    }
    @Test void minorsAndMissingMeasurementsDoNotGetTargets() throws Exception {
        String auth=login("2015-01-01","MAINTAIN_WEIGHT");
        mvc.perform(put("/api/nutrition/preferences").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"activityLevel\":\"LIGHT\"}"));
        mvc.perform(get("/api/nutrition/recommendation").header("Authorization",auth).param("date","2026-01-01")).andExpect(jsonPath("$.available").value(false));
        measurement(auth,"2026-01-01",50);
        mvc.perform(get("/api/nutrition/recommendation").header("Authorization",auth).param("date","2026-01-01")).andExpect(jsonPath("$.available").value(false)).andExpect(jsonPath("$.calories").isEmpty());
    }
    @Test void femaleGainAndEveryActivityFactorAreApplied() throws Exception {
        String auth=login("1995-01-01","GAIN_WEIGHT"); measurement(auth,"2026-01-01",70);
        mvc.perform(patch("/api/users/profile").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"gender\":\"FEMALE\",\"birthDate\":\"1995-01-01\",\"goalType\":\"GAIN_WEIGHT\"}")).andExpect(status().isOk());
        for (var activity:com.hwanghj.dietmanager.refactoring.backend.nutrition.ActivityLevel.values()) {
            mvc.perform(put("/api/nutrition/preferences").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"activityLevel\":\""+activity.name()+"\"}"))
                .andExpect(status().isOk());
            double target=BigDecimal.valueOf(1446.5*activity.factor+300).setScale(2,java.math.RoundingMode.HALF_UP).doubleValue();
            mvc.perform(get("/api/nutrition/recommendation").header("Authorization",auth).param("date","2026-01-01"))
                .andExpect(jsonPath("$.restingCalories").value(1446.5)).andExpect(jsonPath("$.calories").value(target));
        }
    }
    @Test void futurePlansCannotBeConsumedAndInvalidAmountsAreRejected() throws Exception {
        String auth=login("1995-01-01","MAINTAIN_WEIGHT"),code=food();
        String body="{\"foodCode\":\""+code+"\",\"plannedDate\":\""+java.time.LocalDate.now().plusDays(1)+"\",\"mealType\":\"LUNCH\",\"amount\":100}";
        var response=mvc.perform(post("/api/meal-plans").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id=mapper.readTree(response).path("id").asLong();
        mvc.perform(post("/api/meal-plans/"+id+"/consume").header("Authorization",auth)).andExpect(status().isConflict());
        mvc.perform(post("/api/meal-plans").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(body.replace("\"amount\":100","\"amount\":-1")))
            .andExpect(status().isBadRequest());
    }
    @Test void measurementEditsDeletesRestoreAndOwnershipWork() throws Exception {
        String auth=login("1995-01-01","MAINTAIN_WEIGHT"), other=login("1995-01-01","MAINTAIN_WEIGHT");
        long old=measurement(auth,"2026-01-01",70), latest=measurement(auth,"2026-01-02",69);
        mvc.perform(patch("/api/users/body-measurements/"+latest).header("Authorization",other).contentType(MediaType.APPLICATION_JSON).content("{\"measuredDate\":\"2026-01-02\",\"height\":170,\"weight\":68}"))
            .andExpect(status().isNotFound());
        mvc.perform(patch("/api/users/body-measurements/"+latest).header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"measuredDate\":\"2026-01-02\",\"height\":170,\"weight\":68}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.weight").value(68));
        mvc.perform(delete("/api/users/body-measurements/"+latest).header("Authorization",auth)).andExpect(status().isNoContent());
        mvc.perform(get("/api/users/body-measurements/latest").header("Authorization",auth)).andExpect(jsonPath("$.measurementId").value(old));
        mvc.perform(get("/api/users/body-measurements/deleted").header("Authorization",auth)).andExpect(jsonPath("$[0].measurementId").value(latest));
        mvc.perform(post("/api/users/body-measurements/"+latest+"/restore").header("Authorization",other)).andExpect(status().isNotFound());
        mvc.perform(post("/api/users/body-measurements/"+latest+"/restore").header("Authorization",auth)).andExpect(status().isOk());
        mvc.perform(get("/api/users/body-measurements/latest").header("Authorization",auth)).andExpect(jsonPath("$.weight").value(68));
    }
    @Test void favoritesArePrivateAndRemainUsableWithoutSearchCache() throws Exception {
        String auth=login("1995-01-01","MAINTAIN_WEIGHT"),other=login("1995-01-01","MAINTAIN_WEIGHT"),code=food();
        mvc.perform(put("/api/foods/favorites").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"foodCode\":\""+code+"\",\"favorite\":true}"))
            .andExpect(status().isOk());
        when(source.selectedFood(code)).thenThrow(new com.hwanghj.dietmanager.refactoring.backend.food.FoodSelectionException("캐시 만료"));
        mvc.perform(get("/api/foods/favorites").header("Authorization",other)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/intakes").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"foodCode\":\""+code+"\",\"eatenDate\":\"2026-01-01\",\"mealType\":\"LUNCH\",\"amount\":150}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.calories").value(300));
        mvc.perform(get("/api/foods/recent").header("Authorization",auth)).andExpect(jsonPath("$[0].food.foodCode").value(code));
        mvc.perform(get("/api/foods/recent").header("Authorization",other)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(put("/api/foods/favorites").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"foodCode\":\""+code+"\",\"favorite\":false}"));
        mvc.perform(get("/api/foods/favorites").header("Authorization",auth)).andExpect(jsonPath("$.length()").value(0));
    }
    @Test void plansAreSeparateConvertOnceAndProtectOtherUsers() throws Exception {
        String auth=login("1995-01-01","MAINTAIN_WEIGHT"),other=login("1995-01-01","MAINTAIN_WEIGHT"),code=food();
        var response=mvc.perform(post("/api/meal-plans").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"foodCode\":\""+code+"\",\"plannedDate\":\"2026-01-01\",\"mealType\":\"LUNCH\",\"amount\":150}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id=mapper.readTree(response).path("id").asLong();
        mvc.perform(get("/api/intakes").header("Authorization",auth).param("date","2026-01-01")).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(post("/api/meal-plans/"+id+"/consume").header("Authorization",other)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/meal-plans/"+id).header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"foodCode\":\""+code+"\",\"plannedDate\":\"2026-01-01\",\"mealType\":\"DINNER\",\"amount\":100}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nutrition.calories").value(200));
        mvc.perform(delete("/api/meal-plans/"+id).header("Authorization",auth)).andExpect(status().isNoContent());
        mvc.perform(post("/api/meal-plans/"+id+"/consume").header("Authorization",auth)).andExpect(status().isConflict());
        mvc.perform(post("/api/meal-plans/"+id+"/restore").header("Authorization",auth)).andExpect(status().isOk());
        mvc.perform(post("/api/meal-plans/"+id+"/consume").header("Authorization",auth)).andExpect(status().isOk()).andExpect(jsonPath("$.intakeId").isNumber());
        mvc.perform(post("/api/meal-plans/"+id+"/consume").header("Authorization",auth)).andExpect(status().isConflict());
        mvc.perform(get("/api/intakes").header("Authorization",auth).param("date","2026-01-01")).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.calories.value").value(200));
    }
    @Test void statisticsPreserveMissingNutrientsAndRestrictRanges() throws Exception {
        String auth=login("1995-01-01","MAINTAIN_WEIGHT"),code=food(); measurement(auth,"2026-01-01",70);
        mvc.perform(post("/api/intakes").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("{\"foodCode\":\""+code+"\",\"eatenDate\":\"2026-01-01\",\"mealType\":\"LUNCH\",\"amount\":100}"));
        mvc.perform(get("/api/statistics").header("Authorization",auth).param("start","2026-01-01").param("end","2026-01-02"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.days.length()").value(2)).andExpect(jsonPath("$.days[0].calories.value").value(200))
            .andExpect(jsonPath("$.days[0].fat.missingCount").value(1)).andExpect(jsonPath("$.days[0].weight").value(70)).andExpect(jsonPath("$.days[1].records").value(0)).andExpect(jsonPath("$.days[1].weight").isEmpty());
        mvc.perform(get("/api/statistics").header("Authorization",auth).param("start","2026-01-02").param("end","2026-01-01")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/statistics").header("Authorization",auth).param("start","2020-01-01").param("end","2026-01-01")).andExpect(status().isBadRequest());
    }
    @Test void allNewEndpointsRequireAuthentication() throws Exception {
        for (String path:new String[]{"/api/nutrition/preferences","/api/nutrition/recommendation?date=2026-01-01","/api/statistics?start=2026-01-01&end=2026-01-02","/api/foods/favorites","/api/foods/recent","/api/meal-plans?date=2026-01-01","/api/users/body-measurements/deleted"}) mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
}
