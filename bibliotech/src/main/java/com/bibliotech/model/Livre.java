package com.bibliotech.model;

import java.time.LocalDate;

public record Livre(long id, String titre, String auteur, int anneePublication,
                    int exemplairesTotal, int exemplairesDisponibles) {

    // Constructeur compact : il s'exécute avant que les champs soient remplis
    public Livre {
        if (titre == null || titre.isBlank()) {
            throw new IllegalArgumentException("Le titre est obligatoire");
        }
        if (auteur == null || auteur.isBlank()) {
            throw new IllegalArgumentException("L'auteur est obligatoire");
        }
        int anneeCourante = LocalDate.now().getYear();
        if (anneePublication < 1500 || anneePublication > anneeCourante) {
            throw new IllegalArgumentException(
                    "L'année doit être entre 1500 et " + anneeCourante);
        }
        if (exemplairesDisponibles > exemplairesTotal) {
            throw new IllegalArgumentException(
                    "Les exemplaires disponibles ne peuvent pas dépasser le total");
        }
    }
}