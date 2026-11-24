package com.hwanghj.dietmanager.refactoring.backend.domain.intake;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.hwanghj.dietmanager.refactoring.backend.support.MySqlIsolation;

@Tag("mysql")
@Transactional
@ActiveProfiles(value = { "local", "maintenance" }, inheritProfiles = false)
@TestPropertySource(properties = { "logging.level.root=ERROR", "spring.jpa.show-sql=false" })
@ContextConfiguration(initializers = MySqlIsolation.class)
class MySqlIntakeIntegrationTest extends IntakeIntegrationTest {
}
