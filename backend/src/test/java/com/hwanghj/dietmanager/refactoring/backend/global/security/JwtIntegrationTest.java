package com.hwanghj.dietmanager.refactoring.backend.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import tools.jackson.databind.ObjectMapper;

/** 실제 DB, 비밀번호 해싱, JWT 발급과 SecurityFilterChain을 함께 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void invalidSignedSubjectsAreUnauthorized() throws Exception {
        var key = Keys.hmacShaKeyFor("dietmanager-test-only-signing-key-at-least-32-bytes".getBytes(StandardCharsets.UTF_8));
        for (String subject : new String[]{"abc", "0", "-1", "9999999999999999999999999", ""}) {
            String token = Jwts.builder().subject(subject).expiration(new Date(System.currentTimeMillis() + 60000)).signWith(key).compact();
            mvc.perform(get("/api/users/account").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void sameDayMeasurementsPreferLastSavedRecord() throws Exception {
        String auth = "Bearer " + registerAndLogin();
        for (int weight : new int[]{65, 66}) {
            mvc.perform(post("/api/users/body-measurements").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"measuredDate\":\"2026-01-01\",\"height\":170,\"weight\":" + weight + "}"))
                .andExpect(status().isOk());
        }
        mvc.perform(get("/api/users/body-measurements/latest").header("Authorization", auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.weight").value(66));
        mvc.perform(get("/api/users/body-measurements").header("Authorization", auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].weight").value(66));
    }

    @Test
    void malformedBodiesAndQueryParametersHaveCommonValidationResponse() throws Exception {
        String auth = "Bearer " + registerAndLogin();
        mvc.perform(patch("/api/users/profile").header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content("{\"gender\":\"INVALID\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/intakes").header("Authorization", auth).param("date", "not-a-date"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/intakes").header("Authorization", auth))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
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
                """.formatted(email))).andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("accessToken").asText();
    }

    @Test
    void issuedTokenAuthenticatesAccountAndPersistsChanges() throws Exception {
        String token = registerAndLogin();
        mvc.perform(get("/api/users/account").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.userName").value("테스트 사용자"));
        String email = UUID.randomUUID() + "@example.test";
        mvc.perform(patch("/api/users/account").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"userName\":\"수정 이름\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/users/account").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void profileMeasurementsAndPasswordWorkThroughAuthenticationFilter() throws Exception {
        String token = registerAndLogin();
        String auth = "Bearer " + token;
        mvc.perform(patch("/api/users/profile").header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gender\":\"FEMALE\",\"birthDate\":\"1990-02-02\",\"goalType\":\"LOSE_WEIGHT\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/users/profile").header("Authorization", auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.goalType").value("LOSE_WEIGHT"));
        mvc.perform(get("/api/users/body-measurements/latest").header("Authorization", auth))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/users/body-measurements").header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"measuredDate\":\"2026-01-01\",\"height\":170,\"weight\":65}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/users/body-measurements/latest").header("Authorization", auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.weight").value(65));
        mvc.perform(get("/api/users/body-measurements").header("Authorization", auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        String account = mvc.perform(get("/api/users/account").header("Authorization", auth))
            .andReturn().getResponse().getContentAsString();
        String email = mapper.readTree(account).get("email").asText();
        mvc.perform(patch("/api/users/password").header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"currentPassword\":\"Password123!\",\"newPassword\":\"Changed123!\"}"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"Password123!\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"Changed123!\"}"))
            .andExpect(status().isOk());
        // 다른 사용자의 토큰으로 앞 사용자의 측정 이력을 조회할 수 없어야 한다.
        mvc.perform(get("/api/users/body-measurements").header("Authorization", "Bearer " + registerAndLogin()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/account", "/profile", "/body-measurements", "/body-measurements/latest"})
    void missingOrMalformedTokenIsRejected(String path) throws Exception {
        mvc.perform(get("/api/users" + path)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users" + path).header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/not-mapped", "/api/users/not-mapped", "/not-mapped"})
    void unlistedPathsRequireAuthentication(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpointsStayOpen() throws Exception {
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void authenticatedUnknownPathIsNotFound() throws Exception {
        mvc.perform(get("/api/not-mapped").header("Authorization", "Bearer " + registerAndLogin()))
            .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/account", "/profile", "/password"})
    void updatesRequireAuthentication(String path) throws Exception {
        mvc.perform(patch("/api/users" + path).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void measurementWritesRequireAuthenticationAndValidateInput() throws Exception {
        mvc.perform(post("/api/users/body-measurements").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        String auth = "Bearer " + registerAndLogin();
        mvc.perform(post("/api/users/body-measurements").header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"measuredDate\":\"2026-01-01\",\"height\":170,\"weight\":-1}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/users/body-measurements").header("Authorization", auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void expiredTamperedAndUnknownUserTokensAreRejected() throws Exception {
        String token = registerAndLogin();
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "." + (parts[2].startsWith("A") ? "B" : "A") + parts[2].substring(1);
        var key = Keys.hmacShaKeyFor("dietmanager-test-only-signing-key-at-least-32-bytes".getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder().subject("99999999").expiration(new Date(0)).signWith(key).compact();
        String unknown = Jwts.builder().subject("99999999").expiration(new Date(System.currentTimeMillis() + 60000)).signWith(key).compact();
        for (String rejected : new String[]{tampered, expired, unknown}) {
            mvc.perform(get("/api/users/account").header("Authorization", "Bearer " + rejected))
                .andExpect(status().isUnauthorized());
        }
    }
}
