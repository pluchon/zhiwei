CREATE TABLE asset_category (
  asset_category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  category_name VARCHAR(100) NOT NULL,
  normalized_name VARCHAR(100) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  delete_state TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_asset_category_name (normalized_name),
  KEY idx_asset_category_status (delete_state, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE asset (
  asset_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  asset_no VARCHAR(30) NOT NULL,
  asset_name VARCHAR(150) NOT NULL,
  asset_category_id BIGINT NOT NULL,
  campus_id BIGINT NOT NULL,
  building_id BIGINT NULL,
  floor VARCHAR(50) NULL,
  room VARCHAR(100) NULL,
  location_detail VARCHAR(500) NULL,
  status VARCHAR(30) NOT NULL,
  description VARCHAR(1000) NULL,
  enabled_date DATE NULL,
  image_object_key VARCHAR(500) NULL,
  active_order_id BIGINT NULL,
  version INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  delete_state TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_asset_no (asset_no),
  KEY idx_asset_query (delete_state, status, asset_category_id, campus_id, building_id),
  KEY idx_asset_name (asset_name),
  KEY idx_asset_active_order (active_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE asset_status_log (
  asset_status_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  asset_id BIGINT NOT NULL,
  before_status VARCHAR(30) NOT NULL,
  after_status VARCHAR(30) NOT NULL,
  change_source VARCHAR(30) NOT NULL,
  related_order_id BIGINT NULL,
  operator_id BIGINT NULL,
  change_reason VARCHAR(500) NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  delete_state TINYINT NOT NULL DEFAULT 0,
  KEY idx_asset_status_log (asset_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE repair_order
  ADD COLUMN repair_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL' AFTER category_id,
  ADD COLUMN asset_id BIGINT NULL AFTER repair_type,
  ADD COLUMN asset_no_snapshot VARCHAR(30) NULL AFTER asset_id,
  ADD COLUMN asset_name_snapshot VARCHAR(150) NULL AFTER asset_no_snapshot,
  ADD COLUMN asset_category_snapshot VARCHAR(100) NULL AFTER asset_name_snapshot,
  ADD COLUMN asset_location_snapshot VARCHAR(1000) NULL AFTER asset_category_snapshot,
  ADD COLUMN exported_flag TINYINT NOT NULL DEFAULT 0 AFTER auto_completed_time,
  ADD COLUMN first_export_time DATETIME NULL AFTER exported_flag,
  ADD KEY idx_order_asset (asset_id, status, delete_state),
  ADD KEY idx_order_exported (exported_flag, delete_state);

CREATE TABLE repair_order_export_log (
  export_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  operator_id BIGINT NOT NULL,
  filter_snapshot TEXT NOT NULL,
  export_count INT NOT NULL,
  result_status VARCHAR(20) NOT NULL,
  file_name VARCHAR(255) NULL,
  failure_reason VARCHAR(1000) NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  delete_state TINYINT NOT NULL DEFAULT 0,
  KEY idx_export_operator_time (operator_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE repairer_suggestion (
  suggestion_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  repairer_id BIGINT NOT NULL,
  category VARCHAR(30) NOT NULL,
  title VARCHAR(200) NOT NULL,
  content VARCHAR(2000) NOT NULL,
  status VARCHAR(20) NOT NULL,
  withdrawn_flag TINYINT NOT NULL DEFAULT 0,
  admin_reply VARCHAR(2000) NULL,
  handler_id BIGINT NULL,
  handled_time DATETIME NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  delete_state TINYINT NOT NULL DEFAULT 0,
  KEY idx_suggestion_repairer (repairer_id, status, create_time),
  KEY idx_suggestion_admin (status, category, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE repair_attachment
  MODIFY COLUMN order_id BIGINT NULL,
  ADD COLUMN draft_deleted_time DATETIME NULL AFTER uploader_id,
  ADD COLUMN cleanup_due_time DATETIME NULL AFTER draft_deleted_time,
  ADD COLUMN oss_delete_status VARCHAR(20) NOT NULL DEFAULT 'NONE' AFTER cleanup_due_time,
  ADD COLUMN oss_delete_retry_count INT NOT NULL DEFAULT 0 AFTER oss_delete_status,
  ADD COLUMN oss_delete_failure_reason VARCHAR(500) NULL AFTER oss_delete_retry_count,
  ADD KEY idx_attachment_cleanup (oss_delete_status, cleanup_due_time, delete_state);

ALTER TABLE user_notification
  ADD COLUMN suggestion_id BIGINT NULL AFTER order_id,
  ADD KEY idx_notification_suggestion (suggestion_id);
