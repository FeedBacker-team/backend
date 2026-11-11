package com.feedbacker.comment.domain.type;

public enum CommentStatus {
    ACTIVE,   // 정상
    DELETED,  // 작성자가 삭제 (답글이 남아 있어 소프트 삭제된 상태)
    HIDDEN    // 관리자 조치로 숨김
}
