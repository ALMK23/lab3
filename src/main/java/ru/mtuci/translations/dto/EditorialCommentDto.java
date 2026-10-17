package ru.mtuci.translations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.mtuci.translations.model.CommentStatus;

import java.time.Instant;

public final class EditorialCommentDto {

    private EditorialCommentDto() {
    }

    public record CreateRequest(
            @NotNull Long translationId,
            @NotBlank String text,
            CommentStatus status) {
    }

    public record PatchRequest(
            Long translationId,
            String text,
            CommentStatus status) {
    }

    public record Response(
            Long id,
            Long translationId,
            String text,
            CommentStatus status,
            Instant createdAt,
            Instant updatedAt) {
    }
}
