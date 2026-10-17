package ru.mtuci.translations.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mtuci.translations.exception.ConflictException;
import ru.mtuci.translations.exception.ForbiddenException;
import ru.mtuci.translations.model.CommentStatus;
import ru.mtuci.translations.model.EditorialComment;
import ru.mtuci.translations.model.ProjectStatus;
import ru.mtuci.translations.model.Translation;
import ru.mtuci.translations.model.TranslationProject;
import ru.mtuci.translations.model.TranslationStatus;
import ru.mtuci.translations.repository.EditorialCommentRepository;
import ru.mtuci.translations.repository.SourceSegmentRepository;
import ru.mtuci.translations.repository.TranslationRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Бизнес-операции согласования переводов.
 * Роли: автор перевода (переводчик) и владелец проекта (редактор). Редактор не может согласовывать собственный перевод.
 * Каждая операция — одна транзакция; строка проекта блокируется, поэтому операции над одним проектом
 * не перемешиваются, а инварианты дополнительно защищены ограничениями БД.
 */
@Service
@Transactional
public class ReviewWorkflowService {

    private static final List<TranslationStatus> ACTIVE = List.of(TranslationStatus.SUBMITTED, TranslationStatus.APPROVED);

    private final TranslationRepository translationRepository;
    private final EditorialCommentRepository commentRepository;
    private final SourceSegmentRepository segmentRepository;
    private final Lookup lookup;
    private final Clock clock;

    public ReviewWorkflowService(TranslationRepository translationRepository,
                                 EditorialCommentRepository commentRepository,
                                 SourceSegmentRepository segmentRepository,
                                 Lookup lookup, Clock clock) {
        this.translationRepository = translationRepository;
        this.commentRepository = commentRepository;
        this.segmentRepository = segmentRepository;
        this.lookup = lookup;
        this.clock = clock;
    }

    /**
     * БО-2. Отправка перевода на согласование: DRAFT | REJECTED → SUBMITTED.
     * Только автор; проект запущен; по переводу нет открытых замечаний;
     * у сегмента нет другого перевода на согласовании или утверждённого.
     * Можно сразу передать исправленный текст.
     */
    public Translation submit(Long translationId, Long userId, String newText) {
        Translation translation = lookup.translation(translationId);
        TranslationProject project = lockProject(translation);
        requireAuthor(translation, userId, "отправить на согласование");
        Rules.requireInProgress(project);
        if (translation.getStatus() != TranslationStatus.DRAFT && translation.getStatus() != TranslationStatus.REJECTED) {
            throw new ConflictException("На согласование можно отправить только перевод в статусе DRAFT или REJECTED, текущий — "
                    + translation.getStatus());
        }
        long open = commentRepository.countByTranslationIdAndStatus(translationId, CommentStatus.OPEN);
        if (open > 0) {
            throw new ConflictException("Сначала устраните открытые замечания к переводу (" + open + " шт.)");
        }
        if (translationRepository.existsBySegmentIdAndStatusInAndIdNot(translation.getSegment().getId(), ACTIVE, translationId)) {
            throw new ConflictException("У сегмента id=" + translation.getSegment().getId()
                    + " уже есть перевод на согласовании или утверждённый");
        }
        if (newText != null) {
            Validation.requireNotBlank(newText, "text");
            translation.setText(newText.trim());
        }
        return touch(translation, TranslationStatus.SUBMITTED);
    }

    /**
     * БО-3. Отклонение перевода с замечанием: SUBMITTED → REJECTED + новое замечание OPEN.
     * Атомарно меняет две сущности: перевод и замечание.
     */
    public RejectResult reject(Long translationId, Long userId, String commentText) {
        Translation translation = lookup.translation(translationId);
        TranslationProject project = lockProject(translation);
        requireEditor(project, translation, userId, "отклонить");
        Rules.requireInProgress(project);
        requireSubmitted(translation, "отклонить");
        touch(translation, TranslationStatus.REJECTED);
        EditorialComment comment = commentRepository.save(
                EditorialCommentService.newOpenComment(translation, commentText, clock));
        return new RejectResult(translation, comment);
    }

    /**
     * БО-4. Утверждение перевода: SUBMITTED → APPROVED.
     * Только редактор (не автор); открытых замечаний быть не должно.
     * Если после этого у всех сегментов проекта есть утверждённый перевод, проект автоматически
     * переходит в REVIEW (финальная вычитка) — меняются перевод и проект.
     */
    public ApproveResult approve(Long translationId, Long userId) {
        Translation translation = lookup.translation(translationId);
        TranslationProject project = lockProject(translation);
        requireEditor(project, translation, userId, "утвердить");
        Rules.requireInProgress(project);
        requireSubmitted(translation, "утвердить");
        if (commentRepository.countByTranslationIdAndStatus(translationId, CommentStatus.OPEN) > 0) {
            throw new ConflictException("Нельзя утвердить перевод с открытыми замечаниями — отклоните его или снимите замечания");
        }
        touch(translation, TranslationStatus.APPROVED);
        translationRepository.flush();

        boolean allApproved = segmentRepository.countWithoutApprovedTranslation(project.getId()) == 0;
        if (allApproved) {
            project.setStatus(ProjectStatus.REVIEW);
            project.setUpdatedAt(Instant.now(clock));
        }
        return new ApproveResult(translation, project);
    }

    /**
     * БО-5. Устранение замечания автором: OPEN → RESOLVED, при необходимости с исправлением текста перевода.
     * Доступно для отклонённого перевода; меняет две сущности — замечание и перевод.
     */
    public ResolveResult resolve(Long commentId, Long userId, String correctedText) {
        EditorialComment comment = lookup.comment(commentId);
        Translation translation = comment.getTranslation();
        TranslationProject project = lockProject(translation);
        requireAuthor(translation, userId, "устранить замечание к");
        Rules.requireInProgress(project);
        requireOpen(comment);
        if (translation.getStatus() != TranslationStatus.REJECTED) {
            throw new ConflictException("Устранять замечания можно только у отклонённого перевода (REJECTED), текущий статус — "
                    + translation.getStatus());
        }
        if (correctedText != null) {
            Validation.requireNotBlank(correctedText, "translationText");
            translation.setText(correctedText.trim());
            translation.setUpdatedAt(Instant.now(clock));
        }
        comment.setStatus(CommentStatus.RESOLVED);
        comment.setUpdatedAt(Instant.now(clock));
        return new ResolveResult(comment, translation);
    }

    /**
     * БО-6. Снятие замечания редактором: OPEN → REJECTED (замечание признано неактуальным).
     * Открывает дорогу к утверждению перевода, по которому оставались вопросы.
     */
    public EditorialComment dismiss(Long commentId, Long userId) {
        EditorialComment comment = lookup.comment(commentId);
        Translation translation = comment.getTranslation();
        TranslationProject project = lockProject(translation);
        lookup.userRef(userId);
        if (!project.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Снять замечание может только редактор — владелец проекта (id="
                    + project.getOwner().getId() + ")");
        }
        Rules.requireInProgress(project);
        requireOpen(comment);
        comment.setStatus(CommentStatus.REJECTED);
        comment.setUpdatedAt(Instant.now(clock));
        return comment;
    }

    public record RejectResult(Translation translation, EditorialComment comment) {
    }

    public record ApproveResult(Translation translation, TranslationProject project) {
    }

    public record ResolveResult(EditorialComment comment, Translation translation) {
    }

    // --- проверки ---

    private TranslationProject lockProject(Translation translation) {
        return lookup.projectForUpdate(translation.getSegment().getProject().getId());
    }

    private void requireAuthor(Translation translation, Long userId, String action) {
        lookup.userRef(userId);
        if (!translation.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Только автор перевода (id=" + translation.getAuthor().getId()
                    + ") может " + action + " перевод");
        }
    }

    private void requireEditor(TranslationProject project, Translation translation, Long userId, String action) {
        lookup.userRef(userId);
        if (!project.getOwner().getId().equals(userId)) {
            throw new ForbiddenException(capitalize(action) + " перевод может только редактор — владелец проекта (id="
                    + project.getOwner().getId() + ")");
        }
        if (translation.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Редактор не может согласовывать собственный перевод");
        }
    }

    private void requireSubmitted(Translation translation, String action) {
        if (translation.getStatus() != TranslationStatus.SUBMITTED) {
            throw new ConflictException("Можно " + action + " только перевод на согласовании (SUBMITTED), текущий статус — "
                    + translation.getStatus());
        }
    }

    private void requireOpen(EditorialComment comment) {
        if (comment.getStatus() != CommentStatus.OPEN) {
            throw new ConflictException("Замечание уже закрыто (статус " + comment.getStatus() + ")");
        }
    }

    private Translation touch(Translation translation, TranslationStatus status) {
        translation.setStatus(status);
        translation.setUpdatedAt(Instant.now(clock));
        return translation;
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
