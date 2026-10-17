package ru.mtuci.translations.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "translation_projects")
public class TranslationProject extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "target_language", nullable = false, length = 20)
    private String targetLanguage;

    /** Владелец проекта — он же редактор, который согласует переводы. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }
}
