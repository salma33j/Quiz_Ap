// auth/repository/UserRepository.java
package com.exemple.quiz_app.auth.repository;

import com.exemple.quiz_app.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, BigInteger> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsById(BigInteger id);
}