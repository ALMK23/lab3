package ru.mtuci.translations.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mtuci.translations.exception.ConflictException;
import ru.mtuci.translations.model.ProjectStatus;
import ru.mtuci.translations.model.TranslationProject;
import ru.mtuci.translations.repository.TranslationProjectRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TranslationProjectService {

    static final String STATUS_OPERATIONS = "/start, /complete, /archive";

    private final TranslationProjectRepository projectRepository;
    private final Lookup lookup;
    private final Clock clock;

    public TranslationProjectService(TranslationProjectRepository projectRepository, Lookup lookup, Clock clock) {
        this.projectRepository = projectRepository;
        this.lookup = lookup;
        this.clock = clock;
    }

    public List<TranslationProject> findAll() {
        return projectRepository.findAllByOrderByIdAsc();
    }

    public TranslationProject findById(Long id) {
        return lookup.project(id);
    }

    /** Новый проект всегда создаётся черновиком. */
    @Transactional
    public TranslationProject create(String name, String targetLanguage, Long ownerId, ProjectStatus status) {
        Validation.forbidDirectStatus(status, STATUS_OPERATIONS);
        TranslationProject project = new TranslationProject();
        project.setName(name.trim());
        project.setTargetLanguage(targetLanguage.trim());
        project.setOwner(lookup.userRef(ownerId));
        project.setStatus(ProjectStatus.DRAFT);
        Instant now = Instant.now(clock);
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        return projectRepository.saveAndFlush(project);
    }

    @Transactional
    public TranslationProject update(Long id, String name, String targetLanguage, Long ownerId, ProjectStatus status) {
        Validation.forbidDirectStatus(status, STATUS_OPERATIONS);
        TranslationProject project = lookup.projectForUpdate(id);
        Rules.requireContentEditable(project);
        if (name != null) {
            Validation.requireNotBlank(name, "name");
            project.setName(name.trim());
        }
        if (targetLanguage != null) {
            Validation.requireNotBlank(targetLanguage, "targetLanguage");
            if (project.getStatus() != ProjectStatus.DRAFT
                    && !targetLanguage.trim().equals(project.getTargetLanguage())) {
                throw new ConflictException("Целевой язык можно менять только у проекта в статусе DRAFT");
            }
            project.setTargetLanguage(targetLanguage.trim());
        }
        if (ownerId != null) {
            project.setOwner(lookup.userRef(ownerId));
        }
        project.setUpdatedAt(Instant.now(clock));
        return projectRepository.saveAndFlush(project);
    }

    /** Сегменты, переводы и замечания проекта удаляются каскадно на уровне БД. */
    @Transactional
    public void delete(Long id) {
        projectRepository.delete(lookup.project(id));
    }
}
