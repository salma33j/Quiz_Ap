package com.exemple.quiz_app.quiz.service;

import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.repository.UserRepository;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.auth.service.EmailService;
import com.exemple.quiz_app.classe.entity.Classe;
import com.exemple.quiz_app.classe.repository.ClasseRepository;
import com.exemple.quiz_app.quiz.dto.QuizReponse;
import com.exemple.quiz_app.quiz.dto.QuizRequest;
import com.exemple.quiz_app.quiz.dto.StudentListDto;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.quiz.entity.QuizStudent;
import com.exemple.quiz_app.quiz.repository.QuizRepository;
import com.exemple.quiz_app.quiz.repository.QuizStudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private static final int MIN_QUESTIONS_TO_PUBLISH = 10;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizStudentRepository quizStudentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ClasseRepository classeRepository;

    // ========== CRUD ==========

    @Transactional
    public QuizReponse createQuiz(QuizRequest request) {
        User enseignant = authService.getCurrentUser();

        if (enseignant.getRole() != Role.ENSEIGNANT && enseignant.getRole() != Role.ADMIN) {
            throw new RuntimeException("Seul un enseignant peut creer un quiz");
        }

        Quiz quiz = new Quiz();
        quiz.setTitre(request.getTitre());
        quiz.setTheme(request.getTheme());
        quiz.setQuestionCount(0);
        quiz.setAvailableFrom(request.getAvailableFrom());
        quiz.setAvailableUntil(request.getAvailableUntil());
        quiz.setTimeLimit(request.getTimeLimit());
        quiz.setEnseignant(enseignant);
        quiz.setClasse(findRequestedClasse(request));
        quiz.setStatus(Quiz.QuizStatus.DRAFT);

        if ("AI".equals(request.getCreationType())) {
            quiz.setCreationType(Quiz.CreationType.AI);
        } else {
            quiz.setCreationType(Quiz.CreationType.MANUAL);
        }

        quiz = quizRepository.save(quiz);
        return mapToResponse(quiz);
    }

    public List<QuizReponse> getMyQuizzes() {
        User enseignant = authService.getCurrentUser();
        return quizRepository.findByEnseignant(enseignant)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public QuizReponse getQuizById(Long id) {
        User currentUser = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

        if (currentUser.getRole() != Role.ADMIN && !quiz.getEnseignant().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Vous n'avez pas acces a ce quiz");
        }
        return mapToResponse(quiz);
    }

    @Transactional
    public QuizReponse updateQuiz(Long id, QuizRequest request) {
        User enseignant = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

        if (!quiz.getEnseignant().getId().equals(enseignant.getId())) {
            throw new RuntimeException("Vous n'etes pas le proprietaire");
        }
        if (!quiz.isModifiable()) {
            throw new RuntimeException("Quiz non modifiable (statut: " + quiz.getStatus() + ")");
        }

        quiz.setTitre(request.getTitre());
        quiz.setTheme(request.getTheme());
        quiz.setAvailableFrom(request.getAvailableFrom());
        quiz.setAvailableUntil(request.getAvailableUntil());
        quiz.setTimeLimit(request.getTimeLimit());
        Classe requestedClasse = findRequestedClasse(request);
        if (requestedClasse != null) {
            quiz.setClasse(requestedClasse);
        }
        quiz.setCreationType("AI".equals(request.getCreationType())
                ? Quiz.CreationType.AI
                : Quiz.CreationType.MANUAL);

        return mapToResponse(quizRepository.save(quiz));
    }

    @Transactional
    public void deleteQuiz(Long id) {
        User enseignant = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

        if (!quiz.getEnseignant().getId().equals(enseignant.getId())) {
            throw new RuntimeException("Vous n'etes pas le proprietaire");
        }
        if (!quiz.isDeletable()) {
            throw new RuntimeException("Quiz non supprimable (statut: " + quiz.getStatus() + ")");
        }

        quizStudentRepository.deleteByQuiz(quiz);
        quizRepository.delete(quiz);
    }

    // ========== PUBLICATION ==========

    @Transactional
    public QuizReponse publishQuiz(Long id) {
        User enseignant = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

        if (!quiz.getEnseignant().getId().equals(enseignant.getId())) {
            throw new RuntimeException("Vous n'etes pas le proprietaire");
        }
        if (quiz.getStatus() != Quiz.QuizStatus.DRAFT) {
            throw new RuntimeException("Quiz deja publie");
        }
        if (quiz.getQuestionCount() == null || quiz.getQuestionCount() < MIN_QUESTIONS_TO_PUBLISH) {
            throw new RuntimeException("Le quiz doit contenir au moins " + MIN_QUESTIONS_TO_PUBLISH + " questions avant de publier");
        }
        if (quizRepository.countAllowedStudents(id) == 0) {
            throw new RuntimeException("Ajoutez au moins un etudiant avant de publier");
        }

        quiz.setStatus(Quiz.QuizStatus.PUBLISHED);
        Quiz savedQuiz = quizRepository.save(quiz);
        notifyAllowedStudents(savedQuiz);
        return mapToResponse(savedQuiz);
    }

    // ========== GESTION DU NOMBRE DE QUESTIONS ==========

    @Transactional
    public void updateQuizQuestionCount(Long quizId, int count) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));
        quiz.setQuestionCount(count);
        quizRepository.save(quiz);
    }

    @Transactional
    public void incrementQuestionCount(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));
        quiz.setQuestionCount((quiz.getQuestionCount() != null ? quiz.getQuestionCount() : 0) + 1);
        quizRepository.save(quiz);
    }

    @Transactional
    public void decrementQuestionCount(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));
        int current = quiz.getQuestionCount() != null ? quiz.getQuestionCount() : 0;
        if (current > 0) {
            quiz.setQuestionCount(current - 1);
            quizRepository.save(quiz);
        }
    }

    public int getQuestionCount(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));
        return quiz.getQuestionCount() != null ? quiz.getQuestionCount() : 0;
    }

    // ========== GESTION DES ETUDIANTS ==========

    @Transactional
    public int addAllowedStudents(Long quizId, StudentListDto studentList) {
        User enseignant = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

        if (!quiz.getEnseignant().getId().equals(enseignant.getId())) {
            throw new RuntimeException("Vous n'etes pas le proprietaire");
        }
        if (quiz.getStatus() == Quiz.QuizStatus.EXPIRED) {
            throw new RuntimeException("Quiz expire, plus d'ajout possible");
        }

        if (studentList.getStudents() == null || studentList.getStudents().isEmpty()) {
            throw new RuntimeException("La liste students est vide ou absente");
        }

        int added = 0;
        for (StudentListDto.StudentInfo info : studentList.getStudents()) {
            if (info == null || info.getEmail() == null || info.getEmail().isBlank()) {
                throw new RuntimeException("Chaque etudiant doit avoir un email");
            }
            String email = info.getEmail().trim();
            User student = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException(
                            "Aucun compte pour l'email \"" + email
                                    + "\". L'etudiant doit d'abord s'inscrire (POST /api/auth/register avec role ETUDIANT)."));

            if (student.getRole() != Role.ETUDIANT && student.getRole() != Role.ADMIN) {
                throw new RuntimeException("L'email \"" + email + "\" n'est pas un compte etudiant");
            }

            if (!quizStudentRepository.existsByQuizAndStudent(quiz, student)) {
                QuizStudent qs = new QuizStudent();
                qs.setQuiz(quiz);
                qs.setStudent(student);
                quizStudentRepository.save(qs);
                added++;
            }
        }
        return added;
    }

    public List<StudentListDto.StudentInfo> getAllowedStudents(Long quizId) {
        User enseignant = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

        if (!quiz.getEnseignant().getId().equals(enseignant.getId()) && enseignant.getRole() != Role.ADMIN) {
            throw new RuntimeException("Acces non autorise");
        }

        return quizStudentRepository.findByQuiz(quiz).stream().map(qs -> {
            StudentListDto.StudentInfo info = new StudentListDto.StudentInfo();
            User s = qs.getStudent();
            // 🔥 CORRECTION : Utiliser getFirstName() et getLastName()
            info.setNom(s.getFirstName() != null ? s.getFirstName() : "");
            info.setPrenom(s.getLastName() != null ? s.getLastName() : "");
            info.setEmail(s.getEmail());
            info.setCne(s.getCne());
            info.setCodeApoge(s.getCodeApoge());
            if (s.getClasse() != null) {
                info.setClasse(s.getClasse().getName());
                info.setFiliere(s.getClasse().getFiliere());
            } else if (quiz.getClasse() != null) {
                info.setClasse(quiz.getClasse().getName());
                info.setFiliere(quiz.getClasse().getFiliere());
            } else {
                info.setClasse("Non definie");
                info.setFiliere("Non definie");
            }
            return info;
        }).collect(Collectors.toList());
    }

    public List<StudentListDto.StudentInfo> getMyStudents() {
        User currentUser = authService.getCurrentUser();
        Map<String, StudentListDto.StudentInfo> studentsByEmail = new HashMap<>();

        if (currentUser.getRole() == Role.ADMIN) {
            userRepository.findAll().stream()
                    .filter(user -> user.getRole() == Role.ETUDIANT)
                    .forEach(student -> studentsByEmail.put(student.getEmail(), mapStudentInfo(student, student.getClasse())));
            return List.copyOf(studentsByEmail.values());
        }

        if (currentUser.getRole() != Role.ENSEIGNANT) {
            throw new RuntimeException("Acces reserve aux enseignants");
        }

        quizRepository.findByEnseignant(currentUser).forEach(quiz ->
                quizStudentRepository.findByQuiz(quiz).forEach(qs -> {
                    User student = qs.getStudent();
                    studentsByEmail.put(student.getEmail(), mapStudentInfo(student, student.getClasse()));
                })
        );

        classeRepository.findByEnseignants_Id(currentUser.getId()).forEach(classe ->
                userRepository.findByClasseId(classe.getId()).stream()
                        .filter(user -> user.getRole() == Role.ETUDIANT)
                        .forEach(student -> studentsByEmail.put(student.getEmail(), mapStudentInfo(student, classe)))
        );

        return List.copyOf(studentsByEmail.values());
    }

    @Transactional
    public Map<String, Object> assignQuizToClass(Long quizId, Long classId) {
        User enseignant = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));
        Classe classe = classeRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Classe introuvable"));

        if (!quiz.getEnseignant().getId().equals(enseignant.getId()) && enseignant.getRole() != Role.ADMIN) {
            throw new RuntimeException("Vous n'etes pas le proprietaire");
        }

        if (enseignant.getRole() != Role.ADMIN) {
            boolean assignedTeacher = classe.getEnseignants().stream()
                    .anyMatch(teacher -> teacher.getId().equals(enseignant.getId()));
            if (!assignedTeacher) {
                throw new RuntimeException("Cette classe n'est pas affectee a cet enseignant");
            }
        }

        List<User> students = userRepository.findByClasseId(classId).stream()
                .filter(user -> user.getRole() == Role.ETUDIANT || user.getRole() == Role.ADMIN)
                .collect(Collectors.toList());

        if (students.isEmpty()) {
            throw new RuntimeException("Aucun etudiant dans cette classe");
        }

        int added = 0;
        for (User student : students) {
            if (!quizStudentRepository.existsByQuizAndStudent(quiz, student)) {
                QuizStudent qs = new QuizStudent();
                qs.setQuiz(quiz);
                qs.setStudent(student);
                quizStudentRepository.save(qs);
                added++;
            }
        }

        quiz.setClasse(classe);
        quizRepository.save(quiz);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Quiz affecte a la classe");
        response.put("addedCount", added);
        response.put("totalStudents", students.size());
        response.put("className", classe.getName());
        return response;
    }

    @Transactional
    public void removeAllowedStudent(Long quizId, Long studentId) {
        User enseignant = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

        if (!quiz.getEnseignant().getId().equals(enseignant.getId())) {
            throw new RuntimeException("Vous n'etes pas le proprietaire");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Etudiant non trouve"));

        QuizStudent qs = quizStudentRepository.findByQuizAndStudent(quiz, student)
                .orElseThrow(() -> new RuntimeException("Etudiant non autorise"));
        quizStudentRepository.delete(qs);
    }

    // ========== STATUT & VERIFICATIONS ==========

    public Map<String, Object> getQuizStatus(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));
        Map<String, Object> status = new HashMap<>();
        status.put("status", quiz.getStatus().name());
        status.put("isModifiable", quiz.isModifiable());
        status.put("isDeletable", quiz.isDeletable());
        status.put("isAvailable", quiz.isAvailable());
        status.put("questionCount", quiz.getQuestionCount());
        int allowedStudentsCount = quizRepository.countAllowedStudents(quizId);
        status.put("allowedStudentsCount", allowedStudentsCount);
        status.put("canBePublished", quiz.getStatus() == Quiz.QuizStatus.DRAFT
                && quiz.getQuestionCount() != null
                && quiz.getQuestionCount() >= MIN_QUESTIONS_TO_PUBLISH
                && allowedStudentsCount > 0);
        return status;
    }

    public boolean isStudentAllowed(Long quizId, Long studentId) {
        return quizRepository.isStudentAllowed(quizId, studentId);
    }

    public int getStudentCount(Long quizId) {
        return quizRepository.countAllowedStudents(quizId);
    }

    private void notifyAllowedStudents(Quiz quiz) {
        List<User> students = quizStudentRepository.findStudentsByQuizId(quiz.getId());
        String className = quiz.getClasse() != null ? quiz.getClasse().getName() : "Non definie";

        for (User student : students) {
            if (student.getEmail() == null || student.getEmail().isBlank()) {
                continue;
            }

            emailService.sendQuizPublishedNotification(
                    student.getEmail(),
                    student.getFirstName(),
                    student.getLastName(),
                    quiz.getTitre(),
                    quiz.getTheme(),
                    className,
                    quiz.getAvailableUntil(),
                    quiz.getTimeLimit()
            );
        }
    }

    private Classe findRequestedClasse(QuizRequest request) {
        Long classId = request.getClassId() != null ? request.getClassId() : request.getClasseId();
        if (classId == null) {
            return null;
        }
        return classeRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Classe introuvable"));
    }

    private StudentListDto.StudentInfo mapStudentInfo(User student, Classe classe) {
        StudentListDto.StudentInfo info = new StudentListDto.StudentInfo();
        info.setNom(student.getFirstName() != null ? student.getFirstName() : "");
        info.setPrenom(student.getLastName() != null ? student.getLastName() : "");
        info.setEmail(student.getEmail());
        info.setCne(student.getCne());
        info.setCodeApoge(student.getCodeApoge());
        info.setClasse(classe != null ? classe.getName() : "Non definie");
        info.setFiliere(classe != null ? classe.getFiliere() : "Non definie");
        return info;
    }

    // ========== MAPPING ==========

    private QuizReponse mapToResponse(Quiz quiz) {
        QuizReponse response = new QuizReponse();
        response.setId(quiz.getId());
        response.setTitre(quiz.getTitre());
        response.setTheme(quiz.getTheme());
        response.setQuestionCount(quiz.getQuestionCount());
        response.setAvailableFrom(quiz.getAvailableFrom());
        response.setAvailableUntil(quiz.getAvailableUntil());
        response.setTimeLimit(quiz.getTimeLimit());
        response.setStatus(quiz.getStatus().name());
        response.setCreationType(quiz.getCreationType().name());
        // 🔥 CORRECTION : Utiliser getFirstName() et getLastName()
        response.setEnseignantNom(quiz.getEnseignant().getFirstName() + " " + quiz.getEnseignant().getLastName());
        response.setTotalStudentsAllowed(quizRepository.countAllowedStudents(quiz.getId()));
        if (quiz.getClasse() != null) {
            response.setClassId(quiz.getClasse().getId());
            response.setClasseId(quiz.getClasse().getId());
            response.setClassName(quiz.getClasse().getName());
            response.setClasseName(quiz.getClasse().getName());
            response.setClassFiliere(quiz.getClasse().getFiliere());
            response.setClasseFiliere(quiz.getClasse().getFiliere());
            response.setClassNiveau(quiz.getClasse().getNiveau());
            response.setClasseNiveau(quiz.getClasse().getNiveau());
        }
        response.setCreatedAt(quiz.getCreatedAt());
        return response;
    }
}
