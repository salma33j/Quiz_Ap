package com.exemple.quiz_app.question.repository;

import com.exemple.quiz_app.question.entity.Question;
import com.exemple.quiz_app.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    // Récupérer toutes les questions d'un quiz
    List<Question> findByQuizId(Long quizId);

    // Récupérer les questions d'un quiz avec leurs réponses
    List<Question> findByQuiz(Quiz quiz);

    // Compter le nombre de questions dans un quiz
    long countByQuiz(Quiz quiz);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.quiz.id = :quizId")
    long countByQuizId(@Param("quizId") Long quizId);

    // Supprimer toutes les questions d'un quiz
    @Modifying
    @Transactional
    @Query("DELETE FROM Question q WHERE q.quiz.id = :quizId")
    void deleteByQuizId(@Param("quizId") Long quizId);

    // Récupérer les questions pour un étudiant (sans la réponse correcte)
    @Query("SELECT q.id, q.enonce, q.choixA, q.choixB, q.choixC, q.choixD, q.type, q.points " +
            "FROM Question q WHERE q.quiz.id = :quizId")
    List<Object[]> findQuizQuestionsForStudent(@Param("quizId") Long quizId);
}