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
import com.exemple.quiz_app.quiz.repository.QuizRepository;
import com.exemple.quiz_app.quiz.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuizRepository quizRepository;
    @Autowired private QuizService quizService;
    @Autowired private AuthService authService;
    @Autowired private AiQuizGenerationService aiQuizGenerationService;

    // ========== VÉRIFICATIONS MÉTIER ==========

    private boolean isTeacherOrAdmin(User user) {
        return user.getRole() == Role.ENSEIGNANT || user.getRole() == Role.ADMIN;
    }

    private boolean isStudent(User user) {
        return user.getRole() == Role.ETUDIANT;
    }

    private void checkTeacherOwnership(Quiz quiz, User teacher) {
        if (!quiz.getEnseignant().getId().equals(teacher.getId())) {
            throw new RuntimeException("Vous n'etes pas le proprietaire de ce quiz");
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
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

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
                .orElseThrow(() -> new RuntimeException("Question non trouvee"));

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
                .orElseThrow(() -> new RuntimeException("Question non trouvee"));

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
            throw new RuntimeException("Seul un enseignant peut voir les reponses des questions");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

        if (currentUser.getRole() != Role.ADMIN) {
            checkTeacherOwnership(quiz, currentUser);
        }

        return questionRepository.findByQuizId(quizId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public QuestionResponseDto getQuestionByIdForTeacher(Long questionId) {
        User currentUser = authService.getCurrentUser();

        if (!isTeacherOrAdmin(currentUser)) {
            throw new RuntimeException("Acces reserve aux enseignants");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question non trouvee"));

        Quiz quiz = question.getQuiz();

        if (currentUser.getRole() != Role.ADMIN) {
            checkTeacherOwnership(quiz, currentUser);
        }

        return mapToResponse(question);
    }

    // ========== GÉNÉRATION IA ==========

    public QuizGenerationResponseDto generateQuestionsByIA(
            String theme, int numberOfQuestions, String difficulty) {
        User currentUser = authService.getCurrentUser();

        if (!isTeacherOrAdmin(currentUser)) {
            throw new RuntimeException("Seul un enseignant peut generer des questions par IA");
        }

        QuizGenerationRequestDto request = new QuizGenerationRequestDto();
        request.setTheme(theme);
        request.setNumberOfQuestions(numberOfQuestions);
        request.setDifficulty(difficulty);

        return aiQuizGenerationService.generateQuizContent(request);
    }

    @Transactional
    public List<QuestionResponseDto> generateAndSaveQuestions(
            Long quizId, String theme, int numberOfQuestions, String difficulty) {

        // 1. Générer via IA
        QuizGenerationResponseDto generated = aiQuizGenerationService
                .generateQuizContent(buildRequest(theme, numberOfQuestions, difficulty));

        // 2. Convertir automatiquement ✅
        List<QuestionRequestDto> converted = generated.getQuestions().stream()
                .map(this::convertToRequestDto)
                .collect(Collectors.toList());

        // 3. Sauvegarder
        return addMultipleQuestions(quizId, converted);
    }

    private QuizGenerationRequestDto buildRequest(String theme, int n, String difficulty) {
        QuizGenerationRequestDto req = new QuizGenerationRequestDto();
        req.setTheme(theme);
        req.setNumberOfQuestions(n);
        req.setDifficulty(difficulty);
        return req;
    }

    private QuestionRequestDto convertToRequestDto(QuizGenerationResponseDto.GeneratedQuestionDto q) {
        QuestionRequestDto dto = new QuestionRequestDto();
        dto.setEnonce(q.getQuestionText());
        dto.setType(q.getType());
        dto.setPoints(q.getPoints() != null ? q.getPoints() : 1);

        List<String> options = q.getOptions();
        if (options != null) {
            if (options.size() > 0) dto.setChoixA(options.get(0));
            if (options.size() > 1) dto.setChoixB(options.get(1));
            if (options.size() > 2) dto.setChoixC(options.get(2));
            if (options.size() > 3) dto.setChoixD(options.get(3));
        }

        // ✅ Toujours convertir correctAnswer en A/B/C/D
        String correct = q.getCorrectAnswer();
        if (options != null && options.contains(correct)) {
            // Trouve l'index et convertit en lettre
            int index = options.indexOf(correct);
            dto.setReponseCorrecte(String.valueOf((char)('A' + index)));
        } else if ("Vrai".equalsIgnoreCase(correct) || "True".equalsIgnoreCase(correct)) {
            dto.setReponseCorrecte("A");
        } else if ("Faux".equalsIgnoreCase(correct) || "False".equalsIgnoreCase(correct)) {
            dto.setReponseCorrecte("B");
        } else {
            // TYPE TEXT — tronquer à 50 caractères max
            dto.setReponseCorrecte(correct != null && correct.length() > 50
                    ? correct.substring(0, 50)
                    : correct);
        }

        return dto;
    }

    @Transactional
    public List<QuestionResponseDto> saveGeneratedQuestions(
            Long quizId, List<QuestionRequestDto> validatedQuestions) {
        User currentUser = authService.getCurrentUser();

        if (!isTeacherOrAdmin(currentUser)) {
            throw new RuntimeException("Seul un enseignant peut sauvegarder des questions generees par IA");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

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

    public List<QuestionDto> getQuestionsByQuizForStudent(Long quizId) {
        User currentUser = authService.getCurrentUser();

        if (!isStudent(currentUser)) {
            throw new RuntimeException("Acces reserve aux etudiants");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

        if (!quizService.isStudentAllowed(quizId, currentUser.getId())) {
            throw new RuntimeException("Vous n'etes pas autorise a acceder a ce quiz");
        }

        if (!quiz.isAvailable()) {
            throw new RuntimeException("Ce quiz n'est pas disponible");
        }

        return questionRepository.findByQuizId(quizId)
                .stream().map(this::mapToStudentDto).collect(Collectors.toList());
    }

    public QuestionDto getQuestionByIdForStudent(Long questionId) {
        User currentUser = authService.getCurrentUser();

        if (!isStudent(currentUser)) {
            throw new RuntimeException("Acces reserve aux etudiants");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question non trouvee"));

        Quiz quiz = question.getQuiz();

        if (!quizService.isStudentAllowed(quiz.getId(), currentUser.getId())) {
            throw new RuntimeException("Acces non autorise");
        }

        if (!quiz.isAvailable()) {
            throw new RuntimeException("Ce quiz n'est pas disponible");
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
            question.setType(Question.QuestionType.valueOf(request.getType()));
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
            question.setType(Question.QuestionType.valueOf(request.getType()));
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