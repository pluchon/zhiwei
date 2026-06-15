-- 工单 AI 关联推荐生成标记
ALTER TABLE repair_order
    ADD COLUMN ai_link_generated TINYINT NOT NULL DEFAULT 0 COMMENT '是否已生成 AI 关联推荐（0否 1是）' AFTER duplicate_reason;

-- 复核重复报修候选查询索引
CREATE INDEX idx_repair_order_dup_candidate
    ON repair_order (delete_state, category_id, status, create_time, campus_id, building_id, asset_id);
