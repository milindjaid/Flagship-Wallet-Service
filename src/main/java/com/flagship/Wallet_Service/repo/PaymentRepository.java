package com.flagship.Wallet_Service.repo;

import com.flagship.Wallet_Service.entities.Payment;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<@NonNull Payment,@NonNull UUID> {

//    @Override
//    Optional<@NonNull Payment> findById(@NonNull Long aLong);

    Optional<Payment> findById(UUID uuid);
}
