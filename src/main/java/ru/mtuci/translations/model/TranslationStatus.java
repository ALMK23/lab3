package ru.mtuci.translations.model;

/**
 * Жизненный цикл перевода:
 * DRAFT → SUBMITTED (отправка) → APPROVED (утверждение) или REJECTED (отклонение с замечанием);
 * REJECTED → SUBMITTED (повторная отправка после устранения замечаний).
 */
public enum TranslationStatus {
    DRAFT, SUBMITTED, APPROVED, REJECTED
}
