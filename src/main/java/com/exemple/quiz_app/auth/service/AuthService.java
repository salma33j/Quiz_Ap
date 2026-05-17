package com.exemple.quiz_app.auth.service;

import com.exemple.quiz_app.auth.dto.*;
import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.repository.UserRepository;
import com.exemple.quiz_app.auth.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    public AuthResponse deleteUser(Long id) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) return AuthResponse.error("Acces refuse");
        if (!userRepository.existsById(id)) return AuthResponse.error("Utilisateur introuvable");
        userRepository.deleteById(id);
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
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}