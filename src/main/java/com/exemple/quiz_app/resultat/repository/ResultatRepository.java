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

    // Trouver le résultat d'un étudiant pour un quiz spécifique
    Optional<Resultat> findByStudentAndQuizId(User student, Long quizId);

    // Trouver le résultat avec statut spécifique
    Optional<Resultat> findByStudentAndQuizIdAndStatus(User student, Long quizId, Resultat.SubmissionStatus status);

    // Trouver tous les résultats d'un étudiant (historique)
    List<Resultat> findByStudentOrderBySubmittedAtDesc(User student);

    // Trouver tous les résultats pour un quiz (pour enseignant)
    @Query("SELECT r FROM Resultat r WHERE r.quizId = :quizId ORDER BY r.scorePercentage DESC")
    List<Resultat> findByQuizIdOrderByScorePercentageDesc(@Param("quizId") Long quizId);

    // Trouver tous les résultats par quizId
    List<Resultat> findByQuizId(Long quizId);

    // ========== FILTRES PAR SCORE ==========

    // Trouver les résultats d'un quiz avec note supérieure à un seuil
    @Query("SELECT r FROM Resultat r WHERE r.quizId = :quizId AND r.scorePercentage >= :minScore AND r.status = 'SUBMITTED'")
    List<Resultat> findByQuizIdAndScorePercentageGreaterThanEqual(@Param("quizId") Long quizId, @Param("minScore") Double minScore);

    // Trouver les résultats d'un quiz avec note inférieure à un seuil
    @Query("SELECT r FROM Resultat r WHERE r.quizId = :quizId AND r.scorePercentage <= :maxScore AND r.status = 'SUBMITTED'")
    List<Resultat> findByQuizIdAndScorePercentageLessThanEqual(@Param("quizId") Long quizId, @Param("maxScore") Double maxScore);

    // ========== STATISTIQUES ==========

    // Compter le nombre d'étudiants ayant complété un quiz
    long countByQuizIdAndStatus(Long quizId, Resultat.SubmissionStatus status);

    // Calculer la moyenne des scores pour un quiz
    @Query("SELECT AVG(r.scorePercentage) FROM Resultat r WHERE r.quizId = :quizId AND r.status = 'SUBMITTED'")
    Double getAverageScoreByQuizId(@Param("quizId") Long quizId);

    // Obtenir le meilleur score pour un quiz
    @Query("SELECT MAX(r.scorePercentage) FROM Resultat r WHERE r.quizId = :quizId AND r.status = 'SUBMITTED'")
    Double getBestScoreByQuizId(@Param("quizId") Long quizId);

    // Obtenir le pire score pour un quiz
    @Query("SELECT MIN(r.scorePercentage) FROM Resultat r WHERE r.quizId = :quizId AND r.status = 'SUBMITTED'")
    Double getWorstScoreByQuizId(@Param("quizId") Long quizId);

    // ========== CLASSEMENT ==========

    // Obtenir le classement des étudiants pour un quiz
    @Query("SELECT r.student.id, r.student.nom, r.student.email, r.scorePercentage, r.earnedPoints, r.totalPoints " +
            "FROM Resultat r WHERE r.quizId = :quizId AND r.status = 'SUBMITTED' ORDER BY r.scorePercentage DESC")
    List<Object[]> getRankingByQuizId(@Param("quizId") Long quizId);

    // ========== VÉRIFICATIONS ==========

    // Vérifier si un étudiant a déjà complété un quiz
    @Query("SELECT COUNT(r) > 0 FROM Resultat r WHERE r.student.id = :studentId AND r.quizId = :quizId AND r.status = 'SUBMITTED'")
    boolean hasStudentCompletedQuiz(@Param("studentId") Long studentId, @Param("quizId") Long quizId);

    // Vérifier si un étudiant a déjà commencé un quiz (non complété)
    @Query("SELECT COUNT(r) > 0 FROM Resultat r WHERE r.student.id = :studentId AND r.quizId = :quizId AND r.status = 'IN_PROGRESS'")
    boolean hasStudentStartedQuiz(@Param("studentId") Long studentId, @Param("quizId") Long quizId);

    // ========== RÉSULTATS EN COURS ==========

    // Trouver les résultats non complétés (en cours)
    List<Resultat> findByStudentAndStatus(User student, Resultat.SubmissionStatus status);

    // Trouver tous les résultats en cours pour un étudiant
    @Query("SELECT r FROM Resultat r WHERE r.student = :student AND r.status = 'IN_PROGRESS'")
    List<Resultat> findInProgressResultsByStudent(@Param("student") User student);

    // ========== FEEDBACK IA ==========

    // Trouver les résultats qui n'ont pas encore de feedback
    @Query("SELECT r FROM Resultat r WHERE r.feedback IS NULL AND r.status = 'SUBMITTED'")
    List<Resultat> findByFeedbackIsNullAndStatusSubmitted();

    // ========== SUPPRESSIONS ==========

    // Supprimer tous les résultats d'un étudiant pour un quiz
    @Modifying
    @Transactional
    @Query("DELETE FROM Resultat r WHERE r.student.id = :studentId AND r.quizId = :quizId")
    void deleteByStudentIdAndQuizId(@Param("studentId") Long studentId, @Param("quizId") Long quizId);

    // Supprimer tous les résultats d'un quiz
    @Modifying
    @Transactional
    @Query("DELETE FROM Resultat r WHERE r.quizId = :quizId")
    void deleteByQuizId(@Param("quizId") Long quizId);

    // Supprimer tous les résultats d'un étudiant
    @Modifying
    @Transactional
    void deleteByStudent(User student);

    // ========== MISE À JOUR ==========

    // Mettre à jour le statut d'un résultat
    @Modifying
    @Transactional
    @Query("UPDATE Resultat r SET r.status = :status, r.submittedAt = :submittedAt WHERE r.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") Resultat.SubmissionStatus status, @Param("submittedAt") LocalDateTime submittedAt);


}