package ru.mtuci.translations.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mtuci.translations.dto.EditorialCommentDto;
import ru.mtuci.translations.dto.WorkflowDto;
import ru.mtuci.translations.service.EditorialCommentService;
import ru.mtuci.translations.service.ReviewWorkflowService;

import java.util.List;

@RestController
@RequestMapping("/api/editorial-comments")
public class EditorialCommentController {

    private final EditorialCommentService service;
    private final ReviewWorkflowService workflow;
    private final ResponseMapper mapper;

    public EditorialCommentController(EditorialCommentService service, ReviewWorkflowService workflow,
                                      ResponseMapper mapper) {
        this.service = service;
        this.workflow = workflow;
        this.mapper = mapper;
    }

    @GetMapping
    public List<EditorialCommentDto.Response> getAll() {
        return service.findAll().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public EditorialCommentDto.Response getById(@PathVariable Long id) {
        return mapper.toResponse(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EditorialCommentDto.Response create(@Valid @RequestBody EditorialCommentDto.CreateRequest request) {
        return mapper.toResponse(service.create(request.translationId(), request.text(), request.status()));
    }

    @PatchMapping("/{id}")
    public EditorialCommentDto.Response update(@PathVariable Long id,
                                               @Valid @RequestBody EditorialCommentDto.PatchRequest request) {
        return mapper.toResponse(service.update(id, request.translationId(), request.text(), request.status()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // --- бизнес-операции ---

    @PostMapping("/{id}/resolve")
    public WorkflowDto.ResolveResponse resolve(@PathVariable Long id, @Valid @RequestBody WorkflowDto.ResolveRequest request) {
        ReviewWorkflowService.ResolveResult result = workflow.resolve(id, request.userId(), request.translationText());
        return new WorkflowDto.ResolveResponse(mapper.toResponse(result.comment()), mapper.toResponse(result.translation()));
    }

    @PostMapping("/{id}/dismiss")
    public EditorialCommentDto.Response dismiss(@PathVariable Long id, @Valid @RequestBody WorkflowDto.ActorRequest request) {
        return mapper.toResponse(workflow.dismiss(id, request.userId()));
    }
}
