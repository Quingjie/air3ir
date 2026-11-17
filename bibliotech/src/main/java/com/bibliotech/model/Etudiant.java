package com.bibliotech.model;

public record Etudiant(long id, String numeroEtudiant, String nom, String prenom, String email) {

    public Etudiant {
        if (numeroEtudiant == null || numeroEtudiant.isBlank()) {
            throw new IllegalArgumentException("Le numéro d'étudiant est obligatoire");
        }
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom est obligatoire");
        }
        if (prenom == null || prenom.isBlank()) {
            throw new IllegalArgumentException("Le prénom est obligatoire");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }
        if (!numeroEtudiant.matches("[A-Z]{2}\\d{6}")) {
            throw new IllegalArgumentException(
                    "Le numéro d'étudiant doit ressembler à ET123456");
        }
        if (!email.contains("@")) {
            throw new IllegalArgumentException("L'email doit contenir un @");
        }
    }
}