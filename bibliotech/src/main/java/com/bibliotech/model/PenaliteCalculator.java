package com.bibliotech.model;

public class PenaliteCalculator {

    private static final double TARIF_PAR_JOUR = 0.50;
    private static final double PLAFOND = 30.0;

    public static double calculerPenalite(StatutEmprunt statut) {
        return switch (statut) {
            case StatutEmprunt.EnCours enCours -> 0.0;
            case StatutEmprunt.Rendu rendu -> 0.0;
            case StatutEmprunt.EnRetard enRetard ->
                Math.min(enRetard.joursDeRetard() * TARIF_PAR_JOUR, PLAFOND);
        };
    }
}