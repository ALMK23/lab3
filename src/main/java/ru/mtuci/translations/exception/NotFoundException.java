package ru.mtuci.translations.exception;

/** Запрашиваемый ресурс не найден → 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String resource, Long id) {
        super(resource + " с id=" + id + " не найден");
    }
}
