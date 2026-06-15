CREATE TABLE sys_role (
  role_id BIGINT AUTO_INCREMENT PRIMARY KEY, role_name VARCHAR(50) NOT NULL, status TINYINT NOT NULL DEFAULT 0,
  remark VARCHAR(255), create_time DATETIME NOT NULL, update_time DATETIME NOT NULL, delete_state TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_role_name (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_user (
  user_id BIGINT AUTO_INCREMENT PRIMARY KEY, user_no VARCHAR(30) NOT NULL, real_name VARCHAR(50) NOT NULL, nick_name VARCHAR(50) NOT NULL,
  role_id BIGINT NOT NULL, email VARCHAR(100), phone_number VARCHAR(20), parent_phone VARCHAR(20), avatar VARCHAR(500),
  password VARCHAR(100) NOT NULL, activation_status TINYINT NOT NULL DEFAULT 0, account_status TINYINT NOT NULL DEFAULT 0,
  security_stamp VARCHAR(64) NOT NULL, phone_confirm_required TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL, update_time DATETIME NOT NULL, delete_state TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_no (user_no), UNIQUE KEY uk_user_phone (phone_number), UNIQUE KEY uk_user_email (email), KEY idx_user_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_dict_type (
  dict_type_id BIGINT AUTO_INCREMENT PRIMARY KEY, dict_name VARCHAR(100) NOT NULL, dict_type VARCHAR(100) NOT NULL, status TINYINT NOT NULL DEFAULT 0,
  remark VARCHAR(255), create_time DATETIME NOT NULL, update_time DATETIME NOT NULL, delete_state TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_dict_data (
  dict_data_id BIGINT AUTO_INCREMENT PRIMARY KEY, dict_type VARCHAR(100) NOT NULL, dict_label VARCHAR(100) NOT NULL, dict_value VARCHAR(100) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0, status TINYINT NOT NULL DEFAULT 0, remark VARCHAR(255),
  create_time DATETIME NOT NULL, update_time DATETIME NOT NULL, delete_state TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_dict_data (dict_type, dict_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_login_log (
  login_log_id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, login_identifier VARCHAR(50), login_type VARCHAR(20) NOT NULL,
  status TINYINT NOT NULL, message VARCHAR(255), login_ip VARCHAR(128),
  create_time DATETIME NOT NULL, update_time DATETIME NOT NULL, delete_state TINYINT NOT NULL DEFAULT 0,
  KEY idx_login_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_operation_log (
  operation_log_id BIGINT AUTO_INCREMENT PRIMARY KEY, operator_id BIGINT NOT NULL, operation_type VARCHAR(50) NOT NULL, target_type VARCHAR(50) NOT NULL,
  target_id BIGINT, description VARCHAR(500), operation_ip VARCHAR(128),
  create_time DATETIME NOT NULL, update_time DATETIME NOT NULL, delete_state TINYINT NOT NULL DEFAULT 0,
  KEY idx_operation_user_time (operator_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE repair_category (
  category_id BIGINT AUTO_INCREMENT PRIMARY KEY, category_name VARCHAR(50) NOT NULL, description VARCHAR(255), status TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL, update_time DATETIME NOT NULL, delete_state TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_category_name (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE repairer_category_capability (
  capability_id BIGINT AUTO_INCREMENT PRIMARY KEY, repairer_id BIGINT NOT NULL, category_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL, update_time DATETIME NOT NULL, delete_state TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_repairer_category (repairer_id, category_id), KEY idx_capability_category (category_id, repairer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE repair_order (
  order_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_no VARCHAR(32) NOT NULL, request_id VARCHAR(64) NOT NULL, reporter_id BIGINT NOT NULL,
  reporter_role_id BIGINT NOT NULL, reporter_nickname VARCHAR(50) NOT NULL, reporter_avatar VARCHAR(500), title VARCHAR(100) NOT NULL,
  description TEXT NOT NULL, category_id BIGINT NOT NULL, campus VARCHAR(100) NOT NULL, building VARCHAR(100), floor VARCHAR(50),
  location_detail VARCHAR(500) NOT NULL, contact_phone VARCHAR(20) NOT NULL, status TINYINT NOT NULL,
  current_repairer_id BIGINT, unresolved_count INT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL, update_time DATETIME NOT NULL, delete_state TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_order_no (order_no), UNIQUE KEY uk_order_request (request_id), KEY idx_order_reporter (reporter_id, create_time),
  KEY idx_order_repairer_status (current_repairer_id, status), KEY idx_order_status_time (status, create_time),
  KEY idx_order_category (category_id), KEY idx_order_location (campus, building)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE repair_assignment (
  assignment_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, repairer_id BIGINT NOT NULL, status TINYINT NOT NULL, return_reason VARCHAR(500),
  create_time DATETIME NOT NULL, update_time DATETIME NOT NULL, delete_state TINYINT NOT NULL DEFAULT 0,
  KEY idx_assignment_order (order_id, create_time), KEY idx_assignment_repairer (repairer_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE repair_record (
  record_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, repairer_id BIGINT NOT NULL, result_description VARCHAR(1000), attempt_no INT NOT NULL,
  create_time DATETIME NOT NULL, update_time DATETIME NOT NULL, delete_state TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_record_attempt (order_id, attempt_no), KEY idx_record_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE repair_attachment (
  attachment_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, record_id BIGINT, object_key VARCHAR(500) NOT NULL, uploader_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL, update_time DATETIME NOT NULL, delete_state TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_attachment_object (object_key), KEY idx_attachment_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE repair_order_comment (
  comment_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, author_id BIGINT, comment_type TINYINT NOT NULL, content VARCHAR(1000) NOT NULL,
  is_pinned TINYINT NOT NULL DEFAULT 0, is_withdrawn TINYINT NOT NULL DEFAULT 0, withdraw_time DATETIME,
  create_time DATETIME NOT NULL, update_time DATETIME NOT NULL, delete_state TINYINT NOT NULL DEFAULT 0,
  KEY idx_comment_order_time (order_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE repair_order_log (
  log_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, operator_id BIGINT, action TINYINT NOT NULL, from_status TINYINT, to_status TINYINT,
  remark VARCHAR(1000), create_time DATETIME NOT NULL, update_time DATETIME NOT NULL, delete_state TINYINT NOT NULL DEFAULT 0,
  KEY idx_order_log_time (order_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE repair_evaluation (
  evaluation_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, repairer_id BIGINT NOT NULL, star TINYINT NOT NULL, content VARCHAR(1000),
  follow_up_content VARCHAR(1000), follow_up_time DATETIME,
  create_time DATETIME NOT NULL, update_time DATETIME NOT NULL, delete_state TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_evaluation_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_notification (
  notification_id BIGINT AUTO_INCREMENT PRIMARY KEY, receiver_id BIGINT NOT NULL, order_id BIGINT, notification_type TINYINT NOT NULL,
  title VARCHAR(100) NOT NULL, content VARCHAR(1000) NOT NULL, is_read TINYINT NOT NULL DEFAULT 0, read_time DATETIME,
  create_time DATETIME NOT NULL, update_time DATETIME NOT NULL, delete_state TINYINT NOT NULL DEFAULT 0,
  KEY idx_notification_receiver (receiver_id, is_read, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
