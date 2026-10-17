package ru.mtuci.translations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Тела запросов бизнес-операций. Аутентификации в проекте нет, поэтому
 * пользователь, выполняющий операцию, передаётся явно в поле userId.
 */
public final class WorkflowDto {

    private WorkflowDto() {
    }

    /** start / complete / archive / approve / dismiss. */
    public record ActorRequest(@NotNull Long userId) {
    }

    /** submit: можно сразу передать исправленный текст перевода. */
    public record SubmitRequest(@NotNull Long userId, String text) {
    }

    /** reject: замечание обязательно — без него автор не поймёт, что исправлять. */
    public record RejectRequest(@NotNull Long userId, @NotBlank String comment) {
    }

    /** resolve: можно одновременно исправить текст перевода. */
    public record ResolveRequest(@NotNull Long userId, String translationText) {
    }

    public record RejectResponse(TranslationDto.Response translation, EditorialCommentDto.Response comment) {
    }

    public record ApproveResponse(TranslationDto.Response translation, TranslationProjectDto.Response project) {
    }

    public record ResolveResponse(EditorialCommentDto.Response comment, TranslationDto.Response translation) {
    }
}
