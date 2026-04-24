package com.skinsshowcase.auth.exception;

/**
 * Ошибка проверки Steam OpenID (is_valid:false или некорректный claimed_id).
 */
public class SteamAuthException extends RuntimeException {

    public SteamAuthException(String message) {
        super(message);
    }

    public SteamAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
