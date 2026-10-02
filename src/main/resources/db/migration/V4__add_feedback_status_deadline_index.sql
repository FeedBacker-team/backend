CREATE INDEX IF NOT EXISTS idx_feedback_status_deadline
    ON feedbacks (status, response_dead_line_at);
