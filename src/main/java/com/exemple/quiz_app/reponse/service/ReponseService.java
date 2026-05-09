package com.exemple.quiz_app.reponse.service;
import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.question.entity.Question;
import com.exemple.quiz_app.question.repository.QuestionRepository;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.quiz.repository.QuizRepository;
import com.exemple.quiz_app.quiz.repository.QuizStudentRepository;
import com.exemple.quiz_app.reponse.dto.ReponseDto;
import com.exemple.quiz_app.reponse.dto.ReponseRequestDto;  // ✅ Renommé
import com.exemple.quiz_app.reponse.entity.Reponse;
import com.exemple.quiz_app.reponse.repository.ReponseRepository;
import com.exemple.quiz_app.resultat.dto.ResultatRequestDto;  // ✅ Renommé
import com.exemple.quiz_app.resultat.service.ResultatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class ReponseService {
    private final ReponseRepository reponseRepository;
    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;
    private final QuizStudentRepository quizStudentRepository;
    private final ResultatService resultatService;
    private final AuthService authService;
    @Transactional
    public ReponseDto saveOrUpdateReponse(ReponseRequestDto request) {
        User currentUser = authService.getCurrentUser();
        if (currentUser.getRole() != Role.ETUDIANT && currentUser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Seuls les étudiants peuvent répondre aux quiz");
        }
        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));
        if (!quiz.isAvailable()) {
            throw new RuntimeException("Ce quiz n'est pas disponible actuellement");
        }
        boolean isAuthorized = quizStudentRepository.existsByQuizAndStudent(quiz, currentUser);
        if (!isAuthorized && currentUser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Vous n'êtes pas autorisé à participer à ce quiz");
        }
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question non trouvée"));
        if (!question.getQuiz().getId().equals(quiz.getId())) {
            throw new RuntimeException("Cette question n'appartient pas au quiz spécifié");
        }
        boolean alreadyAnswered = reponseRepository.existsByStudentAndQuestionAndQuiz(
                currentUser, question, quiz
        );
        boolean isCorrect = question.checkAnswer(request.getStudentAnswer());
        Integer pointsEarned = isCorrect ? question.getPoints() : 0;
        Reponse reponse;
        if (alreadyAnswered) {
            reponse = reponseRepository.findByStudentAndQuestion(currentUser, question)
                    .orElseThrow(() -> new RuntimeException("Réponse non trouvée"));
            reponse.setStudentAnswer(request.getStudentAnswer());
            reponse.setIsCorrect(isCorrect);
            reponse.setPointsEarned(pointsEarned);
        } else {
            reponse = new Reponse();
            reponse.setQuiz(quiz);
            reponse.setQuestion(question);
            reponse.setStudent(currentUser);
            reponse.setStudentAnswer(request.getStudentAnswer());
            reponse.setIsCorrect(isCorrect);
            reponse.setPointsEarned(pointsEarned);
        }
        Reponse savedReponse = reponseRepository.save(reponse);
        updateResultat(currentUser, quiz);
        return mapToResponseDto(savedReponse);
    }
    @Transactional
    public List<ReponseDto> saveAllReponses(List<ReponseRequestDto> requests) {
        List<ReponseDto> responses = requests.stream()
                .map(this::saveOrUpdateReponse)
                .collect(Collectors.toList());
        if (!requests.isEmpty()) {
            Long quizId = requests.get(0).getQuizId();
            Quiz quiz = quizRepository.findById(quizId).orElse(null);
            if (quiz != null) {
                User currentUser = authService.getCurrentUser();
                markQuizAsCompleted(currentUser, quiz);
            }
        }
        return responses;
    }
    /**
     * Mettre à jour le résultat après chaque réponse
     */
    private void updateResultat(User student, Quiz quiz) {
        // Calculer le score total
        Integer totalPointsEarned = reponseRepository.sumPointsEarnedByStudentAndQuiz(student, quiz);
        if (totalPointsEarned == null) {
            totalPointsEarned = 0;
        }
        // Récupérer toutes les questions du quiz
        List<Question> questions = questionRepository.findByQuizId(quiz.getId());
        int totalPointsPossible = questions.stream().mapToInt(Question::getPoints).sum();
        double scorePercentage = totalPointsPossible > 0 ?
                (totalPointsEarned * 100.0) / totalPointsPossible : 0;
        // ✅ Utilisation de ResultatRequestDto (renommé)
        ResultatRequestDto resultatRequest = new ResultatRequestDto();
        resultatRequest.setQuizId(quiz.getId());
        resultatRequest.setScore((double) totalPointsEarned);
        resultatRequest.setTotalPoints(totalPointsPossible);
        resultatRequest.setEarnedPoints(totalPointsEarned);
        resultatRequest.setScorePercentage(scorePercentage);
        resultatService.saveOrUpdateResultat(resultatRequest);
    }
    /**
     * Marquer le quiz comme complété (toutes les questions ont été répondues)
     */
    private void markQuizAsCompleted(User student, Quiz quiz) {
        List<Question> questions = questionRepository.findByQuizId(quiz.getId());
        List<Reponse> reponses = reponseRepository.findByStudentAndQuiz(student, quiz);
        boolean isCompleted = reponses.size() == questions.size();
        if (isCompleted) {
            // Calculer le score final
            Integer totalPointsEarned = reponseRepository.sumPointsEarnedByStudentAndQuiz(student, quiz);
            if (totalPointsEarned == null) {
                totalPointsEarned = 0;
            }
            int totalPointsPossible = questions.stream().mapToInt(Question::getPoints).sum();
            double scorePercentage = totalPointsPossible > 0 ?
                    (totalPointsEarned * 100.0) / totalPointsPossible : 0;
            // ✅ Utilisation de ResultatRequestDto (renommé)
            ResultatRequestDto resultatRequest = new ResultatRequestDto();
            resultatRequest.setQuizId(quiz.getId());
            resultatRequest.setIsCompleted(true);
            resultatRequest.setCompletedDate(LocalDateTime.now());
            resultatRequest.setScore((double) totalPointsEarned);
            resultatRequest.setEarnedPoints(totalPointsEarned);
            resultatRequest.setTotalPoints(totalPointsPossible);
            resultatRequest.setScorePercentage(scorePercentage);
            resultatService.saveOrUpdateResultat(resultatRequest);
        }
    }
    public List<ReponseDto> getReponsesByStudentAndQuiz(Long quizId) {
        User currentUser = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));
        List<Reponse> reponses = reponseRepository.findByStudentAndQuiz(currentUser, quiz);
        return reponses.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    public List<ReponseDto> getReponsesByQuiz(Long quizId) {
        User currentUser = authService.getCurrentUser();
        if (currentUser.getRole() != Role.ENSEIGNANT && currentUser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Accès non autorisé. Seuls les enseignants peuvent voir toutes les réponses.");
        }
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));
        List<Reponse> reponses = reponseRepository.findByQuizWithStudent(quiz);
        return reponses.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    public List<ReponseDto> getReponsesByStudentIdAndQuizId(Long studentId, Long quizId) {
        User currentUser = authService.getCurrentUser();
        if (currentUser.getRole() != Role.ENSEIGNANT && currentUser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Accès non autorisé");
        }
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));
        throw new UnsupportedOperationException("À implémenter avec le service User");
    }
    @Transactional
    public void deleteReponsesByStudentAndQuiz(Long quizId) {
        User currentUser = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));
        if (currentUser.getRole() != Role.ETUDIANT && currentUser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Accès non autorisé");
        }
        reponseRepository.deleteByStudentAndQuiz(currentUser, quiz);
        resultatService.deleteResultatByStudentAndQuiz(quizId);
    }
    public boolean isQuizCompleted(Long quizId) {
        User currentUser = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));
        List<Question> questions = questionRepository.findByQuizId(quiz.getId());
        List<Reponse> reponses = reponseRepository.findByStudentAndQuiz(currentUser, quiz);
        return reponses.size() == questions.size();
    }
    public Integer getCurrentScore(Long quizId) {
        User currentUser = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));
        return reponseRepository.sumPointsEarnedByStudentAndQuiz(currentUser, quiz);
    }
    public boolean isStudentAuthorizedForQuiz(Long quizId) {
        User currentUser = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));
        if (currentUser.getRole() == Role.ADMIN) {
            return true;
        }
        if (currentUser.getRole() == Role.ENSEIGNANT) {
            return quiz.getEnseignant().getId().equals(currentUser.getId());
        }
        return quizStudentRepository.existsByQuizAndStudent(quiz, currentUser);
    }
    private ReponseDto mapToResponseDto(Reponse reponse) {
        Question question = reponse.getQuestion();
        return ReponseDto.builder()
                .id(reponse.getId())
                .quizId(reponse.getQuiz() != null ? reponse.getQuiz().getId() : null)
                .quizTitle(reponse.getQuiz() != null ? reponse.getQuiz().getTitre() : null)
                .questionId(question != null ? question.getId() : null)
                .questionText(question != null ? question.getEnonce() : null)
                .questionType(question != null && question.getType() != null ? question.getType().name() : "MCQ")
                .studentAnswer(reponse.getStudentAnswer())
                .correctAnswer(question != null ? question.getCorrectAnswerText() : null)
                .isCorrect(reponse.getIsCorrect())
                .pointsEarned(reponse.getPointsEarned())
                .pointsMax(question != null ? question.getPoints() : 0)
                .answeredAt(reponse.getAnsweredAt())
                .feedback(reponse.getIsCorrect() != null && reponse.getIsCorrect() ?
                        "✓ Bonne réponse !" :
                        "✗ Mauvaise réponse. La bonne réponse était : " +
                                (question != null ? question.getCorrectAnswerText() : "Inconnue"))
                .build();
    }
}