package com.exemple.quiz_app.classe.service;

import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.repository.UserRepository;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.auth.service.EmailService;
import com.exemple.quiz_app.classe.dto.ClassStudentDto;
import com.exemple.quiz_app.classe.dto.ClassStudentRequest;
import com.exemple.quiz_app.classe.dto.ClasseDto;
import com.exemple.quiz_app.classe.dto.ClasseRequest;
import com.exemple.quiz_app.classe.entity.Classe;
import com.exemple.quiz_app.classe.repository.ClasseRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClasseService {

    private final ClasseRepository classeRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public ClasseService(
            ClasseRepository classeRepository,
            UserRepository userRepository,
            AuthService authService,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.classeRepository = classeRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public List<ClasseDto> getVisibleClasses() {
        User currentUser = authService.getCurrentUser();

        if (currentUser.getRole() == Role.ADMIN) {
            return classeRepository.findAll().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }

        if (currentUser.getRole() == Role.ENSEIGNANT) {
            return classeRepository.findByEnseignants_Id(currentUser.getId()).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }

        throw new RuntimeException("Acces reserve aux enseignants");
    }

    public List<ClasseDto> getAllClasses() {
        requireAdmin();
        return classeRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClasseDto createClasse(ClasseRequest request) {
        requireAdmin();

        String name = clean(request.getName());
        if (name.isBlank()) {
            throw new RuntimeException("Le nom de la classe est obligatoire");
        }

        Classe classe = new Classe();
        classe.setName(name);
        classe.setFiliere(clean(request.getFiliere()));
        classe.setNiveau(clean(request.getNiveau()));
        classe.setEnseignants(loadTeachers(request.getTeacherIds()));

        return toDto(classeRepository.save(classe));
    }

    @Transactional
    public ClasseDto updateClasse(Long id, ClasseRequest request) {
        requireAdmin();

        Classe classe = getClasse(id);
        String name = clean(request.getName());
        if (name.isBlank()) {
            throw new RuntimeException("Le nom de la classe est obligatoire");
        }

        classe.setName(name);
        classe.setFiliere(clean(request.getFiliere()));
        classe.setNiveau(clean(request.getNiveau()));
        classe.setEnseignants(loadTeachers(request.getTeacherIds()));

        return toDto(classeRepository.save(classe));
    }

    @Transactional
    public void deleteClasse(Long id) {
        requireAdmin();
        Classe classe = getClasse(id);

        for (User student : userRepository.findByClasseId(id)) {
            student.setClasse(null);
            userRepository.save(student);
        }

        classe.getEnseignants().clear();
        classeRepository.delete(classe);
    }

    @Transactional
    public ClasseDto assignTeachers(Long classId, List<Long> teacherIds) {
        requireAdmin();
        Classe classe = getClasse(classId);
        classe.setEnseignants(loadTeachers(teacherIds));
        return toDto(classeRepository.save(classe));
    }

    public List<ClassStudentDto> getStudents(Long classId) {
        Classe classe = getClasse(classId);
        ensureCanReadClasse(classe);

        return userRepository.findByClasseId(classId).stream()
                .filter(user -> user.getRole() == Role.ETUDIANT || user.getRole() == Role.ADMIN)
                .map(this::toStudentDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClassStudentDto addStudent(Long classId, ClassStudentRequest request) {
        requireAdmin();
        Classe classe = getClasse(classId);
        User student;

        if (request.getStudentId() != null) {
            student = userRepository.findById(request.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Etudiant introuvable"));
        } else {
            String email = clean(request.getEmail()).toLowerCase();
            if (email.isBlank()) {
                throw new RuntimeException("L'email de l'etudiant est obligatoire");
            }

            student = userRepository.findByEmail(email).orElse(null);
            if (student == null) {
                String password = genererMotDePasse("Etu");
                student = new User(
                        clean(request.getFirstName()),
                        clean(request.getLastName()),
                        email,
                        passwordEncoder.encode(password),
                        Role.ETUDIANT
                );
                student.setMustChangePassword(true);
                student.setCne(clean(request.getCne()));
                student.setCodeApoge(clean(request.getCodeApoge()));
                student.setClasse(classe);
                student = userRepository.save(student);
                emailService.sendEtudiantCredentials(email, student.getFirstName(), student.getLastName(), password);
                return toStudentDto(student);
            }
        }

        if (student.getRole() != Role.ETUDIANT && student.getRole() != Role.ADMIN) {
            throw new RuntimeException("Le compte selectionne n'est pas un etudiant");
        }

        if (request.getCne() != null) student.setCne(clean(request.getCne()));
        if (request.getCodeApoge() != null) student.setCodeApoge(clean(request.getCodeApoge()));
        student.setClasse(classe);
        return toStudentDto(userRepository.save(student));
    }

    @Transactional
    public void removeStudent(Long classId, Long studentId) {
        requireAdmin();
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Etudiant introuvable"));

        if (student.getClasse() != null && student.getClasse().getId().equals(classId)) {
            student.setClasse(null);
            userRepository.save(student);
        }
    }

    @Transactional
    public Map<String, Object> importStudents(Long classId, MultipartFile file) {
        requireAdmin();
        Classe classe = getClasse(classId);

        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            int rowNumber = 0;

            for (Row row : sheet) {
                rowNumber++;
                if (rowNumber == 1 || isEmpty(row)) continue;

                try {
                    String cne = getCellValue(row, 0);
                    String firstName = getCellValue(row, 1);
                    String lastName = getCellValue(row, 2);
                    String email = clean(getCellValue(row, 3)).toLowerCase();
                    String codeApoge = getCellValue(row, 4);

                    if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
                        skipped++;
                        errors.add("Ligne " + rowNumber + " : nom ou prenom manquant");
                        continue;
                    }
                    if (email.isBlank() || !email.contains("@")) {
                        skipped++;
                        errors.add("Ligne " + rowNumber + " : email invalide");
                        continue;
                    }

                    User student = userRepository.findByEmail(email).orElse(null);
                    if (student == null) {
                        String password = genererMotDePasse("Etu");
                        student = new User(firstName, lastName, email, passwordEncoder.encode(password), Role.ETUDIANT);
                        student.setMustChangePassword(true);
                        student.setCne(clean(cne));
                        student.setCodeApoge(clean(codeApoge));
                        student.setClasse(classe);
                        userRepository.save(student);
                        emailService.sendEtudiantCredentials(email, firstName, lastName, password);
                    } else {
                        student.setCne(clean(cne));
                        student.setCodeApoge(clean(codeApoge));
                        student.setClasse(classe);
                        userRepository.save(student);
                    }

                    imported++;
                } catch (Exception e) {
                    skipped++;
                    errors.add("Ligne " + rowNumber + " : " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Impossible de lire le fichier Excel : " + e.getMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("imported", imported);
        response.put("skipped", skipped);
        response.put("errors", errors);
        return response;
    }

    public Classe getClasse(Long id) {
        return classeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Classe introuvable"));
    }

    private Set<User> loadTeachers(List<Long> teacherIds) {
        if (teacherIds == null || teacherIds.isEmpty()) {
            return new HashSet<>();
        }

        return teacherIds.stream()
                .filter(Objects::nonNull)
                .map(id -> userRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Enseignant introuvable : " + id)))
                .peek(user -> {
                    if (user.getRole() != Role.ENSEIGNANT && user.getRole() != Role.ADMIN) {
                        throw new RuntimeException("Le compte " + user.getEmail() + " n'est pas un enseignant");
                    }
                })
                .collect(Collectors.toSet());
    }

    private void ensureCanReadClasse(Classe classe) {
        User currentUser = authService.getCurrentUser();

        if (currentUser.getRole() == Role.ADMIN) return;

        boolean assignedTeacher = classe.getEnseignants().stream()
                .anyMatch(teacher -> teacher.getId().equals(currentUser.getId()));

        if (!assignedTeacher) {
            throw new RuntimeException("Vous n'avez pas acces a cette classe");
        }
    }

    private void requireAdmin() {
        User currentUser = authService.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Acces reserve aux administrateurs");
        }
    }

    private ClasseDto toDto(Classe classe) {
        return ClasseDto.builder()
                .id(classe.getId())
                .name(classe.getName())
                .filiere(classe.getFiliere())
                .niveau(classe.getNiveau())
                .studentCount(userRepository.findByClasseId(classe.getId()).size())
                .enseignantIds(classe.getEnseignants().stream().map(User::getId).collect(Collectors.toList()))
                .enseignantNames(classe.getEnseignants().stream().map(User::getFullName).collect(Collectors.toList()))
                .build();
    }

    private ClassStudentDto toStudentDto(User user) {
        return ClassStudentDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .cne(user.getCne())
                .codeApoge(user.getCodeApoge())
                .classId(user.getClasse() != null ? user.getClasse().getId() : null)
                .className(user.getClasse() != null ? user.getClasse().getName() : null)
                .build();
    }

    private boolean isEmpty(Row row) {
        if (row == null) return true;
        for (int index = 0; index < 5; index++) {
            Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private String getCellValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String genererMotDePasse(String prefix) {
        int number = (int) (Math.random() * 9000 + 1000);
        String[] specials = {"@", "#", "!", "&"};
        String special = specials[(int) (Math.random() * specials.length)];
        return prefix + special + number;
    }
}
