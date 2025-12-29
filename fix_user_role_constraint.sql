-- Fix the user table constraint to allow all UserRole enum values (0-7)
-- This includes: ROLE_ADMIN, ROLE_USER, ROLE_CASHIER, ROLE_BRANCH_MANAGER, 
-- ROLE_STORE_MANAGER, ROLE_STORE_ADMIN, ROLE_STORE_EMPLOYEE, ROLE_BRANCH_CASHIER

-- Drop the existing constraint (MySQL syntax)
ALTER TABLE `user` DROP CHECK `user_chk_1`;

-- Add the updated constraint to allow values from 0 to 7
ALTER TABLE `user` ADD CONSTRAINT `user_chk_1` CHECK ((`role` between 0 and 7));

