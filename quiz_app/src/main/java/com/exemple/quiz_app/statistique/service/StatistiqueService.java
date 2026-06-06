package com.exemple.quiz_app.statistique.service;

import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.repository.UserRepository;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.question.entity.Question;
import com.exemple.quiz_app.question.repository.QuestionRepository;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.quiz.repository.QuizRepository;
import com.exemple.quiz_app.reponse.entity.Reponse;
import com.exemple.quiz_app.reponse.repository.ReponseRepository;
import com.exemple.quiz_app.resultat.entity.Resultat;
import com.exemple.quiz_app.resultat.repository.ResultatRepository;
import com.exemple.quiz_app.statistique.dto.StatistiqueDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatistiqueService {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private ResultatRepository resultatRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ReponseRepository reponseRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    // ========== STATISTIQUES POUR ENSEIGNANT ==========

    public StatistiqueDto getQuizStatistics(Long quizId) {
        User currentUser = authService.getCurrentUser();

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

        if (currentUser.getRole() != Role.ADMIN && !quiz.getEnseignant().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Vous n'etes pas autorise a voir les statistiques de ce quiz");
        }

        List<Resultat> resultats = resultatRepository.findByQuizIdOrderByScorePercentageDesc(quizId);
        List<Question> questions = questionRepository.findByQuizId(quizId);

        return StatistiqueDto.builder()
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitre())
                .quizTheme(quiz.getTheme())
                .enseignantNom(quiz.getEnseignant().getFirstName() + " " + quiz.getEnseignant().getLastName())
                .totalParticipants(resultats.size())
                .totalStudentsAllowed(quizRepository.countAllowedStudents(quizId))
                .totalQuestions(questions.size())
                .moyenneScore(calculerMoyenneScore(resultats))
                .meilleurScore(calculerMeilleurScore(resultats))
                .pireScore(calculerPireScore(resultats))
                .medianeScore(calculerMedianeScore(resultats))
                .scoreDistribution(calculerDistributionScores(resultats))
                .classement(calculerClassement(resultats))
                .top5(calculerTop5(resultats))
                .bottom5(calculerBottom5(resultats))
                .questionsStats(calculerStatsParQuestion(quizId, questions, resultats))
                .tempsMoyenReponse(calculerTempsMoyenReponse(resultats))
                .periodeDebut(quiz.getAvailableFrom())
                .periodeFin(quiz.getAvailableUntil())
                .build();
    }

    public List<StatistiqueDto> getTeacherDashboardStats() {
        User enseignant = authService.getCurrentUser();

        if (enseignant.getRole() != Role.ENSEIGNANT && enseignant.getRole() != Role.ADMIN) {
            throw new RuntimeException("Acces reserve aux enseignants");
        }

        List<Quiz> quizzes = quizRepository.findByEnseignant(enseignant);

        return quizzes.stream()
                .map(quiz -> getQuizStatistics(quiz.getId()))
                .collect(Collectors.toList());
    }

    public List<StatistiqueDto> getAllQuizzesStatistics() {
        User currentUser = authService.getCurrentUser();

        if (currentUser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Acces reserve aux administrateurs");
        }

        List<Quiz> quizzes = quizRepository.findAll();

        return quizzes.stream()
                .map(quiz -> getQuizStatistics(quiz.getId()))
                .collect(Collectors.toList());
    }

    // ========== STATISTIQUES POUR ETUDIANT ==========

    public StatistiqueDto getMyPerformance() {
        User student = authService.getCurrentUser();

        if (student.getRole() != Role.ETUDIANT && student.getRole() != Role.ADMIN) {
            throw new RuntimeException("Acces reserve aux etudiants");
        }

        List<Resultat> myResults = resultatRepository.findByStudentOrderByCompletedDateDesc(student)
                .stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsCompleted()))
                .collect(Collectors.toList());

        return StatistiqueDto.builder()
                .totalParticipants(myResults.size())
                .moyenneScore(calculerMoyenneScore(myResults))
                .meilleurScore(calculerMeilleurScore(myResults))
                .pireScore(calculerPireScore(myResults))
                .classement(Collections.emptyList())
                .build();
    }

    public StatistiqueDto.StudentStatDto getMyRanking(Long quizId) {
        User student = authService.getCurrentUser();

        if (student.getRole() != Role.ETUDIANT && student.getRole() != Role.ADMIN) {
            throw new RuntimeException("Acces reserve aux etudiants");
        }

        List<Resultat> results = resultatRepository.findByQuizIdOrderByScorePercentageDesc(quizId);

        int rang = 1;
        for (Resultat r : results) {
            if (r.getStudent().getId().equals(student.getId())) {
                break;
            }
            rang++;
        }

        Resultat myResult = results.stream()
                .filter(r -> r.getStudent().getId().equals(student.getId()))
                .findFirst()
                .orElse(null);

        if (myResult == null) {
            return null;
        }

        return StatistiqueDto.StudentStatDto.builder()
                .studentId(student.getId().longValue())
                .studentNom(student.getFirstName())
                .studentPrenom(student.getLastName())
                .scorePourcentage(myResult.getScorePercentage())
                .earnedPoints(myResult.getEarnedPoints())
                .totalPoints(myResult.getTotalPoints())
                .rang(rang)
                .grade(myResult.getGrade())
                .completedDate(myResult.getCompletedDate())
                .build();
    }

    // ========== STATISTIQUES GLOBALES (ADMIN) ==========

    public Map<String, Object> getGlobalStatistics() {
        User currentUser = authService.getCurrentUser();

        if (currentUser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Acces reserve aux administrateurs");
        }

        long totalUsers = userRepository.count();
        long totalStudents = userRepository.countByRole(Role.ETUDIANT);
        long totalTeachers = userRepository.countByRole(Role.ENSEIGNANT);
        long totalQuizzes = quizRepository.count();
        long totalPublishedQuizzes = quizRepository.countByStatus(Quiz.QuizStatus.PUBLISHED);
        long totalResults = resultatRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUtilisateurs", totalUsers);
        stats.put("totalEtudiants", totalStudents);
        stats.put("totalEnseignants", totalTeachers);
        stats.put("totalQuiz", totalQuizzes);
        stats.put("totalQuizPublies", totalPublishedQuizzes);
        stats.put("totalParticipations", totalResults);
        stats.put("tauxReussiteGlobal", calculerTauxReussiteGlobal());

        return stats;
    }

    // ========== METHODES DE CALCUL PRIVEES ==========

    private Double calculerMoyenneScore(List<Resultat> resultats) {
        return resultats.stream()
                .filter(r -> r.getScorePercentage() != null)
                .mapToDouble(Resultat::getScorePercentage)
                .average()
                .orElse(0.0);
    }

    private Double calculerMeilleurScore(List<Resultat> resultats) {
        return resultats.stream()
                .filter(r -> r.getScorePercentage() != null)
                .mapToDouble(Resultat::getScorePercentage)
                .max()
                .orElse(0.0);
    }

    private Double calculerPireScore(List<Resultat> resultats) {
        return resultats.stream()
                .filter(r -> r.getScorePercentage() != null)
                .mapToDouble(Resultat::getScorePercentage)
                .min()
                .orElse(0.0);
    }

    private Double calculerMedianeScore(List<Resultat> resultats) {
        List<Double> scores = resultats.stream()
                .filter(r -> r.getScorePercentage() != null)
                .map(Resultat::getScorePercentage)
                .sorted()
                .collect(Collectors.toList());

        if (scores.isEmpty()) return 0.0;

        int size = scores.size();
        if (size % 2 == 0) {
            return (scores.get(size / 2 - 1) + scores.get(size / 2)) / 2;
        } else {
            return scores.get(size / 2);
        }
    }

    private StatistiqueDto.ScoreDistributionDto calculerDistributionScores(List<Resultat> resultats) {
        int excellent = 0, tresBien = 0, bien = 0, assezBien = 0, moyen = 0, insuffisant = 0;

        for (Resultat r : resultats) {
            Double score = r.getScorePercentage();
            if (score == null) continue;

            if (score >= 90) excellent++;
            else if (score >= 80) tresBien++;
            else if (score >= 70) bien++;
            else if (score >= 60) assezBien++;
            else if (score >= 50) moyen++;
            else insuffisant++;
        }

        return StatistiqueDto.ScoreDistributionDto.builder()
                .excellent(excellent)
                .tresBien(tresBien)
                .bien(bien)
                .assezBien(assezBien)
                .moyen(moyen)
                .insuffisant(insuffisant)
                .build();
    }

    private List<StatistiqueDto.StudentStatDto> calculerClassement(List<Resultat> resultats) {
        List<StatistiqueDto.StudentStatDto> classement = new ArrayList<>();
        int rang = 1;

        for (Resultat r : resultats) {
            User student = r.getStudent();
            classement.add(StatistiqueDto.StudentStatDto.builder()
                    .rang(rang++)
                    .studentId(student.getId().longValue())
                    .studentNom(student.getFirstName())
                    .studentPrenom(student.getLastName())
                    .studentEmail(student.getEmail())
                    .scorePourcentage(r.getScorePercentage())
                    .earnedPoints(r.getEarnedPoints())
                    .totalPoints(r.getTotalPoints())
                    .grade(r.getGrade())
                    .completedDate(r.getCompletedDate())
                    .build());
        }
        return classement;
    }

    private List<StatistiqueDto.StudentStatDto> calculerTop5(List<Resultat> resultats) {
        return calculerClassement(resultats).stream()
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<StatistiqueDto.StudentStatDto> calculerBottom5(List<Resultat> resultats) {
        List<StatistiqueDto.StudentStatDto> classement = calculerClassement(resultats);
        int size = classement.size();
        return classement.stream()
                .skip(Math.max(0, size - 5))
                .collect(Collectors.toList());
    }

    private List<StatistiqueDto.QuestionStatDto> calculerStatsParQuestion(Long quizId, List<Question> questions, List<Resultat> resultats) {
        List<StatistiqueDto.QuestionStatDto> stats = new ArrayList<>();

        for (Question q : questions) {
            int totalReponses = 0;
            int reponsesCorrectes = 0;
            Map<String, Integer> reponsesDistribution = new HashMap<>();

            for (Resultat r : resultats) {
                List<Reponse> reponses = reponseRepository.findByStudentAndQuestionQuizId(r.getStudent(), quizId);
                Reponse reponse = reponses.stream()
                        .filter(rep -> rep.getQuestion().getId().equals(q.getId()))
                        .findFirst()
                        .orElse(null);

                if (reponse != null) {
                    totalReponses++;
                    if (Boolean.TRUE.equals(reponse.getIsCorrect())) {
                        reponsesCorrectes++;
                    }
                    String answer = reponse.getStudentAnswer();
                    if (answer != null && !answer.isEmpty()) {
                        reponsesDistribution.merge(answer, 1, Integer::sum);
                    }
                }
            }

            double tauxReussite = totalReponses > 0 ? (reponsesCorrectes * 100.0) / totalReponses : 0;

            stats.add(StatistiqueDto.QuestionStatDto.builder()
                    .questionId(q.getId())
                    .questionText(q.getEnonce())
                    .points(q.getPoints())
                    .totalReponses(totalReponses)
                    .reponsesCorrectes(reponsesCorrectes)
                    .tauxReussite(tauxReussite)
                    .reponsesDistribution(reponsesDistribution)
                    .build());
        }
        return stats;
    }

    private Double calculerTempsMoyenReponse(List<Resultat> resultats) {
        return resultats.stream()
                .filter(r -> r.getStartedAt() != null && r.getCompletedDate() != null)
                .mapToDouble(r -> Duration.between(r.getStartedAt(), r.getCompletedDate()).getSeconds())
                .average()
                .orElse(0.0);
    }

    private List<StatistiqueDto.StudentStatDto> calculerClassementEtudiant(User student, List<Resultat> myResults) {
        List<StatistiqueDto.StudentStatDto> classement = new ArrayList<>();

        for (Resultat r : myResults) {
            List<Resultat> quizResults = resultatRepository.findByQuizIdOrderByScorePercentageDesc(r.getQuizId());
            int rang = 1;
            for (Resultat rr : quizResults) {
                if (rr.getStudent().getId().equals(student.getId())) break;
                rang++;
            }

            classement.add(StatistiqueDto.StudentStatDto.builder()
                    .rang(rang)
                    .studentId(student.getId().longValue())
                    .studentNom(student.getFirstName())
                    .studentPrenom(student.getLastName())
                    .scorePourcentage(r.getScorePercentage())
                    .earnedPoints(r.getEarnedPoints())
                    .totalPoints(r.getTotalPoints())
                    .grade(r.getGrade())
                    .completedDate(r.getCompletedDate())
                    .build());
        }
        return classement;
    }

    private Double calculerTauxReussiteGlobal() {
        List<Resultat> allResults = resultatRepository.findAll();
        long total = allResults.size();
        if (total == 0) return 0.0;

        long reussis = allResults.stream()
                .filter(r -> r.getScorePercentage() != null && r.getScorePercentage() >= 60)
                .count();

        return (reussis * 100.0) / total;
    }
}