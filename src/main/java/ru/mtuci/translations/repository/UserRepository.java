package ru.mtuci.translations.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mtuci.translations.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findAllByOrderByIdAsc();

    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Есть ли у пользователя переводы в чужих проектах, которые уже прошли согласование
     * (REVIEW / COMPLETED / ARCHIVED). Удалять такого автора нельзя — проект потеряет утверждённый текст.
     */
    @Query("""
            select case when count(t) > 0 then true else false end
            from Translation t
            where t.author.id = :userId
              and t.segment.project.owner.id <> :userId
              and t.segment.project.status in (
                  ru.mtuci.translations.model.ProjectStatus.REVIEW,
                  ru.mtuci.translations.model.ProjectStatus.COMPLETED,
                  ru.mtuci.translations.model.ProjectStatus.ARCHIVED)
            """)
    boolean hasTranslationsInFinishedForeignProjects(@Param("userId") Long userId);
}
