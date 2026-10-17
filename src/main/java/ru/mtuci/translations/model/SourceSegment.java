package ru.mtuci.translations.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "source_segments")
public class SourceSegment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private TranslationProject project;

    @Column(name = "source_text", nullable = false, columnDefinition = "text")
    private String sourceText;

    @Column(nullable = false)
    private Integer position;

    public TranslationProject getProject() { return project; }
    public void setProject(TranslationProject project) { this.project = project; }

    public String getSourceText() { return sourceText; }
    public void setSourceText(String sourceText) { this.sourceText = sourceText; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}
