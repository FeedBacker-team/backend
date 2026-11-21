-- 회원/피드백 모집글이 삭제되어도 피드백 기록은 남기고 FK만 NULL 처리한다
ALTER TABLE feedbacks
    ALTER COLUMN tester_id DROP NOT NULL,
    ALTER COLUMN feedback_post_id DROP NOT NULL;

ALTER TABLE feedbacks
    DROP CONSTRAINT fk_feedbacks_tester,
    DROP CONSTRAINT fk_feedbacks_feedback_post;

ALTER TABLE feedbacks
    ADD CONSTRAINT fk_feedbacks_tester
        FOREIGN KEY (tester_id) REFERENCES member (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_feedbacks_feedback_post
        FOREIGN KEY (feedback_post_id) REFERENCES feedback_posts (feedback_post_id) ON DELETE SET NULL;
