package com.exemple.quiz_app.reponse.entity;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.question.entity.Question;
import com.exemple.quiz_app.quiz.entity.Quiz;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
@Entity
@Table(name = "reponses", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "question_id", "quiz_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Reponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;
    @Column(name = "student_answer", columnDefinition = "TEXT")
    private String studentAnswer;
    @Column(name = "is_correct")
    private Boolean isCorrect;
    @Column(name = "points_earned")
    private Integer pointsEarned;
    @CreatedDate
    @Column(name = "answered_at", updatable = false)
    private LocalDateTime answeredAt;
}