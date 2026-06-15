ALTER TABLE asset_import_batch
  ADD COLUMN source_type VARCHAR(20) NOT NULL DEFAULT 'EXCEL' AFTER file_name;

ALTER TABLE asset_import_item
  ADD COLUMN enabled_date DATE NULL AFTER purchase_date,
  ADD COLUMN asset_description VARCHAR(1000) NULL AFTER enabled_date,
  ADD COLUMN source_image_object_key VARCHAR(500) NULL AFTER asset_description,
  ADD COLUMN ai_recognize_status VARCHAR(20) NULL AFTER source_image_object_key;

ALTER TABLE repair_order
  ADD COLUMN suspected_duplicate TINYINT NOT NULL DEFAULT 0 AFTER exported_flag,
  ADD COLUMN duplicate_reason VARCHAR(1000) NULL AFTER suspected_duplicate,
  ADD KEY idx_repair_order_suspected (suspected_duplicate, status, create_time);

CREATE TABLE repair_order_ai_link (
  link_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  source_order_id BIGINT NOT NULL,
  target_order_id BIGINT NOT NULL,
  link_type VARCHAR(20) NOT NULL,
  ai_reason VARCHAR(1000) NULL,
  confirmed TINYINT NOT NULL DEFAULT 0,
  operator_id BIGINT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  delete_state TINYINT NOT NULL DEFAULT 0,
  KEY idx_ai_link_source (source_order_id, confirmed, delete_state),
  KEY idx_ai_link_target (target_order_id, delete_state),
  UNIQUE KEY uk_ai_link_pair (source_order_id, target_order_id, delete_state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_audit_log (
  audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  operator_id BIGINT NULL,
  scene_type VARCHAR(50) NOT NULL,
  target_type VARCHAR(50) NULL,
  target_id BIGINT NULL,
  result_status VARCHAR(20) NOT NULL,
  failure_reason VARCHAR(500) NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  delete_state TINYINT NOT NULL DEFAULT 0,
  KEY idx_ai_audit_operator (operator_id, create_time),
  KEY idx_ai_audit_scene (scene_type, create_time),
  KEY idx_ai_audit_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_assistant_session (
  session_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  scene_type VARCHAR(50) NOT NULL,
  expire_time DATETIME NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  delete_state TINYINT NOT NULL DEFAULT 0,
  KEY idx_ai_session_user (user_id, expire_time, delete_state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_assistant_message (
  message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL,
  content_summary VARCHAR(2000) NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  delete_state TINYINT NOT NULL DEFAULT 0,
  KEY idx_ai_message_session (session_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
