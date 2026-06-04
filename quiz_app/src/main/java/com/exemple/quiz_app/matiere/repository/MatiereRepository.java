package com.exemple.quiz_app.matiere.repository;

import com.exemple.quiz_app.matiere.entity.Matiere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MatiereRepository extends JpaRepository<Matiere, Long> {

    List<Matiere> findAllByOrderByCreatedAtDesc();

    List<Matiere> findByClasseIdOrderByNomAsc(Long classId);

    List<Matiere> findByEnseignantIdOrderByCreatedAtDesc(Long teacherId);

    boolean existsByNomIgnoreCaseAndClasseId(String nom, Long classId);

    boolean existsByNomIgnoreCaseAndClasseIdAndIdNot(String nom, Long classId, Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM matieres WHERE classe_id = :classId", nativeQuery = true)
    void deleteByClasseId(@Param("classId") Long classId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM matieres WHERE enseignant_id = :teacherId", nativeQuery = true)
    void deleteByEnseignantId(@Param("teacherId") Long teacherId);
}
