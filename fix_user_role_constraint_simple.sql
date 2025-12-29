-- Simple version: Fix the user table constraint to allow all UserRole enum values (0-7)
-- Run this if the complex version doesn't work

USE pos_db;

-- Drop the constraint (will fail if it doesn't exist, but that's okay)
ALTER TABLE `user` DROP CHECK `user_chk_1`;

-- Add the updated constraint to allow values from 0 to 7
ALTER TABLE `user` ADD CONSTRAINT `user_chk_1` CHECK ((`role` between 0 and 7));

