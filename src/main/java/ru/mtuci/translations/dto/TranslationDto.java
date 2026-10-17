package ru.mtuci.translations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.mtuci.translations.model.TranslationStatus;

import java.time.Instant;

public final class TranslationDto {

    private TranslationDto() {
    }

    public record CreateRequest(
            @NotNull Long segmentId,
            @NotNull Long authorId,
            @NotBlank String text,
            TranslationStatus status) {
    }

    public record PatchRequest(
            Long segmentId,
            Long authorId,
            String text,
            TranslationStatus status) {
    }

    public record Response(
            Long id,
            Long segmentId,
            Long authorId,
            String text,
            TranslationStatus status,
            Instant createdAt,
            Instant updatedAt) {
    }
}
