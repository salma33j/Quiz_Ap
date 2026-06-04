package com.exemple.quiz_app.matiere.controller;

import com.exemple.quiz_app.matiere.dto.MatiereResponse;
import com.exemple.quiz_app.matiere.service.MatiereService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/matieres")
@PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
public class StudentMatiereController {

    private final MatiereService matiereService;

    public StudentMatiereController(MatiereService matiereService) {
        this.matiereService = matiereService;
    }

    @GetMapping
    public ResponseEntity<List<MatiereResponse>> getStudentMatieres() {
        return ResponseEntity.ok(matiereService.getMatieresForCurrentStudent());
    }
}
