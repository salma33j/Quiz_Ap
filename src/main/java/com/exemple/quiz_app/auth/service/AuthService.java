package com.exemple.quiz_app.auth.service;
// auth/service/AuthService.java


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
            return new AuthResponse(null, null, null, null, null, null, "Email déjà utilisé");
        }

        Role role;
        try {
            role = request.getRole() != null ? Role.valueOf(request.getRole().toUpperCase()) : Role.STUDENT;
        } catch (IllegalArgumentException e) {
            role = Role.STUDENT;
        }

        User user = new User(request.getFirstName(), request.getLastName(), request.getEmail(),
                passwordEncoder.encode(request.getPassword()), role);
        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponse(token, user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getRole().name(), "Inscription réussie");
    }

    // ================= LOGIN =================

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return new AuthResponse(null, null, null, null, null, null, "Email ou mot de passe incorrect");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponse(token, user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getRole().name(), "Connexion réussie");
    }

    // ================= GET CURRENT USER INFO =================

    public AuthResponse getCurrentUserInfo() {
        try {
            User user = getCurrentUser();
            return new AuthResponse(null, user.getId(), user.getFirstName(), user.getLastName(),
                    user.getEmail(), user.getRole().name(), "Utilisateur trouvé");
        } catch (RuntimeException e) {
            return new AuthResponse(null, null, null, null, null, null, e.getMessage());
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

    public AuthResponse getUserById(Long id) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN && !requester.getId().equals(id)) {
            return new AuthResponse(null, null, null, null, null, null, "Accès refusé");
        }
        return userRepository.findById(id)
                .map(user -> new AuthResponse(null, user.getId(), user.getFirstName(), user.getLastName(),
                        user.getEmail(), user.getRole().name(), "OK"))
                .orElse(new AuthResponse(null, null, null, null, null, null, "Utilisateur introuvable"));
    }

    public AuthResponse promoteToTeacher(Long userId) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) {
            return new AuthResponse(null, null, null, null, null, null, "Accès refusé - Réservé aux administrateurs");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return new AuthResponse(null, null, null, null, null, null, "Utilisateur introuvable");
        }
        if (user.getRole() == Role.TEACHER) {
            return new AuthResponse(null, null, null, null, null, null, "L'utilisateur est déjà enseignant");
        }

        user.setRole(Role.TEACHER);
        userRepository.save(user);

        return new AuthResponse(null, user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getRole().name(), "Utilisateur promu enseignant");
    }

    public AuthResponse deleteUser(Long id) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) {
            return new AuthResponse(null, null, null, null, null, null, "Accès refusé");
        }
        if (!userRepository.existsById(id)) {
            return new AuthResponse(null, null, null, null, null, null, "Utilisateur introuvable");
        }
        userRepository.deleteById(id);
        return new AuthResponse(null, null, null, null, null, null, "Utilisateur supprimé");
    }

    public AuthResponse updateProfile(Long id, RegisterRequest request) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN && !requester.getId().equals(id)) {
            return new AuthResponse(null, null, null, null, null, null, "Accès refusé");
        }

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return new AuthResponse(null, null, null, null, null, null, "Utilisateur introuvable");
        }

        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            return new AuthResponse(null, null, null, null, null, null, "Email déjà utilisé");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        userRepository.save(user);

        return new AuthResponse(null, user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getRole().name(), "Profil mis à jour");
    }

    public AuthResponse changePassword(Long id, ChangePasswordRequest request) {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN && !requester.getId().equals(id)) {
            return new AuthResponse(null, null, null, null, null, null, "Accès refusé");
        }

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return new AuthResponse(null, null, null, null, null, null, "Utilisateur introuvable");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return new AuthResponse(null, null, null, null, null, null, "Ancien mot de passe incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return new AuthResponse(null, null, null, null, null, null, "Mot de passe modifié");
    }

    private UserDto mapToDto(User user) {
        return new UserDto(user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getRole().name(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
