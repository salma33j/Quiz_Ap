package com.exemple.quiz_app.matiere.controller;

import com.exemple.quiz_app.matiere.dto.MatiereRequest;
import com.exemple.quiz_app.matiere.dto.MatiereResponse;
import com.exemple.quiz_app.matiere.service.MatiereService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/matieres")
@PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
public class MatiereController {

    private final MatiereService matiereService;

    public MatiereController(MatiereService matiereService) {
        this.matiereService = matiereService;
    }

    @GetMapping
    public ResponseEntity<List<MatiereResponse>> getMatieres() {
        return ResponseEntity.ok(matiereService.getMatieres());
    }

    @GetMapping("/classe/{classId}")
    public ResponseEntity<List<MatiereResponse>> getMatieresByClasse(@PathVariable Long classId) {
        return ResponseEntity.ok(matiereService.getMatieresByClasse(classId));
    }

    @PostMapping
    public ResponseEntity<MatiereResponse> createMatiere(@RequestBody MatiereRequest request) {
        return ResponseEntity.ok(matiereService.createMatiere(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MatiereResponse> updateMatiere(
            @PathVariable Long id,
            @RequestBody MatiereRequest request
    ) {
        return ResponseEntity.ok(matiereService.updateMatiere(id, request));
    }

    @PostMapping("/{id}/update")
    public ResponseEntity<MatiereResponse> updateMatiereWithPost(
            @PathVariable Long id,
            @RequestBody MatiereRequest request
    ) {
        return ResponseEntity.ok(matiereService.updateMatiere(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteMatiere(@PathVariable Long id) {
        matiereService.deleteMatiere(id);
        return ResponseEntity.ok(Map.of("message", "Matiere supprimee."));
    }
}
