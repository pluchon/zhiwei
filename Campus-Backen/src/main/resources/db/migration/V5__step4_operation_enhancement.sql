ALTER TABLE asset
  ADD COLUMN purchase_date DATE NULL AFTER enabled_date;

CREATE TABLE asset_import_batch (
  batch_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  file_name VARCHAR(255) NOT NULL,
  operator_id BIGINT NOT NULL,
  total_count INT NOT NULL DEFAULT 0,
  pending_count INT NOT NULL DEFAULT 0,
  confirmed_count INT NOT NULL DEFAULT 0,
  ignored_count INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  delete_state TINYINT NOT NULL DEFAULT 0,
  KEY idx_import_batch_operator (operator_id, create_time),
  KEY idx_import_batch_active (delete_state, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE asset_import_item (
  item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_id BIGINT NOT NULL,
  `row_number` INT NOT NULL,
  asset_name VARCHAR(150) NULL,
  category_text VARCHAR(100) NULL,
  asset_category_id BIGINT NULL,
  purchase_date DATE NULL,
  location_text VARCHAR(500) NULL,
  campus_id BIGINT NULL,
  building_id BIGINT NULL,
  floor VARCHAR(50) NULL,
  room VARCHAR(100) NULL,
  location_detail VARCHAR(500) NULL,
  status VARCHAR(20) NOT NULL,
  duplicate_hint VARCHAR(500) NULL,
  failure_reason VARCHAR(1000) NULL,
  confirmed_asset_id BIGINT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  delete_state TINYINT NOT NULL DEFAULT 0,
  KEY idx_import_item_batch_status (batch_id, status, delete_state),
  KEY idx_import_item_confirmed_asset (confirmed_asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE sys_user
  ADD COLUMN accepting_state VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' AFTER phone_confirm_required,
  ADD COLUMN pause_reason VARCHAR(500) NULL AFTER accepting_state,
  ADD COLUMN expected_resume_time DATETIME NULL AFTER pause_reason,
  ADD KEY idx_repairer_accepting (accepting_state, delete_state);

CREATE TABLE manual_account_recovery (
  recovery_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  target_user_id BIGINT NOT NULL,
  new_phone VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL,
  applicant_admin_id BIGINT NOT NULL,
  reviewer_admin_id BIGINT NULL,
  identity_check_note VARCHAR(1000) NOT NULL,
  review_note VARCHAR(1000) NULL,
  approved_time DATETIME NULL,
  expire_time DATETIME NULL,
  completed_time DATETIME NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  delete_state TINYINT NOT NULL DEFAULT 0,
  active_target_user_id BIGINT GENERATED ALWAYS AS (
    CASE
      WHEN delete_state = 0 AND status IN ('PENDING', 'APPROVED')
      THEN target_user_id
      ELSE NULL
    END
  ) STORED,
  UNIQUE KEY uk_recovery_active_target (active_target_user_id),
  KEY idx_recovery_target_status (target_user_id, status, delete_state),
  KEY idx_recovery_expire (status, expire_time, delete_state),
  KEY idx_recovery_applicant (applicant_admin_id, create_time),
  KEY idx_recovery_reviewer (reviewer_admin_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE management_statistics_export_log (
  export_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  operator_id BIGINT NOT NULL,
  range_type VARCHAR(20) NOT NULL,
  result_status VARCHAR(20) NOT NULL,
  file_name VARCHAR(255) NULL,
  failure_reason VARCHAR(1000) NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  delete_state TINYINT NOT NULL DEFAULT 0,
  KEY idx_statistics_export_operator (operator_id, create_time),
  KEY idx_statistics_export_result (result_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
