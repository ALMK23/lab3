package ru.mtuci.translations.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class UserDto {

    private UserDto() {
    }

    public record CreateRequest(
            @NotBlank String name,
            @NotBlank @Email String email) {
    }

    public record PatchRequest(
            String name,
            @Email String email) {
    }

    public record Response(
            Long id,
            String name,
            String email,
            Instant createdAt,
            Instant updatedAt) {
    }
}
