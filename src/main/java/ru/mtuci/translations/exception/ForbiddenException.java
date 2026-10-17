package ru.mtuci.translations.exception;

/** Операцию пытается выполнить пользователь, которому она не разрешена → 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
