-- 피드백 모집글 / 프로젝트 게시글 공용 댓글
-- target_type + target_id 로 게시글을 다형 참조한다 (FK 없음).
-- 작성자 회원이 물리 삭제되면 댓글은 남기고 writer_id 만 NULL 로 만든다.

CREATE TABLE comments (
    comment_id  UUID PRIMARY KEY,
    target_type VARCHAR(20)   NOT NULL,
    target_id   UUID          NOT NULL,
    writer_id   UUID,
    parent_id   UUID,
    root_id     UUID,
    content     VARCHAR(1000) NOT NULL,
    status      VARCHAR(20)   NOT NULL,
    like_count  INTEGER       NOT NULL DEFAULT 0,
    edited_at   TIMESTAMP(6),
    created_at  TIMESTAMP(6),
    updated_at  TIMESTAMP(6),
    CONSTRAINT fk_comments_writer FOREIGN KEY (writer_id) REFERENCES member (id) ON DELETE SET NULL
);

-- 게시글의 루트 댓글 목록 조회
CREATE INDEX idx_comment_target_root ON comments (target_type, target_id, root_id, created_at);
-- 루트 댓글별 답글 목록 커서 조회 (created_at, comment_id)
CREATE INDEX idx_comment_root_created ON comments (root_id, created_at, comment_id);
-- 회원 물리 삭제 시 ON DELETE SET NULL 처리용
CREATE INDEX idx_comment_writer ON comments (writer_id);


-- 댓글 좋아요 기록 (개수는 comments.like_count 캐시 사용)
-- member_id 는 FK 를 두지 않는다: 회원 삭제 시 기록이 지워지면 like_count 와 어긋나기 때문.
-- 댓글이 물리 삭제되면 좋아요 기록도 함께 삭제한다.
CREATE TABLE comment_likes (
    comment_like_id UUID PRIMARY KEY,
    comment_id      UUID NOT NULL,
    member_id       UUID NOT NULL,
    created_at      TIMESTAMP(6),
    CONSTRAINT uk_comment_like_comment_member UNIQUE (comment_id, member_id),
    CONSTRAINT fk_comment_likes_comment FOREIGN KEY (comment_id) REFERENCES comments (comment_id) ON DELETE CASCADE
);
