-- Participation을 Feedback으로 통합
-- 참여(예약) 단계부터 Feedback row를 생성하고, participations 테이블은 제거한다.

-- 1. 스냅샷 컬럼 제거 (tester, feedbackPost 연관관계로 대체)
ALTER TABLE feedbacks
    DROP COLUMN tester_name,
    DROP COLUMN post_title;

-- 2. 컬럼 추가 / 이름 변경
ALTER TABLE feedbacks
    ADD COLUMN participate_at TIMESTAMP(6),
    ADD COLUMN cancel_at TIMESTAMP(6);

ALTER TABLE feedbacks RENAME COLUMN processed_at TO response_at;

-- 제출 전(WRITING/CANCELED/EXPIRED)에는 제출 시각이 없다
ALTER TABLE feedbacks ALTER COLUMN submit_at DROP NOT NULL;

-- 3. 기존 제출 피드백: 참여 정보로 참여 시각/제출 기한 채우기
--    expire_at 의미 변경: 제출 시각 + 24h -> 참여 시각 + 24h
UPDATE feedbacks f
SET participate_at = p.reserved_at,
    expire_at      = p.submission_deadline_at
FROM participations p
WHERE p.feedback_post_id = f.feedback_post_id
  AND p.tester_id = f.tester_id;

-- 참여 정보가 없는 피드백은 제출 시각으로 대체
UPDATE feedbacks
SET participate_at = submit_at,
    expire_at      = submit_at + INTERVAL '24 hours'
WHERE participate_at IS NULL;

-- 4. 제출 전 참여(예약/포기/만료)를 Feedback row로 이관
INSERT INTO feedbacks (
    feedback_id,
    feedback_post_id,
    tester_id,
    status,
    participate_at,
    expire_at,
    cancel_at,
    created_at,
    updated_at
)
SELECT gen_random_uuid(),
       p.feedback_post_id,
       p.tester_id,
       CASE p.status
           WHEN 'RESERVED'  THEN 'WRITING'
           WHEN 'ABANDONED' THEN 'CANCELED'
           WHEN 'EXPIRED'   THEN 'EXPIRED'
       END,
       p.reserved_at,
       p.submission_deadline_at,
       p.abandoned_at,
       p.created_at,
       p.updated_at
FROM participations p
WHERE p.status IN ('RESERVED', 'ABANDONED', 'EXPIRED')
  AND NOT EXISTS (
      SELECT 1
      FROM feedbacks f
      WHERE f.feedback_post_id = p.feedback_post_id
        AND f.tester_id = p.tester_id
  );

ALTER TABLE feedbacks ALTER COLUMN participate_at SET NOT NULL;

-- 5. 제약조건
ALTER TABLE feedbacks
    ADD CONSTRAINT uk_feedback_post_tester UNIQUE (feedback_post_id, tester_id),
    ADD CONSTRAINT fk_feedbacks_feedback_post FOREIGN KEY (feedback_post_id) REFERENCES feedback_posts (feedback_post_id),
    ADD CONSTRAINT fk_feedbacks_tester FOREIGN KEY (tester_id) REFERENCES member (id);

-- 6. 인덱스
CREATE INDEX idx_feedback_tester ON feedbacks (tester_id);
CREATE INDEX idx_feedback_status_expire ON feedbacks (status, expire_at);  -- 만료 스케줄러용

-- 7. participations 제거
DROP TABLE participations;
