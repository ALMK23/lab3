package ru.mtuci.translations.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mtuci.translations.exception.BadRequestException;
import ru.mtuci.translations.exception.ConflictException;
import ru.mtuci.translations.model.CommentStatus;
import ru.mtuci.translations.model.EditorialComment;
import ru.mtuci.translations.model.Translation;
import ru.mtuci.translations.model.TranslationStatus;
import ru.mtuci.translations.repository.EditorialCommentRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EditorialCommentService {

    static final String STATUS_OPERATIONS = "/resolve, /dismiss";

    private final EditorialCommentRepository commentRepository;
    private final Lookup lookup;
    private final Clock clock;

    public EditorialCommentService(EditorialCommentRepository commentRepository, Lookup lookup, Clock clock) {
        this.commentRepository = commentRepository;
        this.lookup = lookup;
        this.clock = clock;
    }

    public List<EditorialComment> findAll() {
        return commentRepository.findAllByOrderByIdAsc();
    }

    public EditorialComment findById(Long id) {
        return lookup.comment(id);
    }

    /** Замечание можно оставить только к переводу, который на согласовании или отклонён. */
    @Transactional
    public EditorialComment create(Long translationId, String text, CommentStatus status) {
        Validation.forbidDirectStatus(status, STATUS_OPERATIONS);
        Translation translation = lookup.translationRef(translationId);
        Rules.requireInProgress(lookup.projectForUpdate(translation.getSegment().getProject().getId()));
        if (translation.getStatus() != TranslationStatus.SUBMITTED && translation.getStatus() != TranslationStatus.REJECTED) {
            throw new ConflictException("Замечание можно оставить только к переводу в статусе SUBMITTED или REJECTED, а перевод id="
                    + translationId + " в статусе " + translation.getStatus());
        }
        return commentRepository.saveAndFlush(newOpenComment(translation, text, clock));
    }

    @Transactional
    public EditorialComment update(Long id, Long translationId, String text, CommentStatus status) {
        Validation.forbidDirectStatus(status, STATUS_OPERATIONS);
        EditorialComment comment = lookup.comment(id);
        Rules.requireInProgress(lookup.projectForUpdate(comment.getTranslation().getSegment().getProject().getId()));
        if (translationId != null && !translationId.equals(comment.getTranslation().getId())) {
            throw new BadRequestException("Замечание нельзя перенести на другой перевод");
        }
        if (text != null) {
            Validation.requireNotBlank(text, "text");
            if (comment.getStatus() != CommentStatus.OPEN) {
                throw new ConflictException("Текст можно менять только у открытого замечания");
            }
            comment.setText(text.trim());
        }
        comment.setUpdatedAt(Instant.now(clock));
        return commentRepository.saveAndFlush(comment);
    }

    @Transactional
    public void delete(Long id) {
        EditorialComment comment = lookup.comment(id);
        Rules.requireContentEditable(lookup.projectForUpdate(comment.getTranslation().getSegment().getProject().getId()));
        commentRepository.delete(comment);
    }

    static EditorialComment newOpenComment(Translation translation, String text, Clock clock) {
        EditorialComment comment = new EditorialComment();
        comment.setTranslation(translation);
        comment.setText(text.trim());
        comment.setStatus(CommentStatus.OPEN);
        Instant now = Instant.now(clock);
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        return comment;
    }
}
