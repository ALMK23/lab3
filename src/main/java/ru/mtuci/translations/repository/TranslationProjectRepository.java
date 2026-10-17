package ru.mtuci.translations.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mtuci.translations.model.TranslationProject;

import java.util.List;
import java.util.Optional;

public interface TranslationProjectRepository extends JpaRepository<TranslationProject, Long> {

    List<TranslationProject> findAllByOrderByIdAsc();

    /**
     * Чтение проекта с блокировкой строки (SELECT ... FOR UPDATE).
     * Бизнес-операции над одним проектом выполняются строго по очереди.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TranslationProject p where p.id = :id")
    Optional<TranslationProject> findByIdForUpdate(@Param("id") Long id);
}
