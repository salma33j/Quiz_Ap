package com.exemple.quiz_app.classe.service;

import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.repository.UserRepository;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.classe.dto.ClasseRequest;
import com.exemple.quiz_app.classe.dto.ClasseResponse;
import com.exemple.quiz_app.classe.dto.StudentRequest;
import com.exemple.quiz_app.classe.dto.StudentResponse;
import com.exemple.quiz_app.classe.entity.Classe;
import com.exemple.quiz_app.classe.repository.ClasseRepository;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.quiz.entity.QuizStudent;
import com.exemple.quiz_app.quiz.repository.QuizRepository;
import com.exemple.quiz_app.quiz.repository.QuizStudentRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClasseService {

    private final ClasseRepository classeRepository;
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final QuizStudentRepository quizStudentRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    public ClasseService(
            ClasseRepository classeRepository,
            UserRepository userRepository,
            QuizRepository quizRepository,
            QuizStudentRepository quizStudentRepository,
            AuthService authService,
            PasswordEncoder passwordEncoder
    ) {
        this.classeRepository = classeRepository;
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;
        this.quizStudentRepository = quizStudentRepository;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
    }

    public List<ClasseResponse> getMyClasses() {
        User enseignant = authService.getCurrentUser();

        return classeRepository.findByEnseignantOrderByCreatedAtDesc(enseignant)
                .stream()
                .map(this::toClasseResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClasseResponse createClasse(ClasseRequest request) {
        User enseignant = authService.getCurrentUser();

        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Le nom de la classe est obligatoire.");
        }

        if (classeRepository.existsByNameAndEnseignant(request.getName().trim(), enseignant)) {
            throw new RuntimeException("Cette classe existe déjà.");
        }

        Classe classe = new Classe();
        classe.setName(request.getName().trim());
        classe.setFiliere(request.getFiliere());
        classe.setNiveau(request.getNiveau());
        classe.setEnseignant(enseignant);

        return toClasseResponse(classeRepository.save(classe));
    }

    @Transactional
    public void deleteClasse(Long classId) {
        User enseignant = authService.getCurrentUser();

        Classe classe = getClasseOwnedByTeacher(classId, enseignant);

        List<User> students = userRepository.findByClasseIdOrderByLastNameAscFirstNameAsc(classId);

        for (User student : students) {
            student.setClasse(null);
            userRepository.save(student);
        }

        classeRepository.delete(classe);
    }

    public List<StudentResponse> getStudents(Long classId) {
        User enseignant = authService.getCurrentUser();

        getClasseOwnedByTeacher(classId, enseignant);

        return userRepository.findByClasseIdOrderByLastNameAscFirstNameAsc(classId)
                .stream()
                .map(this::toStudentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public StudentResponse addStudent(Long classId, StudentRequest request) {
        User enseignant = authService.getCurrentUser();
        Classe classe = getClasseOwnedByTeacher(classId, enseignant);

        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            throw new RuntimeException("Le prénom est obligatoire.");
        }

        if (request.getLastName() == null || request.getLastName().isBlank()) {
            throw new RuntimeException("Le nom est obligatoire.");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("L'email est obligatoire.");
        }

        User student = userRepository.findByEmail(request.getEmail().trim())
                .orElse(null);

        if (student == null) {
            student = new User();
            student.setFirstName(request.getFirstName().trim());
            student.setLastName(request.getLastName().trim());
            student.setEmail(request.getEmail().trim());
            student.setRole(Role.ETUDIANT);
            student.setPassword(passwordEncoder.encode("Etudiant@123"));
            student.setMustChangePassword(true);
        }

        student.setCne(request.getCne());
        student.setClasse(classe);

        return toStudentResponse(userRepository.save(student));
    }

    @Transactional
    public void deleteStudentFromClass(Long classId, Long studentId) {
        User enseignant = authService.getCurrentUser();

        getClasseOwnedByTeacher(classId, enseignant);

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Étudiant introuvable."));

        student.setClasse(null);
        userRepository.save(student);
    }

    @Transactional
    public int importStudents(Long classId, MultipartFile file) {
        User enseignant = authService.getCurrentUser();
        Classe classe = getClasseOwnedByTeacher(classId, enseignant);

        int imported = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (row == null) continue;

                String cne = getCell(row, 0);
                String firstName = getCell(row, 1);
                String lastName = getCell(row, 2);
                String email = getCell(row, 3);

                if (email == null || email.isBlank()) continue;

                User student = userRepository.findByEmail(email.trim())
                        .orElse(null);

                if (student == null) {
                    student = new User();
                    student.setFirstName(firstName);
                    student.setLastName(lastName);
                    student.setEmail(email);
                    student.setRole(Role.ETUDIANT);
                    student.setPassword(passwordEncoder.encode("Etudiant@123"));
                    student.setMustChangePassword(true);
                }

                student.setCne(cne);
                student.setClasse(classe);

                userRepository.save(student);
                imported++;
            }

        } catch (Exception e) {
            throw new RuntimeException("Erreur import Excel : " + e.getMessage());
        }

        return imported;
    }

    @Transactional
    public int assignQuizToClass(Long quizId, Long classId) {
        User enseignant = authService.getCurrentUser();

        Classe classe = getClasseOwnedByTeacher(classId, enseignant);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz introuvable."));

        if (!quiz.getEnseignant().getId().equals(enseignant.getId())) {
            throw new RuntimeException("Vous n'êtes pas propriétaire de ce quiz.");
        }

        List<User> students = userRepository.findByClasseIdOrderByLastNameAscFirstNameAsc(classe.getId());

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

        return added;
    }

    private Classe getClasseOwnedByTeacher(Long classId, User enseignant) {
        Classe classe = classeRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Classe introuvable."));

        if (!classe.getEnseignant().getId().equals(enseignant.getId())) {
            throw new RuntimeException("Accès interdit à cette classe.");
        }

        return classe;
    }

    private ClasseResponse toClasseResponse(Classe classe) {
        ClasseResponse response = new ClasseResponse();
        response.setId(classe.getId());
        response.setName(classe.getName());
        response.setFiliere(classe.getFiliere());
        response.setNiveau(classe.getNiveau());
        response.setStudentCount(
                (long) userRepository.findByClasseIdOrderByLastNameAscFirstNameAsc(classe.getId()).size()
        );
        return response;
    }

    private StudentResponse toStudentResponse(User user) {
        StudentResponse response = new StudentResponse();
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setCne(user.getCne());
        return response;
    }

    private String getCell(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return "";

        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
}