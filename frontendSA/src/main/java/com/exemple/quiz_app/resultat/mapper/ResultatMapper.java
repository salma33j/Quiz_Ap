package com.exemple.quiz_app.resultat.mapper;

import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.resultat.dto.ResultatDto;
import com.exemple.quiz_app.resultat.dto.ResultatRequestDto;
import com.exemple.quiz_app.resultat.entity.Resultat;
import org.springframework.stereotype.Component;

@Component
public class ResultatMapper {

    // ========== MÉTHODES PRIVÉES ==========

    private Long getQuizId(Resultat resultat) {
        return resultat.getQuiz() != null ? resultat.getQuiz().getId() : null;
    }

    private String getQuizTitle(Resultat resultat) {
        return resultat.getQuiz() != null ? resultat.getQuiz().getTitre() : null;
    }

    private String getQuizTheme(Resultat resultat) {
        return resultat.getQuiz() != null ? resultat.getQuiz().getTheme() : null;
    }

    private Long getStudentId(Resultat resultat) {
        return resultat.getStudent() != null ? resultat.getStudent().getId().longValue() : null;
    }

    private String getStudentName(Resultat resultat) {
        if (resultat.getStudent() == null) return null;
        return resultat.getStudent().getFirstName() + " " + resultat.getStudent().getLastName();
    }

    private String getClassName(Resultat resultat) {
        if (resultat.getStudent() != null && resultat.getStudent().getClasse() != null) {
            return resultat.getStudent().getClasse().getName();
        }
        if (resultat.getQuiz() != null && resultat.getQuiz().getClasse() != null) {
            return resultat.getQuiz().getClasse().getName();
        }
        return null;
    }

    private Long getClassId(Resultat resultat) {
        if (resultat.getStudent() != null && resultat.getStudent().getClasse() != null) {
            return resultat.getStudent().getClasse().getId();
        }
        if (resultat.getQuiz() != null && resultat.getQuiz().getClasse() != null) {
            return resultat.getQuiz().getClasse().getId();
        }
        return null;
    }

    // ========== ENTITY VERS DTO ==========

    /**
     * Convertir Entity Resultat → ResultatDto
     */
    public ResultatDto toDto(Resultat resultat) {
        if (resultat == null) {
            return null;
        }

        return ResultatDto.builder()
                .id(resultat.getId())
                .quizId(getQuizId(resultat))
                .quizTitle(getQuizTitle(resultat))
                .quizTheme(getQuizTheme(resultat))
                .studentId(getStudentId(resultat))
                .studentName(getStudentName(resultat))
                .studentFirstName(resultat.getStudent() != null ? resultat.getStudent().getFirstName() : null)
                .studentLastName(resultat.getStudent() != null ? resultat.getStudent().getLastName() : null)
                .studentEmail(resultat.getStudent() != null ? resultat.getStudent().getEmail() : null)
                .cne(resultat.getStudent() != null ? resultat.getStudent().getCne() : null)
                .codeApoge(resultat.getStudent() != null ? resultat.getStudent().getCodeApoge() : null)
                .classId(getClassId(resultat))
                .classeId(getClassId(resultat))
                .className(getClassName(resultat))
                .classeName(getClassName(resultat))
                .subjectName(getQuizTheme(resultat))
                .matiereName(getQuizTheme(resultat))
                .score(resultat.getScore())
                .totalPoints(resultat.getTotalPoints())
                .earnedPoints(resultat.getEarnedPoints())
                .scorePercentage(resultat.getScorePercentage())
                .isCompleted(resultat.getIsCompleted())
                .feedbackIa(resultat.getFeedbackIa())
                .strengths(resultat.getStrengths())
                .weaknesses(resultat.getWeaknesses())
                .recommendations(resultat.getRecommendations())
                .suggestedQuiz(resultat.getSuggestedQuiz())
                .grade(resultat.getGrade())
                .startedAt(resultat.getStartedAt())
                .completedDate(resultat.getCompletedDate())
                .build();
    }

    // ========== DTO VERS ENTITY ==========

    /**
     * Convertir ResultatRequestDto → Entity Resultat
     */
    public Resultat toEntity(ResultatRequestDto requestDto, Quiz quiz, User student) {
        if (requestDto == null) {
            return null;
        }

        Resultat resultat = new Resultat();
        resultat.setQuiz(quiz);
        resultat.setStudent(student);
        resultat.setScore(requestDto.getScore());
        resultat.setTotalPoints(requestDto.getTotalPoints());
        resultat.setEarnedPoints(requestDto.getEarnedPoints());
        resultat.setScorePercentage(requestDto.getScorePercentage());
        resultat.setIsCompleted(requestDto.getIsCompleted() != null ? requestDto.getIsCompleted() : false);

        if (requestDto.getCompletedDate() != null) {
            resultat.setCompletedDate(requestDto.getCompletedDate());
        }

        return resultat;
    }

    // ========== MISE À JOUR ENTITY ==========

    /**
     * Mettre à jour une entité existante avec les données du DTO
     */
    public void updateEntity(ResultatRequestDto requestDto, Resultat resultat) {
        if (requestDto == null || resultat == null) {
            return;
        }

        if (requestDto.getScore() != null) {
            resultat.setScore(requestDto.getScore());
        }
        if (requestDto.getTotalPoints() != null) {
            resultat.setTotalPoints(requestDto.getTotalPoints());
        }
        if (requestDto.getEarnedPoints() != null) {
            resultat.setEarnedPoints(requestDto.getEarnedPoints());
        }
        if (requestDto.getScorePercentage() != null) {
            resultat.setScorePercentage(requestDto.getScorePercentage());
        }
        if (requestDto.getIsCompleted() != null) {
            resultat.setIsCompleted(requestDto.getIsCompleted());
        }
        if (requestDto.getCompletedDate() != null) {
            resultat.setCompletedDate(requestDto.getCompletedDate());
        }
    }
}
