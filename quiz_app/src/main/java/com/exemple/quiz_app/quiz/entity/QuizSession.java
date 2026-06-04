package com.exemple.quiz_app.quiz.entity;

import com.exemple.quiz_app.auth.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_session", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "quiz_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    private LocalDateTime startTime;

    private LocalDateTime lastActivity;

    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.ACTIVE;

    public enum SessionStatus {
        ACTIVE, EXPIRED, COMPLETED
    }

    @PrePersist
    protected void onCreate() {
        startTime = LocalDateTime.now();
        lastActivity = LocalDateTime.now();
    }

    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    public boolean isExpired() {
        if (status != SessionStatus.ACTIVE) return true;
        if (quiz.getTimeLimit() == null) return false;

        LocalDateTime expiryTime = startTime.plusMinutes(quiz.getTimeLimit());
        return LocalDateTime.now().isAfter(expiryTime);
    }

    public long getRemainingSeconds() {
        if (quiz.getTimeLimit() == null) return -1;
        LocalDateTime expiryTime = startTime.plusMinutes(quiz.getTimeLimit());
        long remaining = java.time.Duration.between(LocalDateTime.now(), expiryTime).getSeconds();
        return Math.max(0, remaining);
    }

    public void markAsExpired() {
        this.status = SessionStatus.EXPIRED;
    }

    public void markAsCompleted() {
        this.status = SessionStatus.COMPLETED;
    }
}