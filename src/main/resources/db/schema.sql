CREATE DATABASE IF NOT EXISTS coach DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE coach;

CREATE TABLE IF NOT EXISTS question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category VARCHAR(32) NOT NULL,
  title VARCHAR(255) NOT NULL,
  answer_standard TEXT NULL,
  key_points JSON NULL,
  difficulty TINYINT DEFAULT 2,
  source VARCHAR(64) NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS attempt (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_id BIGINT NOT NULL,
  user_answer TEXT NOT NULL,
  score TINYINT NULL,
  ai_comment TEXT NULL,
  missed_points JSON NULL,
  followup TEXT NULL,
  status VARCHAR(16) DEFAULT 'pending',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_question_created (question_id, created_at),
  CONSTRAINT fk_attempt_question FOREIGN KEY (question_id) REFERENCES question(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS review_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  attempt_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  mastered TINYINT DEFAULT 0,
  note VARCHAR(500) NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_attempt (attempt_id),
  KEY idx_mastered (mastered)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;