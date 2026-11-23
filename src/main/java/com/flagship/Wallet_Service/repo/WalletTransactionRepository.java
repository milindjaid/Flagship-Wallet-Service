package com.flagship.Wallet_Service.repo;

import com.flagship.Wallet_Service.entities.Direction;
import com.flagship.Wallet_Service.entities.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository
        extends JpaRepository<WalletTransaction, UUID> {

    boolean existsByPaymentIdAndDirection(
            UUID paymentId,
            Direction direction
    );

    Optional<WalletTransaction> findByPaymentIdAndDirection(UUID paymentId, Direction direction);
}

