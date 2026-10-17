package ru.mtuci.translations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.mtuci.translations.model.ProjectStatus;

import java.time.Instant;

public final class TranslationProjectDto {

    private TranslationProjectDto() {
    }

    public record CreateRequest(
            @NotBlank String name,
            @NotBlank String targetLanguage,
            @NotNull Long ownerId,
            ProjectStatus status) {
    }

    public record PatchRequest(
            String name,
            String targetLanguage,
            Long ownerId,
            ProjectStatus status) {
    }

    public record Response(
            Long id,
            String name,
            String targetLanguage,
            Long ownerId,
            ProjectStatus status,
            Instant createdAt,
            Instant updatedAt) {
    }
}
