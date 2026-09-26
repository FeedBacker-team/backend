package com.feedbacker.member;

import com.feedbacker.feedback.domain.AcornHistory;
import com.feedbacker.global.common.BaseTimeEntity;
import com.feedbacker.project.domain.ProjectTag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_member_provider_provider_id", columnNames = {"provider", "provider_id"}))
public class Member extends BaseTimeEntity {

    public static final BigDecimal DEFAULT_HUMIDITY = new BigDecimal("30.00");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    // 카카오 가입자는 이메일 동의를 안 하면 null
    @Column(unique = true)
    private String email;

    // 카카오 가입자는 null
    private String password;

    @Column(unique = true, length = 10)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    // 카카오 회원번호 (이메일 가입자는 null)
    @Column(name = "provider_id")
    private String providerId;

    @Column(length = 512)
    private String profileImage;

    @Column(length = 512)
    private String portfolioLink;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal humidity = DEFAULT_HUMIDITY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TreeGrade grade = TreeGrade.NORMAL;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AcornWallet acornWallet;

    @OneToMany(mappedBy = "member", cascade = CascadeType.REMOVE)
    private List<AcornHistory> acornHistories = new ArrayList<>();

    // 관심 분야 (0~5개)
    @ElementCollection
    @CollectionTable(name = "member_interest", joinColumns = @JoinColumn(name = "member_id"))
    @OrderColumn(name = "interest_order")
    @Enumerated(EnumType.STRING)
    @Column(name = "interest", nullable = false, length = 20)
    private List<ProjectTag> interests = new ArrayList<>();

    private LocalDateTime deactivatedAt;

    private Member(String email, String password, AuthProvider provider, String providerId) {
        this.email = email;
        this.password = password;
        this.provider = provider;
        this.providerId = providerId;
        this.humidity = DEFAULT_HUMIDITY;
        this.grade = TreeGrade.fromHumidity(DEFAULT_HUMIDITY);
    }

    /** 이메일 회원가입 (password는 암호화된 값) */
    public static Member createEmailMember(String email, String encodedPassword) {
        return new Member(email, encodedPassword, AuthProvider.EMAIL, null);
    }

    /** 카카오 회원가입 (email은 null 가능) */
    public static Member createKakaoMember(String kakaoId, String email) {
        return new Member(email, null, AuthProvider.KAKAO, kakaoId);
    }

    /** 4-2 기본 프로필 설정 */
    public void updateProfile(String profileImage, String nickname, Role role,
                              String portfolioLink, List<ProjectTag> interests) {
        this.profileImage = profileImage;
        this.nickname = nickname;
        this.role = role;
        this.portfolioLink = portfolioLink;
        this.interests.clear();
        if (interests != null) {
            this.interests.addAll(interests);
        }
    }

    /** 습도 변경 시 나무 등급도 같이 갱신 */
    public void changeHumidity(BigDecimal humidity) {
        this.humidity = humidity;
        this.grade = TreeGrade.fromHumidity(humidity);
    }

    /** 필수 프로필(닉네임, 직군)을 입력했는지 */
    public boolean isProfileCompleted() {
        return nickname != null && role != null;
    }

    public List<ProjectTag> getInterests() {
        return List.copyOf(interests);
    }

}