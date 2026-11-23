package com.hwanghj.dietmanager.refactoring.backend.maintenance;
import java.util.List;
public final class BackupModel {
    private BackupModel() {}
    static final List<String> TABLES=List.of("users","user_profiles","body_measurements","intake_records",
        "nutrition_preferences","saved_foods","meal_plans","auth_sessions","refresh_tokens","food_api_usage","flyway_schema_history");
    public record Table(String name,String ddl,List<String> columns,List<List<String>> rows) {}
    public record Backup(int format,List<Table> tables) {}
}
