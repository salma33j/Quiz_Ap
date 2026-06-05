package com.exemple.quiz_app.quiz.service;

import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.repository.UserRepository;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.auth.service.EmailService;
import com.exemple.quiz_app.classe.entity.Classe;
import com.exemple.quiz_app.classe.repository.ClasseRepository;
import com.exemple.quiz_app.matiere.entity.Matiere;
import com.exemple.quiz_app.matiere.repository.MatiereRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QuizService {

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

    @Autowired
    private MatiereRepository matiereRepository;

    @Transactional
    public QuizReponse createQuiz(QuizRequest request) {
        User enseignant = authService.getCurrentUser();

        if (enseignant.getRole() != Role.ENSEIGNANT && enseignant.getRole() != Role.ADMIN) {
            throw new RuntimeException("Seul un enseignant peut creer un quiz");
        }

        Quiz quiz = new Quiz();
        quiz.setTitre(request.getTitre());
        quiz.setDescription(cleanText(request.getDescription()));
        quiz.setDifficulty(normalizeDifficulty(request.getDifficulty()));
        quiz.setQuestionCount(0);
        quiz.setAvailableFrom(request.getAvailableFrom());
        quiz.setAvailableUntil(request.getAvailableUntil());
        quiz.setTimeLimit(request.getTimeLimit());
        quiz.setEnseignant(enseignant);
        quiz.setStatus(Quiz.QuizStatus.DRAFT);

        if ("AI".equalsIgnoreCase(request.getCreationType())) {
            quiz.setCreationType(Quiz.CreationType.AI);
        } else {
            quiz.setCreationType(Quiz.CreationType.MANUAL);
        }

        if (request.getClasseId() != null) {
            Classe classe = classeRepository.findById(request.getClasseId())
                    .orElseThrow(() -> new RuntimeException("Classe introuvable"));
            quiz.setClasse(classe);
        }

        if (request.getMatiereId() != null) {
            Matiere matiere = matiereRepository.findById(request.getMatiereId())
                    .orElseThrow(() -> new RuntimeException("Matiere introuvable"));
            quiz.setMatiere(matiere);
            if (quiz.getClasse() == null) {
                quiz.setClasse(matiere.getClasse());
            }
        }

        quiz.setTheme(resolveTheme(request, quiz.getMatiere()));

        Quiz savedQuiz = quizRepository.save(quiz);
        assignClassStudentsToQuiz(savedQuiz);

        return mapToResponse(savedQuiz);
    }

    @Transactional
    public List<QuizReponse> getMyQuizzes() {
        User enseignant = authService.getCurrentUser();

        return quizRepository.findByEnseignant(enseignant)
                .stream()
                .peek(this::assignClassStudentsToQuiz)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public QuizReponse getQuizById(Long id) {
        User currentUser = authService.getCurrentUser();

        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

        if (currentUser.getRole() != Role.ADMIN &&
                !quiz.getEnseignant().getId().equals(currentUser.getId())) {
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
        quiz.setDescription(cleanText(request.getDescription()));
        quiz.setDifficulty(normalizeDifficulty(request.getDifficulty()));
        quiz.setAvailableFrom(request.getAvailableFrom());
        quiz.setAvailableUntil(request.getAvailableUntil());
        quiz.setTimeLimit(request.getTimeLimit());

        if ("AI".equalsIgnoreCase(request.getCreationType())) {
            quiz.setCreationType(Quiz.CreationType.AI);
        } else {
            quiz.setCreationType(Quiz.CreationType.MANUAL);
        }

        if (request.getClasseId() != null) {
            Classe classe = classeRepository.findById(request.getClasseId())
                    .orElseThrow(() -> new RuntimeException("Classe introuvable"));
            quiz.setClasse(classe);
        } else {
            quiz.setClasse(null);
        }

        if (request.getMatiereId() != null) {
            Matiere matiere = matiereRepository.findById(request.getMatiereId())
                    .orElseThrow(() -> new RuntimeException("Matiere introuvable"));
            quiz.setMatiere(matiere);
            if (quiz.getClasse() == null) {
                quiz.setClasse(matiere.getClasse());
            }
        } else {
            quiz.setMatiere(null);
        }

        quiz.setTheme(resolveTheme(request, quiz.getMatiere()));

        Quiz savedQuiz = quizRepository.save(quiz);
        assignClassStudentsToQuiz(savedQuiz);

        return mapToResponse(savedQuiz);
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

        if (quiz.getQuestionCount() == null || quiz.getQuestionCount() < 15) {
            throw new RuntimeException("Ajoutez au moins 15 questions avant de publier");
        }

        assignClassStudentsToQuiz(quiz);

        if (quizRepository.countAllowedStudents(id) == 0) {
            throw new RuntimeException("Ajoutez au moins un etudiant avant de publier");
        }

        quiz.setStatus(Quiz.QuizStatus.PUBLISHED);
        Quiz savedQuiz = quizRepository.save(quiz);
        notifyClassStudentsAboutPublishedQuiz(savedQuiz);
        return mapToResponse(savedQuiz);
    }

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
                    .orElseThrow(() -> new RuntimeException("Aucun compte pour l'email \"" + email + "\"."));

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

        if (!quiz.getEnseignant().getId().equals(enseignant.getId()) &&
                enseignant.getRole() != Role.ADMIN) {
            throw new RuntimeException("Acces non autorise");
        }

        return quizStudentRepository.findByQuiz(quiz)
                .stream()
                .map(qs -> {
                    StudentListDto.StudentInfo info = new StudentListDto.StudentInfo();
                    User s = qs.getStudent();

                    info.setNom(s.getLastName() != null ? s.getLastName() : "");
                    info.setPrenom(s.getFirstName() != null ? s.getFirstName() : "");
                    info.setEmail(s.getEmail());

                    if (s.getClasse() != null) {
                        info.setClasse(s.getClasse().getName());
                        info.setFiliere(s.getClasse().getFiliere());
                    } else {
                        info.setClasse("Non definie");
                        info.setFiliere("Non definie");
                    }

                    return info;
                })
                .collect(Collectors.toList());
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

    public Map<String, Object> getQuizStatus(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

        Map<String, Object> status = new HashMap<>();
        status.put("status", quiz.getStatus().name());
        status.put("isModifiable", quiz.isModifiable());
        status.put("isDeletable", quiz.isDeletable());
        status.put("isAvailable", quiz.isAvailable());
        status.put("questionCount", quiz.getQuestionCount());
        status.put("allowedStudentsCount", quizRepository.countAllowedStudents(quizId));
        status.put("canBePublished",
                quiz.getStatus() == Quiz.QuizStatus.DRAFT &&
                        quiz.getQuestionCount() != null &&
                        quiz.getQuestionCount() >= 15 &&
                        quizRepository.countAllowedStudents(quizId) > 0
        );

        return status;
    }

    public boolean isStudentAllowed(Long quizId, Long studentId) {
        return quizRepository.isStudentAllowed(quizId, studentId);
    }

    public int getStudentCount(Long quizId) {
        return quizRepository.countAllowedStudents(quizId);
    }

    private void notifyClassStudentsAboutPublishedQuiz(Quiz quiz) {
        List<User> students = quizStudentRepository.findByQuiz(quiz).stream()
                .map(QuizStudent::getStudent)
                .filter(student -> student != null && student.getRole() == Role.ETUDIANT)
                .collect(Collectors.toList());

        int attempted = 0;
        int sent = 0;

        for (User student : students) {
            if (student.getEmail() == null || student.getEmail().isBlank()) {
                continue;
            }

            attempted++;
            if (emailService.sendQuizPublishedEmail(student, quiz)) {
                sent++;
            }
        }

        System.out.println("[QuizService] Notifications quiz " + quiz.getId()
                + " envoyees: " + sent + "/" + attempted + " etudiant(s).");
    }

    private Classe resolveQuizClasse(Quiz quiz) {
        Classe classe = quiz.getClasse();

        if (classe == null && quiz.getMatiere() != null) {
            classe = quiz.getMatiere().getClasse();
            quiz.setClasse(classe);
        }

        return classe;
    }

    private int assignClassStudentsToQuiz(Quiz quiz) {
        Classe classe = resolveQuizClasse(quiz);

        if (classe == null) {
            return 0;
        }

        List<User> students = userRepository.findByClasseIdOrderByLastNameAscFirstNameAsc(classe.getId());
        int added = 0;

        for (User student : students) {
            if (student.getRole() != Role.ETUDIANT) {
                continue;
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

    private QuizReponse mapToResponse(Quiz quiz) {
        QuizReponse response = new QuizReponse();

        response.setId(quiz.getId());
        response.setTitre(quiz.getTitre());
        response.setTheme(quiz.getTheme());
        response.setDescription(quiz.getDescription());
        response.setDifficulty(quiz.getDifficulty());
        response.setQuestionCount(quiz.getQuestionCount());
        response.setAvailableFrom(quiz.getAvailableFrom());
        response.setAvailableUntil(quiz.getAvailableUntil());
        response.setTimeLimit(quiz.getTimeLimit());
        response.setStatus(quiz.getStatus().name());
        response.setCreationType(quiz.getCreationType().name());

        if (quiz.getEnseignant() != null) {
            response.setEnseignantNom(
                    quiz.getEnseignant().getFirstName() + " " + quiz.getEnseignant().getLastName()
            );
        }

        if (quiz.getClasse() != null) {
            response.setClasseId(quiz.getClasse().getId());
            response.setClasseName(quiz.getClasse().getName());
            response.setClassFiliere(quiz.getClasse().getFiliere());
            response.setClassNiveau(quiz.getClasse().getNiveau());
        }

        if (quiz.getMatiere() != null) {
            response.setMatiereId(quiz.getMatiere().getId());
            response.setMatiereName(quiz.getMatiere().getNom());

            if (response.getClasseId() == null && quiz.getMatiere().getClasse() != null) {
                response.setClasseId(quiz.getMatiere().getClasse().getId());
                response.setClasseName(quiz.getMatiere().getClasse().getName());
                response.setClassFiliere(quiz.getMatiere().getClasse().getFiliere());
                response.setClassNiveau(quiz.getMatiere().getClasse().getNiveau());
            }
        }

        response.setTotalStudentsAllowed(quizRepository.countAllowedStudents(quiz.getId()));
        response.setCreatedAt(quiz.getCreatedAt());

        return response;
    }

    private String cleanText(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeDifficulty(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "MOYEN";
        }

        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "FACILE", "DIFFICILE" -> normalized;
            default -> "MOYEN";
        };
    }

    private String resolveTheme(QuizRequest request, Matiere matiere) {
        if (matiere != null && matiere.getNom() != null && !matiere.getNom().trim().isEmpty()) {
            return matiere.getNom().trim();
        }

        if (request.getTheme() != null && !request.getTheme().trim().isEmpty()) {
            return request.getTheme().trim();
        }

        return request.getTitre();
    }
}
