package com.flagship.Wallet_Service.repo;

import com.flagship.Wallet_Service.entities.Wallet;
import jakarta.persistence.LockModeType;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<@NonNull Wallet,@NonNull UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Wallet> findByCustomerIdAndCurrency(
            UUID customerId,
            String currency
    );
}
