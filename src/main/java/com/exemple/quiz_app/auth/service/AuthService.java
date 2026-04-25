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

import java.math.BigInteger;
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

    // ================= CURRENT USER =================

    public User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().equals("anonymousUser")) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    // ================= REGISTER =================

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return AuthResponse.error("Email déjà utilisé");
        }

        Role role = Role.ETUDIANT;

        User user = new User(
                request.getNom(),
                request.getEmail(),
                passwordEncoder.encode(request.getMotDePasse()),
                role
        );

        user = userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUserId(user.getId());  // 🔥 setUserId au lieu de setId
        response.setUsername(user.getNom());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setMessage("Inscription réussie");
        response.setSuccess(true);
        response.setType("Bearer");

        return response;
    }

    // ================= LOGIN =================

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return AuthResponse.error("Email ou mot de passe incorrect");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUserId(user.getId());  // 🔥 setUserId au lieu de setId
        response.setUsername(user.getNom());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setMessage("Connexion réussie");
        response.setSuccess(true);
        response.setType("Bearer");

        return response;
    }

    // ================= GET CURRENT USER INFO =================

    public AuthResponse getCurrentUserInfo() {
        try {
            User user = getCurrentUser();
            AuthResponse response = new AuthResponse();
            response.setUserId(user.getId());  // 🔥 setUserId au lieu de setId
            response.setUsername(user.getNom());
            response.setEmail(user.getEmail());
            response.setRole(user.getRole().name());
            response.setMessage("Utilisateur trouvé");
            response.setSuccess(true);
            return response;
        } catch (RuntimeException e) {
            return AuthResponse.error(e.getMessage());
        }
    }

    // ================= ADMIN : GESTION DES UTILISATEURS =================

    public List<UserDto> getAllUsers() {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) {
            throw new RuntimeException("Accès refusé - Réservé aux administrateurs");
        }
        return userRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public AuthResponse getUserById(BigInteger id) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN && !requester.getId().equals(id)) {
            return AuthResponse.error("Accès refusé");
        }

        return userRepository.findById(id)
                .map(user -> {
                    AuthResponse response = new AuthResponse();
                    response.setUserId(user.getId());  // 🔥 setUserId au lieu de setId
                    response.setUsername(user.getNom());
                    response.setEmail(user.getEmail());
                    response.setRole(user.getRole().name());
                    response.setMessage("OK");
                    response.setSuccess(true);
                    return response;
                })
                .orElse(AuthResponse.error("Utilisateur introuvable"));
    }

    public AuthResponse promoteToTeacher(BigInteger userId) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) {
            return AuthResponse.error("Accès refusé - Réservé aux administrateurs");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return AuthResponse.error("Utilisateur introuvable");
        }
        if (user.getRole() == Role.ENSEIGNANT) {
            return AuthResponse.error("L'utilisateur est déjà enseignant");
        }

        user.setRole(Role.ENSEIGNANT);
        userRepository.save(user);

        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());  // 🔥 setUserId au lieu de setId
        response.setUsername(user.getNom());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setMessage("Utilisateur promu enseignant");
        response.setSuccess(true);
        return response;
    }

    public AuthResponse deleteUser(BigInteger id) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) {
            return AuthResponse.error("Accès refusé");
        }
        if (!userRepository.existsById(id)) {
            return AuthResponse.error("Utilisateur introuvable");
        }
        userRepository.deleteById(id);
        return AuthResponse.success("Utilisateur supprimé");
    }

    public AuthResponse updateProfile(BigInteger id, RegisterRequest request) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN && !requester.getId().equals(id)) {
            return AuthResponse.error("Accès refusé");
        }

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return AuthResponse.error("Utilisateur introuvable");
        }

        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            return AuthResponse.error("Email déjà utilisé");
        }

        user.setNom(request.getNom());
        user.setEmail(request.getEmail());
        userRepository.save(user);

        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());  // 🔥 setUserId au lieu de setId
        response.setUsername(user.getNom());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setMessage("Profil mis à jour");
        response.setSuccess(true);
        return response;
    }

    public AuthResponse changePassword(BigInteger id, ChangePasswordRequest request) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN && !requester.getId().equals(id)) {
            return AuthResponse.error("Accès refusé");
        }

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return AuthResponse.error("Utilisateur introuvable");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return AuthResponse.error("Ancien mot de passe incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return AuthResponse.success("Mot de passe modifié");
    }

    // ================= MAPPING =================

    private UserDto mapToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setNom(user.getNom());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}