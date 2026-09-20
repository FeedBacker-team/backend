package com.feedbacker.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AcornWalletRepository extends JpaRepository<AcornWallet, UUID> {
}