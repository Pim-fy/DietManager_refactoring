-- 목표·활동량을 날짜별로 보관해 과거 날짜의 권장량을 당시 설정으로 계산한다.
CREATE TABLE setting_histories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    effective_date DATE NOT NULL,
    goal_type VARCHAR(30) NOT NULL,
    activity_level VARCHAR(20),
    CONSTRAINT uq_setting_user_date UNIQUE (user_id, effective_date),
    CONSTRAINT fk_setting_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 기존 사용자는 현재 설정을 프로필 생성일부터 적용된 것으로 채운다.
INSERT INTO setting_histories (user_id, effective_date, goal_type, activity_level)
SELECT p.user_id, DATE(COALESCE(p.created_at, CURRENT_TIMESTAMP)), p.goal_type, n.activity_level
FROM user_profiles p
LEFT JOIN nutrition_preferences n ON n.user_id = p.user_id;
