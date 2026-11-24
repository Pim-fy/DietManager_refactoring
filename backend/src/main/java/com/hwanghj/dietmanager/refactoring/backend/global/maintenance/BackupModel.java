package com.hwanghj.dietmanager.refactoring.backend.global.maintenance;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

public final class BackupModel {
    private BackupModel() {
    }

    static final List<String> TABLES = List.of("users", "user_profiles", "body_measurements", "intake_records",
            "nutrition_preferences", "saved_foods", "meal_plans", "auth_sessions", "refresh_tokens", "food_api_usage",
            "setting_histories", "login_failures", "flyway_schema_history");

    /** 테이블 하나의 백업 내용. 백업 JSON 파일 형식이므로 필드 이름을 바꾸지 않는다. */
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class Table {
        private String name;
        private String ddl;
        private List<String> columns;
        private List<List<String>> rows;
    }

    /** 백업 파일 전체. format은 백업 형식 버전. */
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class Backup {
        private int format;
        private List<Table> tables;
    }
}
