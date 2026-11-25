-- 로그인 실패 횟수를 DB에 보관해 서버를 재시작해도 제한이 유지되게 한다.
-- failure_key는 'account:' 또는 'ip:' 뒤에 이메일·IP의 SHA-256 해시를 붙인 값이다(원문은 저장하지 않음).
CREATE TABLE login_failures (
    failure_key VARCHAR(80) NOT NULL PRIMARY KEY,
    failure_count INT NOT NULL,
    started_at DATETIME(6) NOT NULL,
    INDEX idx_login_failures_started (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
