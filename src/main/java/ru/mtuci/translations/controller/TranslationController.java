package ru.mtuci.translations.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mtuci.translations.dto.TranslationDto;
import ru.mtuci.translations.dto.WorkflowDto;
import ru.mtuci.translations.service.ReviewWorkflowService;
import ru.mtuci.translations.service.TranslationService;

import java.util.List;

@RestController
@RequestMapping("/api/translations")
public class TranslationController {

    private final TranslationService service;
    private final ReviewWorkflowService workflow;
    private final ResponseMapper mapper;

    public TranslationController(TranslationService service, ReviewWorkflowService workflow, ResponseMapper mapper) {
        this.service = service;
        this.workflow = workflow;
        this.mapper = mapper;
    }

    @GetMapping
    public List<TranslationDto.Response> getAll() {
        return service.findAll().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TranslationDto.Response getById(@PathVariable Long id) {
        return mapper.toResponse(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TranslationDto.Response create(@Valid @RequestBody TranslationDto.CreateRequest request) {
        return mapper.toResponse(service.create(request.segmentId(), request.authorId(), request.text(), request.status()));
    }

    @PatchMapping("/{id}")
    public TranslationDto.Response update(@PathVariable Long id,
                                          @Valid @RequestBody TranslationDto.PatchRequest request) {
        return mapper.toResponse(service.update(id, request.segmentId(), request.authorId(),
                request.text(), request.status()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // --- бизнес-операции ---

    @PostMapping("/{id}/submit")
    public TranslationDto.Response submit(@PathVariable Long id, @Valid @RequestBody WorkflowDto.SubmitRequest request) {
        return mapper.toResponse(workflow.submit(id, request.userId(), request.text()));
    }

    @PostMapping("/{id}/reject")
    public WorkflowDto.RejectResponse reject(@PathVariable Long id, @Valid @RequestBody WorkflowDto.RejectRequest request) {
        ReviewWorkflowService.RejectResult result = workflow.reject(id, request.userId(), request.comment());
        return new WorkflowDto.RejectResponse(mapper.toResponse(result.translation()), mapper.toResponse(result.comment()));
    }

    @PostMapping("/{id}/approve")
    public WorkflowDto.ApproveResponse approve(@PathVariable Long id, @Valid @RequestBody WorkflowDto.ActorRequest request) {
        ReviewWorkflowService.ApproveResult result = workflow.approve(id, request.userId());
        return new WorkflowDto.ApproveResponse(mapper.toResponse(result.translation()), mapper.toResponse(result.project()));
    }
}
