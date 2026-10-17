-- Схема предметной области «Согласование переводов».
-- Целостность обеспечивается на уровне БД: внешние ключи, каскадное удаление,
-- уникальность, CHECK-ограничения на статусы и пустые строки.

CREATE TABLE users (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(200) NOT NULL CHECK (btrim(name) <> ''),
    email       VARCHAR(320) NOT NULL CHECK (position('@' IN email) > 1),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0
);
-- email уникален без учёта регистра
CREATE UNIQUE INDEX ux_users_email ON users (lower(email));

CREATE TABLE translation_projects (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name             VARCHAR(200) NOT NULL CHECK (btrim(name) <> ''),
    target_language  VARCHAR(20)  NOT NULL CHECK (btrim(target_language) <> ''),
    -- владелец (редактор) проекта; удаление пользователя удаляет его проекты
    owner_id         BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                     CHECK (status IN ('DRAFT', 'IN_PROGRESS', 'REVIEW', 'COMPLETED', 'ARCHIVED')),
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX ix_projects_owner ON translation_projects (owner_id);

CREATE TABLE source_segments (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id   BIGINT      NOT NULL REFERENCES translation_projects (id) ON DELETE CASCADE,
    source_text  TEXT        NOT NULL CHECK (btrim(source_text) <> ''),
    position     INTEGER     NOT NULL CHECK (position >= 0),
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    version      BIGINT      NOT NULL DEFAULT 0,
    -- в одном проекте два сегмента не могут стоять на одном месте
    CONSTRAINT ux_segments_project_position UNIQUE (project_id, position)
);

CREATE TABLE translations (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    segment_id  BIGINT      NOT NULL REFERENCES source_segments (id) ON DELETE CASCADE,
    author_id   BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    text        TEXT        NOT NULL CHECK (btrim(text) <> ''),
    status      VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED')),
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0
);
CREATE INDEX ix_translations_segment ON translations (segment_id);
CREATE INDEX ix_translations_author ON translations (author_id);
-- У сегмента не больше одного «активного» перевода: на согласовании или утверждённого.
-- Черновиков и отклонённых вариантов может быть сколько угодно.
CREATE UNIQUE INDEX ux_translations_one_active_per_segment
    ON translations (segment_id) WHERE status IN ('SUBMITTED', 'APPROVED');

CREATE TABLE editorial_comments (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    translation_id  BIGINT      NOT NULL REFERENCES translations (id) ON DELETE CASCADE,
    text            TEXT        NOT NULL CHECK (btrim(text) <> ''),
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                    CHECK (status IN ('OPEN', 'RESOLVED', 'REJECTED')),
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    version         BIGINT      NOT NULL DEFAULT 0
);
CREATE INDEX ix_comments_translation ON editorial_comments (translation_id);
