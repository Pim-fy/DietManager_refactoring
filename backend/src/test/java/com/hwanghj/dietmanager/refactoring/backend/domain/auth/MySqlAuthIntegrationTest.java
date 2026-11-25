package com.hwanghj.dietmanager.refactoring.backend.domain.auth;

import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.global.maintenance.BackupModel;
import com.hwanghj.dietmanager.refactoring.backend.global.maintenance.DatabaseMaintenance;
import com.hwanghj.dietmanager.refactoring.backend.support.MySqlIsolation;

/** 랜덤 테스트 계정만 생성하며 테스트 트랜잭션은 모두 롤백한다. */
@Tag("mysql")
@ActiveProfiles(value = { "local", "maintenance" }, inheritProfiles = false)
@TestPropertySource(properties = { "logging.level.root=ERROR", "spring.jpa.show-sql=false" })
@Transactional
@ContextConfiguration(initializers = MySqlIsolation.class)
class MySqlAuthIntegrationTest extends AuthSessionIntegrationTest {
    @Autowired
    DataSource dataSource;

    @Test
    void verifiesLegacyBitColumnsFromBackup() throws Exception {
        var table = new BackupModel.Table("body_measurements",
                "CREATE TABLE `body_measurements` (`id` BIGINT PRIMARY KEY, `deleted` BIT(1), `visible` BOOLEAN) ENGINE=InnoDB",
                List.of("id", "deleted", "visible"),
                List.of(List.of("1", "1", "0"), List.of("2", "0", "1"),
                        Arrays.asList("3", null, null)));
        var backup = new BackupModel.Backup(1, List.of(table));
        try (var connection = dataSource.getConnection()) {
            DatabaseMaintenance.restoreAndVerify(connection, backup, false);
        }
    }

    @Test
    void restoredTablesCanBeRetainedForRecovery() throws Exception {
        var table = new BackupModel.Table("users",
                "CREATE TABLE `users` (`id` BIGINT PRIMARY KEY, `marker` VARCHAR(30)) ENGINE=InnoDB",
                List.of("id", "marker"), List.of(List.of("1", "restore-test")));
        var backup = new BackupModel.Backup(1, List.of(table));
        try (var connection = dataSource.getConnection()) {
            String prefix = DatabaseMaintenance.restoreAndVerify(connection, backup, true);
            try (var statement = connection.createStatement()) {
                try (var rows = statement.executeQuery("SELECT marker FROM `" + prefix + "users`")) {
                    Assertions.assertThat(rows.next()).isTrue();
                    Assertions.assertThat(rows.getString(1)).isEqualTo("restore-test");
                }
            } finally {
                if (!prefix.matches("dm_restore_[0-9a-f]{12}_"))
                    throw new IllegalStateException("검증 접두사 오류");
                try (var statement = connection.createStatement()) {
                    statement.execute("DROP TABLE `" + prefix + "users`");
                }
            }
        }
    }
}
