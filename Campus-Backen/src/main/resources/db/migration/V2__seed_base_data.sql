INSERT INTO sys_role VALUES
(1, 'STUDENT', 0, '学生', NOW(), NOW(), 0),
(2, 'TEACHER', 0, '教师', NOW(), NOW(), 0),
(3, 'REPAIRER', 0, '维修师傅', NOW(), NOW(), 0),
(4, 'ADMIN', 0, '管理员', NOW(), NOW(), 0);

INSERT INTO sys_dict_type VALUES
(1, '维修退回原因', 'repair_return_reason', 0, NULL, NOW(), NOW(), 0),
(2, '报修驳回原因', 'repair_reject_reason', 0, NULL, NOW(), NOW(), 0),
(3, '工单关闭原因', 'repair_close_reason', 0, NULL, NOW(), NOW(), 0);

INSERT INTO sys_dict_data VALUES
(1, 'repair_return_reason', '能力不匹配', 'CAPABILITY_MISMATCH', 1, 0, NULL, NOW(), NOW(), 0),
(2, 'repair_return_reason', '其他', 'OTHER', 99, 0, NULL, NOW(), NOW(), 0),
(3, 'repair_reject_reason', '无效报修', 'INVALID', 1, 0, NULL, NOW(), NOW(), 0),
(4, 'repair_reject_reason', '其他', 'OTHER', 99, 0, NULL, NOW(), NOW(), 0),
(5, 'repair_close_reason', '重复且无需处理', 'DUPLICATE', 1, 0, NULL, NOW(), NOW(), 0),
(6, 'repair_close_reason', '其他', 'OTHER', 99, 0, NULL, NOW(), NOW(), 0);

INSERT INTO repair_category VALUES
(1, '水电维修', '照明、供水与供电故障', 0, NOW(), NOW(), 0),
(2, '门窗维修', '门锁、门窗及玻璃故障', 0, NOW(), NOW(), 0),
(3, '网络故障', '校园网络与信息点故障', 0, NOW(), NOW(), 0);
