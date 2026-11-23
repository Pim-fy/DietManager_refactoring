package com.hwanghj.dietmanager.refactoring.backend.security;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
/** 랜덤 테스트 계정만 생성하며 테스트 트랜잭션은 모두 롤백한다. */
@Tag("mysql")
@ActiveProfiles(value={"local","maintenance"},inheritProfiles=false)
@TestPropertySource(properties={"logging.level.root=ERROR","spring.jpa.show-sql=false"})
@Transactional
@org.springframework.test.context.ContextConfiguration(initializers=MySqlIsolation.class)
class MySqlAuthIntegrationTest extends AuthSessionIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired javax.sql.DataSource dataSource;
    @org.junit.jupiter.api.Test void restoredTablesCanBeRetainedForRecovery() throws Exception {
        var table=new com.hwanghj.dietmanager.refactoring.backend.maintenance.BackupModel.Table("users",
            "CREATE TABLE `users` (`id` BIGINT PRIMARY KEY, `marker` VARCHAR(30)) ENGINE=InnoDB",
            java.util.List.of("id","marker"),java.util.List.of(java.util.List.of("1","restore-test")));
        var backup=new com.hwanghj.dietmanager.refactoring.backend.maintenance.BackupModel.Backup(1,java.util.List.of(table));
        try(var connection=dataSource.getConnection()) {
            String prefix=com.hwanghj.dietmanager.refactoring.backend.maintenance.DatabaseMaintenance.restoreAndVerify(connection,backup,true);
            try(var statement=connection.createStatement()) {
                try(var rows=statement.executeQuery("SELECT marker FROM `"+prefix+"users`")) {
                    org.assertj.core.api.Assertions.assertThat(rows.next()).isTrue();
                    org.assertj.core.api.Assertions.assertThat(rows.getString(1)).isEqualTo("restore-test");
                }
            } finally {
                if(!prefix.matches("dm_restore_[0-9a-f]{12}_")) throw new IllegalStateException("검증 접두사 오류");
                try(var statement=connection.createStatement()) { statement.execute("DROP TABLE `"+prefix+"users`"); }
            }
        }
    }
}
