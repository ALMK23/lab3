package ru.mtuci.translations.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mtuci.translations.model.Translation;
import ru.mtuci.translations.model.TranslationStatus;

import java.util.Collection;
import java.util.List;

public interface TranslationRepository extends JpaRepository<Translation, Long> {

    List<Translation> findAllByOrderByIdAsc();

    boolean existsBySegmentId(Long segmentId);

    boolean existsBySegmentIdAndStatusIn(Long segmentId, Collection<TranslationStatus> statuses);

    /** Есть ли у сегмента другой перевод в одном из статусов (кроме переданного). */
    boolean existsBySegmentIdAndStatusInAndIdNot(Long segmentId, Collection<TranslationStatus> statuses, Long id);

    @Query("""
            select count(t) from Translation t
            where t.segment.project.id = :projectId and t.status = :status
            """)
    long countByProjectIdAndStatus(@Param("projectId") Long projectId,
                                   @Param("status") TranslationStatus status);
}
