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

    // ========== ETUDIANT - QUIZ DISPONIBLES ==========
    @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN q.allowedStudents qs LEFT JOIN q.matiere m " +
            "WHERE (qs.student = :student " +
            "OR (:classId IS NOT NULL AND q.classe.id = :classId) " +
            "OR (:classId IS NOT NULL AND m.classe.id = :classId)) " +
            "AND q.status = 'PUBLISHED' " +
            "AND (q.availableFrom IS NULL OR q.availableFrom <= :now) " +
            "AND (q.availableUntil IS NULL OR q.availableUntil >= :now)")
    List<Quiz> findAvailableQuizzesForStudent(
            @Param("student") User student,
            @Param("classId") Long classId,
            @Param("now") LocalDateTime now
    );

    // ========== ETUDIANT - TOUS LES QUIZ AUTORISES ==========
    @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN q.allowedStudents qs LEFT JOIN q.matiere m " +
            "WHERE qs.student = :student " +
            "OR (:classId IS NOT NULL AND q.classe.id = :classId) " +
            "OR (:classId IS NOT NULL AND m.classe.id = :classId)")
    List<Quiz> findAllQuizzesForStudent(@Param("student") User student, @Param("classId") Long classId);

    // ========== VERIFICATION AUTORISATION ==========
    @Query("SELECT COUNT(q) > 0 FROM Quiz q LEFT JOIN q.allowedStudents qs LEFT JOIN q.matiere m " +
            "WHERE q.id = :quizId AND (" +
            "qs.student.id = :studentId " +
            "OR q.classe.id = (SELECT u.classe.id FROM User u WHERE u.id = :studentId) " +
            "OR m.classe.id = (SELECT u.classe.id FROM User u WHERE u.id = :studentId))")
    boolean isStudentAllowed(@Param("quizId") Long quizId, @Param("studentId") Long studentId);

    // ========== COMPTER LES ETUDIANTS AUTORISES ==========
    @Query("SELECT COUNT(qs) FROM QuizStudent qs WHERE qs.quiz.id = :quizId")
    int countAllowedStudents(@Param("quizId") Long quizId);

    // ========== VERIFIER SI UN QUIZ EXISTE ==========
    boolean existsById(Long id);

    boolean existsByTitreAndEnseignantId(String titre, Long enseignantId);
}
