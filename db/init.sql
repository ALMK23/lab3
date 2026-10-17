-- Первичная настройка PostgreSQL. Выполняется ОДИН раз от суперпользователя postgres.
-- Пароли передаются как переменные psql, чтобы не хранить их в репозитории:
--
--   psql -U postgres -v owner_password=... -v app_password=... -f db/init.sql
--
-- (в docker-compose это делает db/docker-init.sh, беря пароли из .env)

-- 1. Две роли с разными правами
--    translations_owner — владелец БД и схемы, выполняет миграции (DDL);
--    translations_app   — под ним работает сервис: только чтение и изменение строк.
CREATE ROLE translations_owner LOGIN PASSWORD :'owner_password';
CREATE ROLE translations_app   LOGIN PASSWORD :'app_password'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;

CREATE DATABASE translations OWNER translations_owner ENCODING 'UTF8' TEMPLATE template0;

\connect translations

-- 2. Никто посторонний не подключается и не создаёт объекты в public
REVOKE ALL ON DATABASE translations FROM PUBLIC;
GRANT CONNECT ON DATABASE translations TO translations_owner, translations_app;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- 3. Отдельная схема приложения, принадлежит владельцу
CREATE SCHEMA app AUTHORIZATION translations_owner;
GRANT USAGE ON SCHEMA app TO translations_app;   -- без CREATE: таблицы приложение создавать не может

-- 4. Права на ещё не созданные таблицы: всё, что создаст владелец (миграции),
--    автоматически станет доступно приложению только на DML.
ALTER DEFAULT PRIVILEGES FOR ROLE translations_owner IN SCHEMA app
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO translations_app;
ALTER DEFAULT PRIVILEGES FOR ROLE translations_owner IN SCHEMA app
    GRANT USAGE, SELECT ON SEQUENCES TO translations_app;

ALTER ROLE translations_app   IN DATABASE translations SET search_path = app;
ALTER ROLE translations_owner IN DATABASE translations SET search_path = app;
