package com.exemple.quiz_app.auth.repository;

import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(Role role);
    List<User> findByClasseIdOrderByLastNameAscFirstNameAsc(Long classeId);

    boolean existsByCne(String cne);

    boolean existsByCodeApoge(String codeApoge);

    boolean existsByCneAndIdNot(String cne, Long id);

    boolean existsByCodeApogeAndIdNot(String codeApoge, Long id);
}
