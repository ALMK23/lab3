package ru.mtuci.translations.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mtuci.translations.exception.ConflictException;
import ru.mtuci.translations.model.User;
import ru.mtuci.translations.repository.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final Lookup lookup;
    private final Clock clock;

    public UserService(UserRepository userRepository, Lookup lookup, Clock clock) {
        this.userRepository = userRepository;
        this.lookup = lookup;
        this.clock = clock;
    }

    public List<User> findAll() {
        return userRepository.findAllByOrderByIdAsc();
    }

    public User findById(Long id) {
        return lookup.user(id);
    }

    @Transactional
    public User create(String name, String email) {
        requireUniqueEmail(email, null);
        User user = new User();
        user.setName(name.trim());
        user.setEmail(email.trim());
        Instant now = Instant.now(clock);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.saveAndFlush(user);
    }

    @Transactional
    public User update(Long id, String name, String email) {
        User user = lookup.user(id);
        if (name != null) {
            Validation.requireNotBlank(name, "name");
            user.setName(name.trim());
        }
        if (email != null) {
            Validation.requireNotBlank(email, "email");
            requireUniqueEmail(email, id);
            user.setEmail(email.trim());
        }
        user.setUpdatedAt(Instant.now(clock));
        return userRepository.saveAndFlush(user);
    }

    /**
     * Удаление пользователя. Его проекты (со всем содержимым) и его переводы (с замечаниями)
     * удаляются каскадно внешними ключами ON DELETE CASCADE — недействительных ссылок не остаётся.
     * Нельзя удалить автора, чьи переводы уже вошли в чужой проект на финальной стадии.
     */
    @Transactional
    public void delete(Long id) {
        User user = lookup.user(id);
        if (userRepository.hasTranslationsInFinishedForeignProjects(id)) {
            throw new ConflictException("Пользователь id=" + id
                    + " — автор переводов в проектах на стадии REVIEW/COMPLETED/ARCHIVED, удалить его нельзя");
        }
        userRepository.delete(user);
    }

    private void requireUniqueEmail(String email, Long selfId) {
        userRepository.findByEmailIgnoreCase(email.trim())
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new ConflictException("Пользователь с email " + email + " уже существует");
                });
    }
}
