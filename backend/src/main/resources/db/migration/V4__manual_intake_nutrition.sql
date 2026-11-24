ALTER TABLE intake_records
ADD COLUMN manual BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN nutrition_edited BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN edited_food_name VARCHAR(500),
ADD COLUMN edited_base_amount DECIMAL(20,6),
ADD COLUMN edited_calories DECIMAL(20,6),
ADD COLUMN edited_carbohydrate DECIMAL(20,6),
ADD COLUMN edited_protein DECIMAL(20,6),
ADD COLUMN edited_fat DECIMAL(20,6);
