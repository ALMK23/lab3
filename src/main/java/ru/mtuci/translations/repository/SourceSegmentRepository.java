package ru.mtuci.translations.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mtuci.translations.model.SourceSegment;

import java.util.List;

public interface SourceSegmentRepository extends JpaRepository<SourceSegment, Long> {

    List<SourceSegment> findAllByOrderByIdAsc();

    long countByProjectId(Long projectId);

    boolean existsByProjectIdAndPosition(Long projectId, Integer position);

    @Query("select coalesce(max(s.position), 0) from SourceSegment s where s.project.id = :projectId")
    int findMaxPosition(@Param("projectId") Long projectId);

    /** Сколько сегментов проекта ещё не имеют утверждённого перевода. */
    @Query("""
            select count(s) from SourceSegment s
            where s.project.id = :projectId
              and not exists (select 1 from Translation t
                              where t.segment = s
                                and t.status = ru.mtuci.translations.model.TranslationStatus.APPROVED)
            """)
    long countWithoutApprovedTranslation(@Param("projectId") Long projectId);
}
