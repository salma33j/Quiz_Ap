package com.exemple.quiz_app.quiz.service;

import com.exemple.quiz_app.quiz.repository.QuizRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class QuizExpirationScheduler {

    private final QuizRepository quizRepository;

    public QuizExpirationScheduler(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

    /**
     * Vérifie automatiquement les quiz expirés.
     * Sur Railway, sans scheduler, un quiz peut rester PUBLISHED dans la base
     * même si available_until est dépassée.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expirePublishedQuizzesPastDeadline() {
        quizRepository.expirePublishedQuizzesPastDeadline(LocalDateTime.now());
    }
}
