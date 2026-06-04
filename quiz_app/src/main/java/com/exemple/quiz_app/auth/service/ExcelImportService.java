package com.exemple.quiz_app.auth.service;

import com.exemple.quiz_app.auth.dto.ExcelImportResult;
import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Iterator;

@Service
public class ExcelImportService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    // =========================================================
    // ✅ IMPORT ÉTUDIANTS depuis Excel
    // Colonnes attendues : CNE | Prénom | Nom | Email
    // =========================================================
    public ExcelImportResult importerEtudiants(MultipartFile file) {
        ExcelImportResult result = new ExcelImportResult();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0); // Première feuille
            Iterator<Row> rows = sheet.iterator();

            int numeroLigne = 0;

            while (rows.hasNext()) {
                Row row = rows.next();
                numeroLigne++;

                // ✅ Ignorer la première ligne (en-têtes)
                if (numeroLigne == 1) continue;

                // ✅ Ignorer les lignes vides
                if (estLigneVide(row)) continue;

                result.setTotalLignes(result.getTotalLignes() + 1);

                try {
                    // Lire les colonnes
                    // Colonne 0 = CNE (on l'ignore, juste pour référence)
                    String cne       = getCellValue(row, 0);
                    String prenom    = getCellValue(row, 1);
                    String nom       = getCellValue(row, 2);
                    String email     = getCellValue(row, 3);

                    // ✅ Validation des champs obligatoires
                    if (prenom == null || prenom.isBlank()) {
                        result.ajouterErreur("Ligne " + numeroLigne + " : Prénom manquant");
                        continue;
                    }
                    if (nom == null || nom.isBlank()) {
                        result.ajouterErreur("Ligne " + numeroLigne + " : Nom manquant");
                        continue;
                    }
                    if (email == null || email.isBlank() || !email.contains("@")) {
                        result.ajouterErreur("Ligne " + numeroLigne + " : Email invalide (" + email + ")");
                        continue;
                    }

                    // ✅ Vérifier si l'email existe déjà
                    if (userRepository.existsByEmail(email)) {
                        result.ajouterErreur("Ligne " + numeroLigne + " : Email déjà utilisé (" + email + ")");
                        continue;
                    }

                    // ✅ Générer mot de passe provisoire
                    String motDePasse = genererMotDePasse("Etu");

                    // ✅ Créer le compte étudiant
                    User user = new User(prenom, nom, email,
                            passwordEncoder.encode(motDePasse), Role.ETUDIANT);
                    user.setMustChangePassword(true);
                    userRepository.save(user);

                    // ✅ Envoyer l'email avec les identifiants
                    emailService.sendEtudiantCredentials(email, prenom, nom, motDePasse);

                    result.ajouterSucces("✅ Ligne " + numeroLigne + " : " + prenom + " " + nom
                            + " (" + email + ") — compte créé");

                } catch (Exception e) {
                    result.ajouterErreur("Ligne " + numeroLigne + " : Erreur inattendue — " + e.getMessage());
                }
            }

        } catch (Exception e) {
            result.ajouterErreur("❌ Impossible de lire le fichier Excel : " + e.getMessage());
        }

        return result;
    }

    // =========================================================
    // ✅ IMPORT ENSEIGNANTS depuis Excel
    // Colonnes attendues : Matricule | Prénom | Nom | Email
    // =========================================================
    public ExcelImportResult importerEnseignants(MultipartFile file) {
        ExcelImportResult result = new ExcelImportResult();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            int numeroLigne = 0;

            while (rows.hasNext()) {
                Row row = rows.next();
                numeroLigne++;

                // Ignorer en-têtes et lignes vides
                if (numeroLigne == 1) continue;
                if (estLigneVide(row)) continue;

                result.setTotalLignes(result.getTotalLignes() + 1);

                try {
                    String matricule = getCellValue(row, 0); // ignoré
                    String prenom    = getCellValue(row, 1);
                    String nom       = getCellValue(row, 2);
                    String email     = getCellValue(row, 3);

                    // Validation
                    if (prenom == null || prenom.isBlank()) {
                        result.ajouterErreur("Ligne " + numeroLigne + " : Prénom manquant");
                        continue;
                    }
                    if (nom == null || nom.isBlank()) {
                        result.ajouterErreur("Ligne " + numeroLigne + " : Nom manquant");
                        continue;
                    }
                    if (email == null || email.isBlank() || !email.contains("@")) {
                        result.ajouterErreur("Ligne " + numeroLigne + " : Email invalide (" + email + ")");
                        continue;
                    }
                    if (userRepository.existsByEmail(email)) {
                        result.ajouterErreur("Ligne " + numeroLigne + " : Email déjà utilisé (" + email + ")");
                        continue;
                    }

                    // Générer mot de passe et créer le compte
                    String motDePasse = genererMotDePasse("Prof");

                    User user = new User(prenom, nom, email,
                            passwordEncoder.encode(motDePasse), Role.ENSEIGNANT);
                    user.setMustChangePassword(true);
                    userRepository.save(user);

                    // Envoyer email
                    emailService.sendEnseignantCredentials(email, prenom, nom, motDePasse);

                    result.ajouterSucces("✅ Ligne " + numeroLigne + " : " + prenom + " " + nom
                            + " (" + email + ") — compte créé");

                } catch (Exception e) {
                    result.ajouterErreur("Ligne " + numeroLigne + " : Erreur — " + e.getMessage());
                }
            }

        } catch (Exception e) {
            result.ajouterErreur("❌ Impossible de lire le fichier Excel : " + e.getMessage());
        }

        return result;
    }

    // =========================================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // =========================================================

    /**
     * Lire la valeur d'une cellule quelle que soit son type
     */
    private String getCellValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                // Si c'est un nombre (ex: CNE = 1234567), convertir en String sans décimales
                long valeur = (long) cell.getNumericCellValue();
                yield String.valueOf(valeur);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default      -> null;
        };
    }

    /**
     * Vérifie si une ligne est complètement vide
     */
    private boolean estLigneVide(Row row) {
        if (row == null) return true;
        for (int i = 0; i < 4; i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    /**
     * Génère un mot de passe provisoire sécurisé
     * Exemple : Etu@4521 ou Prof#8832
     */
    private String genererMotDePasse(String prefix) {
        int nombre = (int) (Math.random() * 9000 + 1000);
        String[] specials = {"@", "#", "!", "&"};
        String special = specials[(int) (Math.random() * specials.length)];
        return prefix + special + nombre;
    }
}