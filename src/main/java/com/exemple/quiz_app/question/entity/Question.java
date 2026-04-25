package com.exemple.quiz_app.question.entity;

import com.exemple.quiz_app.quiz.entity.Quiz;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "question")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String enonce;

    @Column(length = 100)
    private String choixA;

    @Column(length = 100)
    private String choixB;

    @Column(length = 100)
    private String choixC;

    @Column(length = 100)
    private String choixD;

    @Column(length = 50)
    private String reponseCorrecte;

    private Integer points = 1;

    @Enumerated(EnumType.STRING)
    private QuestionType type = QuestionType.MCQ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_quiz", nullable = false)
    private Quiz quiz;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 🔥 Enumération des types de questions
    public enum QuestionType {
        MCQ,        // Choix multiples (A, B, C, D)
        TRUE_FALSE, // Vrai/Faux
        TEXT        // Question ouverte
    }

    // 🔥 Récupérer toutes les options pour un QCM
    public List<String> getAllOptions() {
        List<String> options = new ArrayList<>();
        if (choixA != null && !choixA.isEmpty()) options.add(choixA);
        if (choixB != null && !choixB.isEmpty()) options.add(choixB);
        if (choixC != null && !choixC.isEmpty()) options.add(choixC);
        if (choixD != null && !choixD.isEmpty()) options.add(choixD);
        return options;
    }

    // 🔥 Vérifier si une réponse est correcte
    public boolean checkAnswer(String studentAnswer) {
        if (studentAnswer == null) return false;

        if (type == QuestionType.MCQ) {
            return reponseCorrecte != null && reponseCorrecte.equalsIgnoreCase(studentAnswer.trim());
        } else if (type == QuestionType.TRUE_FALSE) {
            return reponseCorrecte != null && reponseCorrecte.equalsIgnoreCase(studentAnswer.trim());
        } else {
            String correct = getCorrectAnswerText();
            return correct != null && correct.equalsIgnoreCase(studentAnswer.trim());
        }
    }

    // 🔥 Récupérer le texte de la réponse correcte
    public String getCorrectAnswerText() {
        if (type == QuestionType.MCQ) {
            switch (reponseCorrecte != null ? reponseCorrecte.toUpperCase() : "") {
                case "A": return choixA;
                case "B": return choixB;
                case "C": return choixC;
                case "D": return choixD;
                default: return reponseCorrecte;
            }
        }
        return reponseCorrecte;
    }
}