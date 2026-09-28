package com.feedbacker.member;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/** 보유 도토리 (회원 1명당 1개, PK = 회원 ID) */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "acorn_wallet")
public class AcornWallet {

    @Id
    @Column(name = "member_id", columnDefinition = "uuid")
    private UUID id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private int balance;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private AcornWallet(Member member, int balance) {
        this.member = member;
        this.balance = balance;
    }

    public static AcornWallet create(Member member, int initialBalance) {
        if (initialBalance < 0) {
            throw new IllegalArgumentException("초기 도토리는 0 이상이어야 합니다.");
        }
        return new AcornWallet(member, initialBalance);
    }

    /** 도토리 적립 */
    public void deposit(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("적립할 도토리는 1개 이상이어야 합니다.");
        }
        this.balance += amount;
    }

    /** 도토리 차감 */
    public void withdraw(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("차감할 도토리는 1개 이상이어야 합니다.");
        }
        if (this.balance < amount) {
            throw new IllegalStateException("보유 도토리가 부족합니다.");
        }
        this.balance -= amount;
    }
}
