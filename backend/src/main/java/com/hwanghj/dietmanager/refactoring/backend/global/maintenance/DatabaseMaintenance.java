package com.hwanghj.dietmanager.refactoring.backend.global.maintenance;

import static com.hwanghj.dietmanager.refactoring.backend.global.maintenance.BackupModel.Backup;
import static com.hwanghj.dietmanager.refactoring.backend.global.maintenance.BackupModel.TABLES;
import static com.hwanghj.dietmanager.refactoring.backend.global.maintenance.DatabaseBackupOperations.exists;
import static com.hwanghj.dietmanager.refactoring.backend.global.maintenance.DatabaseBackupOperations.restoreAndVerify;
import static com.hwanghj.dietmanager.refactoring.backend.global.maintenance.DatabaseBackupOperations.snapshot;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;

import org.flywaydb.core.Flyway;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.StandardEnvironment;

/** 런타임 설정만 사용하며 접속 정보와 백업 내용은 출력하지 않는다. */
public final class DatabaseMaintenance {
    public static void main(String[] args) throws Exception {
        var environment=new StandardEnvironment();
        environment.setActiveProfiles("local");
        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        String url=environment.getRequiredProperty("spring.datasource.url");
        String username=environment.getRequiredProperty("spring.datasource.username");
        String password=environment.getRequiredProperty("spring.datasource.password");
        try (Connection source=DriverManager.getConnection(url,username,password)) {
            if (!source.getMetaData().getDatabaseProductName().equals("MySQL")) throw new IllegalStateException("MySQL 연결이 필요합니다.");
            String operation=args.length==0 ? "verify" : args[0];
            if (!Set.of("backup","verify","migrate","restore").contains(operation)) throw new IllegalArgumentException("backup / verify / migrate / restore 중 선택하세요.");
            if(operation.equals("restore")) {
                if(args.length!=2) throw new IllegalArgumentException("복원할 백업 파일 경로가 필요합니다.");
                restoreAndVerify(source, BackupFiles.read(Path.of(args[1])), true);
                return;
            }
            Backup backup=snapshot(source);
            Path file = BackupFiles.write(backup);
            System.out.println("백업 생성: "+file+" ("+backup.getTables().size()+"개 테이블, 내용 비공개)");
            if (!operation.equals("backup")) {
                DatabaseBackupOperations.restoreAndVerify(source, BackupFiles.read(file));
            }
            if (operation.equals("migrate")) {
                var flyway=Flyway.configure().dataSource(url,username,password).locations("classpath:db/migration")
                    .baselineVersion("1").baselineOnMigrate(false).load();
                if (!exists(source,"flyway_schema_history") && exists(source,"users")) {
                    for (String table:TABLES.subList(0,7)) if (!exists(source,table)) throw new IllegalStateException("기존 V1 테이블 누락: "+table);
                    flyway.baseline();
                }
                flyway.migrate(); flyway.validate();
                System.out.println("마이그레이션 및 체크섬 검증 완료");
            }
        } catch (Exception error) {
            // JDBC 예외에는 URL·계정이 포함될 수 있어 원문을 출력하지 않는다.
            System.err.println("DB 유지보수 실패: "+error.getClass().getSimpleName());
            if(error instanceof SQLException sql) System.err.println("SQL 상태="+sql.getSQLState()+", 오류 코드="+sql.getErrorCode());
            System.exit(1);
        }
    }
    public static String restoreAndVerify(Connection connection, Backup backup, boolean keep) throws Exception {
        return DatabaseBackupOperations.restoreAndVerify(connection, backup, keep);
    }
}
