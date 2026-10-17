package ru.mtuci.translations.exception;

/** Операция недопустима при текущем состоянии данных → 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
