package ru.mtuci.translations.service;

import org.springframework.stereotype.Component;
import ru.mtuci.translations.exception.BadRequestException;
import ru.mtuci.translations.exception.NotFoundException;
import ru.mtuci.translations.model.EditorialComment;
import ru.mtuci.translations.model.SourceSegment;
import ru.mtuci.translations.model.Translation;
import ru.mtuci.translations.model.TranslationProject;
import ru.mtuci.translations.model.User;
import ru.mtuci.translations.repository.EditorialCommentRepository;
import ru.mtuci.translations.repository.SourceSegmentRepository;
import ru.mtuci.translations.repository.TranslationProjectRepository;
import ru.mtuci.translations.repository.TranslationRepository;
import ru.mtuci.translations.repository.UserRepository;

/**
 * Загрузка сущностей с единообразными ошибками:
 * сам ресурс из URL не найден → 404, ссылка из тела запроса ведёт в никуда → 400.
 */
@Component
class Lookup {

    private final UserRepository users;
    private final TranslationProjectRepository projects;
    private final SourceSegmentRepository segments;
    private final TranslationRepository translations;
    private final EditorialCommentRepository comments;

    Lookup(UserRepository users, TranslationProjectRepository projects, SourceSegmentRepository segments,
           TranslationRepository translations, EditorialCommentRepository comments) {
        this.users = users;
        this.projects = projects;
        this.segments = segments;
        this.translations = translations;
        this.comments = comments;
    }

    User user(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("Пользователь", id));
    }

    TranslationProject project(Long id) {
        return projects.findById(id).orElseThrow(() -> new NotFoundException("Проект", id));
    }

    /** Проект с блокировкой строки до конца транзакции. */
    TranslationProject projectForUpdate(Long id) {
        return projects.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Проект", id));
    }

    SourceSegment segment(Long id) {
        return segments.findById(id).orElseThrow(() -> new NotFoundException("Сегмент", id));
    }

    Translation translation(Long id) {
        return translations.findById(id).orElseThrow(() -> new NotFoundException("Перевод", id));
    }

    EditorialComment comment(Long id) {
        return comments.findById(id).orElseThrow(() -> new NotFoundException("Замечание", id));
    }

    // --- ссылки из тела запроса ---

    User userRef(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new BadRequestException("Пользователь с id=" + id + " не существует"));
    }

    /** Проект по ссылке из тела запроса, сразу с блокировкой строки. */
    TranslationProject projectRefForUpdate(Long id) {
        return projects.findByIdForUpdate(id)
                .orElseThrow(() -> new BadRequestException("Проект с id=" + id + " не существует"));
    }

    SourceSegment segmentRef(Long id) {
        return segments.findById(id)
                .orElseThrow(() -> new BadRequestException("Сегмент с id=" + id + " не существует"));
    }

    Translation translationRef(Long id) {
        return translations.findById(id)
                .orElseThrow(() -> new BadRequestException("Перевод с id=" + id + " не существует"));
    }
}
