package com.exemple.quiz_app.quiz.repository;

import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Quiz q SET q.status = 'EXPIRED' " +
            "WHERE q.status = 'PUBLISHED' " +
            "AND q.availableUntil IS NOT NULL " +
            "AND q.availableUntil <= :now")
    int expirePublishedQuizzesPastDeadline(@Param("now") LocalDateTime now);

    // ========== ETUDIANT - QUIZ DISPONIBLES ==========
    @Query(value = """
            SELECT DISTINCT q.*
            FROM quiz q
            LEFT JOIN quiz_students qs ON qs.quiz_id = q.id
            LEFT JOIN matieres m ON m.id = q.matiere_id
            WHERE (
                qs.student_id = :studentId
                OR (:classId IS NOT NULL AND q.classe_id = :classId)
                OR (:classId IS NOT NULL AND m.classe_id = :classId)
                OR (:classId IS NOT NULL AND EXISTS (
                    SELECT 1 FROM matieres sm
                    WHERE sm.classe_id = :classId
                    AND q.theme IS NOT NULL
                    AND LOWER(sm.nom) = LOWER(q.theme)
                ))
            )
            AND q.status = 'PUBLISHED'
            AND (q.available_from IS NULL OR q.available_from <= :now)
            AND (q.available_until IS NULL OR q.available_until >= :now)
            """, nativeQuery = true)
    List<Quiz> findAvailableQuizzesForStudent(
            @Param("studentId") Long studentId,
            @Param("classId") Long classId,
            @Param("now") LocalDateTime now
    );

    // ========== ETUDIANT - TOUS LES QUIZ AUTORISES ==========
    @Query(value = """
            SELECT DISTINCT q.*
            FROM quiz q
            LEFT JOIN quiz_students qs ON qs.quiz_id = q.id
            LEFT JOIN matieres m ON m.id = q.matiere_id
            WHERE (
                qs.student_id = :studentId
                OR (:classId IS NOT NULL AND q.classe_id = :classId)
                OR (:classId IS NOT NULL AND m.classe_id = :classId)
                OR (:classId IS NOT NULL AND EXISTS (
                    SELECT 1 FROM matieres sm
                    WHERE sm.classe_id = :classId
                    AND q.theme IS NOT NULL
                    AND LOWER(sm.nom) = LOWER(q.theme)
                ))
            )
            AND q.status IN ('PUBLISHED', 'EXPIRED', 'ARCHIVED')
            """, nativeQuery = true)
    List<Quiz> findAllQuizzesForStudent(@Param("studentId") Long studentId, @Param("classId") Long classId);

    // ========== VERIFICATION AUTORISATION ==========
    @Query(value = """
            SELECT COUNT(q.id) > 0
            FROM quiz q
            LEFT JOIN quiz_students qs ON qs.quiz_id = q.id
            LEFT JOIN matieres m ON m.id = q.matiere_id
            LEFT JOIN users u ON u.id = :studentId
            WHERE q.id = :quizId
            AND (
                qs.student_id = :studentId
                OR (u.classe_id IS NOT NULL AND q.classe_id = u.classe_id)
                OR (u.classe_id IS NOT NULL AND m.classe_id = u.classe_id)
                OR (u.classe_id IS NOT NULL AND EXISTS (
                    SELECT 1 FROM matieres sm
                    WHERE sm.classe_id = u.classe_id
                    AND q.theme IS NOT NULL
                    AND LOWER(sm.nom) = LOWER(q.theme)
                ))
            )
            """, nativeQuery = true)
    boolean isStudentAllowed(@Param("quizId") Long quizId, @Param("studentId") Long studentId);

    // ========== COMPTER LES ETUDIANTS AUTORISES ==========
    @Query("SELECT COUNT(qs) FROM QuizStudent qs WHERE qs.quiz.id = :quizId")
    int countAllowedStudents(@Param("quizId") Long quizId);

    // ========== VERIFIER SI UN QUIZ EXISTE ==========
    boolean existsById(Long id);

    boolean existsByTitreAndEnseignantId(String titre, Long enseignantId);
}
