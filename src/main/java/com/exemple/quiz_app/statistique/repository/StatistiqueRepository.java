package com.exemple.quiz_app.statistique.repository;

import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.question.entity.Question;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.reponse.entity.Reponse;
import com.exemple.quiz_app.resultat.entity.Resultat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour les requêtes statistiques
 * Utilise les repositories existants
 */
@Repository
public interface StatistiqueRepository {

    // Les méthodes seront implémentées dans StatistiqueService
    // car ce repository n'a pas d'entité propre
}