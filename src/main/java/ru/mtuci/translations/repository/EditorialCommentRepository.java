package ru.mtuci.translations.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mtuci.translations.model.CommentStatus;
import ru.mtuci.translations.model.EditorialComment;

import java.util.List;

public interface EditorialCommentRepository extends JpaRepository<EditorialComment, Long> {

    List<EditorialComment> findAllByOrderByIdAsc();

    long countByTranslationIdAndStatus(Long translationId, CommentStatus status);

    @Query("""
            select count(c) from EditorialComment c
            where c.translation.segment.project.id = :projectId and c.status = :status
            """)
    long countByProjectIdAndStatus(@Param("projectId") Long projectId,
                                   @Param("status") CommentStatus status);
}
