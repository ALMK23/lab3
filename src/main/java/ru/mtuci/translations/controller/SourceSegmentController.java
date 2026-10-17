package ru.mtuci.translations.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mtuci.translations.dto.SourceSegmentDto;
import ru.mtuci.translations.service.SourceSegmentService;

import java.util.List;

@RestController
@RequestMapping("/api/source-segments")
public class SourceSegmentController {

    private final SourceSegmentService service;
    private final ResponseMapper mapper;

    public SourceSegmentController(SourceSegmentService service, ResponseMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<SourceSegmentDto.Response> getAll() {
        return service.findAll().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SourceSegmentDto.Response getById(@PathVariable Long id) {
        return mapper.toResponse(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SourceSegmentDto.Response create(@Valid @RequestBody SourceSegmentDto.CreateRequest request) {
        return mapper.toResponse(service.create(request.projectId(), request.sourceText(), request.position()));
    }

    @PatchMapping("/{id}")
    public SourceSegmentDto.Response update(@PathVariable Long id,
                                            @Valid @RequestBody SourceSegmentDto.PatchRequest request) {
        return mapper.toResponse(service.update(id, request.projectId(), request.sourceText(), request.position()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
