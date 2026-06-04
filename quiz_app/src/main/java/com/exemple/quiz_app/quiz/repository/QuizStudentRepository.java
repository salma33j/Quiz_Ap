package com.exemple.quiz_app.quiz.repository;

import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.quiz.entity.QuizStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizStudentRepository extends JpaRepository<QuizStudent, Long> {

    // ========== RECHERCHE ==========
    List<QuizStudent> findByQuiz(Quiz quiz);

    List<QuizStudent> findByStudent(User student);

    Optional<QuizStudent> findByQuizAndStudent(Quiz quiz, User student);

    boolean existsByQuizAndStudent(Quiz quiz, User student);

    long countByQuiz(Quiz quiz);

    @Query("SELECT qs.student FROM QuizStudent qs WHERE qs.quiz.id = :quizId")
    List<User> findStudentsByQuizId(@Param("quizId") Long quizId);

    // ========== SUPPRESSION ==========
    @Modifying
    @Transactional
    void deleteByQuiz(Quiz quiz);

    @Modifying
    @Transactional
    @Query("DELETE FROM QuizStudent qs WHERE qs.quiz.id = :quizId")
    void deleteByQuizId(@Param("quizId") Long quizId);

    @Modifying
    @Transactional
    void deleteByStudent(User student);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM quiz_students WHERE student_id = :studentId", nativeQuery = true)
    void deleteByStudentId(@Param("studentId") Long studentId);

    @Modifying
    @Transactional
    @Query("DELETE FROM QuizStudent qs WHERE qs.quiz.id = :quizId AND qs.student.id = :studentId")
    void deleteByQuizIdAndStudentId(@Param("quizId") Long quizId, @Param("studentId") Long studentId);
}
