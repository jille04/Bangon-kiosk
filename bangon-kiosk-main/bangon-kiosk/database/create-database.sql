CREATE DATABASE IF NOT EXISTS bangon_kiosk CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- Run as an administrator; replace the placeholder with a strong password.
CREATE USER IF NOT EXISTS 'bangon_app'@'localhost' IDENTIFIED BY 'CHANGE_THIS_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON bangon_kiosk.* TO 'bangon_app'@'localhost';
