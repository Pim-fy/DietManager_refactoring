-- V1 설치와 이전 스키마에서 이름이 다른 이메일 고유 인덱스를 같은 이름으로 맞춘다.
SET @dm_email_index = (
    SELECT INDEX_NAME
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND NON_UNIQUE = 0
      AND INDEX_NAME <> 'PRIMARY'
    GROUP BY INDEX_NAME
    HAVING COUNT(*) = 1 AND MAX(COLUMN_NAME = 'email' AND SUB_PART IS NULL) = 1
);

SET @dm_email_ddl = CASE
    WHEN @dm_email_index = 'uq_users_email' THEN 'SELECT 1'
    WHEN @dm_email_index IS NULL THEN 'ALTER TABLE users ADD UNIQUE INDEX uq_users_email (email)'
    ELSE CONCAT('ALTER TABLE users RENAME INDEX `', REPLACE(@dm_email_index, '`', '``'), '` TO uq_users_email')
END;
PREPARE dm_email_statement FROM @dm_email_ddl;
EXECUTE dm_email_statement;
DEALLOCATE PREPARE dm_email_statement;
