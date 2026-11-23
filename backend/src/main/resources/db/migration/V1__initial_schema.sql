CREATE TABLE users (
id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, email VARCHAR(255) NOT NULL UNIQUE,
password_hash VARCHAR(255) NOT NULL, user_name VARCHAR(30) NOT NULL, role VARCHAR(255) NOT NULL,
created_at DATETIME(6), updated_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE user_profiles (
id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL UNIQUE,
gender VARCHAR(255) NOT NULL, birth_date DATE NOT NULL, goal_type VARCHAR(255) NOT NULL,
created_at DATETIME(6), updated_at DATETIME(6), FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE body_measurements (
id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL,
measured_date DATE NOT NULL, height DOUBLE NOT NULL, weight DOUBLE NOT NULL,
deleted BOOLEAN NOT NULL DEFAULT FALSE, created_at DATETIME(6), updated_at DATETIME(6),
FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 기존 테이블을 변경하지 않고 섭취 기록 테이블만 추가한다.
CREATE TABLE IF NOT EXISTS intake_records (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    eaten_date DATE NOT NULL,
    meal_type VARCHAR(20) NOT NULL,
    food_code VARCHAR(100) NOT NULL,
    food_name VARCHAR(500) NOT NULL,
    maker_name VARCHAR(500),
    amount DECIMAL(20,6) NOT NULL,
    base_amount DECIMAL(20,6) NOT NULL,
    unit VARCHAR(10) NOT NULL,
    base_calories DECIMAL(20,6),
    base_carbohydrate DECIMAL(20,6),
    base_protein DECIMAL(20,6),
    base_fat DECIMAL(20,6),
    source_updated_date VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    INDEX idx_intake_user_date (user_id, eaten_date),
    CONSTRAINT fk_intake_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS nutrition_preferences (
user_id BIGINT NOT NULL PRIMARY KEY, activity_level VARCHAR(20) NOT NULL,
CONSTRAINT fk_nutrition_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS saved_foods (
id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL,
food_code VARCHAR(100) NOT NULL, food_name VARCHAR(500) NOT NULL, maker_name VARCHAR(500),
serving_size VARCHAR(100), base_amount DECIMAL(20,6) NOT NULL, base_unit VARCHAR(10) NOT NULL,
calories DECIMAL(20,6), carbohydrate DECIMAL(20,6), protein DECIMAL(20,6), fat DECIMAL(20,6), source_updated_date VARCHAR(50),
favorite BOOLEAN NOT NULL DEFAULT FALSE, last_used_at DATETIME(6),
CONSTRAINT uq_saved_user_food UNIQUE(user_id,food_code),
CONSTRAINT fk_saved_food_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS meal_plans (
id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL,
food_code VARCHAR(100) NOT NULL, food_name VARCHAR(500) NOT NULL, maker_name VARCHAR(500),
serving_size VARCHAR(100), base_amount DECIMAL(20,6) NOT NULL, base_unit VARCHAR(10) NOT NULL,
calories DECIMAL(20,6), carbohydrate DECIMAL(20,6), protein DECIMAL(20,6), fat DECIMAL(20,6), source_updated_date VARCHAR(50),
planned_date DATE NOT NULL, meal_type VARCHAR(20) NOT NULL, amount DECIMAL(20,6) NOT NULL,
deleted BOOLEAN NOT NULL DEFAULT FALSE, intake_id BIGINT, created_at DATETIME(6), updated_at DATETIME(6),
INDEX idx_plan_user_date(user_id,planned_date),
CONSTRAINT fk_plan_user FOREIGN KEY(user_id) REFERENCES users(id),
CONSTRAINT fk_plan_intake FOREIGN KEY(intake_id) REFERENCES intake_records(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
