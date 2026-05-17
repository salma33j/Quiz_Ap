package com.exemple.quiz_app.auth.controller;

import com.exemple.quiz_app.auth.dto.ExcelImportResult;
import com.exemple.quiz_app.auth.service.ExcelImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/import")
public class ExcelImportController {

    @Autowired
    private ExcelImportService excelImportService;

    // =========================================================
    // ✅ POST /api/admin/import/etudiants
    // Upload fichier Excel → crée tous les comptes étudiants
    // + envoie email à chacun
    //
    // Format Excel attendu :
    // Colonne A : CNE
    // Colonne B : Prénom
    // Colonne C : Nom
    // Colonne D : Email
    // =========================================================
    @PostMapping("/etudiants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importerEtudiants(
            @RequestParam("file") MultipartFile file) {

        // Vérifier que c'est bien un fichier Excel
        if (!estFichierExcel(file)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("❌ Le fichier doit être au format Excel (.xlsx)");
        }

        ExcelImportResult result = excelImportService.importerEtudiants(file);
        return ResponseEntity.ok(result);
    }

    // =========================================================
    // ✅ POST /api/admin/import/enseignants
    // Upload fichier Excel → crée tous les comptes enseignants
    // + envoie email à chacun
    //
    // Format Excel attendu :
    // Colonne A : Matricule
    // Colonne B : Prénom
    // Colonne C : Nom
    // Colonne D : Email
    // =========================================================
    @PostMapping("/enseignants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importerEnseignants(
            @RequestParam("file") MultipartFile file) {

        if (!estFichierExcel(file)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("❌ Le fichier doit être au format Excel (.xlsx)");
        }

        ExcelImportResult result = excelImportService.importerEnseignants(file);
        return ResponseEntity.ok(result);
    }

    // =========================================================
    // MÉTHODE UTILITAIRE
    // =========================================================
    private boolean estFichierExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        String filename = file.getOriginalFilename();
        return filename != null && filename.endsWith(".xlsx");
    }
}