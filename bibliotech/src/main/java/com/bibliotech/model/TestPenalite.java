package com.bibliotech.model;

import java.time.LocalDate;

public class TestPenalite {
    public static void main(String[] args) {
        LocalDate d = LocalDate.of(2026, 11, 15);
        System.out.println(PenaliteCalculator.calculerPenalite(new StatutEmprunt.EnCours(d)));
        System.out.println(PenaliteCalculator.calculerPenalite(new StatutEmprunt.Rendu(d, true)));
        System.out.println(PenaliteCalculator.calculerPenalite(new StatutEmprunt.EnRetard(d, 2)));
        System.out.println(PenaliteCalculator.calculerPenalite(new StatutEmprunt.EnRetard(d, 100)));
    }
}