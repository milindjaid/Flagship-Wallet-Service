package com.flagship.Wallet_Service.entities;

import com.flagship.common_lib.constant.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_idempotency", columnNames = "idempotency_key")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @Column(name = "payment_id")
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency;

    // 🔑 SWIFT routing
    @Column(name = "beneficiary_bic", nullable = false, length = 11)
    private String beneficiaryBic;

    // 🔑 Final credit destination
    @Column(name = "beneficiary_account", nullable = false, length = 34)
    private String beneficiaryAccount;

    @Column(name = "rejection_reason", length = 50)
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    // 🛑 Exactly-once payment initiation
    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;

    @Version
    private Long version;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

}
