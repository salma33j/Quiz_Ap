package com.exemple.quiz_app.question.service;

import com.exemple.quiz_app.AI.dto.QuizGenerationRequestDto;
import com.exemple.quiz_app.AI.dto.QuizGenerationResponseDto;
import com.exemple.quiz_app.AI.service.AiQuizGenerationService;
import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.question.dto.QuestionDto;
import com.exemple.quiz_app.question.dto.QuestionRequestDto;
import com.exemple.quiz_app.question.dto.QuestionResponseDto;
import com.exemple.quiz_app.question.entity.Question;
import com.exemple.quiz_app.question.repository.QuestionRepository;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.quiz.entity.QuizSession;
import com.exemple.quiz_app.quiz.repository.QuizRepository;
import com.exemple.quiz_app.quiz.repository.QuizSessionRepository;
import com.exemple.quiz_app.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;
    private final QuizService quizService;
    private final AuthService authService;
    private final AiQuizGenerationService aiQuizGenerationService;
    private final QuizSessionRepository quizSessionRepository;  // 🔥 AJOUTÉ

    // ========== VÉRIFICATIONS MÉTIER ==========

    private boolean isTeacherOrAdmin(User user) {
        return user.getRole() == Role.ENSEIGNANT || user.getRole() == Role.ADMIN;
    }

    private boolean isStudent(User user) {
        return user.getRole() == Role.ETUDIANT;
    }

    private void checkTeacherOwnership(Quiz quiz, User teacher) {
        if (!quiz.getEnseignant().getId().equals(teacher.getId())) {
            throw new RuntimeException("Vous n'êtes pas le propriétaire de ce quiz");
        }
    }

    // ========== ENSEIGNANT : AJOUTER QUESTION ==========

    @Transactional
    public QuestionResponseDto addQuestion(Long quizId, QuestionRequestDto request) {
        User currentUser = authService.getCurrentUser();

        if (!isTeacherOrAdmin(currentUser)) {
            throw new RuntimeException("Seul un enseignant peut ajouter des questions");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        if (currentUser.getRole() != Role.ADMIN) {
            checkTeacherOwnership(quiz, currentUser);
        }

        if (!quiz.isModifiable()) {
            throw new RuntimeException("Ce quiz n'est plus modifiable");
        }

        Question question = createQuestionFromRequest(request, quiz);
        Question saved = questionRepository.save(question);
        quizService.incrementQuestionCount(quizId);
        return mapToResponse(saved);
    }

    @Transactional
    public List<QuestionResponseDto> addMultipleQuestions(Long quizId, List<QuestionRequestDto> requests) {
        List<QuestionResponseDto> responses = new ArrayList<>();
        for (QuestionRequestDto request : requests) {
            responses.add(addQuestion(quizId, request));
        }
        return responses;
    }

    // ========== ENSEIGNANT : MODIFIER QUESTION ==========

    @Transactional
    public QuestionResponseDto updateQuestion(Long questionId, QuestionRequestDto request) {
        User currentUser = authService.getCurrentUser();

        if (!isTeacherOrAdmin(currentUser)) {
            throw new RuntimeException("Seul un enseignant peut modifier des questions");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question non trouvée"));

        Quiz quiz = question.getQuiz();

        if (currentUser.getRole() != Role.ADMIN) {
            checkTeacherOwnership(quiz, currentUser);
        }

        if (!quiz.isModifiable()) {
            throw new RuntimeException("Ce quiz n'est plus modifiable");
        }

        updateQuestionFromRequest(question, request);
        return mapToResponse(questionRepository.save(question));
    }

    // ========== ENSEIGNANT : SUPPRIMER QUESTION ==========

    @Transactional
    public void deleteQuestion(Long questionId) {
        User currentUser = authService.getCurrentUser();

        if (!isTeacherOrAdmin(currentUser)) {
            throw new RuntimeException("Seul un enseignant peut supprimer des questions");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question non trouvée"));

        Quiz quiz = question.getQuiz();
        Long quizId = quiz.getId();

        if (currentUser.getRole() != Role.ADMIN) {
            checkTeacherOwnership(quiz, currentUser);
        }

        if (!quiz.isModifiable()) {
            throw new RuntimeException("Ce quiz n'est plus modifiable");
        }

        questionRepository.delete(question);
        quizService.decrementQuestionCount(quizId);
    }

    // ========== ENSEIGNANT : VOIR QUESTIONS ==========

    public List<QuestionResponseDto> getQuestionsByQuizForTeacher(Long quizId) {
        User currentUser = authService.getCurrentUser();

        if (!isTeacherOrAdmin(currentUser)) {
            throw new RuntimeException("Seul un enseignant peut voir les réponses des questions");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        if (currentUser.getRole() != Role.ADMIN) {
            checkTeacherOwnership(quiz, currentUser);
        }

        return questionRepository.findByQuizId(quizId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public QuestionResponseDto getQuestionByIdForTeacher(Long questionId) {
        User currentUser = authService.getCurrentUser();

        if (!isTeacherOrAdmin(currentUser)) {
            throw new RuntimeException("Accès réservé aux enseignants");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question non trouvée"));

        Quiz quiz = question.getQuiz();

        if (currentUser.getRole() != Role.ADMIN) {
            checkTeacherOwnership(quiz, currentUser);
        }

        return mapToResponse(question);
    }

    // ========== GÉNÉRATION IA ==========

    public QuizGenerationResponseDto generateQuestionsByIA(String theme, int numberOfQuestions, String difficulty) {
        User currentUser = authService.getCurrentUser();

        if (!isTeacherOrAdmin(currentUser)) {
            throw new RuntimeException("Seul un enseignant peut générer des questions par IA");
        }

        QuizGenerationRequestDto request = new QuizGenerationRequestDto();
        request.setTheme(theme);
        request.setMatiere(theme);
        request.setTitre(theme);
        request.setDescription(theme);
        request.setNumberOfQuestions(numberOfQuestions);
        request.setDifficulty(difficulty);

        return aiQuizGenerationService.generateQuizContent(request);
    }

    @Transactional
    public List<QuestionResponseDto> generateAndSaveQuestions(Long quizId, String theme, int numberOfQuestions, String difficulty) {
        // 1. Générer via IA
        QuizGenerationResponseDto generated = aiQuizGenerationService
                .generateQuizContent(buildRequest(theme, numberOfQuestions, difficulty));

        // 2. Convertir automatiquement avec sécurisation
        List<QuestionRequestDto> converted = generated.getQuestions().stream()
                .map(this::convertToRequestDto)
                .collect(Collectors.toList());

        // 3. Sauvegarder
        return addMultipleQuestions(quizId, converted);
    }

    private QuizGenerationRequestDto buildRequest(String theme, int n, String difficulty) {
        QuizGenerationRequestDto req = new QuizGenerationRequestDto();
        req.setTheme(theme);
        req.setMatiere(theme);
        req.setTitre(theme);
        req.setDescription(theme);
        req.setNumberOfQuestions(n);
        req.setDifficulty(difficulty);
        return req;
    }

    /**
     * 🔥 SÉCURISÉ : Convertir une question générée par IA en QuestionRequestDto
     * Gère tous les cas : options null, taille insuffisante, type TEXT, etc.
     */
    private QuestionRequestDto convertToRequestDto(QuizGenerationResponseDto.GeneratedQuestionDto q) {
        QuestionRequestDto dto = new QuestionRequestDto();

        // 1. Énoncé
        dto.setEnonce(q.getQuestionText() != null ? q.getQuestionText() : "Question sans énoncé");

        // 2. Type
        dto.setType(q.getType() != null ? q.getType() : "MCQ");

        // 3. Points
        dto.setPoints(q.getPoints() != null && q.getPoints() > 0 ? q.getPoints() : 1);

        // 4. Options (sécurisé)
        List<String> options = q.getOptions();
        if (options != null && !options.isEmpty()) {
            dto.setChoixA(options.size() > 0 ? options.get(0) : null);
            dto.setChoixB(options.size() > 1 ? options.get(1) : null);
            dto.setChoixC(options.size() > 2 ? options.get(2) : null);
            dto.setChoixD(options.size() > 3 ? options.get(3) : null);
        } else {
            dto.setChoixA(null);
            dto.setChoixB(null);
            dto.setChoixC(null);
            dto.setChoixD(null);
        }

        // 5. Réponse correcte (sécurisé)
        String correct = q.getCorrectAnswer();
        List<String> opts = q.getOptions();

        // Cas 1 : QCM avec options et réponse trouvable
        if ("MCQ".equalsIgnoreCase(q.getType()) && opts != null && correct != null && !opts.isEmpty()) {
            // Recherche de la réponse dans les options
            int index = -1;
            for (int i = 0; i < opts.size(); i++) {
                if (opts.get(i) != null && opts.get(i).equalsIgnoreCase(correct)) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                dto.setReponseCorrecte(String.valueOf((char) ('A' + index)));
            } else {
                // Si non trouvé, chercher par A/B/C/D
                if ("A".equalsIgnoreCase(correct) || "a".equals(correct)) dto.setReponseCorrecte("A");
                else if ("B".equalsIgnoreCase(correct)) dto.setReponseCorrecte("B");
                else if ("C".equalsIgnoreCase(correct)) dto.setReponseCorrecte("C");
                else if ("D".equalsIgnoreCase(correct)) dto.setReponseCorrecte("D");
                else dto.setReponseCorrecte("A"); // Fallback
            }
        }
        // Cas 2 : Vrai/Faux
        else if ("TRUE_FALSE".equalsIgnoreCase(q.getType())) {
            if ("Vrai".equalsIgnoreCase(correct) || "True".equalsIgnoreCase(correct) || "true".equals(correct)) {
                dto.setReponseCorrecte("A");
            } else if ("Faux".equalsIgnoreCase(correct) || "False".equalsIgnoreCase(correct) || "false".equals(correct)) {
                dto.setReponseCorrecte("B");
            } else {
                dto.setReponseCorrecte("A"); // Fallback
            }
        }
        // Cas 3 : Question ouverte (TEXT)
        else if ("TEXT".equalsIgnoreCase(q.getType())) {
            dto.setReponseCorrecte(correct != null && correct.length() > 200
                    ? correct.substring(0, 200) : correct);
        }
        // Cas 4 : Fallback
        else {
            dto.setReponseCorrecte(correct != null && correct.length() > 200
                    ? correct.substring(0, 200) : correct);
        }

        return dto;
    }

    @Transactional
    public List<QuestionResponseDto> saveGeneratedQuestions(Long quizId, List<QuestionRequestDto> validatedQuestions) {
        User currentUser = authService.getCurrentUser();

        if (!isTeacherOrAdmin(currentUser)) {
            throw new RuntimeException("Seul un enseignant peut sauvegarder des questions générées par IA");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        if (currentUser.getRole() != Role.ADMIN) {
            checkTeacherOwnership(quiz, currentUser);
        }

        if (!quiz.isModifiable()) {
            throw new RuntimeException("Ce quiz n'est plus modifiable");
        }

        List<QuestionResponseDto> savedQuestions = new ArrayList<>();
        for (QuestionRequestDto request : validatedQuestions) {
            savedQuestions.add(addQuestion(quizId, request));
        }
        return savedQuestions;
    }

    // ========== ÉTUDIANT : VOIR QUESTIONS ==========

    /**
     * 🔥 CORRIGÉ : Avec vérification de session et de temps
     */
    public List<QuestionDto> getQuestionsByQuizForStudent(Long quizId) {
        User currentUser = authService.getCurrentUser();

        if (!isStudent(currentUser)) {
            throw new RuntimeException("Accès réservé aux étudiants");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        if (!quizService.isStudentAllowed(quizId, currentUser.getId().longValue())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à accéder à ce quiz");
        }

        if (!quiz.isAvailable()) {
            throw new RuntimeException("Ce quiz n'est pas disponible");
        }

        // 🔥 VÉRIFICATION DE LA SESSION ET DU TEMPS
        Optional<QuizSession> session = quizSessionRepository.findByStudentAndQuiz(currentUser, quiz);
        if (session.isPresent() && session.get().isExpired()) {
            throw new RuntimeException("Temps écoulé ! Vous ne pouvez plus répondre aux questions.");
        }

        // Si la session n'existe pas encore, on ne bloque pas (le quiz n'a pas encore été démarré)
        // Le frontend devra appeler /start avant de pouvoir répondre

        return questionRepository.findByQuizId(quizId)
                .stream().map(this::mapToStudentDto).collect(Collectors.toList());
    }

    public QuestionDto getQuestionByIdForStudent(Long questionId) {
        User currentUser = authService.getCurrentUser();

        if (!isStudent(currentUser)) {
            throw new RuntimeException("Accès réservé aux étudiants");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question non trouvée"));

        Quiz quiz = question.getQuiz();

        if (!quizService.isStudentAllowed(quiz.getId(), currentUser.getId().longValue())) {
            throw new RuntimeException("Accès non autorisé");
        }

        if (!quiz.isAvailable()) {
            throw new RuntimeException("Ce quiz n'est pas disponible");
        }

        // 🔥 VÉRIFICATION DE LA SESSION ET DU TEMPS
        Optional<QuizSession> session = quizSessionRepository.findByStudentAndQuiz(currentUser, quiz);
        if (session.isPresent() && session.get().isExpired()) {
            throw new RuntimeException("Temps écoulé ! Vous ne pouvez plus répondre aux questions.");
        }

        return mapToStudentDto(question);
    }

    // ========== MÉTHODES PRIVÉES ==========

    private Question createQuestionFromRequest(QuestionRequestDto request, Quiz quiz) {
        Question question = new Question();
        question.setEnonce(request.getEnonce());
        question.setChoixA(request.getChoixA());
        question.setChoixB(request.getChoixB());
        question.setChoixC(request.getChoixC());
        question.setChoixD(request.getChoixD());
        question.setReponseCorrecte(request.getReponseCorrecte());
        question.setPoints(request.getPoints() != null ? request.getPoints() : 1);
        if (request.getType() != null) {
            try {
                question.setType(Question.QuestionType.valueOf(request.getType()));
            } catch (IllegalArgumentException e) {
                question.setType(Question.QuestionType.MCQ);
            }
        }
        question.setQuiz(quiz);
        return question;
    }

    private void updateQuestionFromRequest(Question question, QuestionRequestDto request) {
        question.setEnonce(request.getEnonce());
        question.setChoixA(request.getChoixA());
        question.setChoixB(request.getChoixB());
        question.setChoixC(request.getChoixC());
        question.setChoixD(request.getChoixD());
        question.setReponseCorrecte(request.getReponseCorrecte());
        if (request.getPoints() != null) question.setPoints(request.getPoints());
        if (request.getType() != null) {
            try {
                question.setType(Question.QuestionType.valueOf(request.getType()));
            } catch (IllegalArgumentException e) {
                // Garder le type existant
            }
        }
    }

    private QuestionResponseDto mapToResponse(Question question) {
        QuestionResponseDto dto = new QuestionResponseDto();
        dto.setId(question.getId());
        dto.setEnonce(question.getEnonce());
        dto.setOptions(question.getAllOptions());
        dto.setPoints(question.getPoints());
        dto.setType(question.getType().name());
        dto.setCreatedAt(question.getCreatedAt());
        return dto;
    }

    private QuestionDto mapToStudentDto(Question question) {
        QuestionDto dto = new QuestionDto();
        dto.setId(question.getId());
        dto.setEnonce(question.getEnonce());
        dto.setOptions(question.getAllOptions());
        dto.setType(question.getType().name());
        dto.setPoints(question.getPoints());
        return dto;
    }
}
