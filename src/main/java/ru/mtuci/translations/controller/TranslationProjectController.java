package ru.mtuci.translations.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mtuci.translations.dto.TranslationProjectDto;
import ru.mtuci.translations.dto.WorkflowDto;
import ru.mtuci.translations.service.ProjectWorkflowService;
import ru.mtuci.translations.service.TranslationProjectService;

import java.util.List;

@RestController
@RequestMapping("/api/translation-projects")
public class TranslationProjectController {

    private final TranslationProjectService service;
    private final ProjectWorkflowService workflow;
    private final ResponseMapper mapper;

    public TranslationProjectController(TranslationProjectService service, ProjectWorkflowService workflow,
                                        ResponseMapper mapper) {
        this.service = service;
        this.workflow = workflow;
        this.mapper = mapper;
    }

    @GetMapping
    public List<TranslationProjectDto.Response> getAll() {
        return service.findAll().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TranslationProjectDto.Response getById(@PathVariable Long id) {
        return mapper.toResponse(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TranslationProjectDto.Response create(@Valid @RequestBody TranslationProjectDto.CreateRequest request) {
        return mapper.toResponse(service.create(request.name(), request.targetLanguage(),
                request.ownerId(), request.status()));
    }

    @PatchMapping("/{id}")
    public TranslationProjectDto.Response update(@PathVariable Long id,
                                                 @Valid @RequestBody TranslationProjectDto.PatchRequest request) {
        return mapper.toResponse(service.update(id, request.name(), request.targetLanguage(),
                request.ownerId(), request.status()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // --- бизнес-операции ---

    @PostMapping("/{id}/start")
    public TranslationProjectDto.Response start(@PathVariable Long id, @Valid @RequestBody WorkflowDto.ActorRequest request) {
        return mapper.toResponse(workflow.start(id, request.userId()));
    }

    @PostMapping("/{id}/complete")
    public TranslationProjectDto.Response complete(@PathVariable Long id, @Valid @RequestBody WorkflowDto.ActorRequest request) {
        return mapper.toResponse(workflow.complete(id, request.userId()));
    }

    @PostMapping("/{id}/archive")
    public TranslationProjectDto.Response archive(@PathVariable Long id, @Valid @RequestBody WorkflowDto.ActorRequest request) {
        return mapper.toResponse(workflow.archive(id, request.userId()));
    }
}
