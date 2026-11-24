package com.hwanghj.dietmanager.refactoring.backend.domain.auth;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuthSessionRepository extends JpaRepository<AuthSession,String> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from AuthSession s where s.expiresAt <= :now or s.revoked = true")
    int deleteInactive(java.time.LocalDateTime now);
}
