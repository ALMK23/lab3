package ru.mtuci.translations.exception;

/** Некорректные данные запроса (в т.ч. ссылка на несуществующий объект) → 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
