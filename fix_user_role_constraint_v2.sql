-- Fix the user table constraint to allow all UserRole enum values (0-7)
-- This includes: ROLE_ADMIN, ROLE_USER, ROLE_CASHIER, ROLE_BRANCH_MANAGER, 
-- ROLE_STORE_MANAGER, ROLE_STORE_ADMIN, ROLE_STORE_EMPLOYEE, ROLE_BRANCH_CASHIER

USE pos_db;

-- First, check if the constraint exists and drop it
-- MySQL 8.0+ syntax
SET @constraint_exists = (
    SELECT COUNT(*) 
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = 'pos_db' 
    AND TABLE_NAME = 'user' 
    AND CONSTRAINT_NAME = 'user_chk_1'
);

-- Drop the constraint if it exists (MySQL 8.0+)
SET @sql = IF(@constraint_exists > 0, 
    'ALTER TABLE `user` DROP CHECK `user_chk_1`', 
    'SELECT "Constraint does not exist" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add the updated constraint to allow values from 0 to 7
ALTER TABLE `user` ADD CONSTRAINT `user_chk_1` CHECK ((`role` between 0 and 7));

-- Verify the constraint was created
SELECT CONSTRAINT_NAME, CHECK_CLAUSE 
FROM information_schema.TABLE_CONSTRAINTS tc
JOIN information_schema.CHECK_CONSTRAINTS cc ON tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
WHERE tc.CONSTRAINT_SCHEMA = 'pos_db' 
AND tc.TABLE_NAME = 'user' 
AND tc.CONSTRAINT_NAME = 'user_chk_1';


