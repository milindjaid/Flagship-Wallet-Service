package com.flagship.Wallet_Service.exceptions;

import java.util.UUID;

public class WalletNotFoundException extends Throwable {
    public WalletNotFoundException(UUID customerId, String currency) {
    }
}
