package com.exemple.quiz_app.classe.repository;

import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.classe.entity.Classe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClasseRepository extends JpaRepository<Classe, Long> {

    List<Classe> findByEnseignantOrderByCreatedAtDesc(User enseignant);

    boolean existsByNameAndEnseignant(String name, User enseignant);
}