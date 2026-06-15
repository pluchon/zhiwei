ALTER TABLE ai_assistant_session
  ADD COLUMN title VARCHAR(100) NULL AFTER scene_type;

ALTER TABLE ai_assistant_message
  ADD COLUMN extra_json TEXT NULL AFTER content_summary;
