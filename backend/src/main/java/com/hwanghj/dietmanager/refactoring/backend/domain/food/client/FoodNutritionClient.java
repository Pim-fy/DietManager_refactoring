package com.hwanghj.dietmanager.refactoring.backend.domain.food.client;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import com.hwanghj.dietmanager.refactoring.backend.domain.food.exception.FoodApiException;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.exception.FoodApiLimitException;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.dto.FoodNutrition;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.service.FoodApiQuota;
@Component
public class FoodNutritionClient {
    private static final String ENDPOINT = "https://apis.data.go.kr/1471000/FoodNtrCpntDbInfo03/getFoodNtrCpntDbInq03";
    private final String serviceKey;
    private final FoodNutritionParser parser;
    private final HttpClient http;
    private final String endpoint;
    private final FoodApiQuota quota;

    @org.springframework.beans.factory.annotation.Autowired
    public FoodNutritionClient(@Value("${food-nutrition.service-key:}") String serviceKey, ObjectMapper mapper, FoodApiQuota quota) {
        this(serviceKey, mapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build(), URI.create(ENDPOINT), quota);
    }

    public FoodNutritionClient(@Value("${food-nutrition.service-key:}") String serviceKey, ObjectMapper mapper) {
        this(serviceKey, mapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build(), URI.create(ENDPOINT));
    }
    FoodNutritionClient(String serviceKey, ObjectMapper mapper, HttpClient http, URI endpoint) {
        this(serviceKey, mapper, http, endpoint, null);
    }
    private FoodNutritionClient(String serviceKey, ObjectMapper mapper, HttpClient http, URI endpoint, FoodApiQuota quota) {
        this.quota = quota;
        this.serviceKey = serviceKey;
        this.parser = new FoodNutritionParser(mapper);
        this.http = http;
        this.endpoint = endpoint.toString();
    }

    public FoodNutrition.SearchResult search(String keyword, int page, int size) {
        if (serviceKey.isBlank()) throw new FoodApiException("식품 API 인증키가 설정되지 않았습니다.");
        if (quota!=null) quota.reserve();
        // Decoding 키를 딱 한 번 인코딩한다. URL과 예외 원문에는 인증키가 있어 로그에 남기지 않는다.
        String url = endpoint + "?serviceKey=" + encode(serviceKey) + "&type=json&pageNo=" + page
            + "&numOfRows=" + size + "&FOOD_NM_KR=" + encode(keyword);
        try {
            var response = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 429) throw new FoodApiLimitException();
            if (response.statusCode() != 200) throw new FoodApiException("식품 API 요청을 처리하지 못했습니다. 잠시 후 다시 시도해주세요.");
            return parse(response.body(), page, size);
        } catch (FoodApiException | FoodApiLimitException e) { throw e; }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FoodApiException("식품 검색이 중단되었습니다.");
        } catch (Exception e) {
            // 원인 예외를 연결하지 않아 요청 URI나 키가 응답/로그로 전파되지 않는다.
            throw new FoodApiException("식품 API에 연결할 수 없거나 응답 형식이 올바르지 않습니다.");
        }
    }

    FoodNutrition.SearchResult parse(String json, int page, int size) {
        return parser.parse(json, page, size);
    }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
