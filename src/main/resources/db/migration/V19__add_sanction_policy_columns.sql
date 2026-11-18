-- 제재 횟수 계산용: 신고 당한 회원을 제재 기록에 직접 저장
ALTER TABLE report_sanctions
    ADD COLUMN suspect_id UUID;

UPDATE report_sanctions s
SET suspect_id = r.suspect_id
FROM reports r
WHERE s.report_id = r.report_id;

ALTER TABLE report_sanctions
    ALTER COLUMN suspect_id SET NOT NULL;

-- 영구 정지 여부, 영구 정지 원복 시 되돌릴 정지 종료 시각
ALTER TABLE report_sanctions
    ADD COLUMN permanent BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN banned_until_before TIMESTAMP(6);

-- 피드백 모집글 강제 삭제 시 소멸시킨 예치 도토리 (원복 시 다시 예치 상태로 되돌림)
ALTER TABLE report_sanctions
    ADD COLUMN acorn_forfeited INTEGER;

CREATE INDEX idx_report_sanctions_suspect_created_at ON report_sanctions (suspect_id, created_at);

-- 소프트 삭제 시각 (30일 후 물리 삭제 기준)
ALTER TABLE project
    ADD COLUMN deleted_at TIMESTAMP(6);

ALTER TABLE feedback_posts
    ADD COLUMN deleted_at TIMESTAMP(6);
