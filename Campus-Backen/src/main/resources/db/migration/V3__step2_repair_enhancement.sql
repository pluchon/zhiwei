CREATE TABLE campus (
  campus_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  campus_name VARCHAR(100) NOT NULL,
  normalized_name VARCHAR(100) NOT NULL,
  name_sort_key VARCHAR(300) NOT NULL,
  description VARCHAR(500),
  status TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  delete_state TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_campus_normalized_name (normalized_name),
  KEY idx_campus_options (delete_state, status, name_sort_key, campus_name, campus_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE building (
  building_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  campus_id BIGINT NOT NULL,
  building_name VARCHAR(100) NOT NULL,
  normalized_name VARCHAR(100) NOT NULL,
  name_sort_key VARCHAR(300) NOT NULL,
  description VARCHAR(500),
  status TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  delete_state TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_building_campus_name (campus_id, normalized_name),
  KEY idx_building_options (campus_id, delete_state, status, name_sort_key, building_name, building_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE repair_order
  ADD COLUMN campus_id BIGINT NULL AFTER category_id,
  ADD COLUMN campus_description_snapshot VARCHAR(500) NULL AFTER campus,
  ADD COLUMN building_id BIGINT NULL AFTER campus_description_snapshot,
  ADD COLUMN building_description_snapshot VARCHAR(500) NULL AFTER building,
  ADD COLUMN room VARCHAR(100) NULL AFTER floor,
  ADD COLUMN completion_time DATETIME NULL,
  ADD COLUMN auto_completed_time DATETIME NULL,
  ADD KEY idx_order_location_id (campus_id, building_id),
  ADD KEY idx_order_completion_time (completion_time, delete_state),
  ADD KEY idx_order_draft_cleanup (status, delete_state, update_time);

ALTER TABLE repair_assignment
  ADD COLUMN assignment_source TINYINT NOT NULL DEFAULT 0 AFTER status,
  ADD COLUMN operator_id BIGINT NULL AFTER assignment_source,
  ADD COLUMN dispatch_note VARCHAR(1000) NULL AFTER operator_id,
  ADD COLUMN capability_mismatch_reason VARCHAR(1000) NULL AFTER dispatch_note;

CREATE TABLE repair_work_cycle (
  work_cycle_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  repairer_id BIGINT NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NULL,
  active_flag TINYINT NULL,
  three_day_reminded TINYINT NOT NULL DEFAULT 0,
  seven_day_reminded TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  delete_state TINYINT NOT NULL DEFAULT 0,
  KEY idx_work_cycle_order (order_id, end_time),
  KEY idx_work_cycle_three_day (end_time, three_day_reminded, start_time),
  KEY idx_work_cycle_seven_day (end_time, seven_day_reminded, start_time),
  UNIQUE KEY uk_work_cycle_active (order_id, active_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE repair_confirmation_cycle (
  confirmation_cycle_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  reporter_id BIGINT NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NULL,
  active_flag TINYINT NULL,
  three_day_reminded TINYINT NOT NULL DEFAULT 0,
  seven_day_reminded TINYINT NOT NULL DEFAULT 0,
  twenty_seven_day_reminded TINYINT NOT NULL DEFAULT 0,
  auto_completed TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  delete_state TINYINT NOT NULL DEFAULT 0,
  KEY idx_confirmation_order (order_id, end_time),
  UNIQUE KEY uk_confirmation_active (order_id, active_flag),
  KEY idx_confirmation_reminder (
    end_time,
    three_day_reminded,
    seven_day_reminded,
    twenty_seven_day_reminded,
    auto_completed,
    start_time
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE user_notification
  ADD KEY idx_notification_filter (receiver_id, is_read, notification_type, create_time);

ALTER TABLE sys_user
  ADD KEY idx_user_real_name (real_name);

ALTER TABLE sys_operation_log
  MODIFY COLUMN operator_id BIGINT NULL;
