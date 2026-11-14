ALTER TABLE feedbacks ADD COLUMN response_dead_line_at TIMESTAMP(6);

UPDATE feedbacks
SET response_dead_line_at = submit_at + INTERVAL '72 hours'
WHERE response_dead_line_at IS NULL;
