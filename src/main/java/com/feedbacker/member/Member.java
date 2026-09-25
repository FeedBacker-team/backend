package com.feedbacker.member;

import com.feedbacker.feedback.domain.AcornHistory;
import com.feedbacker.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    private AuthProvider authProvider;

    @Column(name = "profile_image_url", length = 255)
    private String profileImageUrl;

    @Column(name = "interest_field", length = 50)
    private String interestField;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AcornWallet acornWallet;

    @OneToMany(mappedBy = "member", cascade = CascadeType.REMOVE)
    private List<AcornHistory> acornHistories = new ArrayList<>();

    @Builder
    public Member(String email, String password, String nickname, Role role, AuthProvider authProvider, String profileImageUrl, String interestField) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role != null ? role : Role.OTHER;
        this.authProvider = authProvider != null ? authProvider : AuthProvider.EMAIL;
        this.profileImageUrl = profileImageUrl;
        this.interestField = interestField;
    }

    public void assignWallet(AcornWallet wallet) {
        this.acornWallet = wallet;
    }

    public void setInitialProfile(String nickname, Role role) {
        this.nickname = nickname;
        this.role = role;
    }

    public void updateProfile(String nickname, String profileImageUrl, String interestField) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
        if (interestField != null) {
            this.interestField = interestField;
        }
    }
}