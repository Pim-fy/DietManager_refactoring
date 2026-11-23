package com.hwanghj.dietmanager.refactoring.backend.security;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :hash")
    Optional<RefreshToken> locked(String hash);
    @Modifying
    @Query("delete from RefreshToken t where t.session.id in (select s.id from AuthSession s where s.expiresAt <= :now or s.revoked = true)")
    int deleteInactive(java.time.LocalDateTime now);
}
