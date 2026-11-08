package com.feedbacker.report.domain.type;

import java.util.EnumSet;
import java.util.Set;

import static com.feedbacker.report.domain.type.ReportReason.*;

public enum ReportType {
    MEMBER(EnumSet.of(                       // 회원
            ABUSIVE_LANGUAGE, HARASSMENT, SPAM, IMPERSONATION,
            SEXUAL_CONTENT, FEEDBACK_ABUSE, FRAUD, OTHER)),
    PROJECT_POST(EnumSet.of(                 // 프로젝트 게시글
            SPAM, ABUSIVE_LANGUAGE, SEXUAL_CONTENT, PRIVACY_VIOLATION,
            COPYRIGHT_INFRINGEMENT, FRAUD, OFF_TOPIC, OTHER)),
    FEEDBACK_POST(EnumSet.of(                // 피드백 모집 게시글
            SPAM, ABUSIVE_LANGUAGE, SEXUAL_CONTENT, PRIVACY_VIOLATION,
            COPYRIGHT_INFRINGEMENT, FRAUD, OFF_TOPIC, OTHER)),
    COMMENT(EnumSet.of(                      // 댓글
            ABUSIVE_LANGUAGE, HARASSMENT, SPAM, SEXUAL_CONTENT,
            PRIVACY_VIOLATION, OFF_TOPIC, MALICIOUS_CRITICISM, OTHER));

    private final Set<ReportReason> allowedReasons;

    ReportType(Set<ReportReason> allowedReasons) {
        this.allowedReasons = allowedReasons;
    }

    public boolean allows(ReportReason reason) {
        return allowedReasons.contains(reason);
    }

    public Set<ReportReason> getAllowedReasons() {
        return EnumSet.copyOf(allowedReasons);
    }
}
