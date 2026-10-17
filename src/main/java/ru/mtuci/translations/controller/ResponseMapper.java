package ru.mtuci.translations.controller;

import org.springframework.stereotype.Component;
import ru.mtuci.translations.dto.EditorialCommentDto;
import ru.mtuci.translations.dto.SourceSegmentDto;
import ru.mtuci.translations.dto.TranslationDto;
import ru.mtuci.translations.dto.TranslationProjectDto;
import ru.mtuci.translations.dto.UserDto;
import ru.mtuci.translations.model.EditorialComment;
import ru.mtuci.translations.model.SourceSegment;
import ru.mtuci.translations.model.Translation;
import ru.mtuci.translations.model.TranslationProject;
import ru.mtuci.translations.model.User;

/** Маппинг сущностей в DTO ответов — общий для всех контроллеров. */
@Component
public class ResponseMapper {

    public UserDto.Response toResponse(User u) {
        return new UserDto.Response(u.getId(), u.getName(), u.getEmail(), u.getCreatedAt(), u.getUpdatedAt());
    }

    public TranslationProjectDto.Response toResponse(TranslationProject p) {
        return new TranslationProjectDto.Response(p.getId(), p.getName(), p.getTargetLanguage(),
                p.getOwner().getId(), p.getStatus(), p.getCreatedAt(), p.getUpdatedAt());
    }

    public SourceSegmentDto.Response toResponse(SourceSegment s) {
        return new SourceSegmentDto.Response(s.getId(), s.getProject().getId(), s.getSourceText(),
                s.getPosition(), s.getCreatedAt(), s.getUpdatedAt());
    }

    public TranslationDto.Response toResponse(Translation t) {
        return new TranslationDto.Response(t.getId(), t.getSegment().getId(), t.getAuthor().getId(),
                t.getText(), t.getStatus(), t.getCreatedAt(), t.getUpdatedAt());
    }

    public EditorialCommentDto.Response toResponse(EditorialComment c) {
        return new EditorialCommentDto.Response(c.getId(), c.getTranslation().getId(), c.getText(),
                c.getStatus(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
