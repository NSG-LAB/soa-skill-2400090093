-- ===================================================
-- In-Lab Task Manager Microservice Database Setup
-- ===================================================

CREATE DATABASE IF NOT EXISTS task_db;
USE task_db;

-- Tasks table definition (auto-synced by Hibernate if ddl-auto=update)
CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    category VARCHAR(50) DEFAULT 'General',
    due_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
