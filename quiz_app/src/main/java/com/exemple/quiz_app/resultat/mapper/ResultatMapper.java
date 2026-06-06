package com.exemple.quiz_app.resultat.mapper;

import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.resultat.dto.ResultatDto;
import com.exemple.quiz_app.resultat.dto.ResultatRequestDto;
import com.exemple.quiz_app.resultat.entity.Resultat;
import org.springframework.stereotype.Component;

@Component
public class ResultatMapper {

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

        String firstName = resultat.getStudent().getFirstName() != null
                ? resultat.getStudent().getFirstName()
                : "";

        String lastName = resultat.getStudent().getLastName() != null
                ? resultat.getStudent().getLastName()
                : "";

        return (firstName + " " + lastName).trim();
    }

    private String getStudentFirstName(Resultat resultat) {
        return resultat.getStudent() != null
                ? resultat.getStudent().getFirstName()
                : null;
    }

    private String getStudentLastName(Resultat resultat) {
        return resultat.getStudent() != null
                ? resultat.getStudent().getLastName()
                : null;
    }

    private String getCne(Resultat resultat) {
        return resultat.getStudent() != null
                ? resultat.getStudent().getCne()
                : null;
    }

    private String getCodeApogee(Resultat resultat) {
        return resultat.getStudent() != null
                ? resultat.getStudent().getCodeApoge()
                : null;
    }

    private String getClassName(Resultat resultat) {
        if (resultat.getStudent() == null || resultat.getStudent().getClasse() == null) {
            return "Classe non définie";
        }

        return resultat.getStudent().getClasse().getName();
    }



    private java.time.LocalDateTime getAvailableUntil(Resultat resultat) {
        return resultat.getQuiz() != null ? resultat.getQuiz().getAvailableUntil() : null;
    }


    private String getSubjectName(Resultat resultat) {
        if (resultat.getQuiz() == null) {
            return "Matiere non definie";
        }

        if (resultat.getQuiz().getMatiere() != null
                && resultat.getQuiz().getMatiere().getNom() != null
                && !resultat.getQuiz().getMatiere().getNom().trim().isEmpty()) {
            return resultat.getQuiz().getMatiere().getNom().trim();
        }

        if (resultat.getQuiz().getTheme() != null && !resultat.getQuiz().getTheme().trim().isEmpty()) {
            return resultat.getQuiz().getTheme().trim();
        }

        return "Matiere non definie";
    }
    private Double getNoteSur20(Resultat resultat) {
        if (resultat.getScorePercentage() == null) {
            return 0.0;
        }

        return (resultat.getScorePercentage() * 20.0) / 100.0;
    }

    private String getMention(Resultat resultat) {
        if (resultat.getScorePercentage() == null) {
            return "Insuffisant";
        }

        double percentage = resultat.getScorePercentage();

        if (percentage >= 80) {
            return "Très bien";
        }

        if (percentage >= 70) {
            return "Bien";
        }

        if (percentage >= 60) {
            return "Assez bien";
        }

        if (percentage >= 50) {
            return "Passable";
        }

        return "Insuffisant";
    }

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
                .studentFirstName(getStudentFirstName(resultat))
                .studentLastName(getStudentLastName(resultat))

                .cne(getCne(resultat))
                .codeApogee(getCodeApogee(resultat))

                .className(getClassName(resultat))
                .subjectName(getSubjectName(resultat))

                .noteSur20(getNoteSur20(resultat))
                .mention(getMention(resultat))

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
                .availableUntil(getAvailableUntil(resultat))

                .build();
    }

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
        resultat.setIsCompleted(
                requestDto.getIsCompleted() != null
                        ? requestDto.getIsCompleted()
                        : false
        );

        if (requestDto.getCompletedDate() != null) {
            resultat.setCompletedDate(requestDto.getCompletedDate());
        }

        return resultat;
    }

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