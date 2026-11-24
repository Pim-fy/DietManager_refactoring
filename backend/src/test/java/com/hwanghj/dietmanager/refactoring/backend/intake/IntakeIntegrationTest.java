package com.hwanghj.dietmanager.refactoring.backend.intake;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.hwanghj.dietmanager.refactoring.backend.food.*;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class IntakeIntegrationTest {
    @Autowired IntakeRepository records;
    private String manualBody() { return """
        {"nutrition":{"foodName":"직접 음식","baseAmount":100,"calories":200,"carbohydrate":30,"protein":10,"fat":null},
        "unit":"g","eatenDate":"2026-01-01","mealType":"LUNCH","amount":150}
        """; }
    @Test void manualNutritionScalesAndEditsPersistAcrossAmountDeleteRestoreAndStatistics() throws Exception {
        String auth=login();
        var created=mvc.perform(post("/api/intakes/manual").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(manualBody()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.manual").value(true)).andExpect(jsonPath("$.calories").value(300))
            .andExpect(jsonPath("$.fat").doesNotExist()).andReturn().getResponse().getContentAsString();
        long id=mapper.readTree(created).path("id").asLong();
        mvc.perform(patch("/api/intakes/"+id+"/nutrition").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content("""
            {"foodName":"수정 음식","baseAmount":50,"calories":120,"carbohydrate":null,"protein":0,"fat":5}
            """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.foodName").value("수정 음식"))
            .andExpect(jsonPath("$.calories").value(360)).andExpect(jsonPath("$.nutritionEdited").value(true));
        mvc.perform(patch("/api/intakes/"+id).header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"eatenDate\":\"2026-01-01\",\"mealType\":\"DINNER\",\"amount\":100}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.calories").value(240));
        mvc.perform(get("/api/intakes").header("Authorization",auth).param("date","2026-01-01"))
            .andExpect(jsonPath("$.calories.value").value(240)).andExpect(jsonPath("$.carbohydrate.missingCount").value(1))
            .andExpect(jsonPath("$.protein.value").value(0)).andExpect(jsonPath("$.fat.value").value(10));
        mvc.perform(get("/api/statistics").header("Authorization",auth).param("start","2026-01-01").param("end","2026-01-01"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.days[0].calories.value").value(240));
        mvc.perform(delete("/api/intakes/"+id).header("Authorization",auth)).andExpect(status().isNoContent());
        mvc.perform(patch("/api/intakes/"+id+"/nutrition").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"foodName\":\"불가\",\"baseAmount\":100}")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INTAKE_CONFLICT"));
        mvc.perform(post("/api/intakes/"+id+"/restore").header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.foodName").value("수정 음식")).andExpect(jsonPath("$.calories").value(240));
        verifyNoInteractions(client);
    }
    @Test void apiNutritionCorrectionPreservesSourceAndOtherUsersCannotEdit() throws Exception {
        String auth=login(); search(auth);
        long id=mapper.readTree(create(auth)).path("id").asLong();
        String correction="{\"foodName\":\"사용자 보정\",\"baseAmount\":100,\"calories\":300,\"protein\":0}";
        mvc.perform(patch("/api/intakes/"+id+"/nutrition").header("Authorization",login()).contentType(MediaType.APPLICATION_JSON).content(correction))
            .andExpect(status().isNotFound());
        mvc.perform(patch("/api/intakes/"+id+"/nutrition").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(correction))
            .andExpect(status().isOk()).andExpect(jsonPath("$.manual").value(false)).andExpect(jsonPath("$.calories").value(450));
        var record=records.findById(id).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(record.getFoodName()).isEqualTo("검증 식품");
        org.assertj.core.api.Assertions.assertThat(record.getBaseCalories()).isEqualByComparingTo("200");
        mvc.perform(post("/api/intakes").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"foodCode\":\""+foodCode+"\",\"eatenDate\":\"2026-01-01\",\"mealType\":\"LUNCH\",\"amount\":150}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.calories").value(300)).andExpect(jsonPath("$.nutritionEdited").value(false));
    }
    @Test void manualInputsRequireAuthenticationAndValidateUnitsNamesAndNumbers() throws Exception {
        mvc.perform(post("/api/intakes/manual").contentType(MediaType.APPLICATION_JSON).content(manualBody())).andExpect(status().isUnauthorized());
        String auth=login();
        for(String invalid:java.util.List.of(manualBody().replace("\"g\"","\"개\""),manualBody().replace("직접 음식"," "),
                manualBody().replace("\"baseAmount\":100","\"baseAmount\":0"),manualBody().replace("\"calories\":200","\"calories\":-1"),
                manualBody().replace("\"amount\":150","\"amount\":0"),manualBody().replace("\"calories\":200","\"calories\":0.0000001"))) {
            mvc.perform(post("/api/intakes/manual").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        mvc.perform(post("/api/intakes/manual").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(manualBody().replace("\"g\"","\"ml\"")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.unit").value("ml"));
    }
    @Test void apiLimitHasRetryAfterAndSafeResponse() throws Exception {
        String auth=login();
        when(client.search(anyString(),anyInt(),anyInt())).thenThrow(new FoodApiLimitException());
        mvc.perform(get("/api/foods").param("keyword",foodCode).header("Authorization",auth))
            .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After","60"))
            .andExpect(jsonPath("$.code").value("FOOD_API_LIMIT"));
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean FoodNutritionClient client;
    private String foodCode;
    @BeforeEach void setup() {
        foodCode = UUID.randomUUID().toString();
        var food = new FoodNutrition(foodCode, "검증 식품", "검증 업체", "100g", new BigDecimal("100"), "g",
            new BigDecimal("200"), new BigDecimal("30"), new BigDecimal("10"), null, "2026-01-01");
        when(client.search(anyString(), anyInt(), anyInt())).thenReturn(new FoodNutrition.SearchResult(List.of(food), 1, 10, 1));
    }
    private String login() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"%s","password":"Password123!","userName":"테스트","gender":"MALE","birthDate":"1990-01-01","goalType":"MAINTAIN_WEIGHT"}
            """.formatted(email))).andExpect(status().isCreated());
        var result = mvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"Password123!\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readTree(result).get("accessToken").asText();
    }
    private void search(String auth) throws Exception {
        mvc.perform(get("/api/foods").param("keyword", foodCode).header("Authorization", auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].foodCode").value(foodCode));
    }
    private String create(String auth) throws Exception {
        return mvc.perform(post("/api/intakes").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"foodCode":"%s","eatenDate":"2026-01-01","mealType":"LUNCH","amount":150,"calories":99999}
                """.formatted(foodCode))).andExpect(status().isCreated())
            .andExpect(jsonPath("$.calories").value(300)).andExpect(jsonPath("$.carbohydrate").value(45))
            .andExpect(jsonPath("$.protein").value(15)).andExpect(jsonPath("$.fat").isEmpty())
            .andReturn().getResponse().getContentAsString();
    }
    @Test void snapshotsPersistAndUpdatesDeleteRestoreRecalculateDailyTotals() throws Exception {
        String auth = login(); search(auth);
        long id = mapper.readTree(create(auth)).get("id").asLong();
        mvc.perform(get("/api/intakes").param("date", "2026-01-01").header("Authorization", auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.calories.value").value(300))
            .andExpect(jsonPath("$.fat.missingCount").value(1));
        var updatedSource = new FoodNutrition(foodCode, "새 원본 이름", "", "100g", new BigDecimal("100"), "g",
            new BigDecimal("900"), null, null, null, "2026-02-02");
        when(client.search(anyString(), anyInt(), anyInt())).thenReturn(new FoodNutrition.SearchResult(List.of(updatedSource), 1, 10, 1));
        mvc.perform(get("/api/foods").param("keyword", "updated-" + foodCode).header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(patch("/api/intakes/" + id).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"eatenDate\":\"2026-01-02\",\"mealType\":\"DINNER\",\"amount\":50}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.foodName").value("검증 식품"))
            .andExpect(jsonPath("$.calories").value(100));
        mvc.perform(get("/api/intakes").param("date", "2026-01-01").header("Authorization", auth))
            .andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(delete("/api/intakes/" + id).header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(get("/api/intakes").param("date", "2026-01-02").header("Authorization", auth))
            .andExpect(jsonPath("$.calories.value").value(0)).andExpect(jsonPath("$.items.length()").value(0))
            .andExpect(jsonPath("$.deletedItems[0].id").value(id));
        mvc.perform(post("/api/intakes/" + id + "/restore").header("Authorization", auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.calories").value(100));
        mvc.perform(get("/api/intakes").param("date", "2026-01-02").header("Authorization", auth))
            .andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.calories.value").value(100))
            .andExpect(jsonPath("$.deletedItems.length()").value(0));
    }
    @Test void anotherUserCannotReadChangeDeleteOrRestoreRecords() throws Exception {
        String auth = login(); search(auth);
        long id = mapper.readTree(create(auth)).get("id").asLong();
        String other = login();
        mvc.perform(get("/api/intakes").param("date", "2026-01-01").header("Authorization", other))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(delete("/api/intakes/" + id).header("Authorization", other)).andExpect(status().isNotFound());
        mvc.perform(post("/api/intakes/" + id + "/restore").header("Authorization", other)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/intakes/" + id).header("Authorization", other).contentType(MediaType.APPLICATION_JSON)
            .content("{\"eatenDate\":\"2026-01-01\",\"mealType\":\"LUNCH\",\"amount\":50}"))
            .andExpect(status().isNotFound());
    }
    @Test void requiresAuthenticationAndValidatesParametersAndAmounts() throws Exception {
        mvc.perform(get("/api/foods").param("keyword", "밥")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/intakes").param("date", "2026-01-01")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/intakes").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        String auth = login();
        mvc.perform(get("/api/foods").param("keyword", "밥").param("size", "5000").header("Authorization", auth))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/foods").param("keyword", " ").header("Authorization", auth)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/intakes").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"foodCode\":\"x\",\"eatenDate\":\"2026-01-01\",\"mealType\":\"LUNCH\",\"amount\":-1}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/intakes").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"foodCode\":\"unknown\",\"eatenDate\":\"2026-01-01\",\"mealType\":\"LUNCH\",\"amount\":1}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("FOOD_SELECTION_CONFLICT"));
    }
    @Test void repeatedSearchUsesCache() throws Exception {
        String auth = login(); search(auth); search(auth);
        verify(client, times(1)).search(foodCode, 1, 10);
    }
}
