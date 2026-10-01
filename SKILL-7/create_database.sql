CREATE DATABASE IF NOT EXISTS library_db;

USE library_db;

-- Books table (managed by Hibernate, included for reference)
-- CREATE TABLE IF NOT EXISTS books (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     title VARCHAR(255) NOT NULL,
--     author VARCHAR(255) NOT NULL,
--     isbn VARCHAR(255) NOT NULL UNIQUE
-- );

-- Users table (managed by Hibernate, included for reference)
-- CREATE TABLE IF NOT EXISTS users (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     username VARCHAR(255) NOT NULL UNIQUE,
--     email VARCHAR(255),
--     password VARCHAR(255),
--     role VARCHAR(20) NOT NULL DEFAULT 'USER',
--     github_id BIGINT UNIQUE,
--     auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL'
-- );

SHOW DATABASES;
SHOW TABLES;
