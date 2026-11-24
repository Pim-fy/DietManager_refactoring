package com.hwanghj.dietmanager.refactoring.backend.domain.auth.service;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hwanghj.dietmanager.refactoring.backend.domain.user.entity.User;
import com.hwanghj.dietmanager.refactoring.backend.global.security.JwtTokenProvider;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.exception.SessionExpiredException;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity.AuthSession;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.entity.RefreshToken;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository.AuthSessionRepository;
import com.hwanghj.dietmanager.refactoring.backend.domain.auth.repository.RefreshTokenRepository;
@Service
public class AuthSessionService {
    private final java.time.Clock clock;
    private final AuthSessionRepository sessions;
    private final RefreshTokenRepository tokens;
    private final JwtTokenProvider jwt;
    private final SecureRandom random=new SecureRandom();
    public AuthSessionService(AuthSessionRepository sessions,RefreshTokenRepository tokens,JwtTokenProvider jwt) { this(sessions, tokens, jwt, java.time.Clock.systemDefaultZone()); }
    @org.springframework.beans.factory.annotation.Autowired
    public AuthSessionService(AuthSessionRepository sessions,RefreshTokenRepository tokens,JwtTokenProvider jwt, java.time.Clock clock) { this.clock = clock; this.sessions=sessions; this.tokens=tokens; this.jwt=jwt;  }
    public record Pair(String accessToken,String refreshToken) {}
    @Transactional public Pair open(User user) { return issue(sessions.save(new AuthSession(user, LocalDateTime.now(clock.withZone(ZoneOffset.UTC))))); }
    @Transactional(noRollbackFor=SessionExpiredException.class)
    public Pair refresh(String value) {
        var token=tokens.locked(hash(value)).orElseThrow(SessionExpiredException::new);
        var session=token.getSession();
        if (token.isUsed()) { session.revoke(); throw new SessionExpiredException(); }
        if (!valid(session) || session.getTokenVersion()!=session.getUser().getTokenVersion()) throw new SessionExpiredException();
        token.consume(); return issue(session);
    }
    @Transactional public void logout(String value) { if (value!=null) tokens.locked(hash(value)).ifPresent(token->token.getSession().revoke()); }
    @Transactional(readOnly=true) public boolean active(String id,Long userId,long version) {
        if (id==null) return true; // 기존 Access Token은 version=0으로 만료 시까지 호환한다.
        return sessions.findById(id).filter(session->valid(session) && session.getUser().getId().equals(userId) && session.getTokenVersion()==version).isPresent();
    }
    private boolean valid(AuthSession session) { return !session.isRevoked() && session.getExpiresAt().isAfter(LocalDateTime.now(clock.withZone(ZoneOffset.UTC))); }
    private Pair issue(AuthSession session) {
        byte[] bytes=new byte[32]; random.nextBytes(bytes);
        String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new RefreshToken(hash(raw),session));
        return new Pair(jwt.createAccessToken(session.getUser(),session.getId()),raw);
    }
    private String hash(String value) {
        if (value==null || value.length()>200) throw new SessionExpiredException();
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
}
