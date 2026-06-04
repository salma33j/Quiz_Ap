package com.exemple.quiz_app.classe.repository;

import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.classe.entity.Classe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ClasseRepository extends JpaRepository<Classe, Long> {

    List<Classe> findAllByOrderByCreatedAtDesc();

    List<Classe> findByEnseignantOrderByCreatedAtDesc(User enseignant);

    List<Classe> findByEnseignantsContaining(User enseignant);

    List<Classe> findDistinctByEnseignantOrEnseignantsContainingOrderByCreatedAtDesc(User enseignant, User assignedTeacher);

    boolean existsByNameAndEnseignant(String name, User enseignant);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM classe_enseignants WHERE enseignant_id = :enseignantId", nativeQuery = true)
    void deleteAssignmentsByEnseignantId(@Param("enseignantId") Long enseignantId);
}
