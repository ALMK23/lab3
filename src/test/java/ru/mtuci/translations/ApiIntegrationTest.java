package ru.mtuci.translations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты на настоящем PostgreSQL (Testcontainers, нужен запущенный Docker;
 * без Docker тесты пропускаются). В контейнере выполняется тот же db/init.sql, что и в проде,
 * поэтому приложение работает от пользователя с ограниченными правами.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ApiIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withCopyFileToContainer(MountableFile.forHostPath("db/init.sql"), "/db/init.sql");

    private static final AtomicInteger SEQ = new AtomicInteger();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) throws Exception {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
            var result = POSTGRES.execInContainer("psql", "-U", POSTGRES.getUsername(), "-d", "postgres",
                    "-v", "ON_ERROR_STOP=1", "-v", "owner_password=owner_pw", "-v", "app_password=app_pw",
                    "-f", "/db/init.sql");
            if (result.getExitCode() != 0) {
                throw new IllegalStateException("init.sql failed: " + result.getStderr());
            }
        }
        registry.add("DB_HOST", POSTGRES::getHost);
        registry.add("DB_PORT", () -> POSTGRES.getMappedPort(5432));
        registry.add("DB_NAME", () -> "translations");
        registry.add("DB_SCHEMA", () -> "app");
        registry.add("DB_OWNER_USER", () -> "translations_owner");
        registry.add("DB_OWNER_PASSWORD", () -> "owner_pw");
        registry.add("DB_APP_USER", () -> "translations_app");
        registry.add("DB_APP_PASSWORD", () -> "app_pw");
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    // ------------------------------------------------------------------ CRUD

    @Test
    void crudAndCascadeDelete() throws Exception {
        long owner = user("Владелец");
        long project = id(send("POST", "/api/translation-projects",
                "{\"name\":\"Сайт\",\"targetLanguage\":\"en\",\"ownerId\":" + owner + "}", 201)
                .andExpect(jsonPath("$.status").value("DRAFT")));
        long segment = id(send("POST", "/api/source-segments",
                "{\"projectId\":" + project + ",\"sourceText\":\"Привет\"}", 201)
                .andExpect(jsonPath("$.position").value(1)));
        send("POST", "/api/source-segments", "{\"projectId\":" + project + ",\"sourceText\":\"Дубль\",\"position\":1}", 409);

        send("PATCH", "/api/translation-projects/" + project, "{\"name\":\"Сайт v2\"}", 200)
                .andExpect(jsonPath("$.name").value("Сайт v2"));
        send("PATCH", "/api/translation-projects/" + project, "{\"status\":\"COMPLETED\"}", 400);

        start(project, owner);
        long translator = user("Переводчик");
        long translation = translation(segment, translator, "Hello");
        send("POST", "/api/translations/" + translation + "/submit", actor(translator), 200);
        long comment = id(send("POST", "/api/editorial-comments",
                "{\"translationId\":" + translation + ",\"text\":\"Проверьте пунктуацию\"}", 201));

        send("DELETE", "/api/translation-projects/" + project, null, 204);
        send("GET", "/api/source-segments/" + segment, null, 404);
        send("GET", "/api/translations/" + translation, null, 404);
        send("GET", "/api/editorial-comments/" + comment, null, 404);
        send("GET", "/api/users/" + owner, null, 200);
    }

    @Test
    void duplicateEmailIsConflict() throws Exception {
        String email = "same" + SEQ.incrementAndGet() + "@example.com";
        send("POST", "/api/users", "{\"name\":\"A\",\"email\":\"" + email + "\"}", 201);
        send("POST", "/api/users", "{\"name\":\"B\",\"email\":\"" + email.toUpperCase() + "\"}", 409);
    }

    @Test
    void referencesToMissingObjectsAreBadRequest() throws Exception {
        send("POST", "/api/translation-projects", "{\"name\":\"X\",\"targetLanguage\":\"en\",\"ownerId\":999999}", 400);
        send("POST", "/api/source-segments", "{\"projectId\":999999,\"sourceText\":\"X\"}", 400);
        send("GET", "/api/translations/999999", null, 404);
    }

    // ------------------------------------------------------- бизнес-операции

    @Test
    void fullReviewWorkflow() throws Exception {
        long editor = user("Редактор");
        long translator = user("Переводчик");
        long project = project(editor);
        long seg1 = segment(project, "Добро пожаловать");
        long seg2 = segment(project, "До свидания");

        // переводы до запуска проекта создавать нельзя
        send("POST", "/api/translations",
                "{\"segmentId\":" + seg1 + ",\"authorId\":" + translator + ",\"text\":\"Welcome\"}", 409);
        send("POST", "/api/translation-projects/" + project + "/start", actor(translator), 403);
        start(project, editor);

        long t1 = translation(seg1, translator, "Welcome");
        long t2 = translation(seg2, translator, "Goodbye");

        send("POST", "/api/translations/" + t1 + "/submit", actor(editor), 403);
        send("POST", "/api/translations/" + t1 + "/submit", actor(translator), 200)
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        // отклонение: перевод и замечание меняются вместе
        String rejected = send("POST", "/api/translations/" + t1 + "/reject",
                "{\"userId\":" + editor + ",\"comment\":\"Добавьте восклицательный знак\"}", 200)
                .andExpect(jsonPath("$.translation.status").value("REJECTED"))
                .andExpect(jsonPath("$.comment.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        long c1 = mapper.readTree(rejected).at("/comment/id").asLong();

        send("POST", "/api/translations/" + t1 + "/submit", actor(translator), 409);   // открытое замечание
        send("POST", "/api/editorial-comments/" + c1 + "/resolve",
                "{\"userId\":" + translator + ",\"translationText\":\"Welcome!\"}", 200)
                .andExpect(jsonPath("$.comment.status").value("RESOLVED"))
                .andExpect(jsonPath("$.translation.text").value("Welcome!"));
        send("POST", "/api/translations/" + t1 + "/submit", actor(translator), 200);

        send("POST", "/api/translations/" + t1 + "/approve", actor(translator), 403);
        send("POST", "/api/translations/" + t1 + "/approve", actor(editor), 200)
                .andExpect(jsonPath("$.project.status").value("IN_PROGRESS"));
        send("POST", "/api/translation-projects/" + project + "/complete", actor(editor), 409);

        send("POST", "/api/translations/" + t2 + "/submit", actor(translator), 200);
        long c2 = id(send("POST", "/api/editorial-comments",
                "{\"translationId\":" + t2 + ",\"text\":\"Вопрос\"}", 201));
        send("POST", "/api/translations/" + t2 + "/approve", actor(editor), 409);
        send("POST", "/api/editorial-comments/" + c2 + "/dismiss", actor(editor), 200)
                .andExpect(jsonPath("$.status").value("REJECTED"));
        send("POST", "/api/translations/" + t2 + "/approve", actor(editor), 200)
                .andExpect(jsonPath("$.translation.status").value("APPROVED"))
                .andExpect(jsonPath("$.project.status").value("REVIEW"));

        send("POST", "/api/source-segments", "{\"projectId\":" + project + ",\"sourceText\":\"Поздно\"}", 409);
        send("POST", "/api/translation-projects/" + project + "/complete", actor(editor), 200)
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        send("DELETE", "/api/users/" + translator, null, 409);
        send("POST", "/api/translation-projects/" + project + "/archive", actor(editor), 200)
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
        send("PATCH", "/api/translation-projects/" + project, "{\"name\":\"Новое\"}", 409);
    }

    @Test
    void onlyOneActiveTranslationPerSegment() throws Exception {
        long editor = user("Редактор");
        long a = user("Автор А");
        long b = user("Автор Б");
        long project = project(editor);
        long seg = segment(project, "Текст");
        start(project, editor);
        long ta = translation(seg, a, "Text A");
        long tb = translation(seg, b, "Text B");
        send("POST", "/api/translations/" + ta + "/submit", actor(a), 200);
        send("POST", "/api/translations/" + tb + "/submit", actor(b), 409);
    }

    @Test
    void editorCannotApproveOwnTranslation() throws Exception {
        long editor = user("Редактор");
        long project = project(editor);
        long seg = segment(project, "Текст");
        start(project, editor);
        long t = translation(seg, editor, "Text");
        send("POST", "/api/translations/" + t + "/submit", actor(editor), 200);
        send("POST", "/api/translations/" + t + "/approve", actor(editor), 403);
    }

    @Test
    void startRequiresSegments() throws Exception {
        long editor = user("Редактор");
        long project = project(editor);
        send("POST", "/api/translation-projects/" + project + "/start", actor(editor), 409);
    }

    // ------------------------------------------------------------- помощники

    private ResultActions send(String method, String url, String json, int expected) throws Exception {
        var builder = switch (method) {
            case "GET" -> get(url);
            case "POST" -> post(url);
            case "PATCH" -> patch(url);
            case "DELETE" -> delete(url);
            default -> throw new IllegalArgumentException(method);
        };
        if (json != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(builder).andExpect(status().is(expected));
    }

    private long id(ResultActions actions) throws Exception {
        JsonNode node = mapper.readTree(actions.andReturn().getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private String actor(long userId) {
        return "{\"userId\":" + userId + "}";
    }

    private long user(String name) throws Exception {
        return id(send("POST", "/api/users",
                "{\"name\":\"" + name + "\",\"email\":\"u" + SEQ.incrementAndGet() + "@test.local\"}", 201));
    }

    private long project(long owner) throws Exception {
        return id(send("POST", "/api/translation-projects",
                "{\"name\":\"Проект\",\"targetLanguage\":\"en\",\"ownerId\":" + owner + "}", 201));
    }

    private long segment(long project, String text) throws Exception {
        return id(send("POST", "/api/source-segments", "{\"projectId\":" + project + ",\"sourceText\":\"" + text + "\"}", 201));
    }

    private long translation(long segment, long author, String text) throws Exception {
        return id(send("POST", "/api/translations",
                "{\"segmentId\":" + segment + ",\"authorId\":" + author + ",\"text\":\"" + text + "\"}", 201));
    }

    private void start(long project, long owner) throws Exception {
        send("POST", "/api/translation-projects/" + project + "/start", actor(owner), 200)
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }
}
