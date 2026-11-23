package com.flagship.Wallet_Service.consumers;

import com.flagship.Wallet_Service.entities.Direction;
import com.flagship.Wallet_Service.entities.Wallet;
import com.flagship.Wallet_Service.entities.WalletTransaction;
import com.flagship.Wallet_Service.repo.WalletRepository;
import com.flagship.Wallet_Service.repo.WalletTransactionRepository;
import com.flagship.common_lib.dto.SettlementCompletedEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
// Money will debit from wallet
@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerPostedWalletConsumer {
        private final WalletRepository walletRepository;
        private final WalletTransactionRepository walletTransactionRepository;

        @KafkaListener(topics = "settlement.completed",groupId = "wallet-settlement-group")
        @Transactional(Transactional.TxType.REQUIRES_NEW)
        public void onSettlementCompleted(SettlementCompletedEvent event, Acknowledgment ack) {
            try {
                log.info("In LedgerPostedWalletConsumer Wallet service.");
                // 0️⃣ Idempotency guard
              /*  if (walletTransactionRepository
                        .existsByPaymentIdAndDirection(
                                event.paymentId(),
                                Direction.DEBIT
                        )) {
                    return;
                } */

                Wallet wallet = walletRepository
                        .findByCustomerIdAndCurrency(event.customerId(), event.currency())
                        .orElseThrow();

                // 1️⃣ Move money out of blocked balance
                wallet.debitBlocked(event.amount());

                walletRepository.save(wallet);

                // 2️⃣ Immutable transaction record
                walletTransactionRepository.save(
                        WalletTransaction.debit(
                                wallet.getWalletId(),
                                event.paymentId(),
                                event.amount(),
                                event.currency()
                        )
                );
                ack.acknowledge();
            }catch (DataIntegrityViolationException e) {
                // ✅ Duplicate event → safe to ignore
                log.warn("Duplicate settlement ignored for paymentId={}", event.paymentId());
                ack.acknowledge();

            } catch (RuntimeException e) {
                log.error("Error processing paymentId={}", event.paymentId(), e);
                ack.acknowledge();
                // ❌ DO NOT ACK → Kafka will retry
//                throw new RuntimeException(e); // TODO for now but need to handle properly
            }
        }

}
