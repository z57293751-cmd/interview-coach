CREATE TABLE IF NOT EXISTS resume (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  filename     VARCHAR(255) NOT NULL,
  content_text MEDIUMTEXT   NULL COMMENT 'PDF 解析出来的正文',
  profile_json TEXT         NULL COMMENT 'AI 抽取的简历画像',
  summary      VARCHAR(1000) NULL COMMENT '一句话概括',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历';

CREATE TABLE IF NOT EXISTS mock_session (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  resume_id   BIGINT      NOT NULL,
  status      VARCHAR(16) NOT NULL DEFAULT 'running' COMMENT 'running/finished',
  total_score INT         NULL,
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME    NULL,
  PRIMARY KEY (id),
  KEY idx_resume (resume_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟面试场次';

CREATE TABLE IF NOT EXISTS mock_turn (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  session_id    BIGINT       NOT NULL,
  seq           INT          NOT NULL,
  category      VARCHAR(32)  NULL,
  question      TEXT         NOT NULL,
  why           VARCHAR(255) NULL COMMENT '出题意图',
  user_answer   TEXT         NULL,
  score         TINYINT      NULL,
  comment       VARCHAR(500) NULL,
  missed_points JSON         NULL,
  followup      TEXT         NULL,
  status        VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/scored',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_session (session_id, seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟面试每一题';
