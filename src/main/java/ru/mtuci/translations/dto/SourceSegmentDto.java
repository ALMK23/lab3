package ru.mtuci.translations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

public final class SourceSegmentDto {

    private SourceSegmentDto() {
    }

    /** position можно не указывать — сервер поставит сегмент в конец проекта. */
    public record CreateRequest(
            @NotNull Long projectId,
            @NotBlank String sourceText,
            @PositiveOrZero Integer position) {
    }

    public record PatchRequest(
            Long projectId,
            String sourceText,
            @PositiveOrZero Integer position) {
    }

    public record Response(
            Long id,
            Long projectId,
            String sourceText,
            Integer position,
            Instant createdAt,
            Instant updatedAt) {
    }
}
