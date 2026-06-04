package com.exemple.quiz_app.auth.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO qui représente le résultat d'un import Excel
 * Contient les succès et les erreurs ligne par ligne
 */
public class ExcelImportResult {

    private int totalLignes;
    private int totalCrees;
    private int totalEchecs;
    private List<String> succes = new ArrayList<>();
    private List<String> erreurs = new ArrayList<>();

    public ExcelImportResult() {}

    // ========== MÉTHODES UTILITAIRES ==========

    public void ajouterSucces(String message) {
        this.succes.add(message);
        this.totalCrees++;
    }

    public void ajouterErreur(String message) {
        this.erreurs.add(message);
        this.totalEchecs++;
    }

    // ========== GETTERS / SETTERS ==========

    public int getTotalLignes() { return totalLignes; }
    public void setTotalLignes(int totalLignes) { this.totalLignes = totalLignes; }

    public int getTotalCrees() { return totalCrees; }
    public void setTotalCrees(int totalCrees) { this.totalCrees = totalCrees; }

    public int getTotalEchecs() { return totalEchecs; }
    public void setTotalEchecs(int totalEchecs) { this.totalEchecs = totalEchecs; }

    public List<String> getSucces() { return succes; }
    public void setSucces(List<String> succes) { this.succes = succes; }

    public List<String> getErreurs() { return erreurs; }
    public void setErreurs(List<String> erreurs) { this.erreurs = erreurs; }
}