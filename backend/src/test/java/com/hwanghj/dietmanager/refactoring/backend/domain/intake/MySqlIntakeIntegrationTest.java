package com.hwanghj.dietmanager.refactoring.backend.domain.intake;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
@Tag("mysql") @Transactional
@ActiveProfiles(value={"local","maintenance"},inheritProfiles=false)
@TestPropertySource(properties={"logging.level.root=ERROR","spring.jpa.show-sql=false"})
@org.springframework.test.context.ContextConfiguration(initializers=com.hwanghj.dietmanager.refactoring.backend.support.MySqlIsolation.class)
class MySqlIntakeIntegrationTest extends IntakeIntegrationTest {}
