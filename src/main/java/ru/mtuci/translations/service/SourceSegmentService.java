package ru.mtuci.translations.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mtuci.translations.exception.ConflictException;
import ru.mtuci.translations.model.SourceSegment;
import ru.mtuci.translations.model.TranslationProject;
import ru.mtuci.translations.model.TranslationStatus;
import ru.mtuci.translations.repository.SourceSegmentRepository;
import ru.mtuci.translations.repository.TranslationRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class SourceSegmentService {

    private static final List<TranslationStatus> ACTIVE = List.of(TranslationStatus.SUBMITTED, TranslationStatus.APPROVED);

    private final SourceSegmentRepository segmentRepository;
    private final TranslationRepository translationRepository;
    private final Lookup lookup;
    private final Clock clock;

    public SourceSegmentService(SourceSegmentRepository segmentRepository, TranslationRepository translationRepository,
                                Lookup lookup, Clock clock) {
        this.segmentRepository = segmentRepository;
        this.translationRepository = translationRepository;
        this.lookup = lookup;
        this.clock = clock;
    }

    public List<SourceSegment> findAll() {
        return segmentRepository.findAllByOrderByIdAsc();
    }

    public SourceSegment findById(Long id) {
        return lookup.segment(id);
    }

    @Transactional
    public SourceSegment create(Long projectId, String sourceText, Integer position) {
        TranslationProject project = lookup.projectRefForUpdate(projectId);
        Rules.requireContentEditable(project);
        int pos = position != null ? position : segmentRepository.findMaxPosition(projectId) + 1;
        requireFreePosition(projectId, pos);
        SourceSegment segment = new SourceSegment();
        segment.setProject(project);
        segment.setSourceText(sourceText.trim());
        segment.setPosition(pos);
        Instant now = Instant.now(clock);
        segment.setCreatedAt(now);
        segment.setUpdatedAt(now);
        return segmentRepository.saveAndFlush(segment);
    }

    @Transactional
    public SourceSegment update(Long id, Long projectId, String sourceText, Integer position) {
        SourceSegment segment = lookup.segment(id);
        TranslationProject current = lookup.projectForUpdate(segment.getProject().getId());
        Rules.requireContentEditable(current);

        if (projectId != null && !projectId.equals(current.getId())) {
            TranslationProject target = lookup.projectRefForUpdate(projectId);
            Rules.requireContentEditable(target);
            if (translationRepository.existsBySegmentId(id)) {
                throw new ConflictException("Сегмент с переводами нельзя перенести в другой проект");
            }
            int pos = position != null ? position : segment.getPosition();
            requireFreePosition(projectId, pos);
            segment.setProject(target);
        }
        if (sourceText != null) {
            Validation.requireNotBlank(sourceText, "sourceText");
            if (!sourceText.trim().equals(segment.getSourceText())
                    && translationRepository.existsBySegmentIdAndStatusIn(id, ACTIVE)) {
                throw new ConflictException("Исходный текст нельзя менять: у сегмента есть перевод на согласовании или утверждённый");
            }
            segment.setSourceText(sourceText.trim());
        }
        if (position != null) {
            Validation.requireNonNegative(position, "position");
            if (!Objects.equals(position, segment.getPosition())) {
                requireFreePosition(segment.getProject().getId(), position);
            }
            segment.setPosition(position);
        }
        segment.setUpdatedAt(Instant.now(clock));
        return segmentRepository.saveAndFlush(segment);
    }

    /** Переводы сегмента и замечания к ним удаляются каскадно на уровне БД. */
    @Transactional
    public void delete(Long id) {
        SourceSegment segment = lookup.segment(id);
        Rules.requireContentEditable(lookup.projectForUpdate(segment.getProject().getId()));
        segmentRepository.delete(segment);
    }

    private void requireFreePosition(Long projectId, int position) {
        if (segmentRepository.existsByProjectIdAndPosition(projectId, position)) {
            throw new ConflictException("В проекте id=" + projectId + " уже есть сегмент на позиции " + position);
        }
    }
}
