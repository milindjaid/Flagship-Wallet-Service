package com.flagship.Wallet_Service.entities;

import com.flagship.Wallet_Service.exceptions.InsufficientBalanceException;
import com.flagship.Wallet_Service.exceptions.WalletInactiveException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "wallet_account",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "wallet_account_customer_id_currency_key",
                        columnNames = {"customer_id", "currency"}
                )
        },
        indexes = {
                @Index(name = "idx_wallet_customer", columnList = "customer_id")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "available_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal availableBalance;

    @Column(name = "blocked_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal blockedBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ---------- Lifecycle Hooks ----------

    @PrePersist
    public void onCreate() {
        if (walletId == null) {
            walletId = UUID.randomUUID();
        }
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    // ---------- Domain Logic (Money Safety) ----------

    public void blockAmount(BigDecimal amount) throws InsufficientBalanceException, WalletInactiveException {
        validateActive();
        if (availableBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }
        availableBalance = availableBalance.subtract(amount);
        blockedBalance = blockedBalance.add(amount);
    }

    public void unblockAmount(BigDecimal amount) {
        blockedBalance = blockedBalance.subtract(amount);
        availableBalance = availableBalance.add(amount);
    }

    public void finalizeDebit(BigDecimal amount) {
        blockedBalance = blockedBalance.subtract(amount);
    }

    private void validateActive() throws WalletInactiveException {
        if (status != WalletStatus.ACTIVE) {
            throw new WalletInactiveException();
        }
    }

    public void debitBlocked(BigDecimal amount) {
        if (blockedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient blocked balance");
        }
        blockedBalance = blockedBalance.subtract(amount);
    }

    public void unblock(BigDecimal amount) {
        if (blockedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient blocked balance");
        }
        blockedBalance = blockedBalance.subtract(amount);
        availableBalance = availableBalance.add(amount);
    }


}

