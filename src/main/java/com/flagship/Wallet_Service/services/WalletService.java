package com.flagship.Wallet_Service.services;

import com.flagship.Wallet_Service.entities.Direction;
import com.flagship.Wallet_Service.entities.Wallet;
import com.flagship.Wallet_Service.entities.WalletTransaction;
import com.flagship.Wallet_Service.exceptions.InsufficientBalanceException;
import com.flagship.Wallet_Service.exceptions.WalletInactiveException;
import com.flagship.Wallet_Service.exceptions.WalletNotFoundException;
import com.flagship.Wallet_Service.repo.PaymentRepository;
import com.flagship.Wallet_Service.repo.WalletRepository;
import com.flagship.Wallet_Service.repo.WalletTransactionRepository;
import com.flagship.common_lib.dto.WalletBlockedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);
    private final WalletRepository walletAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

//    @Transactional
//    public void blockAmount(
//            UUID paymentId,
//            UUID customerId,
//            BigDecimal amount,
//            String currency
//    ) throws WalletInactiveException,
//            InsufficientBalanceException,
//            WalletNotFoundException {
//
//        // 0️⃣ Idempotency check (VERY IMPORTANT)
//        if (walletTransactionRepository
//                .existsByPaymentIdAndDirection(paymentId, Direction.BLOCK)) {
//            return; // already processed
//        }
//
//        // 1️⃣ Load wallet (with optimistic locking via @Version)
//        Wallet wallet = walletAccountRepository
//                .findByCustomerIdAndCurrency(customerId, currency)
//                .orElseThrow(() ->
//                        new WalletNotFoundException(customerId, currency)
//                );
//
//        // 2️⃣ Domain logic (money safety)
//        wallet.blockAmount(amount);
//
//        // 3️⃣ Persist wallet snapshot
//        walletAccountRepository.save(wallet);
//
//        // 4️⃣ Persist immutable wallet transaction
//        walletTransactionRepository.save(
//                WalletTransaction.block(
//                        wallet.getWalletId(),
//                        paymentId,
//                        amount,
//                        currency
//                )
//        );
//
//        // 5️⃣ Publish event AFTER successful DB write
//        kafkaTemplate.send(
//                "wallet.blocked",
//                new WalletBlockedEvent(
//                        paymentId,
//                        customerId,
//                        amount,
//                        currency
//                )
//        );
//    }

    @Transactional
    public void blockAmount(
            UUID paymentId,
            UUID customerId,
            BigDecimal amount,
            String currency
    ) {

        try {
            log.info("Processing paymentId={} thread={}", paymentId, Thread.currentThread().getName());
            // 1️⃣ Try to insert transaction FIRST (acts as idempotency lock)
//           WalletTransaction walletTransaction =  walletTransactionRepository.save(
//                    WalletTransaction.block(
//                            null, // walletId set later if needed
//                            paymentId,
//                            amount,
//                            currency
//                    )
//            );

        // 1️⃣ Validate payment exists
        if (!paymentRepository.existsById(paymentId)) {
            log.error("Invalid paymentId={} → skipping", paymentId);
            return;
        }

                    // 0️⃣ Idempotency check (VERY IMPORTANT)
        if (walletTransactionRepository
                .existsByPaymentIdAndDirection(paymentId, Direction.DEBIT)) {
            return; // already processed
        }

            // 2️⃣ Load wallet
            Wallet wallet = walletAccountRepository
                    .findByCustomerIdAndCurrency(customerId, currency)
                    .orElseThrow(() ->
                            new WalletNotFoundException(customerId, currency)
                    );

            // 3️⃣ Block amount
            wallet.blockAmount(amount);

            // 4️⃣ Save wallet
            Wallet entity = walletAccountRepository.save(wallet);

            // Updating the wallet transaction with wallet id
//            walletTransaction.setWalletId(entity.getWalletId());
//            walletTransactionRepository.save(walletTransaction);
            WalletTransaction walletTransaction =  walletTransactionRepository.save(
                    WalletTransaction.block(
                            entity.getWalletId(), // walletId set later if needed
                            paymentId,
                            amount,
                            currency
                    )
            );


            // 5️⃣ Publish event
            kafkaTemplate.send(
                    "wallet.blocked",
                    new WalletBlockedEvent(
                            paymentId,
                            customerId,
                            amount,
                            currency
                    )
            );

        } catch (DataIntegrityViolationException e) {
            // ✅ Duplicate event → IGNORE
            log.info("Duplicate event for payment id {}",paymentId);
            return;
        } catch (WalletNotFoundException | InsufficientBalanceException | WalletInactiveException e) {
            throw new RuntimeException(e);
        }
    }
}
