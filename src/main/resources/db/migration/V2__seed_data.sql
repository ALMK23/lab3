-- Тестовые данные. Все записи согласованы с правилами предметной области:
-- у сегмента не больше одного активного перевода, утверждённые переводы без открытых замечаний и т.д.

INSERT INTO users (name, email, created_at, updated_at) VALUES
    ('Анна Редакторова', 'anna.editor@demo.local',      now(), now()),
    ('Борис Переводов',  'boris.translator@demo.local', now(), now()),
    ('Вера Лингвистова', 'vera.translator@demo.local',  now(), now());

-- Проект 1: в работе — есть утверждённый, отклонённый с замечанием и черновой переводы
INSERT INTO translation_projects (name, target_language, owner_id, status, created_at, updated_at)
SELECT 'Локализация мобильного банка', 'en', id, 'IN_PROGRESS', now(), now()
FROM users WHERE email = 'anna.editor@demo.local';

-- Проект 2: черновик, ещё не запущен
INSERT INTO translation_projects (name, target_language, owner_id, status, created_at, updated_at)
SELECT 'Руководство пользователя', 'de', id, 'DRAFT', now(), now()
FROM users WHERE email = 'anna.editor@demo.local';

INSERT INTO source_segments (project_id, source_text, position, created_at, updated_at)
SELECT p.id, s.txt, s.pos, now(), now()
FROM translation_projects p
CROSS JOIN (VALUES (1, 'Добро пожаловать в приложение!'),
                   (2, 'Введите код из СМС'),
                   (3, 'Перевод выполнен успешно')) AS s(pos, txt)
WHERE p.name = 'Локализация мобильного банка';

INSERT INTO source_segments (project_id, source_text, position, created_at, updated_at)
SELECT p.id, s.txt, s.pos, now(), now()
FROM translation_projects p
CROSS JOIN (VALUES (1, 'Глава 1. Установка'),
                   (2, 'Глава 2. Первый запуск')) AS s(pos, txt)
WHERE p.name = 'Руководство пользователя';

-- Переводы проекта 1
INSERT INTO translations (segment_id, author_id, text, status, created_at, updated_at)
SELECT s.id, u.id, 'Welcome to the app!', 'APPROVED', now(), now()
FROM source_segments s
JOIN translation_projects p ON p.id = s.project_id AND p.name = 'Локализация мобильного банка'
JOIN users u ON u.email = 'boris.translator@demo.local'
WHERE s.position = 1;

INSERT INTO translations (segment_id, author_id, text, status, created_at, updated_at)
SELECT s.id, u.id, 'Enter the code from SMS', 'REJECTED', now(), now()
FROM source_segments s
JOIN translation_projects p ON p.id = s.project_id AND p.name = 'Локализация мобильного банка'
JOIN users u ON u.email = 'vera.translator@demo.local'
WHERE s.position = 2;

INSERT INTO translations (segment_id, author_id, text, status, created_at, updated_at)
SELECT s.id, u.id, 'Transfer completed successfully', 'DRAFT', now(), now()
FROM source_segments s
JOIN translation_projects p ON p.id = s.project_id AND p.name = 'Локализация мобильного банка'
JOIN users u ON u.email = 'boris.translator@demo.local'
WHERE s.position = 3;

-- Замечание к отклонённому переводу
INSERT INTO editorial_comments (translation_id, text, status, created_at, updated_at)
SELECT t.id, 'В интерфейсе принято «verification code», а не просто «code»', 'OPEN', now(), now()
FROM translations t WHERE t.text = 'Enter the code from SMS';
