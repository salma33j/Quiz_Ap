package com.exemple.quiz_app.quiz.repository;

import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    // ========== ENSEIGNANT ==========
    List<Quiz> findByEnseignant(User enseignant);

    List<Quiz> findByEnseignantAndStatus(User enseignant, Quiz.QuizStatus status);

    Optional<Quiz> findByIdAndEnseignantId(Long id, Long enseignantId);

    // ========== ADMIN ==========
    List<Quiz> findByStatus(Quiz.QuizStatus status);

    long countByStatus(Quiz.QuizStatus status);

    // ========== ÉTUDIANT - QUIZ DISPONIBLES ==========
    @Query("SELECT q FROM Quiz q JOIN q.allowedStudents qs WHERE qs.student = :student " +
            "AND q.status = 'PUBLISHED' " +
            "AND (q.availableFrom IS NULL OR q.availableFrom <= :now) " +
            "AND (q.availableUntil IS NULL OR q.availableUntil >= :now)")
    List<Quiz> findAvailableQuizzesForStudent(@Param("student") User student, @Param("now") LocalDateTime now);

    // ========== ÉTUDIANT - TOUS LES QUIZ AUTORISÉS ==========
    @Query("SELECT q FROM Quiz q JOIN q.allowedStudents qs WHERE qs.student = :student")
    List<Quiz> findAllQuizzesForStudent(@Param("student") User student);

    // ========== VÉRIFICATION AUTORISATION ==========
    @Query("SELECT COUNT(qs) > 0 FROM QuizStudent qs WHERE qs.quiz.id = :quizId AND qs.student.id = :studentId")
    boolean isStudentAllowed(@Param("quizId") Long quizId, @Param("studentId") Long studentId);

    // ========== COMPTER LES ÉTUDIANTS AUTORISÉS ==========
    @Query("SELECT COUNT(qs) FROM QuizStudent qs WHERE qs.quiz.id = :quizId")
    int countAllowedStudents(@Param("quizId") Long quizId);

    // ========== VÉRIFIER SI UN QUIZ EXISTE ==========
    boolean existsById(Long id);

    boolean existsByTitreAndEnseignantId(String titre, Long enseignantId);
}