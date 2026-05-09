package com.exemple.quiz_app.resultat.repository;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.quiz.entity.Quiz;
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
    // Trouver le résultat d'un étudiant pour un quiz spécifique
    Optional<Resultat> findByStudentAndQuiz(User student, Quiz quiz);
    // Trouver tous les résultats d'un étudiant (historique)
    List<Resultat> findByStudentOrderByCompletedDateDesc(User student);
    // Trouver tous les résultats pour un quiz (pour enseignant)
    List<Resultat> findByQuizOrderByScorePercentageDesc(Quiz quiz);
    // Trouver les résultats d'un quiz avec note supérieure à un seuil
    List<Resultat> findByQuizAndScorePercentageGreaterThanEqual(Quiz quiz, Double minScore);
    // Trouver les résultats d'un quiz avec note inférieure à un seuil
    List<Resultat> findByQuizAndScorePercentageLessThanEqual(Quiz quiz, Double maxScore);
    // Compter le nombre d'étudiants ayant complété un quiz
    long countByQuizAndIsCompletedTrue(Quiz quiz);
    // Calculer la moyenne des scores pour un quiz
    @Query("SELECT AVG(r.scorePercentage) FROM Resultat r WHERE r.quiz = :quiz AND r.isCompleted = true")
    Double getAverageScoreByQuiz(@Param("quiz") Quiz quiz);
    // Obtenir le classement des étudiants pour un quiz
    @Query("SELECT r.student.id, r.student.nom, r.student.prenom, r.scorePercentage, r.earnedPoints, r.totalPoints " +
            "FROM Resultat r WHERE r.quiz = :quiz AND r.isCompleted = true ORDER BY r.scorePercentage DESC")
    List<Object[]> getRankingByQuiz(@Param("quiz") Quiz quiz);
    // Trouver le meilleur score pour un quiz
    @Query("SELECT MAX(r.scorePercentage) FROM Resultat r WHERE r.quiz = :quiz AND r.isCompleted = true")
    Double getBestScoreByQuiz(@Param("quiz") Quiz quiz);
    // Trouver les résultats qui n'ont pas encore de feedback IA
    List<Resultat> findByFeedbackIaIsNullAndIsCompletedTrue();
    // Supprimer tous les résultats d'un étudiant pour un quiz
    @Modifying
    @Transactional
    void deleteByStudentAndQuiz(User student, Quiz quiz);
    // Supprimer tous les résultats d'un quiz
    @Modifying
    @Transactional
    void deleteByQuiz(Quiz quiz);
    // Vérifier si un étudiant a déjà complété un quiz
    boolean existsByStudentAndQuizAndIsCompletedTrue(User student, Quiz quiz);
    // Trouver les résultats non complétés (en cours)
    List<Resultat> findByStudentAndIsCompletedFalse(User student);
}
