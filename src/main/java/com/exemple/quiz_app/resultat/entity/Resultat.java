package com.exemple.quiz_app.resultat.entity;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.quiz.entity.Quiz;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
@Entity
@Table(name = "resultats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "quiz_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Resultat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;
    @Column(name = "score")
    private Double score;
    @Column(name = "total_points")
    private Integer totalPoints;
    @Column(name = "earned_points")
    private Integer earnedPoints;
    @Column(name = "score_percentage")
    private Double scorePercentage;
    @Column(name = "is_completed")
    private Boolean isCompleted = false;
    @Column(name = "feedback_ia", columnDefinition = "TEXT")
    private String feedbackIa;
    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;
    @Column(name = "weaknesses", columnDefinition = "TEXT")
    private String weaknesses;
    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;
    @Column(name = "suggested_quiz")
    private String suggestedQuiz;
    @Column(name = "grade")
    private String grade;
    @CreatedDate
    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;
    @LastModifiedDate
    @Column(name = "completed_date")
    private LocalDateTime completedDate;
    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
        isCompleted = false;
    }
}
