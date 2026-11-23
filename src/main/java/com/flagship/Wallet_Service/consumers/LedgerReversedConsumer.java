package com.flagship.Wallet_Service.consumers;

import com.flagship.Wallet_Service.entities.Direction;
import com.flagship.Wallet_Service.entities.Wallet;
import com.flagship.Wallet_Service.entities.WalletTransaction;
import com.flagship.Wallet_Service.repo.WalletRepository;
import com.flagship.Wallet_Service.repo.WalletTransactionRepository;
import com.flagship.common_lib.dto.LedgerReversedEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
// Money will credit into wallet
@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerReversedConsumer {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @RetryableTopic( attempts = "3", backOff = @BackOff(delay = 2000,multiplier = 2))
    @KafkaListener(topics = "ledger.reversed",groupId = "wallet-ledger-group")
    @Transactional
    public void onLedgerReversed(LedgerReversedEvent event, Acknowledgment ack) {

        log.info("In Wallet service's LedgerReversedConsumer.");
        try {

            // 0️⃣ Idempotency guard
            if (walletTransactionRepository
                    .existsByPaymentIdAndDirection(
                            event.paymentId(),
                            Direction.CREDIT
                    )) {
                return;
            }

            Wallet wallet = walletRepository
                    .findByCustomerIdAndCurrency(event.customerId(), event.currency())
                    .orElseThrow();

            // 1️⃣ Release blocked funds
            wallet.unblock(event.amount());

            walletRepository.save(wallet);

            // 2️⃣ Immutable transaction record
            walletTransactionRepository.save(
                    WalletTransaction.credit(
                            wallet.getWalletId(),
                            event.paymentId(),
                            event.amount(),
                            event.currency()
                    )
            );
            ack.acknowledge();
        } catch (RuntimeException e) {
            log.error("Error processing paymentId={}", event.paymentId(), e);
//            throw new RuntimeException(e);
        }
    }
}

