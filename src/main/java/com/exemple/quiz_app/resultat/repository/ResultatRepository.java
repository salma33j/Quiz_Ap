package com.exemple.quiz_app.resultat.repository;

import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.resultat.entity.Resultat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultatRepository extends JpaRepository<Resultat, Long> {

    // ========== RECHERCHES AVEC @Query ==========

    @Query("SELECT r FROM Resultat r WHERE r.student = :student AND r.quiz.id = :quizId")
    Optional<Resultat> findByStudentAndQuizId(@Param("student") User student, @Param("quizId") Long quizId);

    @Query("SELECT r FROM Resultat r WHERE r.student = :student AND r.quiz.id = :quizId AND r.status = :status")
    Optional<Resultat> findByStudentAndQuizIdAndStatus(@Param("student") User student, @Param("quizId") Long quizId, @Param("status") Resultat.SubmissionStatus status);

    @Query("SELECT r FROM Resultat r WHERE r.student = :student ORDER BY r.completedDate DESC")
    List<Resultat> findByStudentOrderByCompletedDateDesc(@Param("student") User student);

    @Query("SELECT r FROM Resultat r WHERE r.quiz.id = :quizId ORDER BY r.scorePercentage DESC")
    List<Resultat> findByQuizIdOrderByScorePercentageDesc(@Param("quizId") Long quizId);

    @Query("SELECT r FROM Resultat r WHERE r.quiz.id = :quizId")
    List<Resultat> findByQuizId(@Param("quizId") Long quizId);

    @Query("SELECT r FROM Resultat r WHERE r.student = :student")
    List<Resultat> findByStudent(@Param("student") User student);

    // ========== STATISTIQUES ==========

    @Query("SELECT AVG(r.scorePercentage) FROM Resultat r WHERE r.quiz.id = :quizId AND r.status = 'SUBMITTED'")
    Double getAverageScoreByQuizId(@Param("quizId") Long quizId);

    @Query("SELECT MAX(r.scorePercentage) FROM Resultat r WHERE r.quiz.id = :quizId AND r.status = 'SUBMITTED'")
    Double getBestScoreByQuizId(@Param("quizId") Long quizId);

    @Query("SELECT MIN(r.scorePercentage) FROM Resultat r WHERE r.quiz.id = :quizId AND r.status = 'SUBMITTED'")
    Double getWorstScoreByQuizId(@Param("quizId") Long quizId);

    @Query("SELECT COUNT(r) FROM Resultat r WHERE r.quiz.id = :quizId AND r.status = :status")
    long countByQuizIdAndStatus(@Param("quizId") Long quizId, @Param("status") Resultat.SubmissionStatus status);

    // ========== CLASSEMENT ==========

    @Query("SELECT r.student.id, r.student.firstName, r.student.lastName, r.student.email, r.scorePercentage, r.earnedPoints, r.totalPoints " +
            "FROM Resultat r WHERE r.quiz.id = :quizId AND r.status = 'SUBMITTED' ORDER BY r.scorePercentage DESC")
    List<Object[]> getRankingByQuizId(@Param("quizId") Long quizId);

    // ========== VÉRIFICATIONS ==========

    @Query("SELECT COUNT(r) > 0 FROM Resultat r WHERE r.student.id = :studentId AND r.quiz.id = :quizId AND r.status = 'SUBMITTED'")
    boolean hasStudentCompletedQuiz(@Param("studentId") Long studentId, @Param("quizId") Long quizId);

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
    @Query("DELETE FROM Resultat r WHERE r.student = :student")
    void deleteByStudent(@Param("student") User student);

    @Query("SELECT r FROM Resultat r WHERE r.student = :student ORDER BY r.completedDate DESC")
    List<Resultat> findStudentResultsOrderByCompletedDateDesc(@Param("student") User student);
}