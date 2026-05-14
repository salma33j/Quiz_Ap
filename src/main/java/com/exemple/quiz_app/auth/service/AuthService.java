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

    public User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().equals("anonymousUser")) {
            throw new RuntimeException("Utilisateur non authentifie");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return AuthResponse.error("Email deja utilise");
        }
        Role role = Role.ETUDIANT;
        if (request.getRole() != null) {
            try { role = Role.valueOf(request.getRole().toUpperCase()); }
            catch (IllegalArgumentException e) { role = Role.ETUDIANT; }
        }
        User user = new User(
                request.getFirstName(), request.getLastName(),
                request.getEmail(), passwordEncoder.encode(request.getPassword()), role
        );
        user = userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getFirstName() + " " + user.getLastName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setMessage("Inscription reussie");
        response.setSuccess(true);
        response.setType("Bearer");
        return response;
    }

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
        response.setMessage("Connexion reussie");
        response.setSuccess(true);
        response.setType("Bearer");
        return response;
    }

    public AuthResponse getCurrentUserInfo() {
        try {
            User user = getCurrentUser();
            AuthResponse response = new AuthResponse();
            response.setUserId(user.getId());
            response.setUsername(user.getFirstName() + " " + user.getLastName());
            response.setEmail(user.getEmail());
            response.setRole(user.getRole().name());
            response.setMessage("Utilisateur trouve");
            response.setSuccess(true);
            return response;
        } catch (RuntimeException e) {
            return AuthResponse.error(e.getMessage());
        }
    }

    public List<UserDto> getAllUsers() {
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) {
            throw new RuntimeException("Acces refuse - Reserve aux administrateurs");
        }
        return userRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public AuthResponse getUserById(Long id) {           // ← Long
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
                    response.setMessage("OK");
                    response.setSuccess(true);
                    return response;
                })
                .orElse(AuthResponse.error("Utilisateur introuvable"));
    }

    public AuthResponse promoteToTeacher(Long userId) {  // ← Long
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

    public AuthResponse deleteUser(Long id) {            // ← Long
        User requester = getCurrentUser();
        if (requester.getRole() != Role.ADMIN) return AuthResponse.error("Acces refuse");
        if (!userRepository.existsById(id)) return AuthResponse.error("Utilisateur introuvable");
        userRepository.deleteById(id);
        return AuthResponse.success("Utilisateur supprime");
    }

    public AuthResponse updateProfile(Long id, RegisterRequest request) {  // ← Long
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

    public AuthResponse changePassword(Long id, ChangePasswordRequest request) {  // ← Long
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
        userRepository.save(user);
        return AuthResponse.success("Mot de passe modifie");
    }

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