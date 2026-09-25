package com.feedbacker.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AcornWalletRepository extends JpaRepository<AcornWallet, UUID> {
}
