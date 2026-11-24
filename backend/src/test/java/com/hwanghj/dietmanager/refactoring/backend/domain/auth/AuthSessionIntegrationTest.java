package com.hwanghj.dietmanager.refactoring.backend.domain.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.hwanghj.dietmanager.refactoring.backend.domain.auth.controller.AuthCookies;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository.AuthSessionRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.service.AuthSessionCleanup;
import com.hwanghj.dietmanager.refactoring.backend.global.security.JwtTokenProvider;

import jakarta.servlet.http.Cookie;
import lombok.AllArgsConstructor;
import lombok.Getter;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSessionIntegrationTest {
    @Test
    void repeatedFailedLoginsHaveSafeRateLimitResponse() throws Exception {
        String body = "{\"email\":\"" + UUID.randomUUID() + "@example.test\",\"password\":\"InvalidPassword123!\"}";
        for (int i = 0; i < 5; i++)
            mvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(result -> Assertions.assertThat(Long.parseLong(result.getResponse().getHeader("Retry-After"))).isBetween(590L, 600L))
                .andExpect(jsonPath("$.code").value("LOGIN_LIMIT"));
    }

    @Test
    void concurrentFailedLoginsOnSameAccountCheckPasswordOnlyUpToLimit() throws Exception {
        String body = "{\"email\":\"" + UUID.randomUUID() + "@example.test\",\"password\":\"InvalidPassword123!\"}";
        String ip = "198.51.100." + ThreadLocalRandom.current().nextInt(1, 255);
        int threads = 30;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            var ready = new CountDownLatch(threads);
            var go = new CountDownLatch(1);
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Callable<Integer> task = () -> {
                    ready.countDown();
                    go.await();
                    return mvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content(body).with(request -> {
                        request.setRemoteAddr(ip);
                        return request;
                    })).andReturn().getResponse().getStatus();
                };
                results.add(pool.submit(task));
            }
            ready.await();
            go.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results)
                statuses.add(result.get());
            Assertions.assertThat(statuses).filteredOn(code -> code == 401).hasSize(5);
            Assertions.assertThat(statuses).filteredOn(code -> code == 429).hasSize(threads - 5);
        } finally {
            pool.shutdownNow();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    JwtTokenProvider jwt;

    @Autowired
    AuthSessionRepository sessions;

    @Autowired
    AuthSessionCleanup cleanup;

    @Getter
    @AllArgsConstructor
    static class Login {
        private final String access;
        private final Cookie refresh;
    }

    Login login() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"Password123!","userName":"인증 검증","gender":"MALE","birthDate":"1990-01-01","goalType":"MAINTAIN_WEIGHT"}
                """.formatted(email))).andExpect(status().isCreated());
        var result = mvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.refreshToken").doesNotExist()).andReturn().getResponse();
        String header = result.getHeader("Set-Cookie");
        Assertions.assertThat(header).contains("HttpOnly", "SameSite=Strict");
        return new Login(mapper.readTree(result.getContentAsString()).get("accessToken").asText(), cookie(header));
    }

    Cookie cookie(String header) {
        return new Cookie(AuthCookies.NAME, header.split(";", 2)[0].split("=", 2)[1]);
    }

    @Test
    void rotatesAndRejectsReplayIncludingIssuedAccessToken() throws Exception {
        var login = login();
        var renewed = mvc.perform(post("/api/users/refresh").cookie(login.getRefresh())).andExpect(status().isOk()).andReturn().getResponse();
        String access = mapper.readTree(renewed.getContentAsString()).get("accessToken").asText();
        mvc.perform(get("/api/users/account").header("Authorization", "Bearer " + access)).andExpect(status().isOk());
        mvc.perform(post("/api/users/refresh").cookie(login.getRefresh())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/users/refresh").cookie(cookie(renewed.getHeader("Set-Cookie")))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/account").header("Authorization", "Bearer " + access)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesBothTokens() throws Exception {
        var login = login();
        mvc.perform(post("/api/users/logout").cookie(login.getRefresh())).andExpect(status().isNoContent());
        mvc.perform(get("/api/users/account").header("Authorization", "Bearer " + login.getAccess())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/users/refresh").cookie(login.getRefresh())).andExpect(status().isUnauthorized());
    }

    @Test
    void passwordChangeRevokesBothTokens() throws Exception {
        var login = login();
        mvc.perform(patch("/api/users/password").header("Authorization", "Bearer " + login.getAccess()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"Password123!\",\"newPassword\":\"NewPassword123!\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/users/account").header("Authorization", "Bearer " + login.getAccess())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/users/refresh").cookie(login.getRefresh())).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsCrossSiteRefreshWithoutConsumingToken() throws Exception {
        var login = login();
        mvc.perform(post("/api/users/refresh").cookie(login.getRefresh()).header("Origin", "https://attacker.example"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/users/refresh").cookie(login.getRefresh()).header("Origin", "http://127.0.0.1:5186"))
                .andExpect(status().isOk());
    }

    @Test
    void expiredRefreshIsRejectedAndCleanedUp() throws Exception {
        var login = login();
        String id = jwt.authentication(login.getAccess()).getSessionId();
        var session = sessions.findById(id).orElseThrow();
        ReflectionTestUtils.setField(session, "expiresAt", LocalDateTime.now(Clock.systemUTC()).minusDays(1));
        sessions.saveAndFlush(session);
        mvc.perform(post("/api/users/refresh").cookie(login.getRefresh())).andExpect(status().isUnauthorized());
        cleanup.cleanup();
        Assertions.assertThat(sessions.existsById(id)).isFalse();
    }
}
