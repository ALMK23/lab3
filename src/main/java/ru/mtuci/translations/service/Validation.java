package ru.mtuci.translations.service;

import ru.mtuci.translations.exception.BadRequestException;

/** Проверки полей, переданных в частичном обновлении (PATCH). */
final class Validation {

    private Validation() {
    }

    static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(field + ": не должно быть пустым");
        }
    }

    static void requireNonNegative(Integer value, String field) {
        if (value != null && value < 0) {
            throw new BadRequestException(field + ": должно быть не меньше 0");
        }
    }

    /** Статусы меняются только бизнес-операциями, чтобы нельзя было обойти правила согласования. */
    static void forbidDirectStatus(Object status, String operationsHint) {
        if (status != null) {
            throw new BadRequestException("status нельзя задать напрямую — используйте бизнес-операции: " + operationsHint);
        }
    }
}
