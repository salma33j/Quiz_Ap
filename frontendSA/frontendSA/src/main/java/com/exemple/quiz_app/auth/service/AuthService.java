package com.exemple.quiz_app.auth.service;

import com.exemple.quiz_app.auth.dto.*;
import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.repository.UserRepository;
import com.exemple.quiz_app.auth.security.JwtUtil;
import com.exemple.quiz_app.classe.entity.Classe;
import com.exemple.quiz_app.classe.repository.ClasseRepository;
import com.exemple.quiz_app.quiz.repository.QuizSessionRepository;
import com.exemple.quiz_app.quiz.repository.QuizStudentRepository;
import com.exemple.quiz_app.reponse.repository.ReponseRepository;
import com.exemple.quiz_app.resultat.repository.ResultatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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
    private QuizSessionRepository quizSessionRepository;

    @Autowired
    private QuizStudentRepository quizStudentRepository;

    @Autowired
    private ResultatRepository resultatRepository;

    @Autowired
    private ReponseRepository reponseRepository;

    @Autowired
    private ClasseRepository classeRepository;

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

    public Map<String, Object> sendAdminEmail(AdminEmailRequest request) {
        String target = request.getTarget() != null ? request.getTarget().trim().toUpperCase() : "";
        String subject = request.getSubject() != null ? request.getSubject().trim() : "";
        String message = request.getMessage() != null ? request.getMessage().trim() : "";

        if (subject.isBlank()) {
            throw new IllegalArgumentException("L'objet de l'email est obligatoire.");
        }

        if (message.isBlank()) {
            throw new IllegalArgumentException("Le message de l'email est obligatoire.");
        }

        List<User> recipients = userRepository.findAll().stream()
                .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                .filter(user -> {
                    if ("ETUDIANTS".equals(target)) {
                        return user.getRole() == Role.ETUDIANT;
                    }
                    if ("ENSEIGNANTS".equals(target)) {
                        return user.getRole() == Role.ENSEIGNANT;
                    }
                    if ("TOUS".equals(target)) {
                        return true;
                    }
                    return false;
                })
                .toList();

        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("Aucun destinataire trouvé pour ce groupe.");
        }

        recipients.forEach(user -> emailService.sendAdminAnnouncement(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                subject,
                message
        ));

        return Map.of(
                "success", true,
                "message", "Email envoye avec succes.",
                "sentCount", recipients.size()
        );
    }

    // =========================================================
    // LOGIN
    // =========================================================
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return AuthResponse.error("Email ou mot de passe incorrect");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getFirstName() + " " + user.getLastName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
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
        user.setMustChangePassword(true);
        user.setCne(request.getCne());
        user.setCodeApoge(request.getCodeApoge());
        user.setClasse(findRequestedClasse(request));
        user = userRepository.save(user);

        emailService.sendEtudiantCredentials(
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
        response.setMessage("Compte etudiant cree avec succes. Un email a ete envoye a " + user.getEmail());
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

        emailService.sendEnseignantCredentials(
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
        response.setMessage("Compte enseignant cree avec succes. Un email a ete envoye a " + user.getEmail());
        response.setSuccess(true);
        return response;
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
            response.setEmail(user.getEmail());
            response.setRole(user.getRole().name());
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
    @Transactional(readOnly = true)
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
        if (requester.getId().equals(id)) return AuthResponse.error("Impossible de supprimer votre propre compte");

        User user = userRepository.findById(id).orElse(null);
        if (user == null) return AuthResponse.error("Utilisateur introuvable");

        reponseRepository.deleteByStudentId(id);
        resultatRepository.deleteByStudentId(id);
        quizSessionRepository.deleteByStudentId(id);
        quizStudentRepository.deleteByStudentId(id);
        userRepository.delete(user);
        return AuthResponse.success("Utilisateur supprime");
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
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            return AuthResponse.error("Email deja utilise");
        }
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        if (requester.getRole() == Role.ADMIN) {
            user.setCne(request.getCne());
            user.setCodeApoge(request.getCodeApoge());
            user.setClasse(findRequestedClasse(request));
        }
        userRepository.save(user);

        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getFirstName() + " " + user.getLastName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
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

    private Classe findRequestedClasse(RegisterRequest request) {
        Long classId = request.getClassId() != null ? request.getClassId() : request.getClasseId();
        if (classId == null) {
            return null;
        }
        return classeRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Classe introuvable"));
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
        if (user.getClasse() != null) {
            dto.setClassId(user.getClasse().getId());
            dto.setClasseId(user.getClasse().getId());
            dto.setClassName(user.getClasse().getName());
            dto.setClasseName(user.getClasse().getName());
            dto.setClassFiliere(user.getClasse().getFiliere());
            dto.setClasseFiliere(user.getClasse().getFiliere());
            dto.setClassNiveau(user.getClasse().getNiveau());
            dto.setClasseNiveau(user.getClasse().getNiveau());
        }
        dto.setBlocked(user.isBlocked());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
