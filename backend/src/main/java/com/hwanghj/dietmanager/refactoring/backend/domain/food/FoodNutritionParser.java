package com.hwanghj.dietmanager.refactoring.backend.domain.food;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.hwanghj.dietmanager.refactoring.backend.domain.food.exception.FoodApiException;
public final class FoodNutritionParser {
    private static final Pattern BASIS = Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)\\s*(g|ml)$", Pattern.CASE_INSENSITIVE);
    private final ObjectMapper mapper;
    public FoodNutritionParser(ObjectMapper mapper) { this.mapper = mapper; }
    public FoodNutrition.SearchResult parse(String json, int page, int size) {
        JsonNode root = mapper.readTree(json);
        if (root.has("response")) root = root.get("response");
        String code = text(root.path("header"), "resultCode");
        if (!"00".equals(code) && !"0".equals(code)) {
            throw new FoodApiException("식품 API 인증 또는 요청에 실패했습니다. 활용승인 상태와 호출 제한을 확인해주세요.");
        }
        JsonNode body = root.path("body");
        if (!body.isObject()) throw new FoodApiException("식품 API 응답 본문을 확인할 수 없습니다.");
        JsonNode items = body.path("items");
        if (items.isObject() && items.has("item")) items = items.get("item");
        List<FoodNutrition> foods = new ArrayList<>();
        if (items.isArray()) for (JsonNode item : items) foods.add(toFood(item));
        else if (items.isObject() && items.has("FOOD_CD")) foods.add(toFood(items));
        int total = body.path("totalCount").asInt(foods.size());
        if (total < 0 || (foods.isEmpty() && total > 0 && (long) (page - 1) * size < total)) {
            throw new FoodApiException("식품 API 검색 결과 형식을 확인할 수 없습니다.");
        }
        return new FoodNutrition.SearchResult(foods, page, size, total);
    }

    private FoodNutrition toFood(JsonNode item) {
        String servingSize = text(item, "SERVING_SIZE");
        var match = BASIS.matcher(servingSize.replace(",", "").trim());
        BigDecimal amount = null; String unit = null;
        if (match.matches()) { amount = new BigDecimal(match.group(1)); unit = match.group(2).toLowerCase(); }
        String code = text(item, "FOOD_CD"); String name = text(item, "FOOD_NM_KR");
        if (code.isBlank() || name.isBlank()) throw new FoodApiException("식품 API에서 식품 식별 정보를 확인할 수 없습니다.");
        return new FoodNutrition(code, name, text(item, "MAKER_NM"), servingSize, amount, unit,
            number(item, "AMT_NUM1"), number(item, "AMT_NUM6"), number(item, "AMT_NUM3"), number(item, "AMT_NUM4"),
            text(item, "UPDATE_DATE"));
    }
    private static String text(JsonNode node, String field) { return node.path(field).isNull() ? "" : node.path(field).asText(""); }
    private static BigDecimal number(JsonNode node, String field) {
        String value = text(node, field).trim().replace(",", "");
        if (!value.matches("[0-9]+(?:\\.[0-9]+)?")) return null;
        return new BigDecimal(value);
    }
}
