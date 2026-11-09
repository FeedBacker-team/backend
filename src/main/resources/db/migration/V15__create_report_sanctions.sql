CREATE TABLE report_sanctions (
    report_sanction_id UUID PRIMARY KEY,
    report_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,
    humidity_decreased NUMERIC(5, 2),
    ban_days INTEGER,
    ban_end_at TIMESTAMP(6),
    processed_by UUID NOT NULL,
    reverted_at TIMESTAMP(6),
    reverted_by UUID,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT fk_report_sanctions_report FOREIGN KEY (report_id) REFERENCES reports (report_id)
);

CREATE INDEX idx_report_sanctions_report_id ON report_sanctions (report_id);
