package com.exemple.quiz_app.auth.service;

import com.exemple.quiz_app.auth.dto.*;
import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.repository.UserRepository;
import com.exemple.quiz_app.auth.security.JwtUtil;
import com.exemple.quiz_app.classe.entity.Classe;
import com.exemple.quiz_app.classe.repository.ClasseRepository;
import com.exemple.quiz_app.quiz.repository.QuizRepository;
import com.exemple.quiz_app.quiz.repository.QuizSessionRepository;
import com.exemple.quiz_app.quiz.repository.QuizStudentRepository;
import com.exemple.quiz_app.reponse.repository.ReponseRepository;
import com.exemple.quiz_app.resultat.repository.ResultatRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ClasseRepository classeRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizSessionRepository quizSessionRepository;

    @Autowired
    private QuizStudentRepository quizStudentRepository;

    @Autowired
    private ResultatRepository resultatRepository;

    @Autowired
    private ReponseRepository reponseRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // =========================================================
    // MÉTHODE UTILITAIRE : obtenir l'utilisateur connecté
    // =========================================================
    public User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().equals("anonymousUser")) {
            throw new RuntimeException("Utilisateur non authentifie");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    // =========================================================
    // ❌ REGISTER — DÉSACTIVÉ (plus d'inscription publique)
    // =========================================================
    public AuthResponse register(RegisterRequest request) {
        return AuthResponse.error("❌ L'inscription publique est désactivée. Seul l'administrateur peut créer des comptes.");
    }

    // =========================================================
    // LOGIN
    // =========================================================
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return AuthResponse.error("Email ou mot de passe incorrect");
        }
        if (user.isBlocked()) {
            return AuthResponse.error("Compte bloque. Veuillez contacter l'administrateur.");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getFirstName() + " " + user.getLastName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setMustChangePassword(user.isMustChangePassword());
        response.setMessage("Connexion reussie");
        response.setSuccess(true);
        response.setType("Bearer");
        return response;
    }

    // =========================================================
    // ✅ ADMIN : Créer un compte ÉTUDIANT
    // =========================================================
    public AuthResponse createEtudiant(RegisterRequest request) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) {
            return AuthResponse.error("Acces refuse - Reserve aux administrateurs");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return AuthResponse.error("Email deja utilise");
        }

        String provisoryPassword = genererMotDePasse("Etu");

        User user = new User(
                request.getFirstName(), request.getLastName(),
                request.getEmail(),
                passwordEncoder.encode(provisoryPassword),
                Role.ETUDIANT
        );

        if (request.getClassId() == null) {
            return AuthResponse.error("Classe obligatoire pour creer un etudiant");
        }

        Classe classe = classeRepository.findById(request.getClassId())
                .orElse(null);
        if (classe == null) {
            return AuthResponse.error("Classe introuvable");
        }

        if (request.getCne() == null || request.getCne().isBlank()) {
            return AuthResponse.error("CNE obligatoire");
        }
        if (request.getCodeApoge() == null || request.getCodeApoge().isBlank()) {
            return AuthResponse.error("Code Apogee obligatoire");
        }
        if (userRepository.existsByCne(request.getCne().trim())) {
            return AuthResponse.error("CNE deja utilise");
        }
        if (userRepository.existsByCodeApoge(request.getCodeApoge().trim())) {
            return AuthResponse.error("Code Apogee deja utilise");
        }

        user.setClasse(classe);
        user.setCne(request.getCne().trim());
        user.setCodeApoge(request.getCodeApoge().trim());
        user.setMustChangePassword(true);
        user = userRepository.save(user);

        boolean emailSent = emailService.sendEtudiantCredentials(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                provisoryPassword
        );

        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getFirstName() + " " + user.getLastName());
        response.setEmail(user.getEmail());
        response.setRole("ETUDIANT");
        response.setMessage(accountEmailMessage("Compte etudiant cree avec succes", user.getEmail(), emailSent));
        response.setSuccess(true);
        return response;
    }

    // =========================================================
    // ✅ ADMIN : Créer un compte ENSEIGNANT
    // =========================================================
    public AuthResponse createEnseignant(RegisterRequest request) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) {
            return AuthResponse.error("Acces refuse - Reserve aux administrateurs");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return AuthResponse.error("Email deja utilise");
        }

        String provisoryPassword = genererMotDePasse("Prof");

        User user = new User(
                request.getFirstName(), request.getLastName(),
                request.getEmail(),
                passwordEncoder.encode(provisoryPassword),
                Role.ENSEIGNANT
        );
        user.setMustChangePassword(true);
        user = userRepository.save(user);

        boolean emailSent = emailService.sendEnseignantCredentials(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                provisoryPassword
        );

        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getFirstName() + " " + user.getLastName());
        response.setEmail(user.getEmail());
        response.setRole("ENSEIGNANT");
        response.setMessage(accountEmailMessage("Compte enseignant cree avec succes", user.getEmail(), emailSent));
        response.setSuccess(true);
      return response;
  }

    public AuthResponse createAdmin(RegisterRequest request) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) {
            return AuthResponse.error("Acces refuse - Reserve aux administrateurs");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return AuthResponse.error("Email deja utilise");
        }

        String provisoryPassword = genererMotDePasse("Admin");

        User user = new User(
                request.getFirstName(), request.getLastName(),
                request.getEmail(),
                passwordEncoder.encode(provisoryPassword),
                Role.ADMIN
        );
        user.setMustChangePassword(true);
        user = userRepository.save(user);

        boolean emailSent = emailService.sendAdminCredentials(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                provisoryPassword
        );

        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getFirstName() + " " + user.getLastName());
        response.setEmail(user.getEmail());
        response.setRole("ADMIN");
        response.setMessage(accountEmailMessage("Compte admin cree avec succes", user.getEmail(), emailSent));
        response.setSuccess(true);
        return response;
    }

    @Transactional
    public int importUsers(String requestedRole, MultipartFile file) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) {
            throw new RuntimeException("Acces refuse - Reserve aux administrateurs");
        }

        Role role = parseImportRole(requestedRole);
        int imported = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String firstName = getCell(row, 0);
                String lastName = getCell(row, 1);
                String email = getCell(row, 2);

                if (firstName.isBlank() || lastName.isBlank() || email.isBlank()) {
                    continue;
                }
                if (userRepository.existsByEmail(email.trim())) {
                    continue;
                }

                String prefix = role == Role.ADMIN ? "Admin" : "Prof";
                String provisoryPassword = genererMotDePasse(prefix);
                User user = new User(
                        firstName.trim(),
                        lastName.trim(),
                        email.trim(),
                        passwordEncoder.encode(provisoryPassword),
                        role
                );
                user.setMustChangePassword(true);
                userRepository.save(user);

                if (role == Role.ADMIN) {
                    emailService.sendAdminCredentials(user.getEmail(), user.getFirstName(), user.getLastName(), provisoryPassword);
                } else {
                    emailService.sendEnseignantCredentials(user.getEmail(), user.getFirstName(), user.getLastName(), provisoryPassword);
                }
                imported++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur import Excel : " + e.getMessage());
        }

        return imported;
    }

    private Role parseImportRole(String requestedRole) {
        String normalized = String.valueOf(requestedRole).trim().toUpperCase();
        if (normalized.equals("ADMIN")) return Role.ADMIN;
        if (normalized.equals("ENSEIGNANT") || normalized.equals("TEACHER")) return Role.ENSEIGNANT;
        throw new RuntimeException("Import Excel disponible seulement pour enseignants et admins.");
    }

    private String getCell(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    // =========================================================
    // GET CURRENT USER INFO
    // =========================================================
    public AuthResponse getCurrentUserInfo() {
        try {
            User user = getCurrentUser();
            AuthResponse response = new AuthResponse();
            response.setUserId(user.getId());
            response.setUsername(user.getFirstName() + " " + user.getLastName());
            response.setFullName(user.getFirstName() + " " + user.getLastName());
            response.setEmail(user.getEmail());
            response.setRole(user.getRole().name());
            response.setCreatedAt(user.getCreatedAt());
            response.setUpdatedAt(user.getUpdatedAt());
            response.setMustChangePassword(user.isMustChangePassword());
            response.setMessage("Utilisateur trouve");
            response.setSuccess(true);
            return response;
        } catch (RuntimeException e) {
            return AuthResponse.error(e.getMessage());
        }
    }

    // =========================================================
    // GET ALL USERS (admin seulement)
    // =========================================================
    public List<UserDto> getAllUsers() {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) {
            throw new RuntimeException("Acces refuse - Reserve aux administrateurs");
        }
        return userRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // =========================================================
    // GET USER BY ID
    // =========================================================
    public AuthResponse getUserById(Long id) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN && !requester.getId().equals(id)) {
            return AuthResponse.error("Acces refuse");
        }
        return userRepository.findById(id)
                .map(user -> {
                    AuthResponse response = new AuthResponse();
                    response.setUserId(user.getId());
                    response.setUsername(user.getFirstName() + " " + user.getLastName());
                    response.setEmail(user.getEmail());
                    response.setRole(user.getRole().name());
                    response.setMustChangePassword(user.isMustChangePassword());
                    response.setMessage("OK");
                    response.setSuccess(true);
                    return response;
                })
                .orElse(AuthResponse.error("Utilisateur introuvable"));
    }

    // =========================================================
    // PROMOTE TO TEACHER (admin seulement)
    // =========================================================
    public AuthResponse promoteToTeacher(Long userId) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) {
            return AuthResponse.error("Acces refuse - Reserve aux administrateurs");
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return AuthResponse.error("Utilisateur introuvable");
        if (user.getRole() == Role.ENSEIGNANT) return AuthResponse.error("L'utilisateur est deja enseignant");
        user.setRole(Role.ENSEIGNANT);
        userRepository.save(user);

        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getFirstName() + " " + user.getLastName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setMessage("Utilisateur promu enseignant");
        response.setSuccess(true);
        return response;
    }

    // =========================================================
    // DELETE USER (admin seulement)
    // =========================================================
    @Transactional
    public AuthResponse deleteUser(Long id) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) return AuthResponse.error("Acces refuse");
        if (requester.getId().equals(id)) {
            return AuthResponse.error("Impossible de supprimer votre propre compte");
        }

        User user = userRepository.findById(id).orElse(null);
        if (user == null) return AuthResponse.error("Utilisateur introuvable");

        List<Classe> ownedClasses = classeRepository.findByEnseignantOrderByCreatedAtDesc(user);
        if (!ownedClasses.isEmpty()) {
            String classNames = ownedClasses.stream()
                    .map(Classe::getName)
                    .collect(Collectors.joining(", "));
            return AuthResponse.error("Impossible de supprimer cet utilisateur : il est responsable de la classe " + classNames + ". Modifiez d'abord cette classe.");
        }

        if (!quizRepository.findByEnseignant(user).isEmpty()) {
            return AuthResponse.error("Impossible de supprimer cet utilisateur : il possede encore des quiz.");
        }

        cleanupUserForeignKeys(id);

        if (user.getRole() == Role.ETUDIANT) {
            user.setClasse(null);
            userRepository.save(user);
        }

        userRepository.delete(user);
        return AuthResponse.success("Utilisateur supprime");
    }

    private void cleanupUserForeignKeys(Long userId) {
        entityManager.createNativeQuery("DELETE FROM reponses WHERE student_id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.createNativeQuery("DELETE FROM resultats WHERE student_id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.createNativeQuery("DELETE FROM quiz_session WHERE student_id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.createNativeQuery("DELETE FROM quiz_students WHERE student_id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.createNativeQuery("DELETE FROM classe_enseignants WHERE enseignant_id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.createNativeQuery("DELETE FROM matieres WHERE enseignant_id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.createNativeQuery("UPDATE users SET classe_id = NULL WHERE id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.flush();
    }

    public AuthResponse sendAnnouncement(String target, String subject, String message) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) return AuthResponse.error("Acces refuse");
        if (subject == null || subject.isBlank() || message == null || message.isBlank()) {
            return AuthResponse.error("Objet et message obligatoires");
        }

        String normalizedTarget = String.valueOf(target).trim().toUpperCase();
        List<User> recipients = userRepository.findAll().stream()
                .filter(user -> {
                    if ("ETUDIANTS".equals(normalizedTarget)) return user.getRole() == Role.ETUDIANT;
                    if ("ENSEIGNANTS".equals(normalizedTarget)) return user.getRole() == Role.ENSEIGNANT;
                    return true;
                })
                .filter(user -> !user.isBlocked())
                .collect(Collectors.toList());

        int sent = 0;
        for (User user : recipients) {
            if (emailService.sendAnnouncement(user.getEmail(), subject, message)) {
                sent++;
            }
        }

        if (recipients.isEmpty()) {
            return AuthResponse.error("Aucun destinataire trouve");
        }
        if (sent == 0) {
            return AuthResponse.error("Aucun email n'a pu etre envoye sur " + recipients.size() + " destinataire(s)");
        }

        return AuthResponse.success(sent + "/" + recipients.size() + " email(s) envoye(s)");
    }

    public AuthResponse blockUser(Long id) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) return AuthResponse.error("Acces refuse");
        if (requester.getId().equals(id)) return AuthResponse.error("Impossible de bloquer votre propre compte");

        User user = userRepository.findById(id).orElse(null);
        if (user == null) return AuthResponse.error("Utilisateur introuvable");

        user.setBlocked(true);
        userRepository.save(user);
        return AuthResponse.success("Utilisateur bloque");
    }

    public AuthResponse unblockUser(Long id) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) return AuthResponse.error("Acces refuse");

        User user = userRepository.findById(id).orElse(null);
        if (user == null) return AuthResponse.error("Utilisateur introuvable");

        user.setBlocked(false);
        userRepository.save(user);
        return AuthResponse.success("Utilisateur debloque");
    }

    public AuthResponse resetPasswordByAdmin(Long id) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) return AuthResponse.error("Acces refuse");

        User user = userRepository.findById(id).orElse(null);
        if (user == null) return AuthResponse.error("Utilisateur introuvable");

        String prefix = user.getRole() == Role.ADMIN
                ? "Admin"
                : user.getRole() == Role.ENSEIGNANT ? "Prof" : "Etu";
        String provisoryPassword = genererMotDePasse(prefix);

        user.setPassword(passwordEncoder.encode(provisoryPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);

        boolean emailSent = sendCredentialsEmail(user, provisoryPassword);

        AuthResponse response = AuthResponse.success(emailSent
                ? "Mot de passe reinitialise et envoye par email"
                : "Mot de passe reinitialise, mais l'email n'a pas pu etre envoye");
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        return response;
    }

    // =========================================================
    // UPDATE PROFILE
    // =========================================================
    public AuthResponse updateProfile(Long id, RegisterRequest request) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN && !requester.getId().equals(id)) {
            return AuthResponse.error("Acces refuse");
        }
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return AuthResponse.error("Utilisateur introuvable");

        String firstName = request.getFirstName() == null ? "" : request.getFirstName().trim();
        String lastName = request.getLastName() == null ? "" : request.getLastName().trim();
        String email = request.getEmail() == null ? "" : request.getEmail().trim();

        if (firstName.isBlank()) return AuthResponse.error("Le prenom est obligatoire");
        if (lastName.isBlank()) return AuthResponse.error("Le nom est obligatoire");
        if (email.isBlank() || !email.contains("@")) return AuthResponse.error("Email invalide");

        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmail(email)) {
            return AuthResponse.error("Email deja utilise");
        }
        if (user.getRole() == Role.ETUDIANT) {
            if (request.getCne() == null || request.getCne().isBlank()) {
                return AuthResponse.error("CNE obligatoire");
            }
            if (request.getCodeApoge() == null || request.getCodeApoge().isBlank()) {
                return AuthResponse.error("Code Apogee obligatoire");
            }
            String cne = request.getCne().trim();
            String codeApoge = request.getCodeApoge().trim();
            if (userRepository.existsByCneAndIdNot(cne, id)) {
                return AuthResponse.error("CNE deja utilise");
            }
            if (userRepository.existsByCodeApogeAndIdNot(codeApoge, id)) {
                return AuthResponse.error("Code Apogee deja utilise");
            }
        }
      user.setFirstName(firstName);
      user.setLastName(lastName);
      user.setEmail(email);
      if (user.getRole() == Role.ETUDIANT) {
          user.setCne(request.getCne().trim());
          user.setCodeApoge(request.getCodeApoge().trim());
      }
      userRepository.save(user);

        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getFirstName() + " " + user.getLastName());
        response.setFullName(user.getFirstName() + " " + user.getLastName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setToken(jwtUtil.generateToken(user.getEmail(), user.getRole().name()));
        response.setMessage("Profil mis a jour");
        response.setSuccess(true);
        return response;
    }

    // =========================================================
    // CHANGE PASSWORD
    // =========================================================
    public AuthResponse changePassword(Long id, ChangePasswordRequest request) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN && !requester.getId().equals(id)) {
            return AuthResponse.error("Acces refuse");
        }
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return AuthResponse.error("Utilisateur introuvable");
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return AuthResponse.error("Ancien mot de passe incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        return AuthResponse.success("Mot de passe modifie avec succes");
    }

    // =========================================================
    // MÉTHODE UTILITAIRE : générer un mot de passe provisoire
    // =========================================================
    private String genererMotDePasse(String prefix) {
        int nombre = (int) (Math.random() * 9000 + 1000);
        String[] specials = {"@", "#", "!", "&"};
        String special = specials[(int) (Math.random() * specials.length)];
        return prefix + special + nombre;
    }

    private boolean sendCredentialsEmail(User user, String password) {
        if (user.getRole() == Role.ADMIN) {
            return emailService.sendAdminCredentials(user.getEmail(), user.getFirstName(), user.getLastName(), password);
        } else if (user.getRole() == Role.ENSEIGNANT) {
            return emailService.sendEnseignantCredentials(user.getEmail(), user.getFirstName(), user.getLastName(), password);
        }
        return emailService.sendEtudiantCredentials(user.getEmail(), user.getFirstName(), user.getLastName(), password);
    }

    private String accountEmailMessage(String prefix, String email, boolean emailSent) {
        if (emailSent) {
            return prefix + ". Un email a ete envoye a " + email;
        }
        return prefix + ", mais l'email n'a pas pu etre envoye. Verifiez la configuration email.";
    }

    // =========================================================
    // MAPPER User → UserDto
    // =========================================================
    private UserDto mapToDto(User user) {
        UserDto dto = new UserDto();

        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());

        dto.setCne(user.getCne());
        dto.setCodeApoge(user.getCodeApoge());

        dto.setBlocked(user.isBlocked());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        if (user.getClasse() != null) {
            dto.setClassId(user.getClasse().getId());
            dto.setClassName(user.getClasse().getName());
            dto.setClassFiliere(user.getClasse().getFiliere());
            dto.setClassNiveau(user.getClasse().getNiveau());
        }

        return dto;
    }
}
