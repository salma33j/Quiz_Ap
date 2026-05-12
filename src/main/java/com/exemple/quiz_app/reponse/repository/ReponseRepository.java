package com.exemple.quiz_app.reponse.repository;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.question.entity.Question;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.reponse.entity.Reponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
@Repository
public interface ReponseRepository extends JpaRepository<Reponse, Long> {
    // Trouver toutes les réponses d'un étudiant pour un quiz spécifique
    List<Reponse> findByStudentAndQuiz(User student, Quiz quiz);
    // Trouver toutes les réponses pour un quiz
    List<Reponse> findByQuiz(Quiz quiz);
    // Trouver la réponse d'un étudiant à une question spécifique
    Optional<Reponse> findByStudentAndQuestion(User student, Question question);
    // Vérifier si un étudiant a déjà répondu à une question
    boolean existsByStudentAndQuestionAndQuiz(User student, Question question, Quiz quiz);
    // Supprimer toutes les réponses d'un étudiant pour un quiz
    @Modifying
    @Transactional
    void deleteByStudentAndQuiz(User student, Quiz quiz);
    // Compter le nombre de réponses correctes d'un étudiant pour un quiz
    long countByStudentAndQuizAndIsCorrectTrue(User student, Quiz quiz);
    // Trouver toutes les réponses d'un quiz avec les détails des étudiants
    @Query("SELECT r FROM Reponse r JOIN FETCH r.student WHERE r.quiz = :quiz")
    List<Reponse> findByQuizWithStudent(@Param("quiz") Quiz quiz);
    // Calculer le score total d'un étudiant pour un quiz (somme des points obtenus)
    @Query("SELECT COALESCE(SUM(r.pointsEarned), 0) FROM Reponse r WHERE r.student = :student AND r.quiz = :quiz")
    Integer sumPointsEarnedByStudentAndQuiz(@Param("student") User student, @Param("quiz") Quiz quiz);
    // Trouver les réponses incorrectes d'un étudiant pour un quiz
    @Query("SELECT r FROM Reponse r WHERE r.student = :student AND r.quiz = :quiz AND r.isCorrect = false")
    List<Reponse> findIncorrectAnswersByStudentAndQuiz(@Param("student") User student, @Param("quiz") Quiz quiz);
    // Trouver toutes les réponses d'un étudiant (pour l'historique)
    List<Reponse> findByStudentOrderByAnsweredAtDesc(User student);
    // Compter le nombre de réponses pour un quiz
    long countByQuiz(Quiz quiz);
    // Compter le nombre d'étudiants ayant répondu à un quiz (distinct)
    @Query("SELECT COUNT(DISTINCT r.student) FROM Reponse r WHERE r.quiz = :quiz")
    long countDistinctStudentsByQuiz(@Param("quiz") Quiz quiz);

    List<Reponse> findByStudentAndQuestionQuizId(User student, Long id);


    // 🔥 AJOUTER CETTE MÉTHODE
    @Modifying
    @Transactional
    @Query("DELETE FROM Reponse r WHERE r.question.quiz.id = :quizId")
    void deleteByQuizId(@Param("quizId") Long quizId);
}