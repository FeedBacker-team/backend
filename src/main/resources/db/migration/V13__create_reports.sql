CREATE TABLE reports (
    report_id UUID PRIMARY KEY,
    reporter_id UUID NOT NULL,
    suspect_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    target_id UUID NOT NULL,
    reasons JSONB NOT NULL,
    detail_reasons JSONB,
    is_processed BOOLEAN NOT NULL DEFAULT FALSE,
    actions JSONB,
    processed_by UUID,
    admin_memo VARCHAR(1000),
    processed_at TIMESTAMP(6),
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT uk_reports_reporter_type_target UNIQUE (reporter_id, type, target_id)
);

CREATE INDEX idx_reports_processed_created_at ON reports (is_processed, created_at DESC);
CREATE INDEX idx_reports_suspect_id ON reports (suspect_id);

ALTER TABLE member
    ADD COLUMN banned_until TIMESTAMP(6);

ALTER TABLE feedback_posts
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
