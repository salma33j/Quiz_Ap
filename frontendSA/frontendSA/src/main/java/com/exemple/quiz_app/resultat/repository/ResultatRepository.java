package com.exemple.quiz_app.resultat.repository;

import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.resultat.entity.Resultat;
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
public interface ResultatRepository extends JpaRepository<Resultat, Long> {

    // ========== RECHERCHES DE BASE ==========

    @Query("SELECT r FROM Resultat r WHERE r.student = :student AND r.quiz.id = :quizId")
    Optional<Resultat> findByStudentAndQuizId(@Param("student") User student, @Param("quizId") Long quizId);

    @Query("SELECT r FROM Resultat r WHERE r.student = :student AND r.quiz.id = :quizId AND r.status = :status")
    Optional<Resultat> findByStudentAndQuizIdAndStatus(@Param("student") User student, @Param("quizId") Long quizId, @Param("status") Resultat.SubmissionStatus status);

    List<Resultat> findByStudentOrderByCompletedDateDesc(User student);

    @Query("SELECT r FROM Resultat r WHERE r.quiz.id = :quizId ORDER BY r.scorePercentage DESC")
    List<Resultat> findByQuizIdOrderByScorePercentageDesc(@Param("quizId") Long quizId);

    @Query("SELECT r FROM Resultat r WHERE r.quiz.id = :quizId")
    List<Resultat> findByQuizId(@Param("quizId") Long quizId);

    // ========== FILTRES PAR SCORE ==========

    @Query("SELECT r FROM Resultat r WHERE r.quiz.id = :quizId AND r.scorePercentage >= :minScore AND r.status = 'SUBMITTED'")
    List<Resultat> findByQuizIdAndScorePercentageGreaterThanEqual(@Param("quizId") Long quizId, @Param("minScore") Double minScore);

    @Query("SELECT r FROM Resultat r WHERE r.quiz.id = :quizId AND r.scorePercentage <= :maxScore AND r.status = 'SUBMITTED'")
    List<Resultat> findByQuizIdAndScorePercentageLessThanEqual(@Param("quizId") Long quizId, @Param("maxScore") Double maxScore);

    // ========== STATISTIQUES ==========

    @Query("SELECT COUNT(r) FROM Resultat r WHERE r.quiz.id = :quizId AND r.status = :status")
    long countByQuizIdAndStatus(@Param("quizId") Long quizId, @Param("status") Resultat.SubmissionStatus status);

    @Query("SELECT AVG(r.scorePercentage) FROM Resultat r WHERE r.quiz.id = :quizId AND r.status = 'SUBMITTED'")
    Double getAverageScoreByQuizId(@Param("quizId") Long quizId);

    @Query("SELECT MAX(r.scorePercentage) FROM Resultat r WHERE r.quiz.id = :quizId AND r.status = 'SUBMITTED'")
    Double getBestScoreByQuizId(@Param("quizId") Long quizId);

    @Query("SELECT MIN(r.scorePercentage) FROM Resultat r WHERE r.quiz.id = :quizId AND r.status = 'SUBMITTED'")
    Double getWorstScoreByQuizId(@Param("quizId") Long quizId);

    // ========== CLASSEMENT ==========

    @Query("SELECT r.student.id, r.student.firstName, r.student.lastName, r.student.email, r.scorePercentage, r.earnedPoints, r.totalPoints " +
            "FROM Resultat r WHERE r.quiz.id = :quizId AND r.status = 'SUBMITTED' ORDER BY r.scorePercentage DESC")
    List<Object[]> getRankingByQuizId(@Param("quizId") Long quizId);

    // ========== VÉRIFICATIONS ==========

    @Query("SELECT COUNT(r) > 0 FROM Resultat r WHERE r.student.id = :studentId AND r.quiz.id = :quizId AND r.status = 'SUBMITTED'")
    boolean hasStudentCompletedQuiz(@Param("studentId") Long studentId, @Param("quizId") Long quizId);

    @Query("SELECT COUNT(r) > 0 FROM Resultat r WHERE r.student.id = :studentId AND r.quiz.id = :quizId AND r.status = 'IN_PROGRESS'")
    boolean hasStudentStartedQuiz(@Param("studentId") Long studentId, @Param("quizId") Long quizId);

    // ========== RÉSULTATS EN COURS ==========

    List<Resultat> findByStudentAndStatus(User student, Resultat.SubmissionStatus status);

    @Query("SELECT r FROM Resultat r WHERE r.student = :student AND r.status = 'IN_PROGRESS'")
    List<Resultat> findInProgressResultsByStudent(@Param("student") User student);

    // ========== FEEDBACK IA ==========

    @Query("SELECT r FROM Resultat r WHERE r.feedbackIa IS NULL AND r.status = 'SUBMITTED'")
    List<Resultat> findByFeedbackIsNullAndStatusSubmitted();

    // ========== SUPPRESSIONS ==========

    @Modifying
    @Transactional
    @Query("DELETE FROM Resultat r WHERE r.student.id = :studentId AND r.quiz.id = :quizId")
    void deleteByStudentIdAndQuizId(@Param("studentId") Long studentId, @Param("quizId") Long quizId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Resultat r WHERE r.quiz.id = :quizId")
    void deleteByQuizId(@Param("quizId") Long quizId);

    @Modifying
    @Transactional
    void deleteByStudent(User student);

    // ========== MISE À JOUR ==========

    @Modifying
    @Transactional
    @Query("UPDATE Resultat r SET r.status = :status, r.completedDate = :submittedAt WHERE r.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") Resultat.SubmissionStatus status, @Param("submittedAt") LocalDateTime submittedAt);
}