package ru.mtuci.translations.model;

/**
 * Жизненный цикл проекта:
 * DRAFT → IN_PROGRESS (запуск) → REVIEW (все сегменты утверждены, автоматически)
 * → COMPLETED (завершение) → ARCHIVED (архивирование).
 */
public enum ProjectStatus {
    DRAFT, IN_PROGRESS, REVIEW, COMPLETED, ARCHIVED;

    /** Можно ли менять состав проекта (сегменты, переводы, замечания). */
    public boolean isContentEditable() {
        return this == DRAFT || this == IN_PROGRESS;
    }
}
