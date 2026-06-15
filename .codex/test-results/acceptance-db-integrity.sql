USE campus_system;

SELECT 'users_without_role' check_name, COUNT(*) issue_count
FROM sys_user u LEFT JOIN sys_role r ON r.role_id = u.role_id
WHERE r.role_id IS NULL
UNION ALL
SELECT 'orders_without_reporter', COUNT(*)
FROM repair_order o LEFT JOIN sys_user u ON u.user_id = o.reporter_id
WHERE u.user_id IS NULL
UNION ALL
SELECT 'orders_without_category', COUNT(*)
FROM repair_order o LEFT JOIN repair_category c ON c.category_id = o.category_id
WHERE c.category_id IS NULL
UNION ALL
SELECT 'assignment_without_order', COUNT(*)
FROM repair_assignment x LEFT JOIN repair_order o ON o.order_id = x.order_id
WHERE o.order_id IS NULL
UNION ALL
SELECT 'assignment_without_repairer', COUNT(*)
FROM repair_assignment x LEFT JOIN sys_user u ON u.user_id = x.repairer_id
WHERE u.user_id IS NULL
UNION ALL
SELECT 'record_without_order', COUNT(*)
FROM repair_record x LEFT JOIN repair_order o ON o.order_id = x.order_id
WHERE o.order_id IS NULL
UNION ALL
SELECT 'record_without_repairer', COUNT(*)
FROM repair_record x LEFT JOIN sys_user u ON u.user_id = x.repairer_id
WHERE u.user_id IS NULL
UNION ALL
SELECT 'log_without_order', COUNT(*)
FROM repair_order_log x LEFT JOIN repair_order o ON o.order_id = x.order_id
WHERE o.order_id IS NULL
UNION ALL
SELECT 'evaluation_without_order', COUNT(*)
FROM repair_evaluation x LEFT JOIN repair_order o ON o.order_id = x.order_id
WHERE o.order_id IS NULL
UNION ALL
SELECT 'attachment_invalid_order', COUNT(*)
FROM repair_attachment x LEFT JOIN repair_order o ON o.order_id = x.order_id
WHERE x.order_id IS NOT NULL AND o.order_id IS NULL
UNION ALL
SELECT 'attachment_invalid_record', COUNT(*)
FROM repair_attachment x LEFT JOIN repair_record r ON r.record_id = x.record_id
WHERE x.record_id IS NOT NULL AND r.record_id IS NULL
UNION ALL
SELECT 'notification_without_receiver', COUNT(*)
FROM user_notification n LEFT JOIN sys_user u ON u.user_id = n.receiver_id
WHERE u.user_id IS NULL
UNION ALL
SELECT 'notification_invalid_order', COUNT(*)
FROM user_notification n LEFT JOIN repair_order o ON o.order_id = n.order_id
WHERE n.order_id IS NOT NULL AND o.order_id IS NULL
UNION ALL
SELECT 'active_order_missing_repairer', COUNT(*)
FROM repair_order
WHERE status IN (3, 4, 5) AND current_repairer_id IS NULL
UNION ALL
SELECT 'completed_order_missing_record', COUNT(*)
FROM repair_order o
LEFT JOIN repair_record r ON r.order_id = o.order_id AND r.delete_state = 0
WHERE o.status IN (5, 6) AND r.record_id IS NULL
UNION ALL
SELECT 'order_missing_log', COUNT(*)
FROM repair_order o
LEFT JOIN repair_order_log l ON l.order_id = o.order_id AND l.delete_state = 0
WHERE o.delete_state = 0 AND l.log_id IS NULL
UNION ALL
SELECT 'unexpected_role', COUNT(*)
FROM sys_role
WHERE role_name NOT IN ('STUDENT', 'TEACHER', 'REPAIRER', 'ADMIN')
   OR delete_state <> 0;
