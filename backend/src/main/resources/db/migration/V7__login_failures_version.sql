-- 정리 작업과 로그인 시도가 같은 행을 동시에 바꿀 때 충돌을 감지하기 위한 낙관적 잠금 버전.
ALTER TABLE login_failures ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- 키 생성 방식이 SHA-256에서 HMAC-SHA256으로 바뀌어 기존 기록은 다시 쓰이지 않는다(10분짜리 임시 기록).
DELETE FROM login_failures;
