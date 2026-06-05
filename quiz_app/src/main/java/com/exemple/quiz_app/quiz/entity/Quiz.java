package com.exemple.quiz_app.quiz.entity;

import com.exemple.quiz_app.auth.model.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.exemple.quiz_app.classe.entity.Classe;
import com.exemple.quiz_app.matiere.entity.Matiere;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String titre;

    @Column(length = 100)
    private String theme;

    @Column(length = 1000)
    private String description;

    @Column(length = 30)
    private String difficulty;

    private Integer questionCount = 0;

    private LocalDateTime availableFrom;

    private LocalDateTime availableUntil;

    private Integer timeLimit;

    @Enumerated(EnumType.STRING)
    private QuizStatus status = QuizStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    private CreationType creationType = CreationType.MANUAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_enseignant", nullable = false)
    private User enseignant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classe_id")
    private Classe classe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matiere_id")
    private Matiere matiere;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizStudent> allowedStudents = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
    private Long deletedBy;

    public enum QuizStatus {
        DRAFT, PUBLISHED, EXPIRED, DELETED, ARCHIVED
    }

    public enum CreationType {
        MANUAL, AI
    }

    public boolean isModifiable() {
        return this.status == QuizStatus.DRAFT;
    }

    public boolean isDeletable() {
        return this.status == QuizStatus.DRAFT || this.status == QuizStatus.EXPIRED;
    }

    public boolean isAvailable() {
        LocalDateTime now = LocalDateTime.now();
        return this.status == QuizStatus.PUBLISHED
                && (availableFrom == null || !now.isBefore(availableFrom))
                && (availableUntil == null || !now.isAfter(availableUntil));
    }

    public boolean isDeleted() {
        return this.status == QuizStatus.DELETED;
    }

    public void setDeletedBy(java.math.BigInteger deletedBy) {
        if (deletedBy != null) {
            this.deletedBy = deletedBy.longValue();
        } else {
            this.deletedBy = null;
        }
    }
}
