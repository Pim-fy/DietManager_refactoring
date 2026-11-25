package com.hwanghj.dietmanager.refactoring.backend.support;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.support.TestPropertySourceUtils;

/** CREATE DATABASE 권한 없이 새 테이블에서 V1부터 실행하며 종료 시 해당 테이블만 정리한다. */
public class MySqlIsolation implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static String currentPrefix;
    private static final List<String> TABLES = List.of("users", "user_profiles", "body_measurements", "intake_records",
            "nutrition_preferences", "saved_foods", "meal_plans", "auth_sessions", "refresh_tokens", "food_api_usage", "setting_histories", "login_failures",
            "flyway_schema_history");

    public static class Naming extends PhysicalNamingStrategyStandardImpl {
        private final String prefix = currentPrefix;

        @Override
        public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment environment) {
            if (prefix == null)
                throw new IllegalStateException("테스트 접두사 없음");
            return Identifier.toIdentifier(prefix + name.getText(), name.isQuoted());
        }

        @Override
        public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment environment) {
            String snake = name.getText().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
            return Identifier.toIdentifier(snake, name.isQuoted());
        }
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        String prefix = "dm_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "_";
        currentPrefix = prefix;
        var env = context.getEnvironment();
        String url = env.getRequiredProperty("spring.datasource.url");
        String username = env.getRequiredProperty("spring.datasource.username");
        String password = env.getRequiredProperty("spring.datasource.password");
        try {
            Path directory = Path.of("build", "mysql-test-migrations", prefix).toAbsolutePath();
            Files.createDirectories(directory);
            for (String file : List.of("V1__initial_schema.sql", "V2__auth_sessions.sql", "V3__food_api_usage.sql", "V4__manual_intake_nutrition.sql",
                    "V5__setting_histories.sql", "V6__login_failures.sql")) {
                String sql;
                try (var input = MySqlIsolation.class.getClassLoader().getResourceAsStream("db/migration/" + file)) {
                    sql = new String(Objects.requireNonNull(input).readAllBytes(), StandardCharsets.UTF_8);
                }
                for (String table : TABLES)
                    sql = sql.replaceAll("\\b" + table + "\\b", prefix + table);
                sql = sql.replaceAll("(?i)CONSTRAINT\\s+([a-zA-Z0-9_]+)", "CONSTRAINT " + prefix + "$1");
                Files.writeString(directory.resolve(file), sql, StandardCharsets.UTF_8);
            }
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                    "spring.flyway.locations=filesystem:" + directory.toString().replace('\\', '/'),
                    "spring.flyway.table=" + prefix + "flyway_schema_history",
                    "spring.flyway.baseline-on-migrate=true", "spring.flyway.baseline-version=0",
                    "spring.jpa.hibernate.naming.physical-strategy=" + Naming.class.getName());
        } catch (Exception error) {
            throw new IllegalStateException("MySQL 격리 마이그레이션 준비 실패");
        }
        context.addApplicationListener(event -> {
            if (!(event instanceof ContextClosedEvent))
                return;
            try (var connection = DriverManager.getConnection(url, username, password); var statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS=0");
                if (!prefix.matches("dm_test_[0-9a-f]{12}_"))
                    throw new IllegalStateException();
                for (String table : TABLES)
                    statement.execute("DROP TABLE IF EXISTS `" + prefix + table + "`");
                statement.execute("SET FOREIGN_KEY_CHECKS=1");
            } catch (Exception error) {
                System.err.println("MySQL 임시 테이블 정리 실패: " + error.getClass().getSimpleName());
            }
        });
    }
}
