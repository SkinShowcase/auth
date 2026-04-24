package com.skinsshowcase.auth.exception;

/**
 * Аккаунт заблокирован модерацией; для пользовательских JWT-эндпоинтов — HTTP 403.
 */
public class AccountBlockedException extends RuntimeException {

    public AccountBlockedException() {
        super("Account blocked");
    }
}
