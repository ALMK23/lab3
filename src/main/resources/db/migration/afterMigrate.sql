-- Колбэк Flyway: выполняется после каждой миграции.
-- Историю миграций пользователь приложения может только читать.
REVOKE INSERT, UPDATE, DELETE, TRUNCATE ON flyway_schema_history FROM translations_app;
