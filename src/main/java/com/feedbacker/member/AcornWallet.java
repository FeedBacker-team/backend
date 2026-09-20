package com.feedbacker.member;

import com.feedbacker.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "acorn_wallet")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AcornWallet extends BaseTimeEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false)
    private UUID id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id")
    private Member member;

    @Column(name = "balance", nullable = false)
    private int balance;

    public AcornWallet(Member member, int initialBalance) {
        this.member = member;
        this.balance = initialBalance;
    }

    // 도토리 충전/보상 지급
    public void deposit(int amount) {
        this.balance += amount;
    }

    // 도토리 차감/예치
    public void withdraw(int amount) {
        if (this.balance < amount) {
            throw new IllegalStateException("보유 도토리가 부족합니다.");
        }
        this.balance -= amount;
    }
}