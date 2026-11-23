package com.flagship.Wallet_Service.consumers;

import com.flagship.Wallet_Service.entities.Direction;
import com.flagship.Wallet_Service.exceptions.InsufficientBalanceException;
import com.flagship.Wallet_Service.exceptions.WalletInactiveException;
import com.flagship.Wallet_Service.exceptions.WalletNotFoundException;
import com.flagship.Wallet_Service.repo.WalletTransactionRepository;
import com.flagship.Wallet_Service.services.WalletService;
import com.flagship.common_lib.dto.PaymentInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
// Money will add a block amount in the wallet
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletConsumer {

    private final WalletService walletService;
    private final WalletTransactionRepository walletTransactionRepository;

    @KafkaListener(topics = "payment.initiated",groupId = "wallet-payment-group")
    public void onPaymentInitiated(PaymentInitiatedEvent event, Acknowledgment ack) throws WalletInactiveException, InsufficientBalanceException, WalletNotFoundException {
       log.info("In onPaymentInitiated inside wallet service...");
       try {
           if (walletTransactionRepository.existsByPaymentIdAndDirection(event.paymentId(), Direction.DEBIT)) {
               log.info("Duplicate event ignored for paymentId={}", event.paymentId());
               return;
           }
           walletService.blockAmount(
                   event.paymentId(),
                   event.customerId(),
                   event.amount(),
                   event.currency()
           );
           ack.acknowledge();
       } catch (Exception e) {
           log.error("Error processing paymentId={}", event.paymentId(), e);
//           throw new RuntimeException(e);
       }
    }
}
