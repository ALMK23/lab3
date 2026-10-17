package ru.mtuci.translations.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mtuci.translations.exception.ConflictException;
import ru.mtuci.translations.exception.ForbiddenException;
import ru.mtuci.translations.model.CommentStatus;
import ru.mtuci.translations.model.ProjectStatus;
import ru.mtuci.translations.model.TranslationProject;
import ru.mtuci.translations.model.TranslationStatus;
import ru.mtuci.translations.repository.EditorialCommentRepository;
import ru.mtuci.translations.repository.SourceSegmentRepository;
import ru.mtuci.translations.repository.TranslationRepository;

import java.time.Clock;
import java.time.Instant;

/**
 * Бизнес-операции жизненного цикла проекта.
 * Каждая операция — одна транзакция с блокировкой строки проекта (SELECT ... FOR UPDATE).
 */
@Service
@Transactional
public class ProjectWorkflowService {

    private final SourceSegmentRepository segmentRepository;
    private final TranslationRepository translationRepository;
    private final EditorialCommentRepository commentRepository;
    private final Lookup lookup;
    private final Clock clock;

    public ProjectWorkflowService(SourceSegmentRepository segmentRepository,
                                  TranslationRepository translationRepository,
                                  EditorialCommentRepository commentRepository,
                                  Lookup lookup, Clock clock) {
        this.segmentRepository = segmentRepository;
        this.translationRepository = translationRepository;
        this.commentRepository = commentRepository;
        this.lookup = lookup;
        this.clock = clock;
    }

    /**
     * БО-1. Запуск проекта в работу: DRAFT → IN_PROGRESS.
     * Выполняет только владелец; в проекте должен быть хотя бы один исходный сегмент.
     */
    public TranslationProject start(Long projectId, Long userId) {
        TranslationProject project = lockAndCheckOwner(projectId, userId);
        requireStatus(project, ProjectStatus.DRAFT, "запустить");
        if (segmentRepository.countByProjectId(projectId) == 0) {
            throw new ConflictException("Нельзя запустить проект без исходных сегментов");
        }
        return changeStatus(project, ProjectStatus.IN_PROGRESS);
    }

    /**
     * БО-7. Завершение проекта: REVIEW → COMPLETED.
     * Повторно проверяет, что у каждого сегмента есть утверждённый перевод и не осталось открытых замечаний.
     */
    public TranslationProject complete(Long projectId, Long userId) {
        TranslationProject project = lockAndCheckOwner(projectId, userId);
        requireStatus(project, ProjectStatus.REVIEW, "завершить");
        long notApproved = segmentRepository.countWithoutApprovedTranslation(projectId);
        if (notApproved > 0) {
            throw new ConflictException("Нельзя завершить проект: сегментов без утверждённого перевода — " + notApproved);
        }
        if (commentRepository.countByProjectIdAndStatus(projectId, CommentStatus.OPEN) > 0) {
            throw new ConflictException("Нельзя завершить проект: остались открытые замечания");
        }
        if (translationRepository.countByProjectIdAndStatus(projectId, TranslationStatus.SUBMITTED) > 0) {
            throw new ConflictException("Нельзя завершить проект: есть переводы на согласовании");
        }
        return changeStatus(project, ProjectStatus.COMPLETED);
    }

    /** БО-8. Архивирование: COMPLETED → ARCHIVED. После этого проект доступен только для чтения и удаления. */
    public TranslationProject archive(Long projectId, Long userId) {
        TranslationProject project = lockAndCheckOwner(projectId, userId);
        requireStatus(project, ProjectStatus.COMPLETED, "отправить в архив");
        return changeStatus(project, ProjectStatus.ARCHIVED);
    }

    private TranslationProject lockAndCheckOwner(Long projectId, Long userId) {
        TranslationProject project = lookup.projectForUpdate(projectId);
        lookup.userRef(userId);
        if (!project.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Управлять статусом проекта может только его владелец (id="
                    + project.getOwner().getId() + ")");
        }
        return project;
    }

    private void requireStatus(TranslationProject project, ProjectStatus expected, String action) {
        if (project.getStatus() != expected) {
            throw new ConflictException("Проект можно " + action + " только из статуса " + expected
                    + ", текущий статус — " + project.getStatus());
        }
    }

    private TranslationProject changeStatus(TranslationProject project, ProjectStatus status) {
        project.setStatus(status);
        project.setUpdatedAt(Instant.now(clock));
        return project;
    }
}
