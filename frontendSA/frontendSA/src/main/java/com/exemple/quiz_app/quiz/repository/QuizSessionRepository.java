package com.exemple.quiz_app.quiz.repository;

import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.quiz.entity.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {

    Optional<QuizSession> findByStudentAndQuiz(User student, Quiz quiz);

    List<QuizSession> findByStudentAndStatus(User student, QuizSession.SessionStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE QuizSession qs SET qs.status = 'EXPIRED' WHERE qs.quiz.id = :quizId AND qs.status = 'ACTIVE'")
    void expireAllActiveSessionsForQuiz(@Param("quizId") Long quizId);

    @Modifying
    @Transactional
    @Query("DELETE FROM QuizSession qs WHERE qs.quiz.id = :quizId")
    void deleteByQuizId(@Param("quizId") Long quizId);
}