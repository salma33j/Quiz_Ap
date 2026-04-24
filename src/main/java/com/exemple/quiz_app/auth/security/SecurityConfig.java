package com.exemple.quiz_app.auth.security;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // ✅ Endpoints publics (authentification)
                        .requestMatchers(
                                "/api/auth/**",
                                "/auth/**"
                        ).permitAll()

                        // ✅ Endpoints publics pour tests et documentation
                        .requestMatchers(
                                "/actuator/health",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // ✅ Rôle ENSEIGNANT uniquement
                        .requestMatchers(
                                "/api/quiz/create",
                                "/api/quiz/update/**",
                                "/api/quiz/delete/**",
                                "/api/quiz/toggle/**",
                                "/api/quiz/activate/**",
                                "/api/quiz/deactivate/**",
                                "/api/question/create/**",
                                "/api/question/update/**",
                                "/api/question/delete/**",
                                "/api/reponse/create/**",
                                "/api/reponse/update/**",
                                "/api/reponse/delete/**",
                                "/api/statistique/**",
                                "/api/statistique/quiz/**",
                                "/api/resultat/quiz/**",
                                "/api/ai/generate-quiz"
                        ).hasRole("TEACHER")

                        // ✅ Rôle ÉTUDIANT uniquement
                        .requestMatchers(
                                "/api/quiz/available",
                                "/api/quiz/active",
                                "/api/quiz/participate/**",
                                "/api/question/quiz/**",
                                "/api/reponse/question/**",
                                "/api/resultat/submit/**",
                                "/api/resultat/my-results",
                                "/api/resultat/history",
                                "/api/resultat/score/**",
                                "/api/statistique/my-performance"
                        ).hasRole("STUDENT")

                        // ✅ Endpoints accessibles aux deux rôles (enseignant + étudiant)
                        .requestMatchers(
                                "/api/quiz/**/details",
                                "/api/quiz/**/info"
                        ).hasAnyRole("TEACHER", "STUDENT")

                        // ✅ Rôle ADMIN (optionnel - gestion globale)
                        .requestMatchers(
                                "/api/admin/**",
                                "/api/auth/users/**",
                                "/api/statistique/all"
                        ).hasRole("ADMIN")

                        // ✅ Toute autre requête nécessite une authentification
                        .anyRequest().authenticated()
                )
                // Ajouter le filtre JWT avant le filtre d'authentification standard
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",      // React local
                "http://localhost:5173",      // Vite React
                "https://votre-frontend.vercel.app"  // Vercel en production
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}