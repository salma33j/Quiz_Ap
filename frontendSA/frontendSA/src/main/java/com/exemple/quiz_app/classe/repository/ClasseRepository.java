package com.exemple.quiz_app.classe.repository;

import com.exemple.quiz_app.classe.entity.Classe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClasseRepository extends JpaRepository<Classe, Long> {
    List<Classe> findByEnseignants_Id(Long enseignantId);
    boolean existsByNameIgnoreCase(String name);
}
